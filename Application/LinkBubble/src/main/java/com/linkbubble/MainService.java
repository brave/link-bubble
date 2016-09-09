/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.webkit.WebIconDatabase;

import com.linkbubble.ui.NotificationCloseAllActivity;
import com.linkbubble.ui.NotificationHideActivity;
import com.linkbubble.ui.NotificationUnhideActivity;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.squareup.otto.Subscribe;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import java.util.Vector;

public class MainService extends Service {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";

    private boolean mRestoreComplete;

    public static class ShowDefaultNotificationEvent {
    }

    public static class ShowUnhideNotificationEvent {
    }

    public static class OnDestroyMainServiceEvent {}

    public static class ReloadMainServiceEvent {
        public ReloadMainServiceEvent(Context context) {
            mContext = context;
        }

        public Context mContext;
    }

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String cmd = intent != null ? intent.getStringExtra("cmd") : null;
        CrashTracking.log("MainService.onStartCommand(), cmd:" + cmd);

        MainController mainController = MainController.get();
        if (mainController == null || intent == null || cmd == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        long urlLoadStartTime = intent.getLongExtra("start_time", System.currentTimeMillis());
        if (cmd.compareTo("open") == 0) {
            String url = intent.getStringExtra("url");
            if (url != null) {
                String openedFromAppName = intent.getStringExtra("openedFromAppName");
                mainController.openUrl(url, urlLoadStartTime, true, openedFromAppName);
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
                                setAsCurrentTab = i == urls.length - 1;
                            }

                            mainController.openUrl(urlAsString, urlLoadStartTime, setAsCurrentTab, Analytics.OPENED_URL_FROM_RESTORE);
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

        setTheme(Settings.get().getDarkThemeEnabled() ? R.style.MainServiceThemeDark : R.style.MainServiceThemeLight);

        super.onCreate();
        Fabric.with(this, new Crashlytics());
        CrashTracking.log("MainService.onCreate()");

        showDefaultNotification();

        Config.init(this);
        Settings.get().onOrientationChange();

        try {
            WebIconDatabase.getInstance().open(getDir("icons", MODE_PRIVATE).getPath());
        }
        catch (RuntimeException exc) {
            CrashTracking.logHandledException(exc);
        }

        MainController.create(this, new MainController.EventHandler() {
                @Override
                public void onDestroy() {
                    Settings.get().saveBubbleRestingPoint();
                    stopSelf();
                    CrashTracking.log("MainService.onCreate(): onDestroy()");
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
        MainApplication.postEvent(MainService.this, new OnDestroyMainServiceEvent());
        MainApplication.unregisterForBus(this, this);
        unregisterReceiver(mScreenReceiver);
        unregisterReceiver(mDialogReceiver);
        unregisterReceiver(mBroadcastReceiver);
        MainController.destroy();
        CrashTracking.log("MainService.onDestroy()");
        super.onDestroy();
    }

    private void cancelCurrentNotification() {
        stopForeground(true);
        //Log.d("blerg", "cancelCurrentNotification()");
    }

    private void showDefaultNotification() {
        Intent closeAllIntent = new Intent(this, NotificationCloseAllActivity.class);
        closeAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent closeAllPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), closeAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent hideIntent = new Intent(this, NotificationHideActivity.class);
        hideIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent hidePendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), hideIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_default_summary))
                //.addAction(R.drawable.ic_action_eye_closed_dark, getString(R.string.notification_action_hide), hidePendingIntent)
                //.addAction(R.drawable.ic_action_cancel_dark, getString(R.string.notification_action_close_all), closeAllPendingIntent)
                .addAction(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? R.drawable.ic_action_cancel_white : R.drawable.ic_action_cancel_dark, getString(R.string.notification_action_close_all), closeAllPendingIntent)
                .setGroup(Constant.NOTIFICATION_GROUP_KEY_ARTICLES)
                .setGroupSummary(true)
                .setLocalOnly(true)
                .setContentIntent(hidePendingIntent);

        // Nuke all previous notifications and generate unique ids
        NotificationManagerCompat.from(this).cancelAll();
        int notificationId = 77;

        startForeground(notificationId, notificationBuilder.build());
    }

    private void showUnhideHiddenNotification() {
        Intent unhideIntent = new Intent(this, NotificationUnhideActivity.class);
        unhideIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent unhidePendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), unhideIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent closeAllIntent = new Intent(this, NotificationCloseAllActivity.class);
        closeAllIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent closeAllPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), closeAllIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_unhide_summary))
                .setLocalOnly(true)
                //.addAction(R.drawable.ic_action_eye_open_dark, getString(R.string.notification_action_unhide), unhidePendingIntent)
                //.addAction(R.drawable.ic_action_cancel_dark, getString(R.string.notification_action_close_all), closeAllPendingIntent)
                .addAction(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? R.drawable.ic_action_cancel_white : R.drawable.ic_action_cancel_dark, getString(R.string.notification_action_close_all), closeAllPendingIntent)
                .setContentIntent(unhidePendingIntent);

        NotificationManagerCompat.from(this).cancelAll();
        startForeground(1, notificationBuilder.build());
        //Log.d("blerg", "showUnhideHiddenNotification()");
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onReloadMainServiceEvent(ReloadMainServiceEvent event) {
        stopSelf();

        final Vector<String> urls = Settings.get().loadCurrentTabs();
        MainApplication.restoreLinks(event.mContext, urls.toArray(new String[urls.size()]));
    }


    private BroadcastReceiver mDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            if (myIntent.getAction().equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                MainController.get().onCloseSystemDialogs();
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {
            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) ) {
                MainController.get().onOrientationChanged();
            }
        }
    };

    private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MainController.get().updateScreenState(intent.getAction());
        }
    };
}
