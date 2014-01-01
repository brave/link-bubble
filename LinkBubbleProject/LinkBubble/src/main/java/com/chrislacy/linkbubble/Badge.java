package com.chrislacy.linkbubble;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;

/**
 * Created by gw on 12/10/13.
 */
public class Badge extends TextView {

    private BubbleView mBubble;

    public Badge(Context context) {
        this(context, null);
    }

    public Badge(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Badge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void show() {
        setAlpha(0f);
        setVisibility(View.VISIBLE);
        setScaleX(0.33f);
        setScaleY(0.33f);
        animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(667)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mShowListener)
                .start();
    }

    public void hide() {
        animate().alpha(0.f).scaleX(0.33f).scaleY(0.33f)
                .setDuration(500)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mHideListener)
                .start();
    }

    // Empty listener is set so that the mHideListener is not still used, potentially setting the view visibilty as GONE
    private Animator.AnimatorListener mShowListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    private Animator.AnimatorListener mHideListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public void setBubbleCount(int count) {
        if (count < 2) {
            if (mBubble != null)
                mBubble.detachBadge();
        } else {
            setText(Integer.toString(count));
            if (mBubble != null)
                mBubble.attachBadge(this);
        }
    }

    public void attach(BubbleView bubble) {
        if (mBubble != null) {
            mBubble.detachBadge();
        }

        if (bubble != null) {
            mBubble = bubble;
            mBubble.attachBadge(this);
        }
    }
}
