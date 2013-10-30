package com.chrislacy.linkbubble;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.webkit.WebIconDatabase;

/**
 * Created by gw on 28/08/13.
 */
public class MainService extends Service {

    private final IBinder serviceBinder = new ServiceBinder();
    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    private static MainController mController;

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    public class ServiceBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public void onCreate() {
        Config.init(this);

        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());

        mController = new MainController(this);

        //Intent i = new Intent();
        //i.setData(Uri.parse("https://t.co/uxMl3bWtMP"));
        //i.setData(Uri.parse("http://t.co/oOyu7GBZMU"));
        //i.setData(Uri.parse("http://goo.gl/abc57"));
        //i.setData(Uri.parse("https://bitly.com/QtQET"));
        //i.setData(Uri.parse("http://www.duckduckgo.com"));
        //openUrl("https://www.duckduckgo.com");
        openUrl("http://www.duckduckgo.com", true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        mController = null;
    }

    public static void openUrl(String url, boolean recordHistory) {
        if (mController != null) {
            mController.onOpenUrl(url, recordHistory);
        }
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) ) {
                mController.onOrientationChanged();
            }
        }
    };
}
