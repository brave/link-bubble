package com.linkbubble.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.linkbubble.R;

import java.net.URL;

public class ProgressIndicator extends FrameLayout {

    private int mMax;
    private int mProgress;
    private URL mUrl;

    private ProgressImageView mIndicatorImage;
    private ProgressArcView mProgressArcView;

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
        if (isInEditMode()) {
            return;
        }

        mMax = 100;
        mProgress = 0;

        mProgressArcView = new ProgressArcView(getContext());
        int bubbleProgressSize = getResources().getDimensionPixelSize(R.dimen.bubble_progress_size);
        FrameLayout.LayoutParams arcLP = new LayoutParams(bubbleProgressSize, bubbleProgressSize);
        arcLP.gravity = Gravity.CENTER;
        addView(mProgressArcView, arcLP);

        mIndicatorImage = new ProgressImageView(getContext());
        mIndicatorImage.setImageResource(R.drawable.loading_dots);

        FrameLayout.LayoutParams indicatorLP = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        indicatorLP.gravity = Gravity.CENTER;
        addView(mIndicatorImage, indicatorLP);
    }

    void showProgressSpinner(boolean show) {
        mIndicatorImage.setVisibility(show ? VISIBLE : GONE);
    }

    boolean isProgressSpinnerShowing() {
        return mIndicatorImage.getVisibility() == VISIBLE;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
        invalidate();
    }

    public URL getUrl() {
        return mUrl;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(boolean show, int progress, URL url) {

        //if (show && progress < 100) {
        //    mIndicatorImage.setVisibility(VISIBLE);
        //} else {
        //    mIndicatorImage.setVisibility(GONE);
        //}

        mUrl = url;
        mProgress = progress;
        mProgressArcView.setProgress(show == false ? 100 : progress, mMax, url);
    }

    boolean isIndicatorShowing() {
        return mIndicatorImage.getVisibility() == VISIBLE;
    }

    private class ProgressImageView extends ImageView {

        private ObjectAnimator mAnimator = ObjectAnimator.ofFloat(this, "rotation", 0.0f, 359.0f);

        public ProgressImageView(Context context) {
            super(context);

            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setRepeatCount(Animation.INFINITE);
            mAnimator.setDuration(1000);
            mAnimator.start();
        }

        @Override
        public void setVisibility(int visibility) {
            int oldVisibility = getVisibility();
            if (visibility != oldVisibility) {
                if (visibility == VISIBLE) {
                    mAnimator.start();
                } else {
                    mAnimator.cancel();
                }
            }

            super.setVisibility(visibility);
        }
    }

    private static class ProgressArcView extends View {
        private Paint mPaint;
        //private Paint mFramePaint;
        private RectF mOval;
        //private float mStart = -90;
        //private float mSweep;
        private float mProgress;
        private static final float SWEEP_INC = 2.5f;
        private static final float START_INC = 0;
        private String mUrl;

        public ProgressArcView(Context context) {
            super(context);

            Resources resources = context.getResources();
            int strokeWidth = resources.getDimensionPixelSize(R.dimen.bubble_progress_stroke);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            //mPaint.setColor(resources.getColor(android.R.color.holo_purple));
            mPaint.setColor(resources.getColor(R.color.color_neutral));
            mPaint.setStrokeWidth(strokeWidth);

            int size = resources.getDimensionPixelSize(R.dimen.bubble_progress_size) - strokeWidth;
            float offset = (float)(strokeWidth)/2.f;
            mOval = new RectF(offset, offset, size+offset, size+offset);

            //mFramePaint = new Paint();
            //mFramePaint.setAntiAlias(true);
            //mFramePaint.setStrokeWidth(strokeWidth);

            mPaint.setStyle(Paint.Style.STROKE);
            //mFramePaint.setStyle(Paint.Style.STROKE);
        }

        void setProgress(int progress, int maxProgress, URL url) {
            float progressN = (float)progress / (float)maxProgress;
            String urlAsString = url.toString();

            // If the url is the same, and currently we're at 100%, and this progress is < 100%,
            // don't change the visual arc as it just looks messy.
            if (progress != 0 && mProgress >= .999f && progressN < .999f && mUrl.equals(urlAsString)) {
                return;
            }
            mUrl = urlAsString;

            mProgress = progressN;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float sweep = 360.f * mProgress;
            canvas.drawArc(mOval, -90, sweep, false, mPaint);

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
