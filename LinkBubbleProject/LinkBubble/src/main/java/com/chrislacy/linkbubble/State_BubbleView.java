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
    private Bubble mBubble;
    private Badge mBadge;
    private boolean mTouchDown;
    private int mTouchFrameCount;

    public State_BubbleView(Canvas canvas, Badge badge) {
        mCanvas = canvas;
        mBadge = badge;
    }

    @Override
    public void OnEnterState() {
        mCanvas.fadeOutTargets();
        mDidMove = false;
        mBubble = null;
        mBadge.show();
        mCanvas.fadeOut();

        MainController mainController = MainController.get();
        for (int i=0 ; i < mainController.getBubbleCount() ; ++i) {
            Bubble b = mainController.getBubble(i);
            int vis = View.VISIBLE;
            if (i != mainController.getBubbleCount()-1)
                vis = View.GONE;
            b.setVisibility(vis);
        }
    }

    @Override
    public boolean OnUpdate(float dt) {

        if (mBubble != null) {
            ++mTouchFrameCount;

            if (mTouchFrameCount == 6) {
                mCanvas.fadeInTargets();
            }

            mBubble.doSnap(mCanvas, mTargetX, mTargetY);
            return true;
        }

        return false;
    }

    @Override
    public void OnExitState() {
        MainController.get().setAllBubblePositions(mBubble);
    }

    @Override
    public void OnPageLoaded(Bubble bubble) {
        if (Settings.get().autoLoadContent()) {
            mBadge.hide();
            MainController mainController = MainController.get();
            mainController.STATE_AnimateToContentView.init(bubble);
            mainController.switchState(mainController.STATE_AnimateToContentView);
        }
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
        mTouchDown = true;
        mCanvas.fadeIn();
        mBubble = sender;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;
        mDidMove = false;

        MainController.get().scheduleUpdate();
        mTouchFrameCount = 0;
    }

    @Override
    public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
        if (mTouchDown) {
            mTargetX = mInitialX + e.dx;
            mTargetY = mInitialY + e.dy;

            mTargetX = Util.clamp(Config.mBubbleSnapLeftX, mTargetX, Config.mBubbleSnapRightX);
            mTargetY = Util.clamp(Config.mBubbleMinY, mTargetY, Config.mBubbleMaxY);

            float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
            if (d >= Config.dpToPx(10.0f)) {
                mCanvas.fadeInTargets();
                mDidMove = true;
            }
        }
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
        if (mTouchDown) {
            sender.clearTargetPos();

            MainController mainController = MainController.get();
            if (mDidMove) {
                mCanvas.fadeOut();
                Canvas.TargetInfo ti = mBubble.getTargetInfo(mCanvas, sender.getXPos(), sender.getYPos());
                if (ti.mAction == Config.BubbleAction.None) {
                    float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                    float threshold = Config.dpToPx(900.0f);
                    if (v > threshold) {
                        mainController.STATE_Flick_BubbleView.init(sender, e.vx, e.vy);
                        mainController.switchState(mainController.STATE_Flick_BubbleView);
                        mainController.hideContentActivity();
                    } else {
                        mainController.STATE_SnapToEdge.init(sender);
                        mainController.switchState(mainController.STATE_SnapToEdge);
                    }
                } else {
                    if (ti.mAction == Config.BubbleAction.Destroy) {
                        int bubbleCount = mainController.getBubbleCount();
                        for (int i=bubbleCount-1 ; i >= 0; --i) {
                            Bubble b = mainController.getBubble(i);
                            mainController.destroyBubble(b, Config.BubbleAction.Destroy);
                        }
                        Util.Assert(mainController.getBubbleCount() == 0);
                        mainController.switchState(mainController.STATE_BubbleView);
                    } else {
                        if (mainController.destroyBubble(mBubble, ti.mAction)) {
                            mainController.switchState(mainController.STATE_AnimateToBubbleView);
                        } else {
                            mainController.switchState(mainController.STATE_BubbleView);
                        }
                    }
                }
            } else {
                mBadge.hide();
                mainController.STATE_AnimateToContentView.init(sender);
                mainController.switchState(mainController.STATE_AnimateToContentView);
            }

            mBubble = null;
        }
    }

    @Override
    public boolean OnNewBubble(Bubble bubble) {
        return true;
    }

    @Override
    public void OnDestroyBubble(Bubble bubble) {
    }

    @Override
    public boolean OnOrientationChanged() {
        mTouchDown = false;
        mBubble = null;
        mCanvas.fadeOut();
        return false;
    }

    @Override
    public void OnCloseDialog() {
    }

    @Override
    public String getName() {
        return "BubbleView";
    }
}
