package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.squareup.otto.Subscribe;


public class CloseTabTargetView extends BubbleTargetView {

    CloseAllView mCloseAllView;

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
    }

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
            mPaint.setTextSize(36);
            mPaint.setTypeface(Typeface.SERIF);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setColor(Color.WHITE);

            int textCircleSize = getResources().getDimensionPixelSize(R.dimen.close_tab_text_circle_size);
            int closeTabSize = getResources().getDimensionPixelSize(R.dimen.close_tab_target_size);
            int start = (closeTabSize - textCircleSize) / 2;

            RectF circle = new RectF();
            circle.set(start, start, start + textCircleSize, start + textCircleSize);

            mPath = new Path();
            mPath.addArc(circle, 180, 180);

            mText = getResources().getString(R.string.action_close_all).toUpperCase();
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawTextOnPath(mText, mPath, 0, 0, mPaint);
        }
    }
}
