package com.chrislacy.linkbubble;

import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Vector;

/**
 * Created by gw on 18/11/13.
 */
public class State_KillBubble extends ControllerState {

    private Canvas mCanvas;
    private float mTime;
    private float mPeriod;

    private Bubble mBubble;
    private float mBubbleY0;

    public State_KillBubble(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(Bubble bubble) {
        Util.Assert(mBubble == null);
        mBubble = bubble;
        mBubbleY0 = mBubble.getYPos();
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mBubble != null);
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

        float dy = t * Config.mScreenHeight;

        mBubble.setExactPos(mBubble.getXPos(), (int) (dy + mBubbleY0));

        mCanvas.setContentViewTranslation(dy);

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
        MainController.get().destroyBubble(mBubble, Config.BubbleAction.Destroy);
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
