package com.linkbubble.physics;

import android.view.View;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public void update(float dt);
    public void onOrientationChanged();

    public interface OnUpdateListener {
        public void onUpdate(Draggable draggable, float dt);
    }
}
