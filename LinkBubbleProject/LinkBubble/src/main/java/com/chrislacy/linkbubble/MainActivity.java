package com.chrislacy.linkbubble;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.List;
import java.util.Vector;

public class MainActivity extends PreferenceActivity {

    //private boolean serviceBound = false;
    private Intent mIntent;
    private List<ResolveInfo> mBrowsers = new Vector<ResolveInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MainService.class));
        getBrowsers();

        Intent intent = getIntent();

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
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
                    /*for (ResolveInfo info : mBrowsers) {
                        String packageName = info.activityInfo.packageName;
                        if (cn.getPackageName().equals(packageName)) {
                            isBlacklisted = true;
                            break;
                        }
                    }*/

                    if (!isBlacklisted) {
                        openLink = true;
                    }
                }
            }

            if (openLink) {
                mIntent = intent;
                Intent serviceIntent = new Intent(this, MainService.class);
                bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
            } else {
                loadInBrowser(this, intent, false);
            }
            finish();
        } else {
            getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        }
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
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainService.ServiceBinder binder = (MainService.ServiceBinder) service;
            MainService mainService = binder.getService();
            //serviceBound = true;

            mainService.openIntent(mIntent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //serviceBound = false;
        }
    };

    private void getBrowsers() {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.google.com"));
        mBrowsers = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    }

    public static void loadInBrowser(Context context, Intent intent, boolean newActivity) {
        Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage("com.android.browser");
        if (browserIntent != null) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            if (newActivity) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            }
            intent.setComponent(browserIntent.getComponent());
            intent.setPackage(browserIntent.getPackage());
            context.startActivity(intent);
        }
    }
}
