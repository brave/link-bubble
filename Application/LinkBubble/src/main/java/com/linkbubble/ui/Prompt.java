package com.linkbubble.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.Config;
import com.linkbubble.R;
import com.linkbubble.util.Util;

public class Prompt {

    public interface OnPromptEventListener {
        public void onClick();
        public void onClose();
    }

    public static final int LENGTH_LONG = 5000;

    private static Prompt sPrompt;

    private boolean mVisible;
    private View mRootView;
    private View mBarView;
    private TextView mMessageView;
    private ImageButton mButton;
    private ViewPropertyAnimator mBarAnimator;
    private Handler mHideHandler = new Handler();
    private OnPromptEventListener mListener;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    public static void initModule(Context context) {
        Util.Assert(sPrompt == null);
        sPrompt = new Prompt(context);
    }

    public static void deinitModule() {
        Util.Assert(sPrompt != null);
        sPrompt = null;
    }

    private Prompt(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = li.inflate(R.layout.view_prompt, null);

        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mLayoutParams.setTitle("LinkBubble: Prompt");

        mBarView = mRootView.findViewById(R.id.prompt);
        mBarAnimator = mBarView.animate();

        mMessageView = (TextView) mBarView.findViewById(R.id.prompt_message);
        mButton = (ImageButton) mBarView.findViewById(R.id.prompt_button);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onClick();
                }
                hidePrompt(false);
            }
        });

        hidePrompt(true);
    }

    private void showPrompt(CharSequence text, Drawable icon, int duration, OnPromptEventListener listener) {
        mMessageView.setText(text);
        mListener = listener;
        mButton.setImageDrawable(icon);

        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, duration);

        mBarView.setVisibility(View.VISIBLE);
        mBarAnimator.cancel();
        mBarAnimator.alpha(1)
                    .setDuration(mBarView.getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);

        mLayoutParams.y = Config.dpToPx(40.0f);
        mWindowManager.addView(mRootView, mLayoutParams);
        mVisible = true;
    }

    private void hidePrompt(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mBarAnimator.cancel();
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            if (mVisible) {
                mWindowManager.removeViewImmediate(mRootView);
                mVisible = false;
            }
            if (mListener != null) {
                mListener.onClose();
                mListener = null;
            }
        } else {
            mBarAnimator.alpha(0)
                        .setDuration(mBarView.getResources().getInteger(android.R.integer.config_shortAnimTime))
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mBarView.setVisibility(View.GONE);
                                if (mVisible) {
                                    mWindowManager.removeViewImmediate(mRootView);
                                    mVisible = false;
                                }
                                if (mListener != null) {
                                    mListener.onClose();
                                    mListener = null;
                                }
                            }
                        });
        }
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hidePrompt(false);
        }
    };

    public static void show(CharSequence text, Drawable icon, int duration, OnPromptEventListener listener) {
        Util.Assert(sPrompt != null);
        if (sPrompt != null) {
            sPrompt.hidePrompt(true);
            sPrompt.showPrompt(text, icon, duration, listener);
        }
    }
}
