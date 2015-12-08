package com.linkbubble.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.linkbubble.BuildConfig;
import com.linkbubble.MainController;
import com.linkbubble.ui.EntryActivity;
import com.linkbubble.ui.ExpandedActivity;
import com.linkbubble.ui.NotificationCloseAllActivity;
import com.linkbubble.ui.NotificationCloseTabActivity;
import com.linkbubble.ui.NotificationHideActivity;
import com.linkbubble.ui.NotificationOpenTabActivity;
import com.linkbubble.ui.NotificationUnhideActivity;

import java.util.List;


public class AppPoller {

    private static final String TAG = "AppPoller";

    public interface AppPollerListener {
        void onAppChanged();
    }

    private static int VERIFY_TIME = 150;

    private static final int ACTION_POLL_CURRENT_APP = 1;
    private static final int LOOP_TIME = 50;

    private Context mContext;
    private AppPollerListener mAppPollingListener;
    private String mCurrentAppFlatComponentName;
    private String mNextAppFlatComponentName;
    private long mNextAppFirstRunningTime = -1;
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
            List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
            if (runningTasks != null && runningTasks.size() > 0) {
                ComponentName componentName = runningTasks.get(0).topActivity;
                mCurrentAppFlatComponentName = componentName.flattenToShortString();
                Log.d(TAG, "beginAppPolling() - current app:" + mCurrentAppFlatComponentName);
            }
        }

        mNextAppFirstRunningTime = -1;
        mNextAppFlatComponentName = null;

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

    // ES FileExplorer seems to employ a nasty hack whereby they start a new Activity when an app is installed/updated.
    // Add this equally nasty hack to ignore this one activity. Stops the Bubbles going into BubbleView mode without any input (see #179)
    private static final String[] IGNORE_ACTIVITIES = {"com.estrongs.android.pop/.app.InstallMonitorActivity",
            "com.ideashower.readitlater.pro/com.ideashower.readitlater.activity.AddActivity",
            BuildConfig.APPLICATION_ID + "/" + ExpandedActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + NotificationCloseAllActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + NotificationCloseTabActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + NotificationHideActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + NotificationUnhideActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + NotificationOpenTabActivity.class.getName(),
            BuildConfig.APPLICATION_ID + "/" + EntryActivity.class.getName()};
    private boolean shouldIgnoreActivity(String flatComponentName) {
        for (String string : IGNORE_ACTIVITIES) {
            if (string.equals(flatComponentName)) {
                if (flatComponentName.contains(ExpandedActivity.class.getName()) == false) {
                    Log.d(TAG, "ignore " + flatComponentName);
                }
                return true;
            }
        }

        return false;
    }

    private static final int sCountToCallGc = 2000;
    private int mCurrentLoopCount = 0;

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_POLL_CURRENT_APP: {
                    mCurrentLoopCount++;
                    mHandler.removeMessages(ACTION_POLL_CURRENT_APP);

                    if (MainController.get() == null) {
                        Log.d(TAG, "No main controller, exit");
                        break;
                    }

                    ActivityManager am = (ActivityManager)mContext.getSystemService(Activity.ACTIVITY_SERVICE);

                    //Log.d(TAG, "Checking current tasks...");
                    List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
                    if (runningTasks.isEmpty()) {
                        CrashTracking.log(TAG + ": No running tasks!");
                        break;
                    }

                    ComponentName componentName = runningTasks.get(0).topActivity;
                    String appFlatComponentName = componentName.flattenToShortString();
                    if (appFlatComponentName != null
                            && mCurrentAppFlatComponentName != null
                            && !appFlatComponentName.equals(mCurrentAppFlatComponentName)) {

                        long currentTime = System.currentTimeMillis();
                        if (mNextAppFirstRunningTime == -1
                                || (mNextAppFlatComponentName != null && mNextAppFlatComponentName.equals(appFlatComponentName) == false)) {
                            mNextAppFirstRunningTime = currentTime;
                            mNextAppFlatComponentName = appFlatComponentName;
                            Log.d(TAG, "next app to maybe be changed from " + mCurrentAppFlatComponentName + " to " + appFlatComponentName);
                        }

                        long timeDelta = currentTime - mNextAppFirstRunningTime;
                        if (shouldIgnoreActivity(appFlatComponentName) == false
                                && mNextAppFlatComponentName.equals(appFlatComponentName) && timeDelta >= VERIFY_TIME
                                && mCurrentAppFlatComponentName.equals(appFlatComponentName) == false) {
                            String oldApp = mCurrentAppFlatComponentName;
                            mCurrentAppFlatComponentName = appFlatComponentName;
                            // It's possible the current app has been set to an app we should ignore (like Pocket or ES File Explorer)
                            // in beginAppPolling(). In that case, change mCurrentAppFlatComponentName, but don't inform the app about the
                            // change as it involved an app we should be ignoring.
                            if (shouldIgnoreActivity(oldApp)) {
                                mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                                Log.d(TAG, "ignore app changing from " + mCurrentAppFlatComponentName + " to " + appFlatComponentName);
                            } else {
                                Log.d(TAG, "current app changed from " + mCurrentAppFlatComponentName + " to " + appFlatComponentName + ", triggering onAppChanged()...");
                                if (mAppPollingListener != null) {
                                    mAppPollingListener.onAppChanged();
                                }
                            }
                        } else {
                            mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                        }
                    } else {
                        if (mNextAppFlatComponentName != null) {
                            Log.d(TAG, "*** successfully ignored setting current app to " + mNextAppFlatComponentName);
                        }
                        mNextAppFirstRunningTime = -1;
                        mNextAppFlatComponentName = null;
                        mHandler.sendEmptyMessageDelayed(ACTION_POLL_CURRENT_APP, LOOP_TIME);
                    }
                    if (mCurrentLoopCount == sCountToCallGc) {
                        System.gc();
                        mCurrentLoopCount = 0;
                    }
                    break;
                }
            }
        }
    };

}
