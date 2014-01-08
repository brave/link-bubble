package com.linkbubble.physics;

import com.linkbubble.ui.BubbleView;

/**
 * Created by gw on 18/11/13.
 */
public abstract class ControllerState {
    public abstract void OnEnterState();
    public abstract boolean OnUpdate(float dt);
    public abstract void OnExitState();
    public abstract void OnMotionEvent_Touch(BubbleView sender, Draggable.TouchEvent e);
    public abstract void OnMotionEvent_Move(BubbleView sender, Draggable.MoveEvent e);
    public abstract void OnMotionEvent_Release(BubbleView sender, Draggable.ReleaseEvent e);
    public abstract boolean OnNewBubble(BubbleView bubble);
    public abstract void OnDestroyBubble(BubbleView bubble);
    public abstract boolean OnOrientationChanged();
    public abstract void OnCloseDialog();
    public void OnPageLoaded(BubbleView bubble) {}
    public abstract String getName();
}
