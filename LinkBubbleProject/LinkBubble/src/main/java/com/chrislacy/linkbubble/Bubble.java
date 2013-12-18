package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.squareup.otto.Bus;

import java.io.Console;
import java.util.Vector;

public class Bubble extends RelativeLayout {

    private Badge mBadge;
    private ImageView mShape;
    protected WindowManager mWindowManager;
    protected WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    private EventHandler mEventHandler;
    private ProgressBar mProgressBar;
    private RelativeLayout.LayoutParams mProgressBarLP;
    private boolean mProgressBarShowing;


    private String mUrl;
    private ContentView mContentView;
    private boolean mRecordHistory;
    private boolean mAlive;

    // Move animation state
    private int mInitialX;
    private int mInitialY;
    private int mTargetX;
    private int mTargetY;
    private float mAnimPeriod;
    private float mAnimTime;
    private boolean mOvershoot;
    private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
    private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator();

    private Vector<InternalMoveEvent> mMoveEvents = new Vector<InternalMoveEvent>();
    private FlingTracker mFlingTracker = null;

    private int mBubbleIndex;

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

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

    public void setBubbleIndex(int i) {
        mBubbleIndex = i;
    }

    public int getBubbleIndex() {
        return mBubbleIndex;
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

    public void OnOrientationChanged(boolean contentViewMode) {
        clearTargetPos();

        int xPos, yPos;

        if (contentViewMode) {
            xPos = (int) Config.getContentViewX(mBubbleIndex);
            yPos = Config.mContentViewBubbleY;
        } else {
            if (mWindowManagerParams.x < Config.mScreenHeight * 0.5f)
                xPos = Config.mBubbleSnapLeftX;
            else
                xPos = Config.mBubbleSnapRightX;
            float yf = (float)mWindowManagerParams.y / (float)Config.mScreenWidth;
            yPos = (int) (yf * Config.mScreenHeight);
        }

        setExactPos(xPos, yPos);
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
        if (mAlive) {
            mWindowManager.updateViewLayout(this, mWindowManagerParams);
        }
    }

    public void setTargetPos(int x, int y, float t, boolean overshoot) {
        if (x != mTargetX || y != mTargetY) {
            mOvershoot = overshoot;

            mInitialX = mWindowManagerParams.x;
            mInitialY = mWindowManagerParams.y;

            mTargetX = x;
            mTargetY = y;

            mAnimPeriod = t;
            mAnimTime = 0.0f;

            MainController.get().scheduleUpdate();
        }
    }

    public Canvas.TargetInfo getTargetInfo(Canvas canvas, int x, int y) {
        Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                y + Config.mBubbleHeight * 0.5f,
                Config.mBubbleWidth * 0.5f);
        Canvas.TargetInfo targetInfo = canvas.getBubbleAction(bubbleCircle);
        return targetInfo;
    }

    public Config.BubbleAction doSnap(Canvas canvas, int targetX, int targetY) {
        Canvas.TargetInfo targetInfo = getTargetInfo(canvas, targetX, targetY);

        if (targetInfo.mAction != Config.BubbleAction.None) {
            setTargetPos((int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f),
                         (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f),
                         0.3f, true);
        } else {
            setTargetPos(targetX, targetY, 0.02f, false);
        }

        return targetInfo.mAction;
    }

    public boolean isSnapping() {
        return mOvershoot;
    }

    public void update(float dt) {
        if (mAnimTime < mAnimPeriod) {
            Util.Assert(mAnimPeriod > 0.0f);

            mAnimTime = Util.clamp(0.0f, mAnimTime + dt, mAnimPeriod);

            float tf = mAnimTime / mAnimPeriod;
            float interpolatedFraction;
            if (mOvershoot) {
                interpolatedFraction = mOvershootInterpolator.getInterpolation(tf);
                //Log.e("GT", "t = " + tf + ", f = " + interpolatedFraction);
            } else {
                interpolatedFraction = mLinearInterpolator.getInterpolation(tf);
                Util.Assert(interpolatedFraction >= 0.0f && interpolatedFraction <= 1.0f);
            }

            int x = (int) (mInitialX + (mTargetX - mInitialX) * interpolatedFraction);
            int y = (int) (mInitialY + (mTargetY - mInitialY) * interpolatedFraction);

            mWindowManagerParams.x = x;
            mWindowManagerParams.y = y;
            mWindowManager.updateViewLayout(this, mWindowManagerParams);

            MainController.get().scheduleUpdate();
        }
    }

    public void destroy() {
        setOnTouchListener(null);
        // Will be null 
        if (mContentView != null) {
            mContentView.destroy();
            mWindowManager.removeView(this);
        }
        mAlive = false;
    }

