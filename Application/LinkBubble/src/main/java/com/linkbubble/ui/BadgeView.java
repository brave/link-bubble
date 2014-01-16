package com.linkbubble.ui;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.physics.Draggable;

/**
 * Created by gw on 12/10/13.
 */
public class BadgeView extends TextView {

    int mCount;

    enum AnimState {
        None,
        Hiding,
        Showing,
    }

    AnimState mAnimState;

    public BadgeView(Context context) {
        this(context, null);
    }

    public BadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BadgeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAnimState = AnimState.None;
        mCount = 0;
    }

    public void show() {
        if (getVisibility() != View.VISIBLE) {
            setAlpha(0f);
            setVisibility(View.VISIBLE);
            setScaleX(0.33f);
            setScaleY(0.33f);
        } else if (mAnimState == AnimState.Hiding) {
            animate().cancel();
            setVisibility(View.VISIBLE);
        }

        animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(667)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mShowListener)
                .start();

        Draggable activeDraggable = MainController.get().getActiveDraggable();
        if (activeDraggable != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            int x = activeDraggable.getDraggableHelper().getXPos();
            if (x > Config.mScreenCenterX) {
                lp.gravity = Gravity.TOP|Gravity.LEFT;
            } else {
                lp.gravity = Gravity.TOP|Gravity.RIGHT;
            }
        }
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
            mAnimState = AnimState.Showing;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mAnimState = AnimState.None;
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
            mAnimState = AnimState.Hiding;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            setVisibility(View.GONE);
            mAnimState = AnimState.None;
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    public void setCount(int count) {
        mCount = count;
        setText(Integer.toString(count));
    }
}
