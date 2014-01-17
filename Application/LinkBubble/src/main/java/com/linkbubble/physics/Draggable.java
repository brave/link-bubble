package com.linkbubble.physics;

import android.view.View;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public View getDraggableView();
    public void update(float dt);
    public void onOrientationChanged(boolean contentViewMode);
    public void readd();

    public interface OnUpdateListener {
        public void onUpdate(Draggable draggable, float dt);
    }
}
