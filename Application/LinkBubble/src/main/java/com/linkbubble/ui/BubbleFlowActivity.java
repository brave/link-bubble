/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import com.linkbubble.MainController;
import com.linkbubble.R;
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

    List<ContentView> mContentViews;
    List<ContentView> mPreClosedContentViews;
    boolean mCollapsed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        synchronized (controller.mBubbleFlowDraggable.mActivitySharedLock) {
            controller.mBubbleFlowDraggable.mActivitySharedLock.notify();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.unregisterReceiver(mBroadcastReceiver);
        MainController controller = getMainController();
        if (null != controller) {
            controller.saveCurrentTabs();
            controller.closeAllBubbles(false);
            controller.finish();
        }

        super.onDestroy();
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
                        setAsCurrentTab(url);

                        break;
                    case COLLAPSE:
                        Log.d("TAG", "!!!!! ACTIVITY GONE");
                        mCollapsed = true;
                        setVisible(false);

                        break;
                    case EXPAND:
                        Log.d("TAG", "!!!!! ACTIVITY VISIBLE");
                        mCollapsed = false;
                        setVisible(true);

                        break;
                    case PRE_CLOSE_VIEW:
                        for (ContentView contentView: mContentViews) {
                            if (contentView.getUrl().toString().equals(url)) {
                                mPreClosedContentViews.add(contentView);
                                mContentViews.remove(contentView);
                                break;
                            }
                        }
                        if (0 == mContentViews.size()) {
                            setVisible(false);
                        }
                        break;
                    case CLOSE_VIEW:
                        for (ContentView contentView: mContentViews) {
                            if (contentView.getUrl().toString().equals(url)) {
                                mContentViews.remove(contentView);
                                break;
                            }
                        }
                        for (ContentView contentView: mPreClosedContentViews) {
                            if (contentView.getUrl().toString().equals(url)) {
                                mPreClosedContentViews.remove(contentView);
                                break;
                            }
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
        // to do debug, sometimes it returns a null object
        if (null == controller) {
            controller = MainController.get();
        }
        //

        return controller;
    }

    private void destroyActivity() {
        MainController controller = getMainController();

        BubbleFlowDraggable.mActivityIsUp = false;
        if (null != controller) {
            controller.mBubbleFlowDraggable.mActivitySharedLock = new Object();
        }
        Log.d("TAG", "!!!!! ACTIVITY DESTROYED");
        finish();
    }

    private void setAsCurrentTab(String url) {
        for (ContentView contentView: mContentViews) {
            if (contentView.getUrl().toString().equals(url)) {
                Log.d("TAG", "!!!!!VISIBLE");
                contentView.setVisibility(View.VISIBLE);
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
        }
        else {
            Log.d("TAG", "!!!!! ACTIVITY1 VISIBLE");
            setVisible(true);
        }
        final LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, -1);
        ContentView contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        mContentViews.add(contentView);
        addContentView(contentView, pr);
        controller.mBubbleFlowDraggable.createTabView(contentView, openUrlSettings, controller);
    }
}
