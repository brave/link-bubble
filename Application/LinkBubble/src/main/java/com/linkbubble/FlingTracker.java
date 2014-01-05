package com.linkbubble;

import android.view.MotionEvent;

import java.util.ArrayDeque;
import java.util.Iterator;

public class FlingTracker {
    static final boolean DEBUG = false;
    final int MAX_EVENTS = 8;
    final float DECAY = 0.75f;
    ArrayDeque<MotionEventCopy> mEventBuf = new ArrayDeque<MotionEventCopy>(MAX_EVENTS);
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
    }
    public void addMovement(MotionEvent event) {
        if (mEventBuf.size() == MAX_EVENTS) {
            mEventBuf.remove();
        }
        mEventBuf.add(new MotionEventCopy(event.getX(), event.getY(), event.getEventTime()));
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
        for (final Iterator<MotionEventCopy> iter = mEventBuf.descendingIterator();
             iter.hasNext();) {
            final MotionEventCopy event = iter.next();
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
        mEventBuf.clear();
    }

    static FlingTracker sTracker;
    static FlingTracker obtain() {
        if (sTracker == null) {
            sTracker = new FlingTracker();
        }
        return sTracker;
    }
}
