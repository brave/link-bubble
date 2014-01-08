package com.linkbubble.physics;

import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public abstract class State_Flick extends ControllerState {

    private CanvasView mCanvasView;
    private BubbleView mBubble;
    private CanvasView.TargetInfo mTargetInfo;

    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator(1.5f);
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private float mTime;
    private float mPeriod;
    private float mInitialX;
    private float mInitialY;
    private float mTargetX;
    private float mTargetY;
    private boolean mLinear;

    public abstract boolean isContentView();

    public State_Flick(CanvasView canvasView) {
        mCanvasView = canvasView;
    }

    public void init(BubbleView bubble, float vx, float vy) {
        mTargetInfo = null;
        mBubble = bubble;

        mInitialX = bubble.getXPos();
        mInitialY = bubble.getYPos();
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
    public void OnEnterState() {
        Util.Assert(mBubble != null);
    }

    @Override
    public boolean OnUpdate(float dt) {
        MainController mainController = MainController.get();
        if (mTargetInfo == null) {
            float tf = mTime / mPeriod;
            float f = (mLinear ? mLinearInterpolator.getInterpolation(tf) : mOvershootInterpolator.getInterpolation(tf));
            mTime += dt;

            float x = mInitialX + (mTargetX - mInitialX) * f;
            float y = mInitialY + (mTargetY - mInitialY) * f;

            CanvasView.TargetInfo ti = mBubble.getDraggableHelper().getTargetInfo(mCanvasView, (int) x, (int) y);
            switch (ti.mAction) {
                case Destroy:
                case ConsumeRight:
                case ConsumeLeft:
                    ti.mTargetX = (int) (0.5f + ti.mTargetX - Config.mBubbleWidth * 0.5f);
                    ti.mTargetY = (int) (0.5f + ti.mTargetY - Config.mBubbleHeight * 0.5f);
                    mTargetInfo = ti;
                    mBubble.setTargetPos(ti.mTargetX, ti.mTargetY, 0.2f, true);
                    break;
                default:
                    {
                        BubbleView b = mBubble;
                        if (mTime >= mPeriod) {
                            x = mTargetX;
                            y = mTargetY;

                            if (isContentView()) {
                                mainController.switchState(mainController.STATE_AnimateToContentView);
                            } else if (x == Config.mBubbleSnapLeftX || x == Config.mBubbleSnapRightX) {
                                Config.BUBBLE_HOME_X = mBubble.getXPos();
                                Config.BUBBLE_HOME_Y = mBubble.getYPos();
                                mainController.switchState(mainController.STATE_BubbleView);
                            } else {
                                mainController.STATE_SnapToEdge.init(b);
                                mainController.switchState(mainController.STATE_SnapToEdge);
                            }
                        }
                        b.setExactPos((int) x, (int) y);
                    }
            }
        } else {
            if (mBubble.getXPos() == mTargetInfo.mTargetX && mBubble.getYPos() == mTargetInfo.mTargetY) {
                if (mainController.destroyBubble(mBubble, mTargetInfo.mAction)) {
                    if (isContentView()) {
                        mainController.switchState(mainController.STATE_AnimateToContentView);
                    } else {
                        mainController.switchState(mainController.STATE_AnimateToBubbleView);
                    }
                } else {
                    mainController.switchState(mainController.STATE_BubbleView);
                }
            }
        }

        return true;
    }

    @Override
    public void OnExitState() {
        if (!isContentView())
            MainController.get().setAllBubblePositions(mBubble);
        mBubble = null;
    }

    @Override
    public void onTouchActionDown(DraggableItem sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(DraggableItem sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(DraggableItem sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean OnNewDraggable(DraggableHelper draggable) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyBubble(BubbleView bubble) {
    }

    @Override
    public boolean OnOrientationChanged() {
        MainController mainController = MainController.get();
        if (isContentView()) {
            mainController.switchState(mainController.STATE_AnimateToContentView);
        } else {
            mainController.switchState(mainController.STATE_BubbleView);
        }
        return isContentView();
    }

    @Override
    public void OnCloseDialog() {
    }

    @Override
    public String getName() {
        return "Flick";
    }
}
