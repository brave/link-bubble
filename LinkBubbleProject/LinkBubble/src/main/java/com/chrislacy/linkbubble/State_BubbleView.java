package com.chrislacy.linkbubble;

import android.graphics.*;
import android.view.View;

/**
 * Created by gw on 18/11/13.
 */
public class State_BubbleView extends ControllerState {

    private Canvas mCanvas;
    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private int mSetX;
    private int mSetY;
    private Bubble mBubble;
    private Badge mBadge;

    public State_BubbleView(Canvas canvas, Badge badge) {
        mCanvas = canvas;
        mBadge = badge;
    }

    @Override
    public void OnEnterState() {
        mDidMove = false;
        mBubble = null;
        mBadge.show();
        mCanvas.fadeOut();

        for (int i=0 ; i < MainController.getBubbleCount() ; ++i) {
            Bubble b = MainController.getBubble(i);
            int vis = View.VISIBLE;
            if (i != MainController.getBubbleCount()-1)
                vis = View.GONE;
            b.setVisibility(vis);
        }
    }

    @Override
    public boolean OnUpdate(float dt) {
        if (mBubble != null) {
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
            if (mBubble.isSnapping() || overshoot) {
                t = 0.2f;
            }

            if (targetX != mSetX || targetY != mSetY) {
                mBubble.setTargetPos(targetX, targetY, t, overshoot);
                mSetX = targetX;
                mSetY = targetY;
            }

            return true;
        }

        return false;
    }

    @Override
    public void OnExitState() {
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
        mCanvas.fadeIn();
        mBubble = sender;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;
        mSetX = -1;
        mSetY = -1;
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
        }
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
        sender.clearTargetPos();
        mBubble = null;

        if (mDidMove) {

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
            mBadge.hide();
            MainController.STATE_AnimateToContentView.init(sender);
            MainController.switchState(MainController.STATE_AnimateToContentView);
        }
    }

    @Override
    public boolean OnNewBubble(Bubble bubble) {
        return true;
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
        return "BubbleView";
    }
}
