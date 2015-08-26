package com.linkbubble.physics;

/**
 * Created by gw on 23/10/13.
 */
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
