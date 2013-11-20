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

    public void init(Bubble b) {
        mBubble = b;
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mBubble != null);

        mTime = 0.0f;
        mPeriod = 0.5f;

        mPosX = (float) mBubble.getXPos();
        if (mPosX < Config.mScreenCenterX) {
            mDistanceX = Config.mBubbleSnapLeftX - mPosX;
        } else {
            mDistanceX = Config.mBubbleSnapRightX - mPosX;
        }
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
            MainController.switchState(MainController.STATE_BubbleView);
        }

        Config.BUBBLE_HOME_X = (int) x;
        Config.BUBBLE_HOME_Y = (int) y;
        b.setExactPos(Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y);

        return true;
    }

    @Override
    public void OnExitState() {
        MainController.setAllBubblePositions(mBubble);
        mBubble = null;
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
        Util.Assert(false);
    }

    @Override
    public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
        Util.Assert(false);
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
        Util.Assert(false);
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
    public void OnOrientationChanged() {
        Util.Assert(false);
    }

    @Override
    public void OnCloseDialog() {
        Util.Assert(false);
    }

    @Override
    public String getName() {
        return "SnapToEdge";
    }
}
