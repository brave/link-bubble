package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;
import com.linkbubble.physics.Circle;

import java.util.Vector;

public class Canvas extends RelativeLayout {

    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    private Vector<BubbleTarget> mTargets = new Vector<BubbleTarget>();

    private final float mMaxAlpha = 1.0f;
    private final float mFadeTime = 0.2f;
    private final float mAlphaDelta = mMaxAlpha / mFadeTime;

    private float mCurrentAlpha = 0.0f;
    private float mTargetAlpha = 0.0f;

    private float mCurrentAlphaContentView = 1.0f;
    private float mTargetAlphaContentView = 1.0f;

    private boolean mEnabled;

    private ContentView mContentView;
    public ContentView getContentView() { return mContentView; }

    public class TargetInfo {

        public TargetInfo() {
            mAction = Config.BubbleAction.None;
            mTargetX = -1;
            mTargetY = -1;
        }

        public Config.BubbleAction mAction;
        public int mTargetX;
        public int mTargetY;
    }

    public Canvas(Context context) {
        super(context);

        mContext = context;
        mEnabled = true;

        applyAlpha();

        setBackground(getResources().getDrawable(R.drawable.masked_background));

        mTargets.add(new BubbleTarget(this, mContext, R.drawable.close_indicator, Config.BubbleAction.Destroy, 0.5f, 0.85f, true));
        mTargets.add(new BubbleTarget(this, mContext, Config.BubbleAction.ConsumeLeft, 0.2f, 0.15f, true));
        mTargets.add(new BubbleTarget(this, mContext, Config.BubbleAction.ConsumeRight, 0.8f, 0.15f, true));

        Settings.setConsumeBubblesChangedEventHandler(new Settings.ConsumeBubblesChangedEventHandler() {
            @Override
            public void onConsumeBubblesChanged() {
                for (int i = 0; i < mTargets.size(); ++i) {
                    mTargets.get(i).onConsumeBubblesChanged();
                }
            }
        });

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = 0;
        mWindowManagerParams.y = 0;
        mWindowManagerParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManagerParams.setTitle("LinkBubble: Canvas");
        mWindowManager.addView(this, mWindowManagerParams);
    }

    public void onOrientationChanged() {
        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTarget bt = mTargets.get(i);
            bt.OnOrientationChanged();
        }
        if (mContentView != null) {
            mContentView.onOrientationChanged();
        }
    }

    public void setContentView(ContentView cv) {
        if (mContentView != null) {
            removeView(mContentView);
            mContentView.setAlpha(1.0f);
            mCurrentAlphaContentView = 1.0f;
            mTargetAlphaContentView = 1.0f;
            mContentView.onCurrentContentViewChanged(false);
        }
        mContentView = cv;
        if (mContentView != null) {
            RelativeLayout.LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            p.topMargin = Config.mContentOffset;
            addView(mContentView, p);
            mContentView.onCurrentContentViewChanged(true);
            mContentView.requestFocus();
        }
    }

    private void applyAlpha() {
        Util.Assert(mCurrentAlpha >= 0.0f && mCurrentAlpha <= 1.0f);

        int color = ((int)(180 * mCurrentAlpha) << 24);
        if (Config.USE_CONTENT_ACTIVITY) {
            MainController.get().updateBackgroundColor(color);
        } else {
            setAlpha(mCurrentAlpha);
            //setBackgroundColor(color);
        }

        if (!mEnabled || mCurrentAlpha == 0.0f) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }

        if (mContentView != null) {
            Util.Assert(mCurrentAlphaContentView >= 0.0f && mCurrentAlphaContentView <= 1.0f);
            mContentView.setAlpha(mCurrentAlphaContentView);
        }
    }

    public void setContentViewTranslation(float ty) {
        if (mContentView != null) {
            mContentView.setTranslationY(ty);
        }
    }

    public void showContentView() {
        Util.Assert(mContentView != null);
        mCurrentAlphaContentView = 1.0f;
        mTargetAlphaContentView = 1.0f;
        mContentView.setAlpha(1.0f);
    }

    public void hideContentView() {
        Util.Assert(mContentView != null);
        mCurrentAlphaContentView = mContentView.getAlpha();
        mTargetAlphaContentView = 0.0f;
        MainController.get().scheduleUpdate();
    }

    public void fadeIn() {
        mTargetAlpha = mMaxAlpha;
        MainController.get().scheduleUpdate();
    }

    public void fadeOut() {
        mTargetAlpha = 0.0f;
        MainController.get().scheduleUpdate();
    }

    public void fadeInTargets() {
        for (int i=0 ; i < mTargets.size() ; ++i) {
            mTargets.get(i).fadeIn();
        }
    }

    public void fadeOutTargets() {
        for (int i=0 ; i < mTargets.size() ; ++i) {
            mTargets.get(i).fadeOut();
        }
    }

    public void destroy() {
        mWindowManager.removeView(this);
    }

    public void update(float dt, BubbleView frontBubble) {
        if (mCurrentAlpha < mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha + mAlphaDelta * dt, mMaxAlpha);
            MainController.get().scheduleUpdate();
        } else if (mCurrentAlpha > mTargetAlpha) {
            mCurrentAlpha = Util.clamp(0.0f, mCurrentAlpha - mAlphaDelta * dt, mMaxAlpha);
            MainController.get().scheduleUpdate();
        }

        if (mCurrentAlphaContentView < mTargetAlphaContentView) {
            mCurrentAlphaContentView = Util.clamp(0.0f, mCurrentAlphaContentView + mAlphaDelta * dt, 1.0f);
            MainController.get().scheduleUpdate();
        } else if (mCurrentAlphaContentView > mTargetAlphaContentView) {
            mCurrentAlphaContentView = Util.clamp(0.0f, mCurrentAlphaContentView - mAlphaDelta * dt, 1.0f);
            MainController.get().scheduleUpdate();
        }

        applyAlpha();

        for (int i=0 ; i < mTargets.size() ; ++i) {
            mTargets.get(i).update(dt, frontBubble);
        }
    }

    public TargetInfo getBubbleAction(Circle bubbleCircle) {
        TargetInfo ti = new TargetInfo();

        for (int i=0 ; i < mTargets.size() ; ++i) {
            BubbleTarget bt = mTargets.get(i);

            Circle snapCircle = bt.GetSnapCircle();
            Circle defaultCircle = bt.GetDefaultCircle();

            if (bubbleCircle.Intersects(snapCircle)) {
                ti.mAction = bt.GetAction();
                ti.mTargetX = (int) defaultCircle.mX;
                ti.mTargetY = (int) defaultCircle.mY;
                break;
            }
        }

        return ti;
    }
}