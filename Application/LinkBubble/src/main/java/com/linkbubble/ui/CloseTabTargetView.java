package com.linkbubble.ui;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.squareup.otto.Subscribe;

import java.util.Locale;


public class CloseTabTargetView extends BubbleTargetView {

    private CloseAllView mCloseAllView;
    private LinearInterpolator mInterpolator = new LinearInterpolator();

    public CloseTabTargetView(Context context) {
        this(context, null);
    }

    public CloseTabTargetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CloseTabTargetView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mCloseAllView = (CloseAllView) findViewById(R.id.close_all_view);
        mCloseAllView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected float getRadius() {
        int closeTabSize = getResources().getDimensionPixelSize(R.dimen.close_tab_target_size);
        return closeTabSize * 0.5f;
    }

    private static final int ANIM_DURATION = 100;
    private static final float MIN_SCALE = .7f;

    @Override
    public void beginLongHovering() {
        super.beginLongHovering();

        mCloseAllView.setAlpha(0f);
        mCloseAllView.setVisibility(View.VISIBLE);
        mCloseAllView.setScaleX(MIN_SCALE);
        mCloseAllView.setScaleY(MIN_SCALE);
        mCloseAllView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIM_DURATION)
                    .setInterpolator(mInterpolator)
                    .setListener(null);
    }

    @Override
    public void endLongHovering() {
        mCloseAllView.animate()
                .alpha(0.f)
                .scaleX(MIN_SCALE)
                .scaleY(MIN_SCALE)
                .setDuration(ANIM_DURATION)
                .setInterpolator(mInterpolator)
                .setListener(mHideCloseAllViewListener);

        super.endLongHovering();
    }

    private Animator.AnimatorListener mHideCloseAllViewListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mCloseAllView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    @Override
    protected void registerForBus() {
        MainApplication.registerForBus(getContext(), this);
    }

    @Override
    protected void unregisterForBus() {
        MainApplication.unregisterForBus(getContext(), this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginBubbleDrag(MainController.BeginBubbleDragEvent e) {
        super.onBeginBubbleDrag(e);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndBubbleDragEvent(MainController.EndBubbleDragEvent e) {
        super.onEndBubbleDragEvent(e);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDraggableBubbleMovedEvent(MainController.DraggableBubbleMovedEvent e) {
        super.onDraggableBubbleMovedEvent(e);
    }

    public static class CloseAllView extends View {

        private Path mPath;
        private Paint mPaint;
        private String mText;

        public CloseAllView(Context context) {
            this(context, null);
        }

        public CloseAllView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CloseAllView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setTextSize(Config.dpToPx(12));
            mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD));
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setColor(Color.WHITE);

            int textCircleSize = getResources().getDimensionPixelSize(R.dimen.close_tab_text_circle_size);
            int closeTabSize = getResources().getDimensionPixelSize(R.dimen.close_tab_target_size);
            int start = (closeTabSize - textCircleSize) / 2;

            RectF circle = new RectF();
            circle.set(start, start, start + textCircleSize, start + textCircleSize);

            mPath = new Path();
            mPath.addArc(circle, 180, 180);

            mText = getResources().getString(R.string.action_close_all).toUpperCase(Locale.getDefault());
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawTextOnPath(mText, mPath, 0, 0, mPaint);
        }
    }
}
