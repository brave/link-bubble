package com.chrislacy.linkbubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class BubbleView extends FrameLayout {

    private BadgeView mBadgeView;
    private ImageView mFavicon;
    private ImageView mAdditionalFaviconView;
    protected WindowManager mWindowManager;
    protected WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    private EventHandler mEventHandler;
    private ProgressIndicator mProgressIndicator;

    private URL mUrl;
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
        public void onMotionEvent_Touch(BubbleView sender, TouchEvent event);
        public void onMotionEvent_Move(BubbleView sender, MoveEvent event);
        public void onMotionEvent_Release(BubbleView sender, ReleaseEvent event);
        public void onSharedLink(BubbleView sender);
    }

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void attachBadge(BadgeView badgeView) {
        if (mBadgeView == null) {
            mBadgeView = badgeView;

            int badgeMargin = getResources().getDimensionPixelSize(R.dimen.badge_margin);
            int badgeSize = getResources().getDimensionPixelSize(R.dimen.badge_size);
            FrameLayout.LayoutParams lp = new LayoutParams(badgeSize, badgeSize);
            lp.gravity = Gravity.TOP|Gravity.RIGHT;
            lp.leftMargin = badgeMargin;
            lp.rightMargin = badgeMargin;
            lp.topMargin = badgeMargin;
            addView(mBadgeView, lp);
        }
    }

    public void detachBadge() {
        if (mBadgeView != null) {
            removeView(mBadgeView);
            mBadgeView = null;
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

    public void setBubbleIndex(int i) {
        mBubbleIndex = i;
        mContentView.setMarkerX((int) Config.getContentViewX(i, MainController.get().getBubbleCount()));
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
            xPos = (int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount());
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

        if (mContentView != null) {
            mContentView.setMarkerX((int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount()));
        }
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

    public void update(float dt, boolean contentView) {
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

            if (contentView) {
                mContentView.setMarkerX(x);
            }

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

    public URL getUrl() {
        return mUrl;
    }

    public Drawable getFavicon() {
        return mFavicon.getDrawable();
    }

    public void setAdditionalFaviconView(ImageView imageView) {
        mAdditionalFaviconView = imageView;
    }

    public void readd() {
        mWindowManager.removeView(this);
        mWindowManager.addView(this, mWindowManagerParams);
    }

    public void configure(String url, int x0, int y0, int targetX, int targetY, float targetTime, long startTime,
                  EventHandler eh) throws MalformedURLException {
        mAlive = true;

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mEventHandler = eh;
        mUrl = new URL(url);
        mRecordHistory = Settings.get().isIncognitoMode() ? false : true;

        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(BubbleView.this, mUrl.toString(), startTime, new ContentView.EventHandler() {
            @Override
            public void onSharedLink() {
                mEventHandler.onSharedLink(BubbleView.this);
            }

            @Override
            public void onPageLoading(String url) {
                showProgressBar(true, 0);
                onReceivedIcon(null);
            }

            @Override
            public void onProgressChanged(int progress) {
                showProgressBar(true, progress);
            }

            @Override
            public void onPageLoaded(ContentView.PageLoadInfo info) {
                showProgressBar(false, 0);

                if (info == null || info.bmp == null) {
                    onReceivedIcon(null);
                }

                if (mRecordHistory && info != null && info.url != null) {
                    MainApplication.saveUrlInHistory(getContext(), null, info.url, info.mHost, info.title);
                }

                MainController.get().onPageLoaded(BubbleView.this);
            }

            @Override
            public void onReceivedIcon(Bitmap bitmap) {
                if (bitmap == null) {
                    mFavicon.setImageResource(R.drawable.fallback_favicon);
                } else {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();

                    int reqW = Math.min((int) (Config.mBubbleWidth * 0.5f), w*2);
                    int reqH = Math.min((int) (Config.mBubbleHeight * 0.5f), h*2);

                    if (w != reqW || h != reqH) {
                        w = reqW;
                        h = reqH;

                        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                    }

                    mFavicon.setImageBitmap(bitmap);
                    if (mAdditionalFaviconView != null) {
                        mAdditionalFaviconView.setImageBitmap(bitmap);
                    }
                }

                mFavicon.setVisibility(VISIBLE);
                showProgressBar(false, 0);
            }
        });

        setVisibility(GONE);

        mFavicon = (ImageView) findViewById(R.id.favicon);
        mProgressIndicator = (ProgressIndicator) findViewById(R.id.progressIndicator);
        showProgressBar(true, 0);

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

                        mEventHandler.onMotionEvent_Touch(BubbleView.this, e);

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
                        mEventHandler.onMotionEvent_Move(BubbleView.this, e);

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

                        mEventHandler.onMotionEvent_Release(BubbleView.this, e);
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
        mWindowManagerParams.x = x0;
        mWindowManagerParams.y = y0;
        int bubbleSize = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        mWindowManagerParams.height = bubbleSize;
        mWindowManagerParams.width = bubbleSize;
        mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mWindowManagerParams.format = PixelFormat.TRANSPARENT;
        mWindowManagerParams.setTitle("LinkBubble: Bubble");

        if (mAlive) {
            mWindowManager.addView(this, mWindowManagerParams);

            setExactPos(x0, y0);
            if (targetX != x0 || targetY != y0) {
                setTargetPos(targetX, targetY, targetTime, true);
            }
        }
    }

    /*
    Handler mHandler = new Handler();
    float mTempProgress = 0.f;
    Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressIndicator.setProgress((int) mTempProgress);
            mTempProgress += .3f;
            if (mTempProgress >= 100) {
                mTempProgress -= 100;
            }
            mHandler.postDelayed(mProgressRunnable, 33);
        }
    };*/

    void showProgressBar(boolean show, int progress) {
        mProgressIndicator.setProgress(show, progress);
    }
}