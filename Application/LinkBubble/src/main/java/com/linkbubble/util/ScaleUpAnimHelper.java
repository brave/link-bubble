package com.linkbubble.util;


import android.animation.Animator;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;

public class ScaleUpAnimHelper {

    enum AnimState {
        None,
        Hiding,
        Showing,
    }

    AnimState mAnimState = AnimState.None;
    View mView;
    private float mAlpha;

    public ScaleUpAnimHelper(View view, float alpha) {
        mView = view;
        mAlpha = alpha;
    }

    public void show() {
        int duration = 667;
        if (mView.getVisibility() != View.VISIBLE) {
            duration = 500;
            mView.animate().cancel();
            mView.setAlpha(0f);
            mView.setVisibility(View.VISIBLE);
            mView.setScaleX(0.33f);
            mView.setScaleY(0.33f);
        } else if (mAnimState == AnimState.Hiding) {
            mView.animate().cancel();
            mView.setVisibility(View.VISIBLE);
        }

        mView.animate().alpha(mAlpha).scaleX(1f).scaleY(1f)
                .setDuration(duration)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mShowListener);

    }

    public void hide() {
        mView.animate().alpha(0.f).scaleX(0.33f).scaleY(0.33f)
                .setDuration(500)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mHideListener);
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
            mView.setVisibility(View.GONE);
            mAnimState = AnimState.None;
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

}
