package com.chrislacy.linkbubble;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class AppPoller {

    private static final String TAG = "AppPoller";

    interface AppPollerListener {
        void onAppChanged();
    }

    private static final int ACTION_POLL_CURRENT_APP = 1;
    private static final int LOOP_TIME = 50;

    private Context mContext;
    private AppPollerListener mAppPollingListener;
    private String mCurrentAppPackgeName;
    private boolean mPolling = false;

    AppPoller(Context context) {
        mContext = context;
    }

    void setListener(AppPollerListener listener) {
        mAppPollingListener = listener;
    }

    void beginAppPolling() {
        ActivityManager am = (ActivityManager)mContext.getSystemService(Activity.ACTIVITY_SERVICE);
        ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
        mCurrentAppPackgeName = componentName.getPackageName();
        Log.d(TAG, "beginAppPolling() - current app:" + mCurrentAppPackgeName);

        if (mPolling == false) {
            mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
        }
        mPolling = true;
    }

    void endAppPolling() {
        mHandler.removeMessages(ACTION_POLL_CURRENT_APP);
        mPolling = false;
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_POLL_CURRENT_APP: {
                    mHandler.removeMessages(ACTION_POLL_CURRENT_APP);

                    ActivityManager am = (ActivityManager)mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                    ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
                    String currentPackageName = componentName.getPackageName();
                    if (currentPackageName != null
                            && mCurrentAppPackgeName != null
                            && !currentPackageName.equals(mCurrentAppPackgeName)) {
                        Log.d(TAG, "current app changed from " + mCurrentAppPackgeName + " to " + currentPackageName);
                        if (mAppPollingListener != null) {
                            mAppPollingListener.onAppChanged();
                        }
                        mCurrentAppPackgeName = currentPackageName;
                    } else {
                        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                    }
                    break;
                }
            }
        }
    };

}
