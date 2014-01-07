package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class BubbleFlowView extends FancyCoverFlow {
    public BubbleFlowView(Context context) {
        this(context, null);
    }

    public BubbleFlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        MainApplication app = (MainApplication) context.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        setBackgroundColor(0x33ff0000);
    }

    void expand() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.width = RelativeLayout.LayoutParams.MATCH_PARENT;
        setLayoutParams(lp);
    }

    void collapse() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        lp.width = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        setLayoutParams(lp);
    }

    void bubblesUpdated() {
        BubbleFlowAdapter adapter = (BubbleFlowAdapter)getAdapter();
        if (adapter.mBubbles == null) {
            adapter.setBubbles(MainController.get().getBubbles());
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBubbleAdded(MainController.BubbleAddedEvent event) {
        bubblesUpdated();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onContentActivityPaused(MainController.BubbleRemovedEvent event) {
        bubblesUpdated();
    }
}
