package com.linkbubble.physics;

import android.view.View;
import com.linkbubble.ui.BubbleView;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public BubbleView getBubbleView();
    public View getDraggableView();
    public void update(float dt, boolean contentView);
    public void onOrientationChanged(boolean contentViewMode);
}
