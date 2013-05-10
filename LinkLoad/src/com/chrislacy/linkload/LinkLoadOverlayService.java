package com.chrislacy.linkload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
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

}