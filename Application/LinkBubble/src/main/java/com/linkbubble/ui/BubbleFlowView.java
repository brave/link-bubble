package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.physics.Circle;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.FlingTracker;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.URL;
import java.util.Vector;

public class BubbleFlowView extends FancyCoverFlow {


    protected WindowManager mWindowManager;
    protected WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
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

    private EventHandler mEventHandler;

    private static class InternalMoveEvent {

        public InternalMoveEvent(float x, float y, long t) {
            mX = x;
            mY = y;
            mTime = t;
        }

        public long mTime;
        public float mX, mY;
    }

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleFlowView sender, Draggable.TouchEvent event);
        public void onMotionEvent_Move(BubbleFlowView sender, Draggable.MoveEvent event);
        public void onMotionEvent_Release(BubbleFlowView sender, Draggable.ReleaseEvent event);
    }

    public BubbleFlowView(Context context) {
        this(context, null);
    }

    public BubbleFlowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        MainApplication app = (MainApplication) context.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        setBackgroundColor(0x33ff0000);
    }

    public void configure(int x0, int y0, int targetX, int targetY, float targetTime, EventHandler eh)  {
        mAlive = true;
        mEventHandler = eh;

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

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
                        Draggable.TouchEvent e = new Draggable.TouchEvent();
                        e.posX = mWindowManagerParams.x;
                        e.posY = mWindowManagerParams.y;
                        e.rawX = mStartTouchXRaw;
                        e.rawY = mStartTouchYRaw;

                        mMoveEvents.clear();
                        InternalMoveEvent me = new InternalMoveEvent(mStartTouchXRaw, mStartTouchYRaw, event.getEventTime());
                        mMoveEvents.add(me);

                        mEventHandler.onMotionEvent_Touch(BubbleFlowView.this, e);

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

                        Draggable.MoveEvent e = new Draggable.MoveEvent();
                        e.dx = deltaX;
                        e.dy = deltaY;
                        mEventHandler.onMotionEvent_Move(BubbleFlowView.this, e);

                        event.offsetLocation(mWindowManagerParams.x - mStartTouchX, mWindowManagerParams.y - mStartTouchY);
                        mFlingTracker.addMovement(event);

                        return true;
                    }
                    case MotionEvent.ACTION_UP: {

                        Draggable.ReleaseEvent e = new Draggable.ReleaseEvent();
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

                        mEventHandler.onMotionEvent_Release(BubbleFlowView.this, e);
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
        //int bubbleSize = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        int bubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_flow_height);
        mWindowManagerParams.height = bubbleFlowHeight;
        int bubbleFlowWidth = getResources().getDimensionPixelSize(R.dimen.bubble_flow_width);
        mWindowManagerParams.width = bubbleFlowWidth;
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

    public void destroy() {
        setOnTouchListener(null);
        // Will be null
        //if (mContentView != null) {
        //    mContentView.destroy();
            mWindowManager.removeView(this);
        //}
        mAlive = false;
    }

    public void readd() {
        mWindowManager.removeView(this);
        mWindowManager.addView(this, mWindowManagerParams);
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

            //if (contentView) {
            //    mContentView.setMarkerX(x);
            //}

            MainController.get().scheduleUpdate();
        }
    }

    public CanvasView.TargetInfo getTargetInfo(CanvasView canvasView, int x, int y) {
        Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                y + Config.mBubbleHeight * 0.5f,
                Config.mBubbleWidth * 0.5f);
        CanvasView.TargetInfo targetInfo = canvasView.getBubbleAction(bubbleCircle);
        return targetInfo;
    }


    public Config.BubbleAction doSnap(CanvasView canvasView, int targetX, int targetY) {
        CanvasView.TargetInfo targetInfo = getTargetInfo(canvasView, targetX, targetY);

        if (targetInfo.mAction != Config.BubbleAction.None) {
            setTargetPos((int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f),
                    (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f),
                    0.3f, true);
        } else {
            setTargetPos(targetX, targetY, 0.02f, false);
        }

        return targetInfo.mAction;
    }

    public int getXPos() {
        return mWindowManagerParams.x;
    }

    public int getYPos() {
        return mWindowManagerParams.y;
    }

    public void expand() {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        lp.width = 400;
        setLayoutParams(lp);
    }

    void collapse() {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        lp.width = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        setLayoutParams(lp);
    }

    void bubblesUpdated() {
        BubbleFlowAdapter adapter = (BubbleFlowAdapter)getAdapter();
        if (adapter.mBubbles == null) {
            adapter.setBubbles(MainController.get().getBubbles());
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    public void OnOrientationChanged(boolean contentViewMode) {
        clearTargetPos();

        int xPos, yPos;

        if (contentViewMode) {
            //xPos = (int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount());
            xPos = (int) Config.getContentViewX(0, MainController.get().getBubbleCount());
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

        //if (mContentView != null) {
        //    mContentView.setMarkerX((int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount()));
        //}
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

    public void attachBadge(BadgeView badgeView) {
        /*
        if (mBadgeView == null) {
            mBadgeView = badgeView;

            int badgeMargin = getResources().getDimensionPixelSize(R.dimen.badge_margin);
            int badgeSize = getResources().getDimensionPixelSize(R.dimen.badge_size);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            lp.gravity = Gravity.TOP|Gravity.RIGHT;
            lp.leftMargin = badgeMargin;
            lp.rightMargin = badgeMargin;
            lp.topMargin = badgeMargin;
            addView(mBadgeView, lp);
        }*/
    }

    public void detachBadge() {
        /*
        if (mBadgeView != null) {
            removeView(mBadgeView);
            mBadgeView = null;
        }*/
    }

    public ContentView getCurrentContentView() {
        return null;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBubbleAdded(MainController.BubbleAddedEvent event) {
        bubblesUpdated();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onContentActivityPaused(MainController.BubbleRemovedEvent event) {
        bubblesUpdated();
    }
}
