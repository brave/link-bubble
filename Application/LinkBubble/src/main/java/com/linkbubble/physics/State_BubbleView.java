package com.linkbubble.physics;

import android.view.View;
import com.linkbubble.ui.BadgeView;
import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.Canvas;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.Settings;
import com.linkbubble.Util;

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
    private BubbleView mBubble;
    private BadgeView mBadgeView;
    private boolean mTouchDown;
    private int mTouchFrameCount;

    public State_BubbleView(Canvas canvas, BadgeView badgeView) {
        mCanvas = canvas;
        mBadgeView = badgeView;
    }

    @Override
    public void OnEnterState() {
        mCanvas.fadeOutTargets();
        mDidMove = false;
        mBubble = null;
        mBadgeView.show();
        mCanvas.fadeOut();

        MainController mainController = MainController.get();
        for (int i=0 ; i < mainController.getBubbleCount() ; ++i) {
            BubbleView b = mainController.getBubble(i);
            int vis = View.VISIBLE;
            if (b != mainController.getActiveBubble())
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
    public void OnPageLoaded(BubbleView bubble) {
        if (Settings.get().getAutoContentDisplayLinkLoaded()) {
            mBadgeView.hide();
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_AnimateToContentView);
        }
    }

    @Override
    public void OnMotionEvent_Touch(BubbleView sender, BubbleView.TouchEvent e) {
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

        mBadgeView.hide();
    }

    @Override
    public void OnMotionEvent_Move(BubbleView sender, BubbleView.MoveEvent e) {
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
    public void OnMotionEvent_Release(BubbleView sender, BubbleView.ReleaseEvent e) {
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
                            BubbleView b = mainController.getBubble(i);
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
                mBadgeView.hide();
                mainController.switchState(mainController.STATE_AnimateToContentView);
            }

            mBubble = null;
        }
    }

    @Override
    public boolean OnNewBubble(BubbleView bubble) {
        return true;
    }

    @Override
    public void OnDestroyBubble(BubbleView bubble) {
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
