package com.linkbubble.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.MainService;
import com.linkbubble.R;
import com.linkbubble.util.CrashTracking;
import com.squareup.otto.Subscribe;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpandedActivity extends Activity {

    private static final String TAG = "ExpandedActivity";

    private static ExpandedActivity sInstance;
    public static ExpandedActivity get() {
        return sInstance;
    }

    public static class MinimizeExpandedActivityEvent {};

    public static class ShowFileBrowserEvent {
        public ValueCallback<Uri[]> getFilePathCallback() {
            return mFilePathCallback;
        }

        public String[] getAcceptTypes() {
            return mAcceptTypes;
        }

        public ValueCallback<Uri[]> mFilePathCallback;
        public String[] mAcceptTypes;
        public ShowFileBrowserEvent(String[] acceptTypes, ValueCallback<Uri[]> filePathCallback) {
            mFilePathCallback = filePathCallback;
            mAcceptTypes = acceptTypes;
        }
    };

    public static class ExpandedActivityReadyEvent {};
    
    private boolean mIsAlive = false;
    private boolean mIsShowing = false;
    private boolean mRegisteredForBus = false;
    private final Handler mHandler = new Handler();

    private LinearLayout mWebRendererContainer;

    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        CrashTracking.log("ExpandedActivity.onCreate()");

        super.onCreate(savedInstanceState);

        // Fixes #454
        if (MainController.get() == null || MainController.get().getActiveTabCount() == 0) {
            CrashTracking.log("early finish() because nothing to display");
            finish();
            return;
        }

        sInstance = this;

        mIsAlive = true;

        setContentView(R.layout.activity_expanded);

        getActionBar().hide();

        registerForBus();

        FrameLayout rootView = (FrameLayout) findViewById(R.id.expanded_root);

        mWebRendererContainer = (LinearLayout) findViewById(R.id.web_renderer_container);
        if (Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
            //mWebRendererContainer.setWillNotDraw(true);
            rootView.removeView(mWebRendererContainer);
            mWebRendererContainer = null;
        }

        if (Constant.EXPANDED_ACTIVITY_DEBUG) {
            rootView.setBackgroundColor(0x5500ff00);
        } else {
            rootView.setWillNotDraw(true);
        }
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        super.onActionModeStarted(actionMode);
        CrashTracking.log("onActionModeStarted()");
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        super.onActionModeFinished(actionMode);
        CrashTracking.log("onActionModeFinished()");
    }

    /*
     * Get the most recent RecentTaskInfo, but ensure the result is not Link Bubble.
     */
    ActivityManager.RecentTaskInfo getPreviousTaskInfo(List<ActivityManager.RecentTaskInfo> recentTasks) {
        for (int i = 0; i < recentTasks.size(); i++) {
            ActivityManager.RecentTaskInfo recentTaskInfo = recentTasks.get(i);
            if (recentTaskInfo.baseIntent != null
                    && recentTaskInfo.baseIntent.getComponent() != null) {
                String packageName = recentTaskInfo.baseIntent.getComponent().getPackageName();
                if (packageName.equals("android") == false && packageName.equals(BuildConfig.APPLICATION_ID) == false) {
                    return recentTaskInfo;
                }
            }
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        CrashTracking.log("***ExpandedActivity.onDestroy()");

        super.onDestroy();

        mIsAlive = false;

        if (sInstance == this) {
            sInstance = null;
        }

        unregisterForBus();
    }

    @Override
    protected void onStart() {
        CrashTracking.log("ExpandedActivity.onStart()");
        Log.e(TAG, "Expand time: " + (System.currentTimeMillis() - MainController.sStartExpandedActivityTime));

        MainApplication.postEvent(this, new ExpandedActivityReadyEvent());

        super.onStart();
    }

    @Override
    protected void onStop() {
        CrashTracking.log("ExpandedActivity.onStop()");
        super.onStop();

        //mTopMaskImage.setVisibility(View.GONE);
        //mBottomMaskImage.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        CrashTracking.log("ExpandedActivity.onResume()");
        super.onResume();

        mIsShowing = true;

        MainController mainController = MainController.get();
        if (mainController == null || mainController.contentViewShowing() == false) {
            minimize();
        }

        if (Constant.ACTIVITY_WEBVIEW_RENDERING && mWebRendererContainer.getChildCount() == 0) {
            TabView current = MainController.get().getCurrentTab();
            if (current != null) {
                setWebRenderer(current.getContentView().getWebRenderer().getView());
            }
        }
    }

    @Override
    public void onPause() {
        CrashTracking.log("ExpandedActivity.onPause()");
        super.onPause();
        mIsShowing = false;
    }

    @Override
    public void finish() {
        CrashTracking.log("ExpandedActivity.finish()");
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        delayedMinimize();
    }

    protected void registerForBus() {
      MainApplication.registerForBus(this, this);
      mRegisteredForBus = true;
    }

    protected void unregisterForBus() {
      if (mRegisteredForBus) {
        MainApplication.unregisterForBus(this, this);
        mRegisteredForBus = false;
      }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // check that the response is a good one
        if (requestCode == FILECHOOSER_RESULTCODE) {
            Uri[] results = new Uri[0];
            if(resultCode==Activity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
            MainController.get().switchToExpandedView();
        }
    }

    void minimize() {
        if (mIsAlive == false) {
            return;
        }

        // Comment out as a fix for #455
        //if (mIsShowing == false) {
        //    Log.d(TAG, "minimize() - mIsShowing:" + mIsShowing + ", exiting...");
        //    return;
        //}

        if (moveTaskToBack(true)) {
            CrashTracking.log("minimize() - moveTaskToBack(true);");
            return;
        }

        if (MainController.get() != null) {
            MainController.get().showBadge(true);
        }

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(16, ActivityManager.RECENT_WITH_EXCLUDED);

        if (recentTasks.size() > 0) {
            ActivityManager.RecentTaskInfo recentTaskInfo = getPreviousTaskInfo(recentTasks);
            ComponentName componentName = recentTaskInfo.baseIntent.getComponent();
            //openedFromAppName = componentName.getPackageName();
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
            CrashTracking.log("minimize() - " + componentName);
        } else {
            CrashTracking.log("minimize() - NONE!");
        }
    }

    void showFileBrowser(final String[] acceptTypes, ValueCallback<Uri[]> filePathCallback) {
        MainController.get().switchToBubbleView();
        mFilePathCallback = filePathCallback;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);

                        // Android intents wants mime types, filepickers can specify
                        // mime types or file types, filter out the file types.
                        ArrayList<String> filteredList = new ArrayList<>();
                        for (String acceptType : acceptTypes) {
                            if (acceptType.contains("/")) {
                                filteredList.add(acceptType);
                            }
                        }
                        if (filteredList.size() == 0) {
                            i.setType("*/*");
                        } else {
                            i.setType(StringUtil.join(filteredList, ","));
                        }
                        startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    void delayedMinimize() {
        // Kill the activity to ensure it is not alive in the event a link is intercepted,
        // thus displaying the ugly UI for a few frames
        //fadeOut();
        mHandler.postDelayed(mMinimizeRunnable, 500);
    }

    void cancelDelayedMinimize() {
        //fadeIn();
        mHandler.removeCallbacks(mMinimizeRunnable);
    }

    private Runnable mMinimizeRunnable = new Runnable() {
        @Override
        public void run() {
            minimize();
        }
    };

    public void setWebRenderer(View view) {
        if (mWebRendererContainer.getChildCount() == 0) {
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            mWebRendererContainer.addView(view);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMinimizeExpandedActivity(MinimizeExpandedActivityEvent e) {
        minimize();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onShowFileBrowserEvent(ShowFileBrowserEvent e) {
        showFileBrowser(e.getAcceptTypes(), e.getFilePathCallback());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHideContentEvent(MainController.HideContentEvent event) {
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void OnOnDestroyMainServiceEvent(MainService.OnDestroyMainServiceEvent event) {
        finish();
    }
}
