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
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ProgressIndicator extends FrameLayout {

    private int mMax;
    private int mProgress;

    private ImageView mIndicator;
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

        mRotationAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotationAnimation.setInterpolator(new LinearInterpolator());
        mRotationAnimation.setRepeatCount(Animation.INFINITE);
        mRotationAnimation.setDuration(1000);

        mArcView = new ArcView(getContext());
        int bubbleProgressSize = getResources().getDimensionPixelSize(R.dimen.bubble_progress_size);
        FrameLayout.LayoutParams arcLP = new LayoutParams(bubbleProgressSize, bubbleProgressSize);
        arcLP.gravity = Gravity.CENTER;
        addView(mArcView, arcLP);
        mArcView.startDraw(Paint.Style.STROKE, false);

        mIndicator = new ImageView(getContext());
        mIndicator.setImageResource(R.drawable.loading_dots);
        FrameLayout.LayoutParams indicatorLP = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        indicatorLP.gravity = Gravity.CENTER;
        addView(mIndicator, indicatorLP);
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

    public void setProgress(boolean show, int progress) {

        if (show && progress < 100) {
            mIndicator.setVisibility(VISIBLE);
            if (mIndicator.getAnimation() == null) {
                mIndicator.setAnimation(mRotationAnimation);
                mRotationAnimation.start();
            }
        } else {
            mIndicator.setVisibility(GONE);
            if (mIndicator.getAnimation() != null) {
                mRotationAnimation.cancel();
                mIndicator.setAnimation(null);
            }
        }

        mProgress = progress;
        mArcView.setProgress(show == false ? 100 : progress, mMax);
    }


    private static class ArcView extends View {
        private Paint mPaint;
        private Paint mFramePaint;
        private RectF mOval;
        //private float mStart = -90;
        //private float mSweep;
        private float mProgress;
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
            mPaint.setColor(resources.getColor(R.color.bubble_border));
            mPaint.setStrokeWidth(strokeWidth);

            int size = resources.getDimensionPixelSize(R.dimen.bubble_progress_size) - strokeWidth;
            float offset = (float)(strokeWidth)/2.f;
            mOval = new RectF(offset, offset, size+offset, size+offset);

            mFramePaint = new Paint();
            mFramePaint.setAntiAlias(true);
            mFramePaint.setStrokeWidth(strokeWidth);
        }

        public void startDraw(Paint.Style style, boolean use_centre) {
            mUseCenter = use_centre;
            mPaint.setStyle(style);
            mFramePaint.setStyle(style);
        }

        void setProgress(int progress, int maxProgress) {
            mProgress = (float)progress / (float)maxProgress;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float sweep = 360.f * mProgress;
            canvas.drawArc(mOval, -90, sweep, mUseCenter, mPaint);

            /*
            mSweep += SWEEP_INC;
            if (mSweep > 360) {
                mSweep -= 360;
                mStart += START_INC;
                if (mStart >= 360) {
                    mStart -= 360;
                }
            }
            invalidate();
            */
        }
    }
}
