package com.chrislacy.linkbubble;

import android.view.animation.OvershootInterpolator;

/**
 * Created by gw on 18/11/13.
 */
public class State_SnapToEdge extends ControllerState {
    private float mPosX;
    private float mDistanceX;
    private OvershootInterpolator mInterpolator = new OvershootInterpolator(1.5f);
    private float mTime;
    private float mPeriod;
    private Bubble mBubble;
    private Canvas mCanvas;

    public State_SnapToEdge(Canvas c) {
        mCanvas = c;
    }

    public void init(Bubble b) {
        mBubble = b;
    }

    @Override
    public void OnEnterState() {
        mCanvas.fadeOutTargets();
        Util.Assert(mBubble != null);

        mTime = 0.0f;
        mPeriod = 0.5f;

        mPosX = (float) mBubble.getXPos();
        if (mPosX < Config.mScreenCenterX) {
            mDistanceX = Config.mBubbleSnapLeftX - mPosX;
        } else {
            mDistanceX = Config.mBubbleSnapRightX - mPosX;
        }

        Config.BUBBLE_HOME_Y = (int) mBubble.getYPos();
    }

    @Override
    public boolean OnUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mPeriod);
        mTime += dt;

        float x = mPosX + mDistanceX * f;
        float y = (float) mBubble.getYPos();

        Bubble b = mBubble;

        if (mTime >= mPeriod) {
            x = Util.clamp(Config.mBubbleSnapLeftX, x, Config.mBubbleSnapRightX);
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
        }

        Config.BUBBLE_HOME_X = (int) x;
        Config.BUBBLE_HOME_Y = (int) y;
        b.setExactPos(Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y);

        return true;
    }

    @Override
    public void OnExitState() {
        MainController.get().setAllBubblePositions(mBubble);
        mBubble = null;
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
    }

    @Override
    public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
    }

    @Override
    public boolean OnNewBubble(Bubble bubble) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void OnDestroyBubble(Bubble bubble) {
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
