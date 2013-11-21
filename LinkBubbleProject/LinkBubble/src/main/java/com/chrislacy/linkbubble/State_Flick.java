package com.chrislacy.linkbubble;

import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by gw on 18/11/13.
 */
public class State_Flick extends ControllerState {

    private Canvas mCanvas;
    private Bubble mBubble;
    private Canvas.TargetInfo mTargetInfo;

    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator(1.5f);
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private float mTime;
    private float mPeriod;
    private float mInitialX;
    private float mInitialY;
    private float mTargetX;
    private float mTargetY;
    private boolean mLinear;
    private boolean mContentViewActive;

    public State_Flick(Canvas canvas) {
        mCanvas = canvas;
    }

    public void init(Bubble bubble, float vx, float vy, boolean contentViewActive) {
        mTargetInfo = null;
        mBubble = bubble;
        mContentViewActive = contentViewActive;

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
        if (mTargetInfo == null) {
            float tf = mTime / mPeriod;
            float f = (mLinear ? mLinearInterpolator.getInterpolation(tf) : mOvershootInterpolator.getInterpolation(tf));
            mTime += dt;

            float x = mInitialX + (mTargetX - mInitialX) * f;
            float y = mInitialY + (mTargetY - mInitialY) * f;

            Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                    y + Config.mBubbleHeight * 0.5f,
                    Config.mBubbleWidth * 0.5f);

            /*Canvas.TargetInfo ti = mCanvas.getBubbleAction(bubbleCircle);
            switch (ti.mAction) {
                case Destroy:
                case ConsumeRight:
                case ConsumeLeft:
                    ti.mTargetX = (int) (0.5f + ti.mTargetX - Config.mBubbleWidth * 0.5f);
                    ti.mTargetY = (int) (0.5f + ti.mTargetY - Config.mBubbleHeight * 0.5f);
                    mTargetInfo = ti;
                    mBubble.setTargetPos(ti.mTargetX, ti.mTargetY, 0.2f, true);
                    break;
                default:*/
                    {
                        Bubble b = mBubble;
                        if (mTime >= mPeriod) {
                            x = mTargetX;
                            y = mTargetY;

                            if (mContentViewActive) {
                                MainController.STATE_AnimateToContentView.init(b);
                                MainController.switchState(MainController.STATE_AnimateToContentView);
                            } else if (x == Config.mBubbleSnapLeftX || x == Config.mBubbleSnapRightX) {
                                MainController.switchState(MainController.STATE_BubbleView);
                            } else {
                                MainController.STATE_SnapToEdge.init(b);
                                MainController.switchState(MainController.STATE_SnapToEdge);
                            }
                        }
                        b.setExactPos((int) x, (int) y);
                    }
/*                    break;
            }*/
        } else {
            if (mBubble.getXPos() == mTargetInfo.mTargetX && mBubble.getYPos() == mTargetInfo.mTargetY) {
                MainController.STATE_SnapToEdge.init(mBubble);
                MainController.switchState(MainController.STATE_SnapToEdge);

                //Util.Assert(false);
                //destroyBubble(mBubble, mTargetInfo.mAction);
            }
        }

        return true;
    }

    @Override
    public void OnExitState() {
        Config.BUBBLE_HOME_X = mBubble.getXPos();
        Config.BUBBLE_HOME_Y = mBubble.getYPos();
        MainController.setAllBubblePositions(mBubble);
        mBubble = null;
    }

    @Override
    public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
    }

    @Override
    public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
    }

    @Override
    public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
    }

    @Override
    public boolean OnNewBubble(Bubble bubble) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void OnDestroyBubble(Bubble bubble) {
        Util.Assert(false);
    }

    @Override
    public void OnOrientationChanged() {
        Util.Assert(false);
    }

    @Override
    public void OnCloseDialog() {
        Util.Assert(false);
    }

    @Override
    public String getName() {
        return "Flick";
    }
}
