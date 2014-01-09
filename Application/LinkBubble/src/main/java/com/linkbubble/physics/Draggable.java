package com.linkbubble.physics;

import android.view.View;
import com.linkbubble.ui.BubbleLegacyView;
import com.linkbubble.ui.ContentView;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public BubbleLegacyView getBubbleLegacyView();
    public View getDraggableView();
    public void update(float dt, boolean contentView);
    public void onOrientationChanged(boolean contentViewMode);
    public void readd();

    public interface OnUpdateListener {
        public void onUpdate(Draggable draggable, float dt, boolean contentView);
    }
}
