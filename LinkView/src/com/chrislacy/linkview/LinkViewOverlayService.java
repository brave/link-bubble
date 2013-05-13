package com.chrislacy.linkview;

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
import com.jawsware.core.share.OverlayService;

import java.util.List;


public class LinkViewOverlayService extends OverlayService {

    public static LinkViewOverlayService mInstance;

    private LoadingOverlayView mLoadingView;
    private ContentOverlayView mContentView;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mLoadingView = new LoadingOverlayView(this);
        mContentView = new ContentOverlayView(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLoadingView != null) {
            mLoadingView.destory();
        }

        if (mContentView != null) {
            mContentView.destory();
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
        mContentView.setUri(data, new ContentOverlayView.UriLoadedListener() {
            @Override
            public void onPageFinished() {
                mContentView.animateOnscreen();
            }
        });
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

    void showContent() {
        mContentView.animateOnscreen();
    }

    void showLoading() {
        mLoadingView.setLoadingState(LoadingOverlayView.LoadingState.Loading);
    }

    interface AppPollingListener {
        void onAppChanged();
    }

    void beginAppPolling(AppPollingListener listener) {
        mAppPollingListener = listener;

        ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        final int taskLimit = 10;
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(taskLimit);
        if (tasks != null) {
            int tasksSize = tasks.size();
            for (int i = 0; i < tasksSize; i++) {
                ComponentName componentName = tasks.get(i).baseActivity;
                String packageName = componentName.getPackageName();
                if (packageName != null) {
                    if (packageName.equals("com.chrislacy.linkview")
                            && componentName.getClassName().equals("com.chrislacy.linkview.MainActivity")) {
                        //mCurrentAppPackgeName = packageName;
                        //Log.d("LinkView", "skip packageName=" + packageName + ", className=" + componentName.getClassName());
                    } else {
                        mCurrentAppPackgeName = packageName;
                        //Log.d("LinkView", "mCurrentAppPackgeName=" + packageName);
                        break;
                    }

                }
            }
        }

        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
    }

    void endAppPolling() {
        mHandler.removeMessages(ACTION_POLL_CURRENT_APP);
        mCurrentAppPackgeName = null;
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

                    ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                    final int taskLimit = 10;
                    List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(taskLimit);
                    if (tasks != null) {
                        int tasksSize = tasks.size();
                        for (int i = 0; i < tasksSize; i++) {
                            ComponentName componentName = activityManager.getRunningTasks(taskLimit).get(i).baseActivity;
                            String currentPackageName = componentName.getPackageName();
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
                    break;
                }
            }
        }
    };
}