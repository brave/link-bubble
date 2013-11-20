package com.chrislacy.linkbubble;

import android.view.View;

/**
 * Created by gw on 18/11/13.
 */
public class State_ContentView extends ControllerState {

    private Canvas mCanvas;
    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private int mSetX;
    private int mSetY;
    private Bubble mTouchBubble;
    private Bubble mActiveBubble;

    public State_ContentView(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(Bubble b) {
        mActiveBubble = b;
    }

    @Override
    public void OnEnterState() {
        Util.Assert(mActiveBubble != null);
        mDidMove = false;
        mTouchBubble = null;
    }

    @Override
    public boolean OnUpdate(float dt) {
        if (mTouchBubble != null) {
            Circle bubbleCircle = new Circle(mTargetX + Config.mBubbleWidth * 0.5f,
                    mTargetY + Config.mBubbleHeight * 0.5f,
                    Config.mBubbleWidth * 0.5f);
            Canvas.TargetInfo targetInfo = mCanvas.getBubbleAction(bubbleCircle);

            int targetX, targetY;

            boolean overshoot = false;
            if (targetInfo.mAction == Config.BubbleAction.None) {
                targetX = mTargetX;
                targetY = mTargetY;
            } else {
                overshoot = true;
                targetX = (int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f);
                targetY = (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f);
            }

            float t = 0.02f;
            if (mTouchBubble.isSnapping() || overshoot) {
                t = 0.2f;
            }

            if (targetX != mSetX || targetY != mSetY) {
                mTouchBubble.setTargetPos(targetX, targetY, t, overshoot);
                mSetX = targetX;
                mSetY = targetY;
            }

            return true;
        }

        return false;
    }

    @Override
    public void OnExitState() {
        mActiveBubble = null;
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
        mTouchBubble = sender;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;
        mSetX = -1;
        mSetY = -1;

        MainController.scheduleUpdate();
    }

    @Override
    public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
        mTargetX = mInitialX + e.dx;
        mTargetY = mInitialY + e.dy;

        mTargetX = Util.clamp(Config.mBubbleSnapLeftX, mTargetX, Config.mBubbleSnapRightX);
        mTargetY = Util.clamp(Config.mBubbleMinY, mTargetY, Config.mBubbleMaxY);

        float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
        if (d >= Config.dpToPx(10.0f)) {
            mDidMove = true;
            mCanvas.hideContentView();
        }

        MainController.scheduleUpdate();
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
        sender.clearTargetPos();
        mTouchBubble = null;

        if (mDidMove) {
            float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
            float threshold = Config.dpToPx(900.0f);
            if (v > threshold) {
                MainController.STATE_Flick.init(sender, e.vx, e.vy, true);
                MainController.switchState(MainController.STATE_Flick);
            } else {
                MainController.STATE_AnimateToContentView.init(sender);
                MainController.switchState(MainController.STATE_AnimateToContentView);
            }
        } else if (mActiveBubble != sender) {
            mActiveBubble = sender;
            mCanvas.setContentView(mActiveBubble.getContentView());
            mCanvas.showContentView();
            mCanvas.setContentViewTranslation(0.0f);
        } else {
            MainController.switchState(MainController.STATE_AnimateToBubbleView);
        }
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
        return "ContentView";
    }
}
