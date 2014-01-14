package com.linkbubble.physics;


import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.util.Util;

import java.util.Vector;

public class DraggableHelper {

    public interface OnTouchActionEventListener {
        void onActionDown(TouchEvent event);
        void onActionMove(MoveEvent event);
        void onActionUp(ReleaseEvent event);
    }

    public static class TouchEvent {
        public int posX;
        public int posY;
        public float rawX;
        public float rawY;
    }

    public static class MoveEvent {
        public int dx;
        public int dy;
    }

    public static class ReleaseEvent {
        public int posX;
        public int posY;
        public float vx;
        public float vy;
        public float rawX;
        public float rawY;
    }

    private static class InternalMoveEvent {

        public InternalMoveEvent(float x, float y, long t) {
            mX = x;
            mY = y;
            mTime = t;
        }

        public long mTime;
        public float mX, mY;
    }

    private View mView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams;
    private boolean mAlive;

    // Move animation state
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;
    private boolean mOvershoot;
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator();

    private Vector<InternalMoveEvent> mMoveEvents = new Vector<InternalMoveEvent>();
    private FlingTracker mFlingTracker = null;

    private OnTouchActionEventListener mOnTouchActionEventListener;

    public DraggableHelper(View view, WindowManager windowManager, WindowManager.LayoutParams windowManagerParams, boolean setOnTouchListener,
                           OnTouchActionEventListener onTouchEventListener) {
        mView = view;
        mAlive = true;
        mWindowManager = windowManager;
        mWindowManagerParams = windowManagerParams;
        mOnTouchActionEventListener = onTouchEventListener;

        if (setOnTouchListener) {
            mView.setOnTouchListener(mOnTouchListener);
        }
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        private float mStartTouchXRaw;
        private float mStartTouchYRaw;
        private int mStartTouchX;
        private int mStartTouchY;

        @Override
        public boolean onTouch(android.view.View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mStartTouchXRaw = event.getRawX();
                    mStartTouchYRaw = event.getRawY();
                    DraggableHelper.TouchEvent e = new DraggableHelper.TouchEvent();
                    e.posX = mWindowManagerParams.x;
                    e.posY = mWindowManagerParams.y;
                    e.rawX = mStartTouchXRaw;
                    e.rawY = mStartTouchYRaw;

                    mMoveEvents.clear();
                    InternalMoveEvent me = new InternalMoveEvent(mStartTouchXRaw, mStartTouchYRaw, event.getEventTime());
                    mMoveEvents.add(me);

                    if (mOnTouchActionEventListener != null) {
                        mOnTouchActionEventListener.onActionDown(e);
                    }

                    mStartTouchX = mWindowManagerParams.x;
                    mStartTouchY = mWindowManagerParams.y;

                    mFlingTracker = FlingTracker.obtain();
                    mFlingTracker.addMovement(event);

                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float touchXRaw = event.getRawX();
                    float touchYRaw = event.getRawY();

                    int deltaX = (int) (touchXRaw - mStartTouchXRaw);
                    int deltaY = (int) (touchYRaw - mStartTouchYRaw);

                    InternalMoveEvent me = new InternalMoveEvent(touchXRaw, touchYRaw, event.getEventTime());
                    mMoveEvents.add(me);

                    DraggableHelper.MoveEvent e = new DraggableHelper.MoveEvent();
                    e.dx = deltaX;
                    e.dy = deltaY;
                    if (mOnTouchActionEventListener != null) {
                        mOnTouchActionEventListener.onActionMove(e);
                    }

                    event.offsetLocation(mWindowManagerParams.x - mStartTouchX, mWindowManagerParams.y - mStartTouchY);
                    mFlingTracker.addMovement(event);

                    return true;
                }
                case MotionEvent.ACTION_UP: {

                    DraggableHelper.ReleaseEvent e = new DraggableHelper.ReleaseEvent();
                    e.posX = mWindowManagerParams.x;
                    e.posY = mWindowManagerParams.y;
                    e.vx = 0.0f;
                    e.vy = 0.0f;
                    e.rawX = event.getRawX();
                    e.rawY = event.getRawY();

                    if (mMoveEvents.size() > 0) {
                        float firstMs = mMoveEvents.get(0).mTime;

                        for (int i = 0; i < mMoveEvents.size(); ++i) {
                            InternalMoveEvent me = mMoveEvents.get(i);
                            float ms = me.mTime - firstMs;
                        }
                    }

                    int moveEventCount = mMoveEvents.size();
                    if (moveEventCount > 2) {
                        InternalMoveEvent lastME = mMoveEvents.lastElement();
                        InternalMoveEvent refME = null;

                        for (int i = moveEventCount - 1; i >= 0; --i) {
                            InternalMoveEvent me = mMoveEvents.get(i);

                            if (lastME.mTime == me.mTime)
                                continue;

                            refME = me;

                            float touchTime = (lastME.mTime - me.mTime) / 1000.0f;
                            if (touchTime > 0.03f) {
                                break;
                            }
                        }

                        if (refME != null) {
                            Util.Assert(refME.mTime != lastME.mTime);
                            float touchTime = (lastME.mTime - refME.mTime) / 1000.0f;
                            e.vx = (lastME.mX - refME.mX) / touchTime;
                            e.vy = (lastME.mY - refME.mY) / touchTime;
                        }
                    }

                    mFlingTracker.computeCurrentVelocity(1000);
                    float fvx = mFlingTracker.getXVelocity();
                    float fvy = mFlingTracker.getYVelocity();

                    e.vx = fvx;
                    e.vy = fvy;

                    mFlingTracker.recycle();

                    if (mOnTouchActionEventListener != null) {
                        mOnTouchActionEventListener.onActionUp(e);
                    }

                    return true;
                }
                //case MotionEvent.ACTION_CANCEL: {
                //    return true;
                //}
            }

