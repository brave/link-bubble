package com.linkbubble.ui;

import android.content.Context;
import android.util.AttributeSet;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
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
