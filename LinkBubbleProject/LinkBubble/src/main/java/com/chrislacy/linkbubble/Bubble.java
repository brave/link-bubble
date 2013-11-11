package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.Vector;

public class Bubble extends RelativeLayout {

    public enum BubbleType {
        Close,
        Consume_Left,
        Consume_Right,
    };

    private Badge mBadge;
    private ImageView mShape;
    protected WindowManager mWindowManager;
    protected WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    private EventHandler mEventHandler;
    private ProgressBar mProgressBar;

    private String mUrl;
    private ContentView mContentView;
    private boolean mRecordHistory;

    // Move animation state
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;

    private Vector<InternalMoveEvent> mMoveEvents = new Vector<InternalMoveEvent>();
    private FlingTracker mFlingTracker = null;

    private static class InternalMoveEvent {

        public InternalMoveEvent(float x, float y, long t) {
            mX = x;
            mY = y;
            mTime = t;
        }

        public long mTime;
        public float mX, mY;
    }

    public static class TouchEvent {
        int posX;
        int posY;
        float rawX;
        float rawY;
    }

    public static class MoveEvent {
        int dx;
        int dy;
    }

    public static class ReleaseEvent {
        int posX;
        int posY;
        float vx;
        float vy;
        float rawX;
        float rawY;
    }

    public interface EventHandler {
        public void onMotionEvent_Touch(Bubble sender, TouchEvent event);
        public void onMotionEvent_Move(Bubble sender, MoveEvent event);
        public void onMotionEvent_Release(Bubble sender, ReleaseEvent event);
        public void onSharedLink(Bubble sender);
    }

    public void attachBadge(Badge badge) {
        if (mBadge == null) {
            mBadge = badge;

            RelativeLayout.LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = Config.dpToPx(40.0f);
            addView(mBadge, lp);
        }
    }

    public void detachBadge() {
        if (mBadge != null) {
            removeView(mBadge);
            mBadge = null;
        }
    }

    public int getXPos() {
        return mWindowManagerParams.x;
    }

    public int getYPos() {
        return mWindowManagerParams.y;
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public void clearTargetPos() {
        mInitialX = -1;
        mInitialY = -1;

        mTargetX = mWindowManagerParams.x;
        mTargetY = mWindowManagerParams.y;

        mAnimPeriod = 0.0f;
        mAnimTime = 0.0f;
    }

    public void setExactPos(int x, int y) {
        mWindowManagerParams.x = x;
        mWindowManagerParams.y = y;
        mTargetX = x;
        mTargetY = y;
        mWindowManager.updateViewLayout(this, mWindowManagerParams);
    }

    public void setTargetPos(int x, int y, float t) {
        if (x != mTargetX || y != mTargetY) {
            mInitialX = mWindowManagerParams.x;
            mInitialY = mWindowManagerParams.y;

            mTargetX = x;
            mTargetY = y;

            mAnimPeriod = t;
            mAnimTime = 0.0f;

            MainController.scheduleUpdate();
        }
    }

    public void update(float dt) {
        if (mWindowManagerParams.x != mTargetX || mWindowManagerParams.y != mTargetY) {
            Util.Assert(mAnimPeriod > 0.0f);

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod);

            float interpolatedFraction = mAnimTime / mAnimPeriod;   // Linear only for now
            Util.Assert(interpolatedFraction >= 0.0f && interpolatedFraction <= 1.0f);

            int x = (int) (mInitialX + (mTargetX - mInitialX) * interpolatedFraction);
            int y = (int) (mInitialY + (mTargetY - mInitialY) * interpolatedFraction);

            mWindowManagerParams.x = x;
            mWindowManagerParams.y = y;
            mWindowManager.updateViewLayout(this, mWindowManagerParams);

            MainController.scheduleUpdate();
        }
    }

    public void destroy() {
        setOnTouchListener(null);
        mWindowManager.removeView(this);
    }

    public String getUrl() { return mUrl; }

