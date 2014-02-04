package com.linkbubble;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.webkit.WebIconDatabase;
import com.crashlytics.android.Crashlytics;

import java.util.Vector;

/**
 * Created by gw on 28/08/13.
 */
public class MainService extends Service {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    private boolean mRestoreComplete;

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String cmd = intent.getStringExtra("cmd");
        long startTime = intent.getLongExtra("start_time", System.currentTimeMillis());

        if (cmd.compareTo("open") == 0) {
            String url = intent.getStringExtra("url");
            MainController.get().onOpenUrl(url, startTime, true);
        } else if (cmd.compareTo("restore") == 0) {
            if (!mRestoreComplete) {
                String [] urls = intent.getStringArrayExtra("urls");
                for (int i = 0; i < urls.length; i++) {
                    MainController.get().onOpenUrl(urls[i], startTime, i == urls.length-1);
                }
                mRestoreComplete = true;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {

        mRestoreComplete = false;

        setTheme(android.R.style.Theme_Holo_Light);

        super.onCreate();

        Crashlytics.start(this);

        Notification.Builder mBuilder = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("LinkBubble")
                        .setPriority(Notification.PRIORITY_MIN)
                        .setContentText("");
        startForeground(1, mBuilder.build());

        Config.init(this);
        Settings.get().onOrientationChange();

        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());

        MainController.create(this, new MainController.EventHandler() {
                @Override
                public void onDestroy() {
                    Settings.get().saveBubbleRestingPoint();
                    stopSelf();
                }
            });

        //Intent i = new Intent();
        //i.setData(Uri.parse("https://t.co/uxMl3bWtMP"));
        //i.setData(Uri.parse("http://t.co/oOyu7GBZMU"));
        //i.setData(Uri.parse("http://goo.gl/abc57"));
        //i.setData(Uri.parse("https://bitly.com/QtQET"));
        //i.setData(Uri.parse("http://www.duckduckgo.com"));
        //openUrl("https://www.duckduckgo.com");
        //openUrl("http://www.duckduckgo.com", true);
        //openUrl("https://t.co/uxMl3bWtMP", true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        registerReceiver(mDialogReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(mPackageBroadcastReceiver, filter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPackageBroadcastReceiver);
        unregisterReceiver(mDialogReceiver);
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mPackageBroadcastReceiver);
        MainController.destroy();
    }

    private static BroadcastReceiver mDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            if (myIntent.getAction().equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                MainController.get().onCloseSystemDialogs();
            }
        }
    };

    private static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) ) {
                MainController.get().onOrientationChanged();
            }
        }
    };

    private static BroadcastReceiver mPackageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                Settings.get().updateBrowsers();
                // Add checks such that in the event getDefaultBrowserPackageName() no longer exists, we use a default.
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                Settings.get().updateBrowsers();
            }
        }
    };
}
