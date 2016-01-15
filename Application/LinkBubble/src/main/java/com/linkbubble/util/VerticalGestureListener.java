/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;


import android.view.GestureDetector;
import android.view.MotionEvent;

import com.linkbubble.Config;

public class VerticalGestureListener extends GestureDetector.SimpleOnGestureListener {

    public enum GestureDirection {
        None,
        Up,
        Down,
        Horizontal,
    }

    GestureDirection mLastGestureDirection;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null ||  e2 == null) {
            return false;
        }
        mLastGestureDirection = GestureDirection.None;
        //final int SWIPE_MIN_DISTANCE = 50;
        final int SWIPE_MIN_DISTANCE = 5;
        final int SWIPE_THRESHOLD_VELOCITY = 80;

        int REL_SWIPE_MIN_DISTANCE = (int)(SWIPE_MIN_DISTANCE * Config.sDensityDpi / 160.0f);
        int REL_SWIPE_THRESHOLD_VELOCITY = (int)(SWIPE_THRESHOLD_VELOCITY * Config.sDensityDpi / 160.0f);

        float swipeVel = Math.abs(velocityY);
        float swipeYDelta = e1.getY() - e2.getY();
        float swipeXDelta = e1.getX() - e2.getX();

        if (Math.abs(swipeXDelta) > Math.abs(swipeYDelta) * .67f) {
            mLastGestureDirection = GestureDirection.Horizontal;
            return false;
        }

        //Log.d("input", "upDelta: " + swipeDelta + ", MIN_DISTANCE: " + REL_SWIPE_MIN_DISTANCE
        //		+ ", vel: " + swipeVel + ", THRESHOLD_VEL: " + REL_SWIPE_THRESHOLD_VELOCITY);
        if (swipeVel > REL_SWIPE_THRESHOLD_VELOCITY) {
            if (swipeYDelta > REL_SWIPE_MIN_DISTANCE) {
                mLastGestureDirection = GestureDirection.Up;
            } else if (swipeYDelta < -REL_SWIPE_MIN_DISTANCE) {
                mLastGestureDirection = GestureDirection.Down;
            }
        }
        return false;
    }

    public GestureDirection getLastGestureDirection() {
        return mLastGestureDirection;
    }

    public void resetLastGestureDirection() {
        mLastGestureDirection = GestureDirection.None;
    }
}