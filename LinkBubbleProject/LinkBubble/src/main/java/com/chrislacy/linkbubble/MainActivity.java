package com.chrislacy.linkbubble;

import android.app.ActivityManager;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MainActivity extends PreferenceActivity {

    static MainActivity sCurrentInstance;

    //private boolean serviceBound = false;
    private String mUrl;
    private List<String> mBrowsers = new Vector<String>();

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

        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
        startService(new Intent(this, MainService.class));
        getBrowsers();



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
                    for (String packageName : mBrowsers) {
                        if (cn.getPackageName().equals(packageName)) {
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
                mUrl = intent.getDataString();
                Intent serviceIntent = new Intent(this, MainService.class);
                bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
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

        try {
            unbindService(mConnection);
        } catch (Exception e) {

        }
        //if (serviceBound) {
        //    unbindService(mConnection);
        //    serviceBound = false;
        //}

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

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.ServiceBinder binder = (MainService.ServiceBinder) service;
            MainService mainService = binder.getService();
            //serviceBound = true;

            mainService.openUrl(mUrl, true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //serviceBound = false;
        }
    };

    private void getBrowsers() {
        //PackageManager packageManager = getPackageManager();
        //Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setData(Uri.parse("http://www.google.com"));
        //mBrowsers = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        mBrowsers.add("com.android.browser");
    }

    public static void loadInBrowser(Context context, Intent intent) {
        Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.browser");
        if (browserIntent != null) {
            intent.setComponent(browserIntent.getComponent());
            intent.setPackage(browserIntent.getPackage());
            context.startActivity(intent);
        }
    }

    public static void loadInBrowser(Context context, String url, boolean newActivity) {
        Uri uri = Uri.parse(url);
        Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.browser");
        if (browserIntent != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            if (newActivity) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            }
            intent.setComponent(browserIntent.getComponent());
            intent.setPackage(browserIntent.getPackage());
            intent.setData(uri);
            context.startActivity(intent);
        }
    }
}
