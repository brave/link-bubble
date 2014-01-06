package com.linkbubble.physics;

import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.Canvas;
import com.linkbubble.Config;
import com.linkbubble.ui.ContentView;
import com.linkbubble.MainController;
import com.linkbubble.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_KillBubble extends ControllerState {

    private Canvas mCanvas;
    private float mTime;
    private float mPeriod;

    private BubbleView mBubble;
    private float mBubbleY0;

    public State_KillBubble(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(BubbleView bubble) {
        Util.Assert(mBubble == null);
        mBubble = bubble;
        mBubbleY0 = mBubble.getYPos();
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mBubble != null);
        mCanvas.fadeOutTargets();
        ContentView contentView = mCanvas.getContentView();
        if (contentView != null) {
            contentView.onAnimateOffscreen();
        }
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
