package com.linkbubble.physics;

import android.view.MotionEvent;

import java.util.ArrayDeque;
import java.util.Iterator;

public class FlingTracker {
    static final boolean DEBUG = false;
    final int MAX_EVENTS = 8;
    final float DECAY = 0.75f;

    private int mEventBufSize = 0;
    private int mEventBufPos = 0;
    MotionEventCopy[] mEventBuf = new MotionEventCopy[MAX_EVENTS];
    float mVX, mVY = 0;
    private static class MotionEventCopy {
        public MotionEventCopy(float x2, float y2, long eventTime) {
            this.x = x2;
            this.y = y2;
            this.t = eventTime;
        }
        public float x, y;
        public long t;
    }
    public FlingTracker() {
        for (int i = 0; i < mEventBuf.length; ++i) {
            mEventBuf[i] = new MotionEventCopy(0, 0, 0);
        }
    }
    public void addMovement(MotionEvent event) {
        if (mEventBufSize == MAX_EVENTS) {
            mEventBufPos = (mEventBufPos + 1) % MAX_EVENTS;
        } else {
            mEventBufSize++;
        }

        MotionEventCopy me = mEventBuf[(mEventBufSize - 1 + mEventBufPos) % MAX_EVENTS];
        me.x = event.getX();
        me.y = event.getY();
        me.t = event.getEventTime();
    }
    public void computeCurrentVelocity(long timebase) {
        //if (FlingTracker.DEBUG) {
        //    Slog.v("FlingTracker", "computing velocities for " + mEventBuf.size() + " events");
        //}
        mVX = mVY = 0;
        MotionEventCopy last = null;
        int i = 0;
        int j = 0;
        float totalweight = 0f;
        float weight = 10f;
        for (int x = 0; x < mEventBufSize; x++) {
            MotionEventCopy event = mEventBuf[(MAX_EVENTS + mEventBufSize  + mEventBufPos - 1 - x) % MAX_EVENTS];
            if (last != null) {
                final float dt = (float) (event.t - last.t) / timebase;
                if (dt == 0) {
                    last = event;
                    continue;
                }
                final float dx = (event.x - last.x);
                final float dy = (event.y - last.y);
                //if (FlingTracker.DEBUG) {
                //    Slog.v("FlingTracker", String.format(" [%d] dx=%.1f dy=%.1f dt=%.0f vx=%.1f vy=%.1f",
                //            i,
                //            dx, dy, dt,
                //            (dx/dt),
                //            (dy/dt)
                //    ));
                //}
                mVX += weight * dx / dt;
                mVY += weight * dy / dt;
                totalweight += weight;
                weight *= DECAY;
                j++;
            }
            last = event;
            i++;
        }
        if (j != 0) {
            mVX /= totalweight;
            mVY /= totalweight;
        }

        //if (FlingTracker.DEBUG) {
        //    Slog.v("FlingTracker", "computed: vx=" + mVX + " vy=" + mVY);
        //}
    }
    public float getXVelocity() {
        return mVX;
    }
    public float getYVelocity() {
        return mVY;
    }
    public void recycle() {
        mEventBufSize = 0;
        mEventBufPos = 0;
    }

    static FlingTracker sTracker;
    public static FlingTracker obtain() {
        if (sTracker == null) {
            sTracker = new FlingTracker();
        }
        return sTracker;
    }
}
