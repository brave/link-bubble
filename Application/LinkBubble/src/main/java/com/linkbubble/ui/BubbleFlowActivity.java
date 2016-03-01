/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    //BubbleFlowDraggable mBubbleFlowView;
    List<ContentView> mContentViews;

    //CanvasView mCanvasView;
    //BubbleDraggable mBubbleDraggable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentViews = new ArrayList<>();
        //moveTaskToBack(true);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_bubble_flow);
        MainController controller = MainController.get();
        //Intent intent = getIntent();
        //String host = intent.getExtras().getString("host");

        //Point size = new Point();
        //WindowManager w = getWindowManager();
        //w.getDefaultDisplay().getSize(size);

        final LayoutInflater inflater = LayoutInflater.from(this);
        //mCanvasView = new CanvasView(this);
        //mBubbleDraggable = (BubbleDraggable) inflater.inflate(R.layout.view_bubble_draggable, null);
        //Point bubbleRestingPoint = Settings.get().getBubbleRestingPoint();
        //int fromX = Settings.get().getBubbleStartingX(bubbleRestingPoint);
        //mBubbleDraggable.configure(fromX, bubbleRestingPoint.y, bubbleRestingPoint.x, bubbleRestingPoint.y,
        //        Constant.BUBBLE_SLIDE_ON_SCREEN_TIME, mCanvasView);

        /*mBubbleDraggable.setOnUpdateListener(new BubbleDraggable.OnUpdateListener() {
            @Override
            public void onUpdate(Draggable draggable, float dt) {
                if (!draggable.isDragging()) {
                    mBubbleFlowView.syncWithBubble(draggable);
                }
            }
        });

        mBubbleFlowView = (BubbleFlowDraggable) findViewById(R.id.bubble_flow);
        mBubbleFlowView.configure(null, false);
        mBubbleFlowView.collapse(0, null);
        mBubbleFlowView.setBubbleDraggable(mBubbleDraggable);
        mBubbleDraggable.setBubbleFlowDraggable(mBubbleFlowView);*/
        //mContentView = (ContentView) findViewById(R.id.content_view_id);
        ContentView contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, -1);//(FrameLayout.LayoutParams) mContentView.getLayoutParams();
        //to do debug
        pr.topMargin = 128;
        //
        addContentView(contentView, pr);
        mContentViews.add(contentView);
        /*mBubbleFlowView.configure(size.x,
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));*/

        TabView bubble = (TabView) inflater.inflate(R.layout.view_tab, null);
        //ContentView contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        try {
            bubble.mContentView = mContentViews.get(0);
            bubble.configure("http://macworld.com", 1, false, true, false);
        }
        catch (MalformedURLException exc) {
        }
        controller.addBubble(bubble, false);
        //controller.mBubbleFlowDraggable.mBubbleDraggable.mBadgeView.setCount(1);
        //controller.mBubbleFlowDraggable.saveCurrentTabs();
        //controller.showBadge(true);
        //mBubbleFlowView.add(bubble, false);
        mContentViews.get(0).setVisibility(View.GONE);
        ///
        TabView bubble2 = (TabView) inflater.inflate(R.layout.view_tab, null);
        contentView = (ContentView) inflater.inflate(R.layout.view_content, null);
        //FrameLayout.LayoutParams pr = new FrameLayout.LayoutParams(-1, -1);//(FrameLayout.LayoutParams) mContentView.getLayoutParams();
        //to do debug
        //pr.topMargin = 128;
        //
        addContentView(contentView, pr);
        mContentViews.add(contentView);
        try {
            bubble2.mContentView = mContentViews.get(1);
            bubble2.configure("http://google.ca", 1, false, true, false);
        }
        catch (MalformedURLException exc) {
        }
        controller.addBubble(bubble2, false);
        //controller.mBubbleFlowDraggable.mBubbleDraggable.mBadgeView.setCount(2);
        //controller.mBubbleFlowDraggable.saveCurrentTabs();
        controller.mBubbleFlowDraggable.setCurrentTab(bubble2);
        //controller.showBadge(true);
        //mBubbleFlowView.add(bubble2, false);
        //mContentViews.get(1).setVisibility(View.GONE);
        //mContentViews.get(0).setVisibility(View.VISIBLE);
        ///

    }

    @Override
    public void onResume() {
        super.onResume();

        /*mBubbleFlowView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBubbleFlowView.setCenterIndex(6);
            }
        }, 100);*/
    }

    @Override
    public void onWindowFocusChanged (boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);


        if (hasWindowFocus) {
            hasWindowFocus = false;
        }
    }
}
