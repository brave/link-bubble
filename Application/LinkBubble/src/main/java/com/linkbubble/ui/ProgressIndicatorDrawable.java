package com.linkbubble.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

public class ProgressIndicatorDrawable extends Drawable {

    private final RectF mBounds = new RectF();
    private Paint mPaint;

    private float mBorderWidth;
    private int mColor;
    private float mProgress;

    ProgressIndicatorDrawable(int color, float borderWidth) {
        mColor = color;
        mBorderWidth = borderWidth;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(borderWidth);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        mPaint.setColor(0x77000000 + mColor);
        canvas.drawArc(mBounds, 0, 360, false, mPaint);

        float sweep = 360.f * mProgress;
        mPaint.setColor(mColor);
        canvas.drawArc(mBounds, -90, sweep, false, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mBounds.left = bounds.left + mBorderWidth / 2f + .5f;
        mBounds.right = bounds.right - mBorderWidth / 2f - .5f;
        mBounds.top = bounds.top + mBorderWidth / 2f + .5f;
        mBounds.bottom = bounds.bottom - mBorderWidth / 2f - .5f;
    }

    public void setColor(Integer rgb) {
        mColor = rgb;
        mPaint.setColor(rgb);
        invalidateSelf();
    }

    public void setProgress(int progress) {
        mProgress = (float)progress / 100.f;
        invalidateSelf();
    }

    public float getProgress() {
        return mProgress;
    }
}