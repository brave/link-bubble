/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.FrameLayout;

import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class BubbleFlowActivity extends Activity {

    static final String ACTIVITY_INTENT_NAME = "com.google.app.brave.bubblesactivity";

    // Commands for service-activity interaction
    public static final int OPEN_URL            = 0;
    public static final int SET_TAB_AS_ACTIVE   = 1;
    //

    List<ContentView> mContentViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentViews = new ArrayList<>();
        moveTaskToBack(true);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_bubble_flow);

        MainController controller = MainController.get();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTIVITY_INTENT_NAME);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(mBroadcastReceiver, filter);
        synchronized (BubbleFlowDraggable.mActivitySharedLock) {
            BubbleFlowDraggable.mActivitySharedLock.notify();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
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
                        openUrl(new BubbleFlowDraggable.OpenUrlSettings(url, urlStartTime, hasShownAppPicker, performEmptyClick, setAsCurrentTab));

                        break;
                    case SET_TAB_AS_ACTIVE:
                        setAsCurrentTab(url);

                        break;
                }
            }
        }
    };

    private void setAsCurrentTab(String url) {
        for (ContentView contentView: mContentViews) {
            if (contentView.getUrl().toString().equals(url)) {
                contentView.setVisibility(View.VISIBLE);
            }
            else {
                contentView.setVisibility(View.GONE);
            }
        }
    }

    public void openUrl(BubbleFlowDraggable.OpenUrlSettings openUrlSettings) {
        MainController controller = MainController.get();
        // to do debug, sometimes it returns a null object
        if (null == controller) {
            controller = MainController.get();
        }
        //

        final LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, -1);
        //to do debug
        //pr.topMargin = 128;
        ContentView contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        mContentViews.add(contentView);
        addContentView(contentView, pr);
        controller.mBubbleFlowDraggable.createTabView(contentView, openUrlSettings, controller);
    }

    @Override
    public void onWindowFocusChanged (boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);


        if (hasWindowFocus) {
            hasWindowFocus = false;
        }
    }
}
