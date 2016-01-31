/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.physics;


import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.linkbubble.MainController;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

public class DraggableHelper {

    public enum AnimationType {
        Linear,
        SmallOvershoot,
        MediumOvershoot,
        LargeOvershoot,
        DistanceProportion
    }

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
        public float rawX;
        public float rawY;
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

    // Reusable events
    TouchEvent mTouchEvent = new TouchEvent();
    MoveEvent mMoveEvent = new MoveEvent();
    ReleaseEvent mReleaseEvent = new ReleaseEvent();

    private View mView;
    private WindowManager.LayoutParams mWindowManagerParams;
    private boolean mAlive;

    // Move animation state
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;
    private AnimationType mAnimType;
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private OvershootInterpolator mOvershootInterpolatorSmall = new OvershootInterpolator(0.5f);
    private OvershootInterpolator mOvershootInterpolatorMedium = new OvershootInterpolator(1.5f);
    private OvershootInterpolator mOvershootInterpolatorLarge = new OvershootInterpolator(2.0f);

    private InternalMoveEvent mStartTouchRaw;
    private InternalMoveEvent mEndTouchRaw;

    private FlingTracker mFlingTracker = null;
    private int mStartTouchX = -1;
    private int mStartTouchY = -1;
    private AnimationEventListener mAnimationListener;

    private OnTouchActionEventListener mOnTouchActionEventListener;

    public interface AnimationEventListener {
        public void onAnimationComplete();
        public void onCancel();
    }

    public DraggableHelper(View view, WindowManager.LayoutParams windowManagerParams, boolean setOnTouchListener,
                           OnTouchActionEventListener onTouchEventListener) {
        mView = view;
        mAlive = true;
        mWindowManagerParams = windowManagerParams;
        mOnTouchActionEventListener = onTouchEventListener;

        mStartTouchRaw = new InternalMoveEvent(0, 0, 0);
        mEndTouchRaw = new InternalMoveEvent(0, 0, 0);


        if (setOnTouchListener) {
            mView.setOnTouchListener(mOnTouchListener);
        }
    }

    private void addMoveEvent(float x, float y, long t) {
        if (mStartTouchRaw.mTime == 0) {
            mStartTouchRaw.mTime = t;
            mStartTouchRaw.mX = x;
            mStartTouchRaw.mY = y;
        }
        mEndTouchRaw.mTime = t;
        mEndTouchRaw.mX = x;
        mEndTouchRaw.mY = y;
    }

    public boolean onTouchActionDown(MotionEvent event) {
        mTouchEvent.posX = mWindowManagerParams.x;
        mTouchEvent.posY = mWindowManagerParams.y;
        mTouchEvent.rawX = event.getRawX();
        mTouchEvent.rawY =  event.getRawY();

        addMoveEvent(event.getRawX(),  event.getRawY(), event.getEventTime());

        if (mOnTouchActionEventListener != null) {
            mOnTouchActionEventListener.onActionDown(mTouchEvent);
        }

        mStartTouchX = mWindowManagerParams.x;
        mStartTouchY = mWindowManagerParams.y;

        mFlingTracker = FlingTracker.obtain();
        mFlingTracker.addMovement(event);

        return true;
    }

    public boolean onTouchActionMove(MotionEvent event) {
        if (mStartTouchX == -1 && mStartTouchY == -1) {
            onTouchActionDown(event);
        }

        float touchXRaw = event.getRawX();
        float touchYRaw = event.getRawY();

        int deltaX = (int) (touchXRaw - mStartTouchRaw.mX);
        int deltaY = (int) (touchYRaw - mStartTouchRaw.mY);

        addMoveEvent(touchXRaw, touchYRaw, event.getEventTime());

        mMoveEvent.dx = deltaX;
        mMoveEvent.dy = deltaY;
        mMoveEvent.rawX = touchXRaw;
        mMoveEvent.rawY = touchYRaw;
        if (mOnTouchActionEventListener != null) {
            mOnTouchActionEventListener.onActionMove(mMoveEvent);
        }

        event.offsetLocation(mWindowManagerParams.x - mStartTouchX, mWindowManagerParams.y - mStartTouchY);
        mFlingTracker.addMovement(event);

        return true;
    }

    public boolean hasAtLeast2TouchEvents() {
        return mStartTouchRaw.mTime != 0 && mEndTouchRaw.mTime != 0 && mEndTouchRaw.mTime != mStartTouchRaw.mTime;
    }

    public boolean onTouchActionUp(MotionEvent event) {
        mReleaseEvent.posX = mWindowManagerParams.x;
        mReleaseEvent.posY = mWindowManagerParams.y;
        mReleaseEvent.vx = 0.0f;
        mReleaseEvent.vy = 0.0f;
        mReleaseEvent.rawX = event.getRawX();
        mReleaseEvent.rawY = event.getRawY();

        if (hasAtLeast2TouchEvents()) {
            float touchTime = (mEndTouchRaw.mTime - mStartTouchRaw.mTime) / 1000.0f;
            mReleaseEvent.vx = (mEndTouchRaw.mX - mStartTouchRaw.mX) / touchTime;
            mReleaseEvent.vy = (mEndTouchRaw.mY - mStartTouchRaw.mY) / touchTime;
        }

        // *Should* always be true, but under certain circumstances, is not. #384
        if (mFlingTracker != null) {
            mFlingTracker.computeCurrentVelocity(1000);
            float fvx = mFlingTracker.getXVelocity();
            float fvy = mFlingTracker.getYVelocity();

            mReleaseEvent.vx = fvx;
            mReleaseEvent.vy = fvy;

            mFlingTracker.recycle();
        }

        if (mOnTouchActionEventListener != null) {
            mOnTouchActionEventListener.onActionUp(mReleaseEvent);
        }

        mStartTouchX = -1;
        mStartTouchY = -1;
        mStartTouchRaw.mTime = 0;
        mEndTouchRaw.mTime = 0;
        return true;
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(android.view.View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    return onTouchActionDown(event);
                }
                case MotionEvent.ACTION_MOVE: {
                    return onTouchActionMove(event);
                }
                case MotionEvent.ACTION_UP: {
                    return onTouchActionUp(event);
                }
                //case MotionEvent.ACTION_CANCEL: {
                //    return true;
                //}
            }

