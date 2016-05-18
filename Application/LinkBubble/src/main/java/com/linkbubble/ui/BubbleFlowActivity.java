/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.util.ArrayList;
import java.util.List;

public class BubbleFlowActivity extends Activity {

    static final String ACTIVITY_INTENT_NAME = "com.google.app.brave.bubblesactivity";

    // Commands for service-activity interaction
    public static final int OPEN_URL            = 0;
    public static final int SET_TAB_AS_ACTIVE   = 1;
    public static final int COLLAPSE            = 2;
    public static final int EXPAND              = 3;
    public static final int PRE_CLOSE_VIEW      = 4;
    public static final int CLOSE_VIEW          = 5;
    public static final int DESTROY_ACTIVITY    = 6;
    public static final int RESTORE_TAB         = 7;
    //

    private List<ContentView> mContentViews;
    private List<ContentView> mPreClosedContentViews;
    private boolean mCollapsed = true;
    private boolean mDestroyed = true;
    private ImageView mTopMaskView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDestroyed = false;
        super.onCreate(savedInstanceState);

        Settings settings = Settings.get();
        if (null != settings) {
            setTheme(settings.getDarkThemeEnabled() ? R.style.ThemeTransparentDark : R.style.ThemeTransparent);
        }
        Log.d("TAG", "!!!!! ON CREATE");
        mContentViews = new ArrayList<>();
        mPreClosedContentViews = new ArrayList<>();
        setVisible(false);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_bubble_flow);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTIVITY_INTENT_NAME);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);
        MainController controller = getMainController();
        if (null != controller && null != controller.mBubbleFlowDraggable) {
            synchronized (MainApplication.mActivitySharedLock) {
                MainApplication.mActivitySharedLock.notify();
            }
            if (null != settings && settings.getWelcomeMessageDisplayed()) {
                moveTaskToBack(true);
            }
        } else {
            moveTaskToBack(true);
            mDestroyed = true;
            finish();
            startActivityForResult(new Intent(this, EntryActivity.class), 0);
        }
        int canvasMaskHeight = getResources().getDimensionPixelSize(R.dimen.canvas_mask_height);
        mTopMaskView = new ImageView(this);
        mTopMaskView.setImageResource(R.drawable.masked_background_half);
        mTopMaskView.setScaleType(ImageView.ScaleType.FIT_XY);
        FrameLayout.LayoutParams topMaskLP = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, canvasMaskHeight);
        topMaskLP.gravity = Gravity.TOP;
        mTopMaskView.setLayoutParams(topMaskLP);
        addContentView(mTopMaskView, topMaskLP);
    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.unregisterReceiver(mBroadcastReceiver);
        MainController controller = getMainController();
        if (null != controller) {
            controller.saveCurrentTabs();
            controller.closeAllBubbles(false);
            controller.finish();
        }

        super.onDestroy();
        MainApplication.mActivityIsUp = false;
        MainApplication.mActivitySharedLock = new Object();
        setFinishedActivityEvent();
        Log.d("TAG", "!!!!! ACTIVITY DESTROYED");
    }

    @Override
    protected void onStop() {
        if (!mCollapsed) {
            MainController controller = getMainController();
            if (null != controller) {
                controller.switchToBubbleView();
            }
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        MainController controller = getMainController();
        if (null != controller && controller.mRecentAppWasClicked) {
            Log.d("TAG", "!!!!!onResume");
            controller.mRecentAppWasClicked = false;
            setVisible(true);
            controller.setHiddenByUser(false);
            controller.doAnimateToContentView();
        }
        super.onResume();
    }

    // handler for received data from service
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BubbleFlowActivity.ACTIVITY_INTENT_NAME)) {
                int command = intent.getIntExtra("command", 0);
                String url = intent.getStringExtra("url");
                if (mDestroyed) {
                    return;
                }
                switch (command) {
                    case OPEN_URL:
                        long urlStartTime = intent.getLongExtra("urlStartTime", 1);
                        boolean hasShownAppPicker = intent.getBooleanExtra("hasShownAppPicker", false);
                        boolean performEmptyClick = intent.getBooleanExtra("performEmptyClick", false);
                        boolean setAsCurrentTab = intent.getBooleanExtra("setAsCurrentTab", false);
                        boolean openedFromItself = intent.getBooleanExtra("openedFromItself", false);
                        openUrl(new BubbleFlowDraggable.OpenUrlSettings(url, urlStartTime, setAsCurrentTab, hasShownAppPicker,
                                performEmptyClick, openedFromItself));

                        break;
                    case SET_TAB_AS_ACTIVE:
                        int index = intent.getIntExtra("index", -1);
                        setAsCurrentTabByIndex(url, index);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Window window = getWindow();
                            window.setStatusBarColor(getResources().getColor(R.color.background_material_dark));
                        }

                        break;
                    case COLLAPSE:
                        Log.d("TAG", "!!!!! ACTIVITY GONE COLLAPSE");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Window window = getWindow();
                            window.setStatusBarColor(getResources().getColor(R.color.transparent));
                        }
                        mCollapsed = true;
                        setVisible(false);
                        moveTaskToBack(true);

                        break;
                    case EXPAND:
                        Log.d("TAG", "!!!!! ACTIVITY VISIBLE EXPAND");
                        mCollapsed = false;
                        setVisible(true);

                        break;
                    case PRE_CLOSE_VIEW:
                        MainController controller = getMainController();
                        boolean useURL = false;
                        if (null == controller || null == controller.mBubbleFlowDraggable
                                || null == controller.mBubbleFlowDraggable.mDelayDeletedItem) {
                            useURL = true;
                        }
                        else {
                            useURL = true;
                            for (ContentView contentView: mContentViews) {
                                if (contentView.equals(controller.mBubbleFlowDraggable.mDelayDeletedItem.getContentView())) {
                                    contentView.setVisibility(View.GONE);
                                    mPreClosedContentViews.add(contentView);
                                    mContentViews.remove(contentView);
                                    useURL = false;
                                    break;
                                }
                            }
                        }
                        if (useURL) {
                            for (ContentView contentView : mContentViews) {
                                if (contentView.getUrl().toString().equals(url)) {
                                    contentView.setVisibility(View.GONE);
                                    mPreClosedContentViews.add(contentView);
                                    mContentViews.remove(contentView);
                                    break;
                                }
                            }
                        }
                        if (0 == mContentViews.size()) {
                            setVisible(false);
                        }
                        break;
                    case CLOSE_VIEW:
                        boolean foundView = false;
                        for (ContentView contentView: mPreClosedContentViews) {
                            if (contentView.getUrl().toString().equals(url)) {
                                mPreClosedContentViews.remove(contentView);
                                foundView = true;
                                break;
                            }
                        }
                        if (!foundView) {
                            for (ContentView contentView : mContentViews) {
                                if (contentView.getUrl().toString().equals(url)) {
                                    mContentViews.remove(contentView);
                                    break;
                                }
                            }
                        }
                        MainController controllerForDelete = getMainController();
                        if (null != controllerForDelete && null != controllerForDelete.mBubbleFlowDraggable
                                && null != controllerForDelete.mBubbleFlowDraggable.mDelayDeletedItem) {
                            controllerForDelete.mBubbleFlowDraggable.mDelayDeletedItem = null;
                        }
                        if (0 == mContentViews.size() && 0 == mPreClosedContentViews.size()) {
                            destroyActivity();
                        }

                        break;
                    case RESTORE_TAB:
                        for (ContentView contentView: mPreClosedContentViews) {
                            if (contentView.getUrl().toString().equals(url)) {
                                mContentViews.add(contentView);
                                mPreClosedContentViews.remove(contentView);
                                break;
                            }
                        }
                        MainController controllerForRestore = getMainController();
                        if (null != controllerForRestore && null != controllerForRestore.mBubbleFlowDraggable
                                && null != controllerForRestore.mBubbleFlowDraggable.mDelayDeletedItem) {
                            controllerForRestore.mBubbleFlowDraggable.mDelayDeletedItem = null;
                        }
                        break;
                    case DESTROY_ACTIVITY:
                        mContentViews.clear();
                        destroyActivity();

                        break;
                }
            }
        }
    };

    MainController getMainController() {
        MainController controller = MainController.get();
        // Sometimes it returns a null object
        if (null == controller) {
            controller = MainController.get();
        }
        //

        return controller;
    }

    private void setFinishedActivityEvent() {
        if (null == MainApplication.mDestroyActivitySharedLock) {
            return;
        }
        synchronized (MainApplication.mDestroyActivitySharedLock) {
            MainApplication.mActivityDestroyed = true;
            MainApplication.mDestroyActivitySharedLock.notify();
        }
    }

    private void destroyActivity() {
        mDestroyed = true;
        finish();
    }

    private void setAsCurrentTabByIndex(String url, int index) {
        MainController controller = getMainController();
        if (-1 == index || mContentViews.size() - 1 < index || null == controller || null == controller.mBubbleFlowDraggable) {
            setAsCurrentTab(url, false);
        }
        boolean found = false;
        for (ContentView contentView: mContentViews) {
            if (index == controller.mBubbleFlowDraggable.getIndexOfContentView(contentView)) {
                Log.d("TAG", "!!!!!VISIBLE");
                contentView.setVisibility(View.VISIBLE);
                found = true;
            }
            else {
                Log.d("TAG", "!!!!!GONE");
                contentView.setVisibility(View.GONE);
            }
        }
        if (!found) {
            setAsCurrentTab(url, false);
        }
    }

    private void setAsCurrentTab(String url, boolean allToGone) {
        boolean foundUrl = false;
        for (ContentView contentView: mContentViews) {
            if (!foundUrl && !allToGone && contentView.getUrl().toString().equals(url)) {
                Log.d("TAG", "!!!!!VISIBLE");
                contentView.setVisibility(View.VISIBLE);
                foundUrl = true;
            }
            else {
                Log.d("TAG", "!!!!!GONE");
                contentView.setVisibility(View.GONE);
            }
        }
    }

    public void openUrl(BubbleFlowDraggable.OpenUrlSettings openUrlSettings) {
        MainController controller = getMainController();

        if (!controller.mBubbleFlowDraggable.isExpanded()) {
            Log.d("TAG", "!!!!! ACTIVITY1 GONE");
            setVisible(false);
            Settings settings = Settings.get();
            if (null != settings && settings.getWelcomeMessageDisplayed()) {
                moveTaskToBack(true);
            }
        }
        else {
            Log.d("TAG", "!!!!! ACTIVITY1 VISIBLE");
            setVisible(true);
        }
        final LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, -1);
        ContentView contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        if (!openUrlSettings.mSetAsCurrentTab) {
            contentView.setVisibility(View.GONE);
        }
        if (openUrlSettings.mSetAsCurrentTab) {
            contentView.setVisibility(View.VISIBLE);
            setAsCurrentTab(openUrlSettings.mUrl, true);
        }
        mContentViews.add(contentView);
        addContentView(contentView, pr);
        controller.mBubbleFlowDraggable.createTabView(contentView, openUrlSettings, controller);
    }
}
