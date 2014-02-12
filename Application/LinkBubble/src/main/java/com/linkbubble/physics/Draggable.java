package com.linkbubble.physics;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public void update(float dt);
    public void onOrientationChanged();

    public interface OnUpdateListener {
        public void onUpdate(Draggable draggable, float dt);
    }
}
