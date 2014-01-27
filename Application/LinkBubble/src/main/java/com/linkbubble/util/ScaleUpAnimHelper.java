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

    AnimState mAnimState = AnimState.None;;
    View mView;

    public ScaleUpAnimHelper(View view) {
        mView = view;
    }

    public void show() {
        if (mView.getVisibility() != View.VISIBLE) {
            mView.animate().cancel();
            mView.setAlpha(0f);
            mView.setVisibility(View.VISIBLE);
            mView.setScaleX(0.33f);
            mView.setScaleY(0.33f);
        } else if (mAnimState == AnimState.Hiding) {
            mView.animate().cancel();
            mView.setVisibility(View.VISIBLE);
        }

        mView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(667)
                .setInterpolator(new AnticipateOvershootInterpolator())
                .setListener(mShowListener)
                .start();

    }

    public void hide() {
        mView.animate().alpha(0.f).scaleX(0.33f).scaleY(0.33f)
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
