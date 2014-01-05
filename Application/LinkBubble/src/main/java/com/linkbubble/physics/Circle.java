package com.linkbubble.physics;

/**
 * Created by gw on 23/10/13.
 */
public class Circle {
    public float mX;
    public float mY;
    public float mRadius;

    public Circle(float x, float y, float r) {
        mX = x;
        mY = y;
        mRadius = r;
    }

    public boolean Intersects(Circle c) {
        float d1 = (mX - c.mX) * (mX - c.mX) + (mY - c.mY) * (mY - c.mY);
        float d2 = (mRadius + c.mRadius) * (mRadius + c.mRadius);

        return d1 <= d2;
    }
}
