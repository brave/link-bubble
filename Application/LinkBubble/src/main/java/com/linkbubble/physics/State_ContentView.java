package com.linkbubble.physics;

import com.linkbubble.Constant;
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
    private Draggable mDraggable;
    private boolean mTouchDown;

    public State_ContentView(CanvasView canvasView) {
        mCanvasView = canvasView;
    }

    @Override
    public void onEnterState() {
        Util.Assert(MainController.get().getActiveDraggable() != null);
        mDidMove = false;
        mDraggable = null;
        MainController.get().beginAppPolling();
        //MainController.get().showBubblePager(true);
    }

    @Override
    public boolean onUpdate(float dt) {
        if (mDraggable != null) {
            if (mDidMove) {
                MainController.get().setActiveDraggable(mDraggable);
                mDraggable.getDraggableHelper().doSnap(mCanvasView, mTargetX, mTargetY);
            }
            return true;
        }

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
        mDraggable = sender;
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
                mCanvasView.hideContentView();
            }

            MainController.get().scheduleUpdate();
        }
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
        MainController mainController = MainController.get();
        if (mTouchDown) {
            sender.getDraggableHelper().clearTargetPos();

            if (mDidMove) {
                // NPE here with sender null: http://pastebin.com/GvQW57Dk
                CanvasView.TargetInfo ti = mDraggable.getDraggableHelper().getTargetInfo(mCanvasView,
                        sender.getDraggableHelper().getXPos(), sender.getDraggableHelper().getYPos());
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
                    if (mainController.destroyCurrentBubble()) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_BubbleView);
                    }
                }
            } else if (MainController.get().getActiveDraggable() != sender) {

                // TODO: GW: I don't think this code path ever gets hit currently. Left as an assert
                // to test if we hit this, and if so, work out what needs to be done.
                Util.Assert(false);

                //setActiveBubble(sender);
            } else {
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }

            mDraggable = null;
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
        mDraggable = null;
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