            return false;
        }
    };

    public void clearTargetPos() {
        mInitialX = -1;
        mInitialY = -1;

        mTargetX = mWindowManagerParams.x;
        mTargetY = mWindowManagerParams.y;

        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
    }

    public void setExactPos(int x, int y) {
        mWindowManagerParams.x = x;
        mWindowManagerParams.y = y;
        mTargetX = x;
        mTargetY = y;
        if (mAlive) {
            mWindowManager.updateViewLayout(mView, mWindowManagerParams);
        }
    }

    public void setTargetPos(int x, int y, float t, boolean overshoot) {
        if (x != mTargetX || y != mTargetY) {
            mOvershoot = overshoot;

            mInitialX = mWindowManagerParams.x;
            mInitialY = mWindowManagerParams.y;

            mTargetX = x;
            mTargetY = y;

            mAnimPeriod = t;
            mAnimTime = 0.0f;

            MainController.get().scheduleUpdate();
        }
    }

    public CanvasView.TargetInfo getTargetInfo(CanvasView canvasView, int x, int y) {
        Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                y + Config.mBubbleHeight * 0.5f,
                Config.mBubbleWidth * 0.5f);
        CanvasView.TargetInfo targetInfo = canvasView.getBubbleAction(bubbleCircle);
        return targetInfo;
    }

    public Config.BubbleAction doSnap(CanvasView canvasView, int targetX, int targetY) {
        CanvasView.TargetInfo targetInfo = getTargetInfo(canvasView, targetX, targetY);

        if (targetInfo.mAction != Config.BubbleAction.None) {
            setTargetPos((int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f),
                    (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f),
                    0.3f, true);
        } else {
            setTargetPos(targetX, targetY, 0.02f, false);
        }

        return targetInfo.mAction;
    }

    public int getXPos() {
        return mWindowManagerParams.x;
    }

    public int getYPos() {
        return mWindowManagerParams.y;
    }

    public WindowManager.LayoutParams getWindowManagerParams() {
        return mWindowManagerParams;
    }

    public boolean isSnapping() {
        return mOvershoot;
    }

    public boolean isAlive() { return mAlive; }

    public View getView() { return mView; }

    public boolean update(float dt, boolean contentView) {
        if (mAnimTime < mAnimPeriod) {
            Util.Assert(mAnimPeriod > 0.0f);

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod);

            float tf = mAnimTime / mAnimPeriod;
            float interpolatedFraction;
            if (mOvershoot) {
                interpolatedFraction = mOvershootInterpolator.getInterpolation(tf);
                //Log.e("GT", "t = " + tf + ", f = " + interpolatedFraction);
            } else {
                interpolatedFraction = mLinearInterpolator.getInterpolation(tf);
                Util.Assert(interpolatedFraction >= 0.0f && interpolatedFraction <= 1.0f);
            }

            int x = (int) (mInitialX + (mTargetX - mInitialX) * interpolatedFraction);
            int y = (int) (mInitialY + (mTargetY - mInitialY) * interpolatedFraction);

            mWindowManagerParams.x = x;
            mWindowManagerParams.y = y;
            mWindowManager.updateViewLayout(mView, mWindowManagerParams);

            MainController.get().scheduleUpdate();

            return true;
        }

        return false;
    }

    public void destroy() {
        mWindowManager.removeView(mView);
        mAlive = false;
    }
}
