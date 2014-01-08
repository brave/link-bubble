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
    private DraggableItem mDraggable;
    private CanvasView mCanvasView;

    public State_SnapToEdge(CanvasView c) {
        mCanvasView = c;
    }

    public void init(DraggableItem b) {
        mDraggable = b;
    }

    @Override
    public void OnEnterState() {
        mCanvasView.fadeOutTargets();
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
    public boolean OnUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mPeriod);
        mTime += dt;

        float x = mPosX + mDistanceX * f;
        float y = (float) mDraggable.getDraggableHelper().getYPos();

        DraggableItem b = mDraggable;

        if (mTime >= mPeriod) {
            x = Util.clamp(Config.mBubbleSnapLeftX, x, Config.mBubbleSnapRightX);
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
        }

        Config.BUBBLE_HOME_X = (int) x;
        Config.BUBBLE_HOME_Y = (int) y;
        b.getDraggableHelper().setExactPos(Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y);

        return true;
    }

    @Override
    public void OnExitState() {
        MainController.get().setAllBubblePositions(mDraggable);
        mDraggable = null;
    }

    @Override
    public void onTouchActionDown(DraggableItem sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(DraggableItem sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(DraggableItem sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean OnNewDraggable(DraggableHelper draggable) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyDraggable(DraggableItem draggableItem) {
        Util.Assert(false);
    }

    @Override
    public boolean OnOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void OnCloseDialog() {
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
