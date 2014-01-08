package com.linkbubble.physics;

import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_ContentView extends ControllerState {

    private CanvasView mCanvasView;
    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private BubbleView mTouchBubble;
    private boolean mTouchDown;
    private int mTouchFrameCount;

    public State_ContentView(CanvasView canvasView) {
        mCanvasView = canvasView;
    }

    @Override
    public void OnEnterState() {
        Util.Assert(MainController.get().getActiveBubble() != null);
        mDidMove = false;
        mTouchBubble = null;
        MainController.get().beginAppPolling();
    }

    @Override
    public boolean OnUpdate(float dt) {
        if (mTouchBubble != null) {
            ++mTouchFrameCount;

            if (mTouchFrameCount == 6) {
                mCanvasView.fadeInTargets();
                mCanvasView.hideContentView();
            }

            if (mDidMove) {
                MainController.get().setActiveBubble(mTouchBubble);
                mTouchBubble.doSnap(mCanvasView, mTargetX, mTargetY);
            }
            return true;
        }

        return false;
    }

    @Override
    public void OnExitState() {
    }

    @Override
    public void OnMotionEvent_Touch(BubbleView sender, Draggable.TouchEvent e) {
        mTouchDown = true;
        mTouchBubble = sender;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;

        MainController.get().scheduleUpdate();
        mTouchFrameCount = 0;
    }

    @Override
    public void OnMotionEvent_Move(BubbleView sender, Draggable.MoveEvent e) {
        if (mTouchDown) {
            mTargetX = mInitialX + e.dx;
            mTargetY = mInitialY + e.dy;

            mTargetX = Util.clamp(Config.mBubbleSnapLeftX, mTargetX, Config.mBubbleSnapRightX);
            mTargetY = Util.clamp(Config.mBubbleMinY, mTargetY, Config.mBubbleMaxY);

            float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
            if (d >= Config.dpToPx(10.0f)) {
                mDidMove = true;
                mCanvasView.hideContentView();
                mCanvasView.fadeInTargets();
            }

            MainController.get().scheduleUpdate();
        }
    }

    @Override
    public void OnMotionEvent_Release(BubbleView sender, Draggable.ReleaseEvent e) {
        MainController mainController = MainController.get();
        if (mTouchDown) {
            sender.clearTargetPos();

            if (mDidMove) {
                // NPE here with sender null: http://pastebin.com/GvQW57Dk
                CanvasView.TargetInfo ti = mTouchBubble.getTargetInfo(mCanvasView, sender.getXPos(), sender.getYPos());
                if (ti.mAction == Config.BubbleAction.None) {
                    float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                    float threshold = Config.dpToPx(900.0f);
                    if (v > threshold) {
                        mainController.STATE_Flick_ContentView.init(sender, e.vx, e.vy);
                        mainController.switchState(mainController.STATE_Flick_ContentView);
                    } else {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    }
                } else {
                    if (mainController.destroyBubble(mTouchBubble, ti.mAction)) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_BubbleView);
                    }
                }
            } else if (MainController.get().getActiveBubble() != sender) {
                mCanvasView.fadeOutTargets();
                setActiveBubble(sender);
            } else {
                mainController.getActiveBubble().readd();
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }

            mTouchBubble = null;
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
        mTouchBubble = null;
        return true;
    }

    @Override
    public void OnCloseDialog() {
    }

    @Override
    public String getName() {
        return "ContentView";
    }

    public void setActiveBubble(BubbleView bubble) {
        MainController.get().setActiveBubble(bubble);
        bubble.setTargetPos((int)Config.getContentViewX(bubble.getBubbleIndex(), MainController.get().getBubbleCount()), bubble.getYPos(), 0.2f, false);
        mCanvasView.setContentView(bubble.getContentView());
        mCanvasView.showContentView();
        mCanvasView.setContentViewTranslation(0.0f);
    }
}
