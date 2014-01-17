package com.linkbubble.physics;

import android.content.Context;
import android.view.View;

import com.linkbubble.MainApplication;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_BubbleView extends ControllerState {

    private Context mContext;
    private CanvasView mCanvasView;
    private boolean mDidMove;
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private Draggable mDraggable;
    private boolean mTouchDown;

    public State_BubbleView(Context context, CanvasView canvasView) {
        mCanvasView = canvasView;
        mContext = context;
    }

    @Override
    public void onEnterState() {
        mDidMove = false;
        mDraggable = null;

        MainController mainController = MainController.get();
        for (int i=0 ; i < mainController.getDraggableCount() ; ++i) {
            Draggable draggable = mainController.getDraggable(i);
            int vis = View.VISIBLE;
            if (draggable != mainController.getActiveDraggable())
                vis = View.GONE;
            draggable.getDraggableView().setVisibility(vis);
        }
    }

    @Override
    public boolean onUpdate(float dt) {

        if (mDraggable != null) {
            mDraggable.getDraggableHelper().doSnap(mCanvasView, mTargetX, mTargetY);
            return true;
        }

        return false;
    }

    @Override
    public void onExitState() {
        MainController.get().setAllDraggablePositions(mDraggable);
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
        mDraggable = sender;
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
        }
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
        if (mTouchDown) {
            sender.getDraggableHelper().clearTargetPos();

            MainController mainController = MainController.get();
            if (mDidMove) {
                CanvasView.TargetInfo ti = mDraggable.getDraggableHelper().getTargetInfo(mCanvasView,
                        sender.getDraggableHelper().getXPos(), sender.getDraggableHelper().getYPos());
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
                        mainController.destroyAllBubbles();
                        mainController.switchState(mainController.STATE_BubbleView);
                    } else {
                        if (mainController.destroyCurrentBubble()) {
                            mainController.switchState(mainController.STATE_AnimateToBubbleView);
                        } else {
                            mainController.switchState(mainController.STATE_BubbleView);
                        }
                    }
                }
            } else {
                mainController.switchState(mainController.STATE_AnimateToContentView);
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
