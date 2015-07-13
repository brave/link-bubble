package com.linkbubble.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.linkbubble.Config;
import com.linkbubble.R;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

public class Prompt {

    public interface OnPromptEventListener {
        void onActionClick();
        void onClose();
    }

    public static final int LENGTH_SHORT = 3000;
    public static final int LENGTH_LONG = 6000;

    private static Prompt sPrompt;
    private static boolean sIsShowing;

    private View mRootView;
    private View mBarView;
    private TextView mMessageView;
    private TextView mPromptButtonTextView;
    private ViewPropertyAnimator mBarAnimator;
    private Handler mHideHandler = new Handler();
    private OnPromptEventListener mListener;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    public static void initModule(Context context) {
        Util.Assert(sPrompt == null, "non-null instance");
        sPrompt = new Prompt(context);
    }

    public static void deinitModule() {
        Util.Assert(sPrompt != null, "null instance");
        sPrompt = null;
    }

    public static boolean isShowing() {
        return sIsShowing;
    }

    private Prompt(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = li.inflate(R.layout.view_prompt, null);

        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                context.getResources().getDimensionPixelSize(R.dimen.snackbar_height),
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        mLayoutParams.setTitle("LinkBubble: Prompt");

        mBarView = mRootView.findViewById(R.id.prompt);
        mBarAnimator = mBarView.animate();

        mMessageView = (TextView) mBarView.findViewById(R.id.prompt_message);
        mPromptButtonTextView = (TextView)mBarView.findViewById(R.id.prompt_button_text_view);

        mPromptButtonTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onActionClick();
                }
                hidePrompt(false);
            }
        });

        hidePrompt(true);
    }

    private void showPrompt(CharSequence text, CharSequence buttonText,
                            int duration, boolean forceSingleLine, OnPromptEventListener listener) {
        mMessageView.setText(text);
        if (forceSingleLine) {
            mMessageView.setSingleLine(true);
            mMessageView.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            mMessageView.setSingleLine(false);
            mMessageView.setEllipsize(null);
        }
        mListener = listener;

        mPromptButtonTextView.setText(buttonText);

        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, duration);

        mBarView.setVisibility(View.VISIBLE);
        mBarAnimator.cancel();
        mBarAnimator.alpha(1)
                    .setDuration(mBarView.getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);

        mWindowManager.addView(mRootView, mLayoutParams);
        sIsShowing = true;
    }

    private void hidePrompt(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mBarAnimator.cancel();
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            if (sIsShowing) {
                mWindowManager.removeViewImmediate(mRootView);
                sIsShowing = false;
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
                                if (sIsShowing) {
                                    mWindowManager.removeViewImmediate(mRootView);
                                    sIsShowing = false;
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

    public static void show(CharSequence text, int duration, OnPromptEventListener listener) {
        show(text, null, duration, false, listener);
    }

    public static void show(CharSequence text, CharSequence buttonText, int duration, boolean forceSingleLine, OnPromptEventListener listener) {
        Util.Assert(sPrompt != null, "null instance");
        if (sPrompt != null) {
            sPrompt.hidePrompt(true);
            CrashTracking.log("Prompt.show() text:" + text);
            sPrompt.showPrompt(text, buttonText, duration, forceSingleLine, listener);
        }
    }
}
