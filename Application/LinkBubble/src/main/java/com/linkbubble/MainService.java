package com.linkbubble;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.webkit.WebIconDatabase;
import com.crashlytics.android.Crashlytics;

/**
 * Created by gw on 28/08/13.
 */
public class MainService extends Service {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public int onStartCommand(Intent cmd, int flags, int startId) {
        String url = cmd.getStringExtra("url");
        long startTime = cmd.getLongExtra("start_time", System.currentTimeMillis());
        MainController.get().onOpenUrl(url, startTime);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

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

        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());

        MainController.create(this, new MainController.EventHandler() {
            @Override
            public void onDestroy() {
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
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mDialogReceiver);
        unregisterReceiver(mBroadcastReceiver);
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
}
