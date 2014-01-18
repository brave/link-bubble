package com.linkbubble.physics;

import android.content.Context;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.linkbubble.MainApplication;
import com.linkbubble.ui.BubbleDraggable;
import com.linkbubble.ui.BubbleTargetView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public abstract class State_Flick extends ControllerState {

    private Draggable mDraggable;

    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator(1.5f);
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private float mTime;
    private float mPeriod;
    private float mInitialX;
    private float mInitialY;
    private float mTargetX;
    private float mTargetY;
    private boolean mLinear;
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();
    private Context mContext;

    public abstract boolean isContentView();

    public State_Flick(Context c) {
        mContext = c;
    }

    public void init(Draggable draggable, float vx, float vy) {
        mDraggable = draggable;

        mInitialX = draggable.getDraggableHelper().getXPos();
        mInitialY = draggable.getDraggableHelper().getYPos();
        mTime = 0.0f;
        mPeriod = 0.0f;
        mLinear = true;

        if (Math.abs(vx) < 0.1f) {
            mTargetX = mInitialX;

            if (vy > 0.0f) {
                mTargetY = Config.mBubbleMaxY;
            } else {
                mTargetY = Config.mBubbleMinY;
            }
        } else {

            if (vx > 0.0f) {
                mTargetX = Config.mBubbleSnapRightX;
            } else {
                mTargetX = Config.mBubbleSnapLeftX;
            }

            float m = vy / vx;

            mTargetY = m * (mTargetX - mInitialX) + mInitialY;

            if (mTargetY < Config.mBubbleMinY) {
                mTargetY = Config.mBubbleMinY;
                mTargetX = mInitialX + (mTargetY - mInitialY) / m;
            } else if (mTargetY > Config.mBubbleMaxY) {
                mTargetY = Config.mBubbleMaxY;
                mTargetX = mInitialX + (mTargetY - mInitialY) / m;
            } else {
                mLinear = false;
                mPeriod += 0.15f;
            }
        }

        float dx = mTargetX - mInitialX;
        float dy = mTargetY - mInitialY;
        float d = (float) Math.sqrt(dx*dx + dy*dy);

        float v = (float) Math.sqrt(vx*vx + vy*vy);

        mPeriod += d/v;
        mPeriod = Util.clamp(0.05f, mPeriod, 0.5f);
    }

    @Override
    public void onEnterState() {
        Util.Assert(mDraggable != null);
    }

    @Override
    public boolean onUpdate(float dt) {
        MainController mainController = MainController.get();

        BubbleTargetView snapTarget = mainController.getBubbleDraggable().getCurrentSnapTarget();
        if (snapTarget != null) {
            Circle c = snapTarget.GetDefaultCircle();
            int x = (int) (c.mX - Config.mBubbleWidth);
            int y = (int) (c.mY - Config.mBubbleHeight);

            BubbleDraggable bd = mainController.getBubbleDraggable();
            bd.setTargetPos(x, y, 0.3f, DraggableHelper.AnimationType.SmallOvershoot);

            DraggableHelper dh = bd.getDraggableHelper();

            float d = Util.distance(x, y, dh.getXPos() - Config.mBubbleWidth * 0.5f, dh.getYPos() - Config.mBubbleHeight * 0.5f);
            if (d <= 1.0f) {
                if (mainController.destroyCurrentBubble(snapTarget.getAction())) {
                    if (isContentView()) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_AnimateToBubbleView);
                    }
                } else {
                    mainController.switchState(mainController.STATE_BubbleView);
                }
            }
        } else {
            float tf = mTime / mPeriod;
            float f = (mLinear ? mLinearInterpolator.getInterpolation(tf) : mOvershootInterpolator.getInterpolation(tf));
            mTime += dt;

            float x = mInitialX + (mTargetX - mInitialX) * f;
            float y = mInitialY + (mTargetY - mInitialY) * f;

            MainController.get().getBubbleDraggable().setTargetPos((int)x, (int)y, 0.0f, DraggableHelper.AnimationType.Linear);

            Draggable draggable = mDraggable;
            if (mTime >= mPeriod) {
                x = mTargetX;
                y = mTargetY;

                if (isContentView()) {
                    mainController.switchState(mainController.STATE_AnimateToContentView);
                } else if (x == Config.mBubbleSnapLeftX || x == Config.mBubbleSnapRightX) {
                    Config.BUBBLE_HOME_X = mDraggable.getDraggableHelper().getXPos();
                    Config.BUBBLE_HOME_Y = mDraggable.getDraggableHelper().getYPos();
                    mainController.switchState(mainController.STATE_BubbleView);
                } else {
                    mainController.STATE_SnapToEdge.init(draggable);
                    mainController.switchState(mainController.STATE_SnapToEdge);
                }

                MainApplication.postEvent(mContext, mEndBubbleDragEvent);
            }
            draggable.getDraggableHelper().setExactPos((int) x, (int) y);
        }

        return true;
    }

    @Override
    public void onExitState() {
        mDraggable = null;
    }

    @Override
    public void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(Draggable sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean onNewDraggable(Draggable draggable) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyDraggable(Draggable draggable) {
    }

    @Override
    public boolean onOrientationChanged() {
        MainController mainController = MainController.get();
        if (isContentView()) {
            mainController.switchState(mainController.STATE_AnimateToContentView);
        } else {
            mainController.switchState(mainController.STATE_BubbleView);
        }
        return isContentView();
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "Flick";
    }
}
