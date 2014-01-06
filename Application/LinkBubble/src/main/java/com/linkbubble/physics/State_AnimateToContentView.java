package com.linkbubble.physics;

import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.Canvas;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

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
    private float mBubblePeriod;
    private float mContentPeriod;
    private Vector<BubbleInfo> mBubbleInfo = new Vector<BubbleInfo>();

    public State_AnimateToContentView(Canvas canvas) {
        mCanvas = canvas;
    }

    @Override
    public void OnEnterState() {
        mCanvas.fadeIn();
        mCanvas.fadeOutTargets();
        if (mCanvas.getContentView() != null) {
            mCanvas.getContentView().onAnimateOnScreen();
        }
        mCanvas.setContentView(MainController.get().getActiveBubble().getContentView());

        mBubbleInfo.clear();
        mTime = 0.0f;
        mBubblePeriod = 0.3f;
        mContentPeriod = mBubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        MainController mainController = MainController.get();
        int bubbleCount = mainController.getBubbleCount();
        for (int i=0 ; i < bubbleCount ; ++i) {
            BubbleInfo bi = new BubbleInfo();
            BubbleView b = mainController.getBubble(i);
            b.setVisibility(View.VISIBLE);

            bi.mPosX = (float) b.getXPos();
            bi.mPosY = (float) b.getYPos();

            bi.mTargetX = Config.getContentViewX(i, MainController.get().getBubbleCount());
            bi.mTargetY = Config.mContentViewBubbleY;
            bi.mDistanceX = bi.mTargetX - bi.mPosX;
            bi.mDistanceY = bi.mTargetY - bi.mPosY;
            mBubbleInfo.add(bi);
        }

        mCanvas.showContentView();
        mCanvas.setContentViewTranslation(Config.mScreenHeight - Config.mContentOffset);

        mainController.beginAppPolling();
    }

    @Override
    public boolean OnUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mBubblePeriod);
        //Log.e("GapTech", "t=" + mTime / mBubblePeriod + ", f=" + f);
        mTime += dt;

        MainController mainController = MainController.get();
        int bubbleCount = mainController.getBubbleCount();
        for (int i=0 ; i < bubbleCount ; ++i) {
            BubbleInfo bi = mBubbleInfo.get(i);
            BubbleView b = mainController.getBubble(i);

            float x = bi.mPosX + bi.mDistanceX * f;
            float y = bi.mPosY + bi.mDistanceY * f;

            if (mTime >= mBubblePeriod) {
                x = bi.mTargetX;
                y = bi.mTargetY;
            }

            b.setExactPos((int) x, (int) y);
        }

        float t = Util.clamp(0.0f, 1.0f - mTime / mContentPeriod, 1.0f);
        mCanvas.setContentViewTranslation(t * (Config.mScreenHeight - Config.mContentOffset));

        if (mTime >= mBubblePeriod && mTime >= mContentPeriod) {
            //mainController.STATE_ContentView.init(mSelectedBubble);
            mainController.switchState(mainController.STATE_ContentView);
        }

        return true;
    }

    @Override
    public void OnExitState() {
        mCanvas.setContentViewTranslation(0.0f);
    }

    @Override
    public void OnMotionEvent_Touch(BubbleView sender, BubbleView.TouchEvent e) {
    }

    @Override
    public void OnMotionEvent_Move(BubbleView sender, BubbleView.MoveEvent e) {
    }

    @Override
    public void OnMotionEvent_Release(BubbleView sender, BubbleView.ReleaseEvent e) {
    }

    @Override
    public boolean OnNewBubble(BubbleView bubble) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void OnDestroyBubble(BubbleView bubble) {
        Util.Assert(false);
    }

    @Override
    public boolean OnOrientationChanged() {
        MainController mainController = MainController.get();
        //mainController.STATE_ContentView.init(mSelectedBubble);
        mainController.switchState(mainController.STATE_ContentView);
        return true;
    }

    @Override
    public void OnCloseDialog() {
    }

    @Override
    public String getName() {
        return "AnimateToContentView";
    }
}
