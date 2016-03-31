/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.linkbubble.R;

public class BubbleFlowActivity extends Activity {

    BubbleFlowView mBubbleFlowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bubble_flow);

        Point size = new Point();
        WindowManager w = getWindowManager();
        w.getDefaultDisplay().getSize(size);

        final LayoutInflater inflater = LayoutInflater.from(this);
        mBubbleFlowView = (BubbleFlowView) findViewById(R.id.bubble_flow);
        mBubbleFlowView.configure(size.x,
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));
        for (int i = 0; i < 19; i++) {
            TabView bubble = (TabView) inflater.inflate(R.layout.view_tab, null);
            mBubbleFlowView.add(bubble, false);
        }

        findViewById(R.id.add_bubble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabView bubble = (TabView) inflater.inflate(R.layout.view_tab, null);
                mBubbleFlowView.add(bubble, false);
            }
        });

        findViewById(R.id.remove_bubble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int centerIndex = mBubbleFlowView.getCenterIndex();
                if (centerIndex > -1) {
                    mBubbleFlowView.remove(centerIndex, false, true);
                }
            }
        });

        final Button animateButton = (Button) findViewById(R.id.animate_bubble_button);
        animateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBubbleFlowView.isExpanded()) {
                    mBubbleFlowView.collapse();
                    animateButton.setText("Expand");
                } else {
                    mBubbleFlowView.expand();
                    animateButton.setText("Collapse");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mBubbleFlowView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBubbleFlowView.setCenterIndex(6);
            }
        }, 100);
    }

    @Override
    public void onWindowFocusChanged (boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);


        if (hasWindowFocus) {
            hasWindowFocus = false;
        }
    }
}
