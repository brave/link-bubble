package com.linkbubble.physics;

import com.linkbubble.ui.BubbleLegacyView;
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
    private int mTouchFrameCount;

    public State_ContentView(CanvasView canvasView) {
        mCanvasView = canvasView;
    }

    @Override
    public void onEnterState() {
        Util.Assert(MainController.get().getActiveDraggable() != null);
        mDidMove = false;
        mDraggable = null;
        MainController.get().beginAppPolling();
    }

    @Override
    public boolean onUpdate(float dt) {
        if (mDraggable != null) {
            ++mTouchFrameCount;

            if (mTouchFrameCount == 6) {
                mCanvasView.fadeInTargets();
                mCanvasView.hideContentView();
            }

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
    }

    @Override
    public void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e) {
        mTouchDown = true;
        mDraggable = sender;
        mInitialX = e.posX;
        mInitialY = e.posY;
        mTargetX = mInitialX;
        mTargetY = mInitialY;

        MainController.get().scheduleUpdate();
        mTouchFrameCount = 0;
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
                mCanvasView.fadeInTargets();
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
                    if (mainController.destroyBubble(mDraggable, ti.mAction)) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_BubbleView);
                    }
                }
            } else if (MainController.get().getActiveDraggable() != sender) {
                mCanvasView.fadeOutTargets();
                setActiveBubble(sender);
            } else {
                mainController.getActiveDraggable().readd();
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }

            mDraggable = null;
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

    public void setActiveBubble(Draggable draggable) {
        MainController.get().setActiveDraggable(draggable);
        BubbleLegacyView bubble = draggable.getBubbleLegacyView();
        draggable.getDraggableHelper().setTargetPos((int) Config.getContentViewX(bubble.getBubbleIndex(),
                MainController.get().getDraggableCount()), bubble.getYPos(), 0.2f, false);
        mCanvasView.setContentView(bubble.getContentView());
        mCanvasView.showContentView();
        mCanvasView.setContentViewTranslation(0.0f);
    }
}
