package com.linkbubble.physics;

import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.ui.ContentView;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_KillBubble extends ControllerState {

    private float mTime;
    private float mPeriod;

    private Draggable mDraggable;
    private float mBubbleY0;

    public void init(Draggable draggable) {
        Util.Assert(mDraggable == null);
        mDraggable = draggable;
        mBubbleY0 = mDraggable.getDraggableHelper().getYPos();
    }

    @Override
    public void onEnterState() {
        Util.Assert(mDraggable != null);
        mTime = 0.0f;
        mPeriod = 0.3f;
        MainController.get().endAppPolling();
    }

    @Override
    public boolean onUpdate(float dt) {
        float t = mTime / mPeriod;
        mTime += dt;

        float dy = t * Config.mScreenHeight;

        mDraggable.getDraggableHelper().setExactPos(mDraggable.getDraggableHelper().getXPos(), (int) (dy + mBubbleY0));

        if (mTime >= mPeriod) {
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
            mainController.hideContentActivity();
        }

        return true;
    }

    @Override
    public void onExitState() {
        MainController.get().destroyCurrentBubble();
        mDraggable = null;
    }

    @Override
    public void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(Draggable sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean onNewDraggable(Draggable draggable) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyDraggable(Draggable draggable) {
    }

    @Override
    public boolean onOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "KillBubble";
    }
}
