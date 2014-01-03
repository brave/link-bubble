package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

public class ProgressIndicator extends FrameLayout {

    private int mMax;
    private int mProgress;

    private View mIndicator;
    private Animation mRotationAnimation;

    ArcView mArcView;

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

        mArcView = new ArcView(getContext());
        int bubbleProgressSize = getResources().getDimensionPixelSize(R.dimen.bubble_progress_size);
        FrameLayout.LayoutParams lp = new LayoutParams(bubbleProgressSize, bubbleProgressSize);
        lp.gravity = Gravity.CENTER;
        addView(mArcView, lp);
        mArcView.startDraw(Paint.Style.STROKE, false);
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

        if (mIndicator == null) {
            mIndicator = findViewById(R.id.indicator_image);

            RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mIndicator.setAnimation(rotate);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setDuration(1000);
        }

        mProgress = progress;
    }


    private static class ArcView extends View {
        private Paint mPaint;
        private Paint mFramePaint;
        private RectF mOval;
        private float mStart;
        private float mSweep;
        private boolean mUseCenter = false;
        private static final float SWEEP_INC = 2.5f;
        private static final float START_INC = 0;

        public ArcView(Context context) {
            super(context);

            Resources resources = context.getResources();
            int strokeWidth = resources.getDimensionPixelSize(R.dimen.bubble_progress_stroke);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            //mPaint.setColor(resources.getColor(android.R.color.holo_purple));
            mPaint.setColor(resources.getColor(R.color.progress_default_progress_color));
            mPaint.setStrokeWidth(strokeWidth);

            int size = resources.getDimensionPixelSize(R.dimen.bubble_progress_size) - strokeWidth;
            int iconStrokeSize = resources.getDimensionPixelSize(R.dimen.bubble_icon_stroke_size);
            float offset = (float)(strokeWidth)/2.f + iconStrokeSize;
            mOval = new RectF(offset, offset, size, size);

            mFramePaint = new Paint();
            mFramePaint.setAntiAlias(true);
            mFramePaint.setStrokeWidth(strokeWidth);
        }

        public void startDraw(Paint.Style style, boolean use_centre) {
            mUseCenter = use_centre;
            mPaint.setStyle(style);
            mFramePaint.setStyle(style);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawArc(mOval, mStart, mSweep, mUseCenter, mPaint);

            mSweep += SWEEP_INC;
            if (mSweep > 360) {
                mSweep -= 360;
                mStart += START_INC;
                if (mStart >= 360) {
                    mStart -= 360;
                }
            }
            invalidate();
        }
    }
}
