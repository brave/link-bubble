package com.chrislacy.linkbubble;

import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Vector;

/**
 * Created by gw on 18/11/13.
 */
public class State_AnimateToBubbleView extends ControllerState {

    private class BubbleInfo {
        public float mPosX;
        public float mPosY;
        public float mDistanceX;
        public float mDistanceY;
        public float mTargetX;
        public float mTargetY;
    }

    private Canvas mCanvas;
    private OvershootInterpolator mInterpolator = new OvershootInterpolator(0.5f);
    private float mTime;
    private float mPeriod;
    private Vector<BubbleInfo> mBubbleInfo = new Vector<BubbleInfo>();

    public State_AnimateToBubbleView(Canvas canvas) {
        mCanvas = canvas;
    }

    @Override
    public void OnEnterState() {
        mBubbleInfo.clear();
        mTime = 0.0f;
        mPeriod = 0.3f;

        int bubbleCount = MainController.getBubbleCount();
        for (int i=0 ; i < bubbleCount ; ++i) {
            BubbleInfo bi = new BubbleInfo();
            Bubble b = MainController.getBubble(i);
            bi.mPosX = (float) b.getXPos();
            bi.mPosY = (float) b.getYPos();

            bi.mTargetX = Config.BUBBLE_HOME_X;
            bi.mTargetY = Config.BUBBLE_HOME_Y;
            bi.mDistanceX = bi.mTargetX - bi.mPosX;
            bi.mDistanceY = bi.mTargetY - bi.mPosY;
            mBubbleInfo.add(bi);
        }

        mCanvas.setContentViewTranslation(0.0f);
    }

    @Override
    public boolean OnUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mPeriod);
        mTime += dt;

        int bubbleCount = MainController.getBubbleCount();
        for (int i=0 ; i < bubbleCount ; ++i) {
            BubbleInfo bi = mBubbleInfo.get(i);
            Bubble b = MainController.getBubble(i);

            float x = bi.mPosX + bi.mDistanceX * f;
            float y = bi.mPosY + bi.mDistanceY * f;

            if (mTime >= mPeriod) {
                x = bi.mTargetX;
                y = bi.mTargetY;
            }

            b.setExactPos((int) x, (int) y);
        }

        float t = mTime / mPeriod;
        mCanvas.setContentViewTranslation(t * (Config.mScreenHeight - Config.mContentOffset));

        if (mTime >= mPeriod) {
            MainController.switchState(MainController.STATE_BubbleView);
        }

        return true;
    }

    @Override
    public void OnExitState() {
        mCanvas.setContentViewTranslation(Config.mScreenHeight - Config.mContentOffset);
        mCanvas.setContentView(null);
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
    public boolean OnOrientationChanged() {
        MainController.switchState(MainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void OnCloseDialog() {
        Util.Assert(false);
    }

    @Override
    public String getName() {
        return "AnimateToBubbleView";
    }
}
