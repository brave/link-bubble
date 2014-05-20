package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

public class EntryActivity extends Activity {

    static EntryActivity sCurrentInstance;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sCurrentInstance = this;

        Intent intent = getIntent();
        boolean isActionView = false;
        boolean isActionSend = false;
        if (intent != null && intent.getAction() != null) {
            isActionView = intent.getAction().equals(Intent.ACTION_VIEW);
            isActionSend = intent.getAction().equals(Intent.ACTION_SEND);
        }

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);

        boolean showingTamperPrompt = Util.checkForTamper(this, new Prompt.OnPromptEventListener() {
            @Override
            public void onClick() {
                MainApplication.openAppStore(EntryActivity.this, BuildConfig.STORE_FREE_URL);
            }

            @Override
            public void onClose() {
                finish();
            }
        });

        List<Intent> browsers = Settings.get().getBrowsers();

        if (isActionView || isActionSend) {
            boolean openLink = false;

            String url = intent.getDataString();

            if (isActionSend) {
                String type = intent.getType();
                Bundle extras = intent.getExtras();
                if (type != null && type.equals("text/plain") && extras.containsKey(Intent.EXTRA_TEXT)) {
                    String text = extras.getString(Intent.EXTRA_TEXT);
                    String[] splitText = text.split(" ");
                    for (String s : splitText) {
                        try {
                            URL _url = new URL(s);
                            url = _url.toString();
                            openLink = true;
                            break;
                        } catch (MalformedURLException ex) {
                        }
                    }

                    if (openLink == false) {
                        Toast.makeText(this, R.string.invalid_send_action, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
            }

            // Special case code for the setting the default browser. If this URL is received, do nothing.
            if (url.equals(Config.SET_DEFAULT_BROWSER_URL)) {
                Toast.makeText(this, R.string.default_browser_set, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String openedFromAppName = null;
            boolean canLoadFromThisApp = true;
            if (Settings.get().isEnabled()) {
                final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(16, ActivityManager.RECENT_WITH_EXCLUDED);

                if (recentTasks.size() > 0) {
                    ActivityManager.RecentTaskInfo recentTaskInfo = getPreviousTaskInfo(recentTasks);
                    ComponentName componentName = recentTaskInfo.baseIntent.getComponent();
                    openedFromAppName = componentName.getPackageName();

                    boolean isBlacklisted = false;
                    for (Intent browser : browsers) {
                        if (componentName.getPackageName().equals(browser.getPackage())) {
                            isBlacklisted = true;
                            break;
                        }
                    }

                    if (url.equals(Constant.TERMS_OF_SERVICE_URL) || url.equals(Constant.PRIVACY_POLICY_URL)) {
                        canLoadFromThisApp = true;
                    } else if (DRM.allowProFeatures() == false) {
                        String interceptFromPackageName = Settings.get().getInterceptLinksFromPackageName();
                        if (interceptFromPackageName == null) {
                            canLoadFromThisApp = true;

                            PackageManager packageManager = getPackageManager();
                            if (packageManager != null && recentTaskInfo.baseIntent != null) {
                                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(recentTaskInfo.baseIntent, 0);
                                if (resolveInfos != null && resolveInfos.size() > 0) {
                                    CharSequence label = resolveInfos.get(0).loadLabel(packageManager);
                                    if (label != null) {
                                        Settings.get().setInterceptLinksFrom(componentName.getPackageName(), label.toString());
                                        MainApplication.showUpgradePrompt(this,
                                                String.format(getString(R.string.intercept_links_from_default_set_message), label),
                                                Analytics.UPGRADE_PROMPT_SINGLE_APP_SET);
                                    }
                                }
                            }
                        } else {
                            if (interceptFromPackageName.equals(componentName.getPackageName())) {
                                canLoadFromThisApp = true;
                            } else {
                                canLoadFromThisApp = false;
                            }
                        }
                    }

                    if (!isBlacklisted && canLoadFromThisApp) {
                        openLink = true;
                    }
                }
            }

            if (canLoadFromThisApp == false && !showingTamperPrompt) {
                if (Util.randInt(0, 30) == 20) {
                    MainApplication.showUpgradePrompt(this, R.string.upgrade_incentive_one_app, Analytics.UPGRADE_PROMPT_SINGLE_APP);
                }
                MainApplication.openInBrowser(this, intent, true);
            } else if (openLink && !showingTamperPrompt) {
                // Don't restore tabs if we've already got tabs open, #389
                if (MainController.get() == null) {
                    // Restore open tabs
                    Vector<String> urls = Settings.get().loadCurrentTabs();
                    if (urls.size() > 0 && DRM.allowProFeatures()) {
                        MainApplication.restoreLinks(this, urls.toArray(new String[urls.size()]));
                    }
                }

                boolean showedWelcomeUrl = false;
                if (Settings.get().getWelcomeMessageDisplayed() == false) {
                    if (!(MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL))) {
                        MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL, null);
                        showedWelcomeUrl = true;
                    }
                }

                MainApplication.openLink(this, url, showedWelcomeUrl ? false : true, openedFromAppName);
            } else {
                MainApplication.openInBrowser(this, intent, true);
            }
        } else {
            if (!showingTamperPrompt) {
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
            }
        }

        if (!showingTamperPrompt) {
            finish();
        }
    }

    /*
     * Get the most recent RecentTaskInfo, but ensure the result is not Link Bubble.
     */
    ActivityManager.RecentTaskInfo getPreviousTaskInfo(List<ActivityManager.RecentTaskInfo> recentTasks) {
        for (int i = 0; i < recentTasks.size(); i++) {
            ActivityManager.RecentTaskInfo recentTaskInfo = recentTasks.get(i);
            if (recentTaskInfo.baseIntent != null
                    && recentTaskInfo.baseIntent.getComponent() != null) {
                String packageName = recentTaskInfo.baseIntent.getComponent().getPackageName();
                if (packageName.equals("android") == false && packageName.equals(BuildConfig.PACKAGE_NAME) == false) {
                    return recentTaskInfo;
                }
            }
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        if (sCurrentInstance == this) {
            sCurrentInstance = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        sCurrentInstance = this;
    }

    @Override
    protected void onStop() {
        super.onStop();

        delayedFinishIfCurrent();
    }

    @Override
    public void onBackPressed() {
        delayedFinishIfCurrent();
    }

    void delayedFinishIfCurrent() {
        // Kill the activity to ensure it is not alive in the event a link is intercepted,
        // thus displaying the ugly UI for a few frames

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sCurrentInstance == EntryActivity.this) {
                    finish();
                }
            }
        }, 500);
    }
}
