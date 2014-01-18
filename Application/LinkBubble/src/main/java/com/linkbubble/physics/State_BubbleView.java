package com.linkbubble.physics;

import android.content.Context;
import android.view.View;

import com.linkbubble.MainApplication;
import com.linkbubble.ui.BubbleTargetView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_BubbleView extends ControllerState {

    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private boolean mTouchDown;
    private Context mContext;
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();

    public State_BubbleView(Context c) {
        mContext = c;
    }

    @Override
    public void onEnterState() {
        mDidMove = false;
    }

    @Override
    public boolean onUpdate(float dt) {
        return false;
    }

    @Override
    public void onExitState() {
    }

    @Override
    public void onPageLoaded() {
        if (Settings.get().getAutoContentDisplayLinkLoaded()) {
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_AnimateToContentView);
        }
    }

    @Override
    public void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e) {
        if (sender == null) {
            throw new RuntimeException("Must have valid sender");
        }
        mTouchDown = true;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;
        mDidMove = false;

        MainController mainController = MainController.get();
        mainController.scheduleUpdate();
    }

    @Override
    public void onTouchActionMove(Draggable sender, DraggableHelper.MoveEvent e) {
        if (mTouchDown) {
            mTargetX = mInitialX + e.dx;
            mTargetY = mInitialY + e.dy;

            mTargetX = Util.clamp(Config.mBubbleSnapLeftX, mTargetX, Config.mBubbleSnapRightX);
            mTargetY = Util.clamp(Config.mBubbleMinY, mTargetY, Config.mBubbleMaxY);

            float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
            if (d >= Config.dpToPx(10.0f)) {
                mDidMove = true;
            }

            MainController.get().getBubbleDraggable().setTargetPos(mTargetX, mTargetY, 0.02f, DraggableHelper.AnimationType.Linear);
        }
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
        if (mTouchDown) {
            sender.getDraggableHelper().clearTargetPos();
            boolean endDragEvent = true;

            MainController mainController = MainController.get();
            if (mDidMove) {
                BubbleTargetView snapTarget = mainController.getBubbleDraggable().getCurrentSnapTarget();

                if (snapTarget == null) {
                    float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                    float threshold = Config.dpToPx(900.0f);
                    if (v > threshold) {
                        mainController.STATE_Flick_BubbleView.init(sender, e.vx, e.vy);
                        mainController.switchState(mainController.STATE_Flick_BubbleView);
                        mainController.hideContentActivity();
                        endDragEvent = false;
                    } else {
                        mainController.STATE_SnapToEdge.init(sender);
                        mainController.switchState(mainController.STATE_SnapToEdge);
                    }
                } else {
                    if (snapTarget.getAction() == Config.BubbleAction.Destroy) {
                        mainController.destroyAllBubbles();
                        mainController.switchState(mainController.STATE_BubbleView);
                    } else {
                        if (mainController.destroyCurrentBubble(snapTarget.getAction())) {
                            mainController.switchState(mainController.STATE_AnimateToBubbleView);
                        } else {
                            mainController.switchState(mainController.STATE_BubbleView);
                        }
                    }
                }
            } else {
                mainController.switchState(mainController.STATE_AnimateToContentView);
            }

            if (endDragEvent) {
                MainApplication.postEvent(mContext, mEndBubbleDragEvent);
            }

            mTouchDown = false;
        }
    }

    @Override
    public boolean onNewDraggable(Draggable draggable) {
        return true;
    }

    @Override
    public void onDestroyDraggable(Draggable draggable) {
    }

    @Override
    public boolean onOrientationChanged() {
        mTouchDown = false;
        return false;
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "BubbleView";
    }
}
