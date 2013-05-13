package com.chrislacy.linkload.old;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.chrislacy.linkload.HideActivity;
import com.chrislacy.linkload.R;
import com.jawsware.core.share.OverlayService;


public class LinkLoadOverlayService extends OverlayService {

    public static LinkLoadOverlayService mInstance;

    private LinkLoadOverlayView mOverlayView;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mOverlayView = new LinkLoadOverlayView(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOverlayView != null) {
            mOverlayView.destory();
        }

    }

    static public void stop() {
        if (mInstance != null) {
            mInstance.stopSelf();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    void handleCommand(Intent intent) {
        Uri data = intent.getData();
        mOverlayView.setUri(data);
    }

    @Override
    protected Notification foregroundNotification(int notificationId) {
        Notification notification;

        notification = new Notification(R.drawable.ic_action_link, getString(R.string.title_notification), System.currentTimeMillis());

        notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;

        notification.setLatestEventInfo(this, getString(R.string.title_notification), getString(R.string.message_notification), notificationIntent());

        return notification;
    }


    private PendingIntent notificationIntent() {
        Intent intent = new Intent(this, HideActivity.class);

        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pending;
    }

    void cancelNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(id);
    }

    interface AppPollingListener {
        void onAppChanged();
    }

    void beginAppPolling(AppPollingListener listener) {
        mAppPollingListener = listener;

        ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
        mCurrentAppPackgeName = componentName.getPackageName();

        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
    }

    void endAppPolling() {
        mHandler.removeMessages(ACTION_POLL_CURRENT_APP);
    }

    private static final int ACTION_POLL_CURRENT_APP = 1;
    private static final int LOOP_TIME = 50;

    private AppPollingListener mAppPollingListener;
    private String mCurrentAppPackgeName;
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_POLL_CURRENT_APP: {
                    mHandler.removeMessages(ACTION_POLL_CURRENT_APP);

                    ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                    ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
                    String currentPackageName = componentName.getPackageName();
                    //Log.d("LinkLoad", "currentAppPackgeName=" + currentPackageName);
                    if (currentPackageName != null
                            && mCurrentAppPackgeName != null
                            && !currentPackageName.equals(mCurrentAppPackgeName)) {
                        if (mAppPollingListener != null) {
                            mAppPollingListener.onAppChanged();
                        }
                        mCurrentAppPackgeName = null;
                    } else {
                        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                    }
                    break;
                }
            }
        }
    };
}