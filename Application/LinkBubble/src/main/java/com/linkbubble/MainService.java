package com.linkbubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.webkit.WebIconDatabase;

import com.linkbubble.ui.NotificationControlActivity;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.FlushCacheService;
import com.squareup.otto.Subscribe;

/**
 * Created by gw on 28/08/13.
 */
public class MainService extends Service {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    private boolean mRestoreComplete;

    public static class ShowDefaultNotificationEvent {
    }

    public static class ShowUnhideNotificationEvent {
    }

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MainController mainController = MainController.get();
        if (mainController == null || intent == null || intent.getStringExtra("cmd") == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String cmd = intent.getStringExtra("cmd");
        long urlLoadStartTime = intent.getLongExtra("start_time", System.currentTimeMillis());
        if (cmd.compareTo("open") == 0) {
            String url = intent.getStringExtra("url");
            if (url != null) {
                boolean doLicenseCheck = intent.getBooleanExtra("doLicenseCheck", true);
                String openedFromAppName = intent.getStringExtra("openedFromAppName");
                mainController.openUrl(url, urlLoadStartTime, true, openedFromAppName, doLicenseCheck);
            }
        } else if (cmd.compareTo("restore") == 0) {
            if (!mRestoreComplete) {
                String [] urls = intent.getStringArrayExtra("urls");
                if (urls != null) {
                    int startOpenTabCount = mainController.getActiveTabCount();

                    for (int i = 0; i < urls.length; i++) {
                        String urlAsString = urls[i];
                        if (urlAsString != null && !urlAsString.equals(Constant.WELCOME_MESSAGE_URL)) {
                            boolean setAsCurrentTab = false;
                            if (startOpenTabCount == 0) {
                                if (DRM.allowProFeatures()) {
                                    setAsCurrentTab = i == urls.length - 1;
                                } else {
                                    // If not using Pro features, only 1 tab is allowed, so ensure the first is set as current
                                    setAsCurrentTab = i == 0;
                                }
                            }

                            mainController.openUrl(urlAsString, urlLoadStartTime, setAsCurrentTab, Analytics.OPENED_URL_FROM_RESTORE, true);
                        }
                    }
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
        CrashTracking.init(this);

        showDefaultNotification();

        Config.init(this);
        Settings.get().onOrientationChange();

        WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());

        MainController.create(this, new MainController.EventHandler() {
                @Override
                public void onDestroy() {
                    Settings.get().saveBubbleRestingPoint();
                    FlushCacheService.doCheck(MainService.this);
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

        filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter);

        MainApplication.registerForBus(this, this);
    }

    @Override
    public void onDestroy() {
        MainApplication.unregisterForBus(this, this);
        unregisterReceiver(mScreenReceiver);
        unregisterReceiver(mDialogReceiver);
        unregisterReceiver(mBroadcastReceiver);
        MainController.destroy();
    }

    private void cancelCurrentNotification() {
        stopForeground(true);
        //Log.d("blerg", "cancelCurrentNotification()");
    }

    private void showDefaultNotification() {
        Intent closeAllIntent = new Intent(this, NotificationControlActivity.class);
        closeAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        closeAllIntent.putExtra(NotificationControlActivity.EXTRA_ACTION, NotificationControlActivity.ACTION_CLOSE_ALL);
        PendingIntent closeAllPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), closeAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent hideIntent = new Intent(this, NotificationControlActivity.class);
        hideIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        hideIntent.putExtra(NotificationControlActivity.EXTRA_ACTION, NotificationControlActivity.ACTION_HIDE);
        PendingIntent hidePendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), hideIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_default_summary))
                .addAction(R.drawable.ic_action_halt_dark, getString(R.string.notification_action_hide), hidePendingIntent)
                .setGroup(Constant.NOTIFICATION_GROUP_KEY_ARTICLES)
                .setGroupSummary(true)
                .setLocalOnly(true)
                .setContentIntent(closeAllPendingIntent);

        // Nuke all previous notifications and generate unique ids
        NotificationManagerCompat.from(this).cancelAll();
        int notificationId = 77;

        startForeground(notificationId, notificationBuilder.build());
    }

    private void showUnhideHiddenNotification() {
        Intent unhideIntent = new Intent(this, NotificationControlActivity.class);
        unhideIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        unhideIntent.putExtra(NotificationControlActivity.EXTRA_ACTION, NotificationControlActivity.ACTION_UNHIDE);
        PendingIntent unhidePendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), unhideIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent closeAllIntent = new Intent(this, NotificationControlActivity.class);
        closeAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        closeAllIntent.putExtra(NotificationControlActivity.EXTRA_ACTION, NotificationControlActivity.ACTION_CLOSE_ALL);
        PendingIntent closeAllPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), closeAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_unhide_summary))
                .addAction(R.drawable.ic_action_cancel_dark, getString(R.string.notification_action_close_all), closeAllPendingIntent)
                .setContentIntent(unhidePendingIntent);

        NotificationManagerCompat.from(this).cancelAll();
        startForeground(1, notificationBuilder.build());
        Log.d("blerg", "showUnhideHiddenNotification()");
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onShowDefaultNotificationEvent(ShowDefaultNotificationEvent event) {
        cancelCurrentNotification();
        showDefaultNotification();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onShowUnhideNotificationEvent(ShowUnhideNotificationEvent event) {
        cancelCurrentNotification();
        showUnhideHiddenNotification();
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

    private static BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MainController.get().updateScreenState(intent.getAction());
        }
    };
}
