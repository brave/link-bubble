package com.linkbubble.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.linkbubble.MainController;


public class AppPoller {

    private static final String TAG = "AppPoller";

    public interface AppPollerListener {
        void onAppChanged();
    }

    private static final int ACTION_POLL_CURRENT_APP = 1;
    private static final int LOOP_TIME = 50;

    private Context mContext;
    private AppPollerListener mAppPollingListener;
    private String mCurrentAppFlatComponentName;
    private boolean mPolling = false;

    public AppPoller(Context context) {
        mContext = context;
    }

    public void setListener(AppPollerListener listener) {
        mAppPollingListener = listener;
    }

    public void beginAppPolling() {
        if (mCurrentAppFlatComponentName == null) {
            ActivityManager am = (ActivityManager)mContext.getSystemService(Activity.ACTIVITY_SERVICE);
            ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
            mCurrentAppFlatComponentName = componentName.flattenToShortString();
            Log.d(TAG, "beginAppPolling() - current app:" + mCurrentAppFlatComponentName);
        }

        if (mPolling == false) {
            mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
        }
        mPolling = true;
    }

    public void endAppPolling() {
        mHandler.removeMessages(ACTION_POLL_CURRENT_APP);
        mPolling = false;
        mCurrentAppFlatComponentName = null;
        Log.d(TAG, "endAppPolling()");
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_POLL_CURRENT_APP: {
                    mHandler.removeMessages(ACTION_POLL_CURRENT_APP);

                    if (MainController.get() == null) {
                        Log.d(TAG, "No main controller, exit");
                        break;
                    }

                    ActivityManager am = (ActivityManager)mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                    //Log.d(TAG, "Checking current tasks...");
                    ComponentName componentName = am.getRunningTasks(1).get(0).topActivity;
                    String currentAppFlatComponentName = componentName.flattenToShortString();
                    if (currentAppFlatComponentName != null
                            && mCurrentAppFlatComponentName != null
                            && !currentAppFlatComponentName.equals(mCurrentAppFlatComponentName)) {
                        Log.d(TAG, "current app changed from " + mCurrentAppFlatComponentName + " to " + currentAppFlatComponentName);
                        mCurrentAppFlatComponentName = currentAppFlatComponentName;
                        if (mAppPollingListener != null) {
                            mAppPollingListener.onAppChanged();
                        }
                    } else {
                        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                    }
                    break;
                }
            }
        }
    };

}
