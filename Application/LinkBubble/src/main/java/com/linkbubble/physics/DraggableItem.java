package com.linkbubble.physics;

import android.view.View;
import com.linkbubble.ui.BubbleView;

public interface DraggableItem {
    public DraggableHelper getDraggableHelper();
    public BubbleView getBubbleView();
    public View getDraggableView();
    public void update(float dt, boolean contentView);
}
