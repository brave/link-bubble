package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

import java.util.List;

public class EntryActivity extends Activity {

    static EntryActivity sCurrentInstance;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sCurrentInstance = this;

        Intent intent = getIntent();
        boolean isActionView = intent.getAction().equals(Intent.ACTION_VIEW);

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);

        boolean showingTamperPrompt = Util.showTamperPrompt(this, new Prompt.OnPromptEventListener() {
            @Override
            public void onClick() {
                Config.openAppStore(EntryActivity.this);
            }
            @Override
            public void onClose() {
                finish();
            }
        });

        List<Intent> browsers = Settings.get().getBrowsers();

        if (isActionView) {
            boolean openLink = false;

            String url = intent.getDataString();
            // Special case code for the setting the default browser. If this URL is received, do nothing.
            if (url.equals(Config.SET_DEFAULT_BROSWER_URL)) {
                Toast.makeText(this, R.string.default_browser_set, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (Settings.get().isEnabled()) {
                final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(16, ActivityManager.RECENT_WITH_EXCLUDED);

                if (recentTasks.size() > 0) {
                    ActivityManager.RecentTaskInfo rt = recentTasks.get(0);
                    Intent baseIntent = rt.baseIntent;
                    ComponentName cn = baseIntent.getComponent();

                    boolean isBlacklisted = false;
                    for (Intent browser : browsers) {
                        if (cn.getPackageName().equals(browser.getPackage())) {
                            isBlacklisted = true;
                            break;
                        }
                    }

                    if (!isBlacklisted) {
                        openLink = true;
                    }
                }
            }

            if (openLink && !showingTamperPrompt) {
                MainApplication.openLink(this, url);
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
