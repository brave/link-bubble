package com.linkbubble.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.squareup.otto.Subscribe;


public class CloseTabTargetView extends BubbleTargetView {

    public CloseTabTargetView(Context context) {
        this(context, null);
    }

    public CloseTabTargetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloseTabTargetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void registerForBus() {
        MainApplication.registerForBus(getContext(), this);
    }

    @Override
    protected void unregisterForBus() {
        MainApplication.unregisterForBus(getContext(), this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        super.onBeginBubbleDrag(e);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        super.onEndBubbleDragEvent(e);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDraggableBubbleMovedEvent(MainController.DraggableBubbleMovedEvent e) {
        super.onDraggableBubbleMovedEvent(e);
    }
}
