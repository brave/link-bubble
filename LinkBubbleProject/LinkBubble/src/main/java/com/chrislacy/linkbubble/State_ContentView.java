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
            mTouchBubble.doSnap(mCanvas, mTargetX, mTargetY);
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

        if (mDidMove) {
            Canvas.TargetInfo ti = mTouchBubble.getTargetInfo(mCanvas, sender.getXPos(), sender.getYPos());
            if (ti.mAction == Config.BubbleAction.None) {
                float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                float threshold = Config.dpToPx(900.0f);
                if (v > threshold) {
                    MainController.STATE_Flick.init(sender, e.vx, e.vy, false);
                    MainController.switchState(MainController.STATE_Flick);
                } else {
                    MainController.STATE_SnapToEdge.init(sender);
                    MainController.switchState(MainController.STATE_SnapToEdge);
                }
            } else {
                if (MainController.destroyBubble(mTouchBubble, ti.mAction)) {
                    MainController.STATE_AnimateToContentView.init(MainController.getBubble(MainController.getBubbleCount()-1));
                    MainController.switchState(MainController.STATE_AnimateToContentView);
                } else {
                    MainController.switchState(MainController.STATE_BubbleView);
                }
            }
        } else if (mActiveBubble != sender) {
            mActiveBubble = sender;
            mCanvas.setContentView(mActiveBubble.getContentView());
            mCanvas.showContentView();
            mCanvas.setContentViewTranslation(0.0f);
        } else {
            MainController.switchState(MainController.STATE_AnimateToBubbleView);
        }

        mTouchBubble = null;
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