            return false;
        }
    };

    public void cancelAnimation() {
        AnimationEventListener listener = mAnimationListener;
        mAnimationListener = null;

        clearTargetPos();

        if (listener != null) {
            listener.onCancel();
        }
    }

    public float getAnimCompleteFraction() {
        float f = 1.0f;

        if (mAnimPeriod > 0.0f) {
            f = Util.clamp(0.0f, mAnimTime / mAnimPeriod, 1.0f);
        }

        return f;
    }

    public void clearTargetPos() {
        // TODO: This probably fires. It can be disabled temporarily if a pain, but should be fixed.
        Util.Assert(mAnimationListener == null, "non-null mAnimationListener");

        mInitialX = -1;
        mInitialY = -1;

        mTargetX = mWindowManagerParams.x;
        mTargetY = mWindowManagerParams.y;

        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
    }

    public void setExactPos(int x, int y) {
        if ( mWindowManagerParams.x == x && mWindowManagerParams.y == y) {
            return;
        }
        mWindowManagerParams.x = x;
        mWindowManagerParams.y = y;
        mTargetX = x;
        mTargetY = y;

        if (mAlive) {
            MainController.updateRootWindowLayout(mView, mWindowManagerParams);
        }
    }

    public void setTargetPos(int x, int y, float t, AnimationType type, AnimationEventListener listener) {
        try {
            Util.Assert(mAnimationListener == null, "non-null mAnimationListener");
        }
        catch (AssertionError exc) {
            CrashTracking.logHandledException(exc);
        }
        mAnimationListener = listener;

        if (x != mTargetX || y != mTargetY) {

            if (type == AnimationType.DistanceProportion) {
                // Something > 0.016 will have a high likelihood of causing < 60fps
                float maxTime = 0.005f;
                float maxDistance = 50.0f;

                float d = Util.distance(x, y, mWindowManagerParams.x, mWindowManagerParams.y);
                t = maxTime * d / maxDistance;
                t = maxTime - Util.clamp(0.0f, t, maxTime);
                type = AnimationType.Linear;
            }

            if (t < 0.0001f) {
                clearTargetPos();
                setExactPos(x, y);
            } else {
                mAnimType = type;

                mInitialX = mWindowManagerParams.x;
                mInitialY = mWindowManagerParams.y;

                mTargetX = x;
                mTargetY = y;

                mAnimPeriod = t;
                mAnimTime = 0.0f;
            }

            if (MainController.get() != null) {
                MainController.get().scheduleUpdate();
            }
        } else if (listener != null) {
            mAnimationListener = null;
            listener.onAnimationComplete();
        }
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

    public boolean isAlive() { return mAlive; }

    public View getView() { return mView; }

    public boolean update(float dt) {
        if (mAnimTime < mAnimPeriod) {
            Util.Assert(mAnimPeriod > 0.0f, "mAnimPeriod:" + mAnimPeriod);

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod);

            float tf = mAnimTime / mAnimPeriod;
            float interpolatedFraction = 0.0f;
            switch (mAnimType) {
                case Linear:
                    interpolatedFraction = mLinearInterpolator.getInterpolation(tf);
                    break;
                case SmallOvershoot:
                    interpolatedFraction = mOvershootInterpolatorSmall.getInterpolation(tf);
                    break;
                case MediumOvershoot:
                    interpolatedFraction = mOvershootInterpolatorMedium.getInterpolation(tf);
                    break;
                case LargeOvershoot:
                    interpolatedFraction = mOvershootInterpolatorLarge.getInterpolation(tf);
                    break;
            }

            int x = (int) (mInitialX + (mTargetX - mInitialX) * interpolatedFraction);
            int y = (int) (mInitialY + (mTargetY - mInitialY) * interpolatedFraction);

            if ( mWindowManagerParams.x != x || mWindowManagerParams.y != y) {
                mWindowManagerParams.x = x;
                mWindowManagerParams.y = y;
                MainController.updateRootWindowLayout(mView, mWindowManagerParams);
            }

            MainController.get().scheduleUpdate();

            if (mAnimTime >= mAnimPeriod) {
                mAnimTime = 0.0f;
                mAnimPeriod = 0.0f;
                if (mAnimationListener != null) {
                    AnimationEventListener l = mAnimationListener;
                    mAnimationListener = null;
                    l.onAnimationComplete();
                }
            }

            return true;
        }

        return false;
    }

    public void destroy() {
        MainController.removeRootWindow(mView);
        mAlive = false;
    }
}