package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class ProgressIndicator extends ProgressBar {

    private int mMax;
    private int mProgress;

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
        mProgress = progress;
        invalidate();
    }
}
