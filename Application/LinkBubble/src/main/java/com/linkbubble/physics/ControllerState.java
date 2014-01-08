package com.linkbubble.physics;

import com.linkbubble.ui.BubbleView;

/**
 * Created by gw on 18/11/13.
 */
public abstract class ControllerState {
    public abstract void OnEnterState();
    public abstract boolean OnUpdate(float dt);
    public abstract void OnExitState();
    public abstract void OnMotionEvent_Touch(BubbleView sender, DraggableHelper.TouchEvent e);
    public abstract void OnMotionEvent_Move(BubbleView sender, DraggableHelper.MoveEvent e);
    public abstract void OnMotionEvent_Release(BubbleView sender, DraggableHelper.ReleaseEvent e);
    public abstract boolean OnNewDraggable(DraggableHelper draggable);
    public abstract void onDestroyBubble(BubbleView bubble);
    public abstract boolean OnOrientationChanged();
    public abstract void OnCloseDialog();
    public void OnPageLoaded(BubbleView bubble) {}
    public abstract String getName();
}
