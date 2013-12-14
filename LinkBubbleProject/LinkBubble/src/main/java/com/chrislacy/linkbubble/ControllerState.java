package com.chrislacy.linkbubble;

/**
 * Created by gw on 18/11/13.
 */
public abstract class ControllerState {
    public abstract void OnEnterState();
    public abstract boolean OnUpdate(float dt);
    public abstract void OnExitState();
    public abstract void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e);
    public abstract void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e);
    public abstract void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e);
    public abstract boolean OnNewBubble(Bubble bubble);
    public abstract void OnDestroyBubble(Bubble bubble);
    public abstract boolean OnOrientationChanged();
    public abstract void OnCloseDialog();
    public void OnPageLoaded(Bubble bubble) {}
    public abstract String getName();
}
