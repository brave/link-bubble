package com.chrislacy.linkbubble;

import android.app.ActivityManager;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MainActivity extends PreferenceActivity {

    static MainActivity sCurrentInstance;

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sCurrentInstance = this;

        Intent intent = getIntent();
        boolean isActionView = intent.getAction().equals(Intent.ACTION_VIEW);

        if (isActionView == false) {
            // must call before super.onCreate()
            setTheme(android.R.style.Theme_Holo_Light);
        }

        super.onCreate(savedInstanceState);
        Crashlytics.start(this);


        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);

        List<Intent> browsers = Settings.get().getBrowsers();

        if (isActionView) {
            boolean openLink = false;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean enabled = prefs.getBoolean("enabled", true);

            if (enabled) {
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

            if (openLink) {
                openLink(this, intent.getDataString(), true);
            } else {
                loadInBrowser(this, intent);
            }
            finish();
        } else {
            setContentView(R.layout.activity_main);
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
                if (sCurrentInstance == MainActivity.this) {

                    finish();
                }
            }
        }, 500);
    }

    public static void openLink(Context context, String url, boolean recordHistory) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("url", url);
        serviceIntent.putExtra("record_history", recordHistory);
        context.startService(serviceIntent);
    }

    public static void loadInBrowser(Context context, Intent intent) {
        Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.browser");
        if (browserIntent != null) {
            intent.setComponent(browserIntent.getComponent());
            intent.setPackage(browserIntent.getPackage());
            context.startActivity(intent);
        }
    }
}
