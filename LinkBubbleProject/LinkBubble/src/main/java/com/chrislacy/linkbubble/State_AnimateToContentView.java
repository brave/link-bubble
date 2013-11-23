package com.chrislacy.linkbubble;

import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Vector;

/**
 * Created by gw on 18/11/13.
 */
public class State_AnimateToContentView extends ControllerState {

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
    private Bubble mSelectedBubble;

    public State_AnimateToContentView(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(Bubble b) {
        mSelectedBubble = b;
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mSelectedBubble != null);
        mCanvas.setContentView(mSelectedBubble.getContentView());

        mBubbleInfo.clear();
        mTime = 0.0f;
        mPeriod = 0.3f;

        int bubbleCount = MainController.getBubbleCount();
        for (int i=0 ; i < bubbleCount ; ++i) {
            BubbleInfo bi = new BubbleInfo();
            Bubble b = MainController.getBubble(i);
            b.setVisibility(View.VISIBLE);

            bi.mPosX = (float) b.getXPos();
            bi.mPosY = (float) b.getYPos();

            bi.mTargetX = Config.getContentViewX(i);
            bi.mTargetY = Config.mContentViewBubbleY;
            bi.mDistanceX = bi.mTargetX - bi.mPosX;
            bi.mDistanceY = bi.mTargetY - bi.mPosY;
            mBubbleInfo.add(bi);
        }

        mCanvas.showContentView();
        mCanvas.setContentViewTranslation(Config.mScreenHeight - Config.mContentOffset);
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

        float t = 1.0f - mTime / mPeriod;
        mCanvas.setContentViewTranslation(t * (Config.mScreenHeight - Config.mContentOffset));

        if (mTime >= mPeriod) {
            MainController.STATE_ContentView.init(mSelectedBubble);
            MainController.switchState(MainController.STATE_ContentView);
        }

        return true;
    }

    @Override
    public void OnExitState() {
        mSelectedBubble = null;
        mCanvas.setContentViewTranslation(0.0f);
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
        MainController.STATE_ContentView.init(mSelectedBubble);
        MainController.switchState(MainController.STATE_ContentView);
        return true;
    }

    @Override
    public void OnCloseDialog() {
        Util.Assert(false);
    }

    @Override
    public String getName() {
        return "AnimateToContentView";
    }
}