    public Bubble(final Context context, String url, int x, int y, boolean recordHistory, EventHandler eh) {
        super(context);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mEventHandler = eh;
        mUrl = url;
        mRecordHistory = recordHistory;

        mContentView = new ContentView(context, Bubble.this, mUrl, new ContentView.EventHandler() {
            @Override
            public void onSharedLink() {
                mEventHandler.onSharedLink(Bubble.this);
            }

            @Override
            public void onPageLoaded(ContentView.PageLoadInfo info) {
                removeView(mProgressBar);
                setBackgroundResource(R.drawable.circle_grey);

                int halfImageWidth;
                int halfImageHeight;

                if (mRecordHistory && info.url != null) {
                    SettingsFragment.addRecentBubble(context, info.url);
                }

                if (info == null || info.bmp == null) {
                    mShape.setImageResource(R.drawable.help_button);
                    halfImageWidth = 8;
                    halfImageHeight = 8;
                } else {
                    int w = info.bmp.getWidth();
                    int h = info.bmp.getHeight();

                    int reqW = (int) (Config.mBubbleWidth * 0.5f);
                    int reqH = (int) (Config.mBubbleHeight * 0.5f);

                    if (w != reqW || h != reqH) {
                        w = reqW;
                        h = reqH;

                        info.bmp = Bitmap.createScaledBitmap(info.bmp, w, h, true);
                    }

                    mShape.setImageBitmap(info.bmp);
                    halfImageWidth = w / 2;
                    halfImageHeight = h / 2;
                }

                int hPad = (int) (Config.mBubbleWidth / 2.0f - halfImageWidth);
                int vPad = (int) (Config.mBubbleHeight / 2.0f - halfImageHeight);

                mShape.setPadding(hPad, vPad, 0, 0);
            }
        });

        mTargetX = x;
        mTargetY = y;

        setVisibility(GONE);

        mShape = new ImageView(context);
        mShape.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mProgressBar = new ProgressBar(context);
        mProgressBar.setIndeterminate(true);
        RelativeLayout.LayoutParams lp = new LayoutParams(Config.dpToPx(30.0f), Config.dpToPx(30.0f));
        lp.leftMargin = Config.dpToPx(60.0f / 2.0f) - Config.dpToPx(30.0f / 2.0f);
        lp.topMargin = Config.dpToPx(60.0f / 2.0f) - Config.dpToPx(30.0f / 2.0f);
        addView(mProgressBar, lp);

        setBackgroundResource(R.drawable.circle_grey);

        addView(mShape);

        setOnTouchListener(new OnTouchListener() {
            private float mStartTouchXRaw;
            private float mStartTouchYRaw;
            private int mStartTouchX;
            private int mStartTouchY;

            @Override
            public boolean onTouch(android.view.View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        mStartTouchXRaw = event.getRawX();
                        mStartTouchYRaw = event.getRawY();
                        TouchEvent e = new TouchEvent();
                        e.posX = mWindowManagerParams.x;
                        e.posY = mWindowManagerParams.y;
                        e.rawX = mStartTouchXRaw;
                        e.rawY = mStartTouchYRaw;

                        mMoveEvents.clear();
                        InternalMoveEvent me = new InternalMoveEvent(mStartTouchXRaw, mStartTouchYRaw, event.getEventTime());
                        mMoveEvents.add(me);

                        mEventHandler.onMotionEvent_Touch(Bubble.this, e);

                        mStartTouchX = mWindowManagerParams.x;
                        mStartTouchY = mWindowManagerParams.y;

                        mFlingTracker = FlingTracker.obtain();
                        mFlingTracker.addMovement(event);

                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float touchXRaw = event.getRawX();
                        float touchYRaw = event.getRawY();

                        int deltaX = (int) (touchXRaw - mStartTouchXRaw);
                        int deltaY = (int) (touchYRaw - mStartTouchYRaw);

                        InternalMoveEvent me = new InternalMoveEvent(touchXRaw, touchYRaw, event.getEventTime());
                        mMoveEvents.add(me);

                        MoveEvent e = new MoveEvent();
                        e.dx = deltaX;
                        e.dy = deltaY;
                        mEventHandler.onMotionEvent_Move(Bubble.this, e);

                        event.offsetLocation(mWindowManagerParams.x - mStartTouchX, mWindowManagerParams.y - mStartTouchY);
                        mFlingTracker.addMovement(event);

                        return true;
                    }
                    case MotionEvent.ACTION_UP: {

                        ReleaseEvent e = new ReleaseEvent();
                        e.posX = mWindowManagerParams.x;
                        e.posY = mWindowManagerParams.y;
                        e.vx = 0.0f;
                        e.vy = 0.0f;
                        e.rawX = event.getRawX();
                        e.rawY = event.getRawY();

                        if (mMoveEvents.size() > 0) {
                            float firstMs = mMoveEvents.get(0).mTime;

                            for (int i=0 ; i < mMoveEvents.size() ; ++i) {
                                InternalMoveEvent me = mMoveEvents.get(i);
                                float ms = me.mTime - firstMs;
                            }
                        }

                        int moveEventCount = mMoveEvents.size();
                        if (moveEventCount > 2) {
                            InternalMoveEvent lastME = mMoveEvents.lastElement();
                            InternalMoveEvent refME = null;

                            for (int i=moveEventCount-1 ; i >= 0 ; --i) {
                                InternalMoveEvent me = mMoveEvents.get(i);

                                if (lastME.mTime == me.mTime)
                                    continue;

                                refME = me;

                                float touchTime = (lastME.mTime - me.mTime) / 1000.0f;
                                if (touchTime > 0.03f) {
                                    break;
                                }
                            }

                            if (refME != null) {
                                Util.Assert(refME.mTime != lastME.mTime);
                                float touchTime = (lastME.mTime - refME.mTime) / 1000.0f;
                                e.vx = (lastME.mX - refME.mX) / touchTime;
                                e.vy = (lastME.mY - refME.mY) / touchTime;
                            }
                        }

                        mFlingTracker.computeCurrentVelocity(1000);
                        float fvx = mFlingTracker.getXVelocity();
                        float fvy = mFlingTracker.getYVelocity();

                        e.vx = fvx;
                        e.vy = fvy;

                        mFlingTracker.recycle();

                        mEventHandler.onMotionEvent_Release(Bubble.this, e);
                        return true;
                    }
                    case MotionEvent.ACTION_CANCEL: {
                        return true;
                    }
                }

                return false;
            }
        });

        mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowManagerParams.x = x;
        mWindowManagerParams.y = y;
        mWindowManagerParams.height = Config.dpToPx(60.0f);
        mWindowManagerParams.width = Config.dpToPx(60.0f);
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManager.addView(this, mWindowManagerParams);
    }
}