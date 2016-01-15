/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.linkbubble.Settings;

public class ProgressIndicatorDrawable extends Drawable {

    private final RectF mBounds = new RectF();
    private Paint mPaint;

    private float mBorderWidth;
    private float mWidth;
    private int mColor;
    private float mProgress;

    ProgressIndicatorDrawable(int color, float width, float borderWidth) {
        mColor = color;
        mWidth = width;
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

        float xOffset = ((float)bounds.right - mWidth)/2.f;
        float yOffset = ((float)bounds.bottom - mWidth)/2.f;

        mBounds.left = xOffset + mBorderWidth / 2f;
        mBounds.right = xOffset + mWidth - mBorderWidth / 2f;
        mBounds.top = yOffset + mBorderWidth / 2f;
        mBounds.bottom = yOffset + mWidth - mBorderWidth / 2f;
    }

    public void setColor(Integer rgb) {
        if (rgb == null || Settings.get().getColoredProgressIndicator() == false) {
            rgb = Settings.get().getThemedDefaultProgressColor();
        }
        //Log.d("blerg", "setColor():" + rgb);

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