    public String getUrl() { return mUrl; }

    public Bubble(final Context context, String url, int x, int y, long startTime,
                  boolean recordHistory, int bubbleIndex, EventHandler eh) {
        super(context);

        mAlive = true;

        if (Settings.get().isIncognitoMode()) {
            recordHistory = false;
        }

        mBubbleIndex = bubbleIndex;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mEventHandler = eh;
        mUrl = url;
        mRecordHistory = recordHistory;

        mContentView = new ContentView(context, Bubble.this, mUrl, startTime, new ContentView.EventHandler() {
            @Override
            public void onSharedLink() {
                mEventHandler.onSharedLink(Bubble.this);
            }

            @Override
            public void onPageLoading(String url) {
                showProgressBar(true);
            }

            @Override
            public void onPageLoaded(ContentView.PageLoadInfo info) {
                showProgressBar(false);
                setBackgroundResource(R.drawable.circle_grey);

                if (mRecordHistory && info != null && info.url != null) {
                    LinkHistoryRecord linkHistoryRecord = new LinkHistoryRecord(info.title, info.url, System.currentTimeMillis());

                    MainApplication app = (MainApplication) getContext().getApplicationContext();

                    app.mDatabaseHelper.addLinkHistoryRecord(linkHistoryRecord);
                    app.getBus().post(new LinkHistoryRecord.ChangedEvent(linkHistoryRecord));
                }

                MainController.get().onPageLoaded(Bubble.this);
            }

            @Override
            public void onReceivedIcon(Bitmap bitmap) {
                int halfImageWidth;
                int halfImageHeight;

                if (bitmap == null) {
                    mShape.setImageResource(R.drawable.help_button);
                    halfImageWidth = 8;
                    halfImageHeight = 8;
                } else {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();

                    int reqW = (int) (Config.mBubbleWidth * 0.5f);
                    int reqH = (int) (Config.mBubbleHeight * 0.5f);

                    if (w != reqW || h != reqH) {
                        w = reqW;
                        h = reqH;

                        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                    }

                    mShape.setImageBitmap(bitmap);
                    halfImageWidth = w / 2;
                    halfImageHeight = h / 2;
                }

                int hPad = (int) (Config.mBubbleWidth / 2.0f - halfImageWidth);
                int vPad = (int) (Config.mBubbleHeight / 2.0f - halfImageHeight);

                mShape.setPadding(hPad, vPad, 0, 0);
                mShape.setVisibility(VISIBLE);
                showProgressBar(false);
            }
        });

        setVisibility(GONE);

        mShape = new ImageView(context);
        mShape.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mProgressBar = new ProgressBar(context);
        mProgressBarLP = new LayoutParams(Config.dpToPx(30.0f), Config.dpToPx(30.0f));
        mProgressBarLP.leftMargin = Config.dpToPx(60.0f / 2.0f) - Config.dpToPx(30.0f / 2.0f);
        mProgressBarLP.topMargin = Config.dpToPx(60.0f / 2.0f) - Config.dpToPx(30.0f / 2.0f);
        showProgressBar(true);

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

                            for (int i = 0; i < mMoveEvents.size(); ++i) {
                                InternalMoveEvent me = mMoveEvents.get(i);
                                float ms = me.mTime - firstMs;
                            }
                        }

                        int moveEventCount = mMoveEvents.size();
                        if (moveEventCount > 2) {
                            InternalMoveEvent lastME = mMoveEvents.lastElement();
                            InternalMoveEvent refME = null;

                            for (int i = moveEventCount - 1; i >= 0; --i) {
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
                    //case MotionEvent.ACTION_CANCEL: {
                    //    return true;
                    //}
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
        mWindowManagerParams.setTitle("LinkBubble: Bubble");

        if (mAlive) {
            mWindowManager.addView(this, mWindowManagerParams);

            if (bubbleIndex == 0) {
                setExactPos((int) (Config.mBubbleSnapLeftX - Config.mBubbleWidth), y);
                setTargetPos(x, y, 0.4f, true);
            } else {
                setExactPos(x, y);
            }
        }
    }

    void showProgressBar(boolean show) {
        if (show) {
            if (mProgressBarShowing == false) {
                mProgressBarShowing = true;
                mProgressBar.setIndeterminate(true);
                addView(mProgressBar, mProgressBarLP);
                mShape.setVisibility(GONE);
            }
        } else {
            if (mProgressBarShowing) {
                removeView(mProgressBar);
                mProgressBarShowing = false;
            }
        }
    }
}