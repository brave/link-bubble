package com.linkbubble.physics;

/**
 * Created by gw on 18/11/13.
 */
public abstract class ControllerState {
    public abstract void onEnterState();
    public abstract boolean onUpdate(float dt);
    public abstract void onExitState();
    public abstract void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e);
    public abstract void onTouchActionMove(Draggable sender, DraggableHelper.MoveEvent e);
    public abstract void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e);
    public abstract boolean onNewDraggable(Draggable draggable);
    public abstract void onDestroyDraggable(Draggable draggable);
    public abstract boolean onOrientationChanged();
    public abstract void onCloseDialog();
    public void onPageLoaded() {}
    public abstract String getName();
}
