package com.linkbubble.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import com.linkbubble.R;

public class BubbleFlowActivity extends Activity {

    BubbleFlowView mBubbleFlowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bubble_flow);

        final LayoutInflater inflater = LayoutInflater.from(this);
        mBubbleFlowView = (BubbleFlowView) findViewById(R.id.bubble_flow);
        mBubbleFlowView.configure(getResources().getDimensionPixelSize(R.dimen.bubble_pager_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));
        for (int i = 0; i < 5; i++) {
            BubbleFlowItemView bubble = (BubbleFlowItemView) inflater.inflate(R.layout.view_bubble_flow_item, null);
            mBubbleFlowView.add(bubble);
        }

        findViewById(R.id.add_bubble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BubbleFlowItemView bubble = (BubbleFlowItemView) inflater.inflate(R.layout.view_bubble_flow_item, null);
                mBubbleFlowView.add(bubble);
            }
        });

        findViewById(R.id.remove_bubble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        findViewById(R.id.animate_bubble_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBubbleFlowView.isExpanded()) {
                    mBubbleFlowView.shrink();
                } else {
                    mBubbleFlowView.expand();
                }
            }
        });
    }

}
