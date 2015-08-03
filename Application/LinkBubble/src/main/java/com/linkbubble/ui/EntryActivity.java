package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.CrashTracking;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    openLink = true;
                } else {
                    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(16, ActivityManager.RECENT_WITH_EXCLUDED);
                    if (recentTasks.size() > 0) {
                        ActivityManager.RecentTaskInfo recentTaskInfo = getPreviousTaskInfo(recentTasks);
                        if (recentTaskInfo != null) {
                            ComponentName componentName = recentTaskInfo.baseIntent.getComponent();
                            openedFromAppName = componentName.getPackageName();

                            if (url.equals(Constant.TERMS_OF_SERVICE_URL)
                                    || url.equals(Constant.PRIVACY_POLICY_URL)
                                    || !Settings.get().ignoreLinkFromPackageName(componentName.getPackageName())) {
                                openLink = true;
                            }
                        } else {
                            openLink = true;
                        }
                    }
                }
            }

            if (canLoadFromThisApp == false) {
                //if (Util.randInt(0, 30) == 20) {
                //    MainApplication.showUpgradePrompt(this, R.string.upgrade_incentive_one_app, Analytics.UPGRADE_PROMPT_SINGLE_APP);
                //}
                MainApplication.openInBrowser(this, intent, true);
            } else if (openLink) {
                MainApplication.checkRestoreCurrentTabs(this);

                boolean showedWelcomeUrl = false;
                if (Settings.get().getWelcomeMessageDisplayed() == false) {
                    if (!(MainController.get() != null && MainController.get().isUrlActive(Constant.WELCOME_MESSAGE_URL))) {
                        MainApplication.openLink(this, Constant.WELCOME_MESSAGE_URL, null);
                        showedWelcomeUrl = true;
                    }
                }

                MainApplication.openLink(this, url, true, showedWelcomeUrl ? false : true, openedFromAppName);
            } else {
                MainApplication.openInBrowser(this, intent, true);
            }
        } else {
            startActivityForResult(new Intent(this, HomeActivity.class), 0);
        }

        finish();
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
                if (packageName.equals("android") == false && packageName.equals(BuildConfig.APPLICATION_ID) == false) {
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
