package com.linkbubble.ui;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.linkbubble.MainApplication;
import com.linkbubble.R;

class TrialTimeView extends View {
    private Paint mBasePaint;
    private Paint mElapsedPaint;
    private Paint mOutlinePaint;
    private RectF mOval;
    private float mProgress;

    public TrialTimeView(Context context) {
        this(context, null);
    }

    public TrialTimeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrialTimeView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);

        Resources resources = context.getResources();
        int strokeWidth = resources.getDimensionPixelSize(R.dimen.bubble_progress_stroke);

        mBasePaint = new Paint();
        mBasePaint.setAntiAlias(true);
        mBasePaint.setColor(resources.getColor(android.R.color.white));
        mBasePaint.setStyle(Paint.Style.FILL);

        mElapsedPaint = new Paint();
        mElapsedPaint.setAntiAlias(true);
        mElapsedPaint.setColor(resources.getColor(android.R.color.holo_orange_dark));
        mElapsedPaint.setStyle(Paint.Style.FILL);

        mOutlinePaint = new Paint();
        mOutlinePaint.setAntiAlias(true);
        mOutlinePaint.setColor(resources.getColor(R.color.bubble_border));
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(strokeWidth);

        int size = resources.getDimensionPixelSize(R.dimen.bubble_progress_size) - strokeWidth;
        mOval = new RectF(getPaddingLeft(), getPaddingTop(), size+getPaddingLeft()+getPaddingRight(), size+getPaddingTop()+getPaddingBottom());

        mProgress = .5f;

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeRemainingPrompt();
            }
        });
    }

    public void showTimeRemainingPrompt() {
        long trialTimeRemaining = MainApplication.getTrialTimeRemaining();
        final long minute = 60 * 1000;
        final long hour = 60 * minute;
        long hoursLeft = trialTimeRemaining / hour;
        long minutesLeft = (trialTimeRemaining - (hour * hoursLeft))/ minute;
        String timeLeft = null;
        if (hoursLeft > 0) {
            timeLeft = hoursLeft + "H, " + minutesLeft + "M";
        } else if (minutesLeft > -1) {
            timeLeft = minutesLeft + "M";
        }

        if (timeLeft != null) {
            String message = String.format(getResources().getString(R.string.trial_time_on_click), timeLeft);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    void setProgress(float progress) {
        Log.d("Trial", "setProgress():" + progress);
        mProgress = Math.max(.01f, progress);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float sweep = 360.f * mProgress;

        if (mProgress < 1.f) {
            canvas.drawArc(mOval, -90 + sweep, 360 - sweep, true, mBasePaint);
        }

        canvas.drawArc(mOval, -90, sweep, true, mElapsedPaint);
        canvas.drawArc(mOval, -90, 360, false, mOutlinePaint);
    }
}