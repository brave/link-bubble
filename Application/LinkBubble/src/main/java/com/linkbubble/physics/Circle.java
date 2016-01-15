/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.physics;

public class Circle {
    public float mX;
    public float mY;
    public float mRadius;

    public Circle(float x, float y, float r) {
        Update(x, y, r);
    }

    public void Update(float x, float y, float r) {
        mX = x;
        mY = y;
        mRadius = r;
    }

    public boolean Intersects(Circle c, float radiusScaler) {
        float r0 = mRadius * radiusScaler;
        float r1 = c.mRadius * radiusScaler;

        float d1 = (mX - c.mX) * (mX - c.mX) + (mY - c.mY) * (mY - c.mY);
        float d2 = (r0 + r1) * (r0 + r1);

        return d1 <= d2;
    }
}
