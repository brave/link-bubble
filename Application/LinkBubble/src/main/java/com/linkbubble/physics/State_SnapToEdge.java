package com.linkbubble.physics;

import android.view.animation.OvershootInterpolator;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_SnapToEdge extends ControllerState {
    private float mPosX;
    private float mDistanceX;
    private OvershootInterpolator mInterpolator = new OvershootInterpolator(1.5f);
    private float mTime;
    private float mPeriod;
    private Draggable mDraggable;

    public void init(Draggable draggable) {
        mDraggable = draggable;
    }

    @Override
    public void onEnterState() {
        Util.Assert(mDraggable != null);

        mTime = 0.0f;
        mPeriod = 0.5f;

        mPosX = (float) mDraggable.getDraggableHelper().getXPos();
        if (mPosX < Config.mScreenCenterX) {
            mDistanceX = Config.mBubbleSnapLeftX - mPosX;
        } else {
            mDistanceX = Config.mBubbleSnapRightX - mPosX;
        }

        Config.BUBBLE_HOME_Y = (int) mDraggable.getDraggableHelper().getYPos();
    }

    @Override
    public boolean onUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mPeriod);
        mTime += dt;

        float x = mPosX + mDistanceX * f;
        float y = (float) mDraggable.getDraggableHelper().getYPos();

        Draggable draggable = mDraggable;

        if (mTime >= mPeriod) {
            x = Util.clamp(Config.mBubbleSnapLeftX, x, Config.mBubbleSnapRightX);
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
        }

        Config.BUBBLE_HOME_X = (int) x;
        Config.BUBBLE_HOME_Y = (int) y;
        draggable.getDraggableHelper().setExactPos(Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y);

        return true;
    }

    @Override
    public void onExitState() {
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
        Util.Assert(false);
    }

    @Override
    public boolean onOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void onCloseDialog() {
        if (mPosX < Config.mScreenCenterX) {
            Config.BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
        } else {
            Config.BUBBLE_HOME_X = Config.mBubbleSnapRightX;
        }
    }

    @Override
    public String getName() {
        return "SnapToEdge";
    }
}
