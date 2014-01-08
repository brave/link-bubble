package com.linkbubble.physics;

/**
 * Created by gw on 18/11/13.
 */
public abstract class ControllerState {
    public abstract void onEnterState();
    public abstract boolean onUpdate(float dt);
    public abstract void onExitState();
    public abstract void onTouchActionDown(DraggableItem sender, DraggableHelper.TouchEvent e);
    public abstract void onTouchActionMove(DraggableItem sender, DraggableHelper.MoveEvent e);
    public abstract void onTouchActionRelease(DraggableItem sender, DraggableHelper.ReleaseEvent e);
    public abstract boolean onNewDraggable(DraggableItem draggableItem);
    public abstract void onDestroyDraggable(DraggableItem draggableItem);
    public abstract boolean onOrientationChanged();
    public abstract void onCloseDialog();
    public void onPageLoaded(DraggableItem draggableItem) {}
    public abstract String getName();
}
