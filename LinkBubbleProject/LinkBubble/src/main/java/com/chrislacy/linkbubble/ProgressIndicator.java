package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class ProgressIndicator extends FrameLayout {

    private int mMax;
    private int mProgress;

    private View mIndicator;
    private Animation mRotationAnimation;

    private Paint mProgressPaint;
    private RectF mTempRectF = new RectF();

    private int mInnerSize;

    public ProgressIndicator(Context context) {
        this(context, null);
    }

    public ProgressIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mMax = 100;
        mProgress = 0;

        final Resources res = getResources();

        int progressColor = res.getColor(R.color.progress_default_progress_color);

        mInnerSize = res.getDimensionPixelSize(R.dimen.progress_inner_size);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mTempRectF.set(-0.5f, -0.5f, mInnerSize + 0.5f, mInnerSize + 0.5f);
        mTempRectF.offset((getWidth() - mInnerSize) / 2, (getHeight() - mInnerSize) / 2);

        canvas.drawArc(mTempRectF, -90, 360 * mProgress / mMax, true, mProgressPaint);
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
        invalidate();
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {

        Log.d("blerg", "progress: " + progress);
        if (mIndicator == null) {
            mIndicator = findViewById(R.id.indicator_image);

            RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mIndicator.setAnimation(rotate);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setDuration(1000);
        }

        mProgress = progress;
        invalidate();
    }
}
