package com.linkbubble.physics;

import android.content.Context;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.ui.BubbleTargetView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_ContentView extends ControllerState {

    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private boolean mTouchDown;
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();
    private Context mContext;

    public State_ContentView(Context c) {
        mContext = c;
    }

    @Override
    public void onEnterState() {
        mDidMove = false;
        MainController.get().beginAppPolling();
        //MainController.get().showBubblePager(true);
    }

    @Override
    public boolean onUpdate(float dt) {
        return false;
    }

    @Override
    public void onExitState() {
        //MainController.get().showBubblePager(false);
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

        MainController.get().scheduleUpdate();
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

            MainController.get().scheduleUpdate();
        }
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
        MainController mainController = MainController.get();
        if (mTouchDown) {
            sender.getDraggableHelper().clearTargetPos();
            boolean endDragEvent = true;

            if (mDidMove) {

                BubbleTargetView snapTarget = mainController.getBubbleDraggable().getCurrentSnapTarget();

                if (snapTarget == null) {
                    float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                    float threshold = Config.dpToPx(900.0f);
                    if (v > threshold) {
                        mainController.STATE_Flick_ContentView.init(sender, e.vx, e.vy);
                        mainController.switchState(mainController.STATE_Flick_ContentView);
                        endDragEvent = false;
                    } else {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    }
                } else {
                    if (mainController.destroyCurrentBubble(snapTarget.getAction())) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_BubbleView);
                    }
                }
            } else {
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }

            mTouchDown = false;

            if (endDragEvent) {
                MainApplication.postEvent(mContext, mEndBubbleDragEvent);
            }
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
        return true;
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "ContentView";
    }
}
