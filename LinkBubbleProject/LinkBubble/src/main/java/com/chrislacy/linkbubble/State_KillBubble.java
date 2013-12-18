package com.chrislacy.linkbubble;

import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Vector;

/**
 * Created by gw on 18/11/13.
 */
public class State_KillBubble extends ControllerState {

    private class BubbleInfo {
        public Bubble mBubble;
        public float mPosX;
        public float mPosY;
        public float mDistanceX;
        public float mDistanceY;
        public float mTargetX;
        public float mTargetY;
    }

    private Canvas mCanvas;
    private float mTime;
    private float mPeriod;
    private BubbleInfo mBubbleInfo;

    public State_KillBubble(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(Bubble bubble) {
        Util.Assert(mBubbleInfo == null);
        mBubbleInfo = new BubbleInfo();
        mBubbleInfo.mBubble = bubble;
        mBubbleInfo.mPosX = (float) bubble.getXPos();
        mBubbleInfo.mPosY = (float) bubble.getYPos();

        mBubbleInfo.mTargetX = mBubbleInfo.mPosX;
        mBubbleInfo.mTargetY = Config.mScreenHeight + Config.mBubbleHeight;
        mBubbleInfo.mDistanceX = mBubbleInfo.mTargetX - mBubbleInfo.mPosX;
        mBubbleInfo.mDistanceY = mBubbleInfo.mTargetY - mBubbleInfo.mPosY;
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mBubbleInfo != null);
        mCanvas.fadeOutTargets();
        mCanvas.getContentView().onAnimateOffscreen();
        mTime = 0.0f;
        mPeriod = 0.3f;
        mCanvas.setContentViewTranslation(0.0f);

        MainController.get().endAppPolling();
    }

    @Override
    public boolean OnUpdate(float dt) {
        float t = mTime / mPeriod;
        mTime += dt;

        float x = mBubbleInfo.mPosX + mBubbleInfo.mDistanceX * t;
        float y = mBubbleInfo.mPosY + mBubbleInfo.mDistanceY * t;

        if (mTime >= mPeriod) {
            x = mBubbleInfo.mTargetX;
            y = mBubbleInfo.mTargetY;
        }

        mBubbleInfo.mBubble.setExactPos((int) x, (int) y);

        mCanvas.setContentViewTranslation(t * (Config.mScreenHeight - Config.mContentOffset));

        if (mTime >= mPeriod) {
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
            mainController.hideContentActivity();
        }

        return true;
    }

    @Override
    public void OnExitState() {
        mCanvas.setContentViewTranslation(Config.mScreenHeight - Config.mContentOffset);
        mCanvas.setContentView(null);
        MainController.get().destroyBubble(mBubbleInfo.mBubble, Config.BubbleAction.Destroy);
        mBubbleInfo = null;
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
    }

    @Override
    public boolean OnOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void OnCloseDialog() {
    }

    @Override
    public String getName() {
        return "KillBubble";
    }
}
