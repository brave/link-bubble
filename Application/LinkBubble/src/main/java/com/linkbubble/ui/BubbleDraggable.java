/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Circle;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

import java.net.MalformedURLException;


public class BubbleDraggable extends BubbleView implements Draggable {

    private static final String TAG = "BubbleDraggable";

    private DraggableHelper mDraggableHelper;
    private OnUpdateListener mOnUpdateListener;
    public BadgeView mBadgeView;
    private CanvasView mCanvasView;
    private BubbleFlowDraggable mBubbleFlowDraggable;
    private Util.Point mTractorBeamIntersectionPoint = new Util.Point();

    private MainController.BeginBubbleDragEvent mBeginBubbleDragEvent = new MainController.BeginBubbleDragEvent();
    private MainController.DraggableBubbleMovedEvent mDraggableBubbleMovedEvent = new MainController.DraggableBubbleMovedEvent();
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();
    private MainController.EndExpandTransitionEvent mEndExpandTransitionEvent = new MainController.EndExpandTransitionEvent();
    private MainController.BeginCollapseTransitionEvent mBeginCollapseTransitionEvent = new MainController.BeginCollapseTransitionEvent();
    private MainController.EndCollapseTransitionEvent mEndCollapseTransitionEvent = new MainController.EndCollapseTransitionEvent();

    // Physics state
    public enum Mode {
        BubbleView,
        ContentView
    }
    private BubbleTargetView mCurrentSnapTarget;
    private boolean mHasMoved;
    private boolean mTouchDown;
    private int mTouchInitialX;
    private int mTouchInitialY;
    private boolean mAnimActive;
    private Mode mMode;
    private float mTimeOnSnapTarget;
    private Circle mCircle;

    public BubbleDraggable(Context context) {
        this(context, null);
    }

    public BubbleDraggable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleDraggable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean isDragging() {
        return mTouchDown;
    }

    public Mode getCurrentMode() {
        return mMode;
    }

    private void onAnimComplete() {
        Util.Assert(mAnimActive, "mAnimActive=" + mAnimActive);
        mAnimActive = false;
    }

    private void doSnap() {
        int xp = (int) (0.5f + mDraggableHelper.getXPos() + Config.mBubbleWidth * 0.5f);
        int yp = mDraggableHelper.getYPos();

        if (xp < Config.mScreenCenterX) {
            xp = Config.mBubbleSnapLeftX;
        } else {
            xp = Config.mBubbleSnapRightX;
        }

        animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(Constant.BUBBLE_ANIM_TIME);
        mBadgeView.animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(Constant.BUBBLE_ANIM_TIME);

        setTargetPos(xp, yp, 0.5f, DraggableHelper.AnimationType.MediumOvershoot, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                onAnimComplete();
                Settings.get().setBubbleRestingPoint(mDraggableHelper.getXPos(), mDraggableHelper.getYPos());
            }

            @Override
            public void onCancel() {
                onAnimComplete();
            }
        });
    }

    public void switchToBubbleView() {
        doAnimateToBubbleView(0);
    }

    public void switchToExpandedView() {
        doAnimateToContentView();
    }

    private void doSnapAction(Constant.BubbleAction action) {
        MainController mainController = MainController.get();

        float snapTime = mTimeOnSnapTarget - Config.ANIMATE_TO_SNAP_TIME;
        if (action == Constant.BubbleAction.Close && snapTime >= Config.CLOSE_ALL_BUBBLES_DELAY) {
            mainController.closeAllBubbles();
            mMode = Mode.BubbleView;
        } else {
            if (mainController.closeCurrentTab(action, false)) {
                if (mMode == Mode.ContentView && action == Constant.BubbleAction.Close) {
                    doAnimateToContentView();
                } else {
                    doAnimateToBubbleView(0);
                }
            } else {
                mMode = Mode.BubbleView;
            }
        }
    }

    private void doFlick(float vx, float vy) {
        DraggableHelper.AnimationType animType = DraggableHelper.AnimationType.Linear;
        BubbleTargetView.enableTractor();

        mCurrentSnapTarget = null;

        int initialX = mDraggableHelper.getXPos();
        int initialY = mDraggableHelper.getYPos();
        int targetX, targetY;

        if (Math.abs(vx) < 0.1f) {
            targetX = initialX;

            if (vy > 0.0f) {
                targetY = Config.mBubbleMaxY;
            } else {
                targetY = Config.mBubbleMinY;
            }
        } else {

            if (vx > 0.0f) {
                targetX = Config.mBubbleSnapRightX;
            } else {
                targetX = Config.mBubbleSnapLeftX;
            }

            float m = vy / vx;

            targetY = (int) (m * (targetX - initialX) + initialY);

            if (targetY < Config.mBubbleMinY) {
                targetY = Config.mBubbleMinY;
                targetX = (int) (initialX + (targetY - initialY) / m);
            } else if (targetY > Config.mBubbleMaxY) {
                targetY = Config.mBubbleMaxY;
                targetX = (int) (initialX + (targetY - initialY) / m);
            } else {
                animType = DraggableHelper.AnimationType.MediumOvershoot;
            }
        }

        float flickDistance = Util.distance(initialX, initialY, targetX, targetY);
        float flickVelocity = (float) Math.sqrt(vx*vx + vy*vy);
        float flickAnimPeriod = flickDistance / flickVelocity;
        flickAnimPeriod = Util.clamp(0.05f, flickAnimPeriod, 0.5f);

        // Check for tractor beam intercept

        // Get center line of flick
        float x0 = initialX + Config.mBubbleWidth * 0.5f;
        float y0 = initialY + Config.mBubbleHeight * 0.5f;
        float x1 = targetX + Config.mBubbleWidth * 0.5f;
        float y1 = targetY + Config.mBubbleHeight * 0.5f;

        // Get the closest (if any) snap target that will be able to grab the bubble.
        final BubbleTargetView tv = mCanvasView.getSnapTarget(x0, y0, x1, y1, mTractorBeamIntersectionPoint);
        if (tv != null) {
            float intBubbleX = mTractorBeamIntersectionPoint.x - Config.mBubbleWidth * 0.5f;
            float intBubbleY = mTractorBeamIntersectionPoint.y - Config.mBubbleHeight * 0.5f;

            float intersectionDistance = Util.distance(initialX, initialY, intBubbleX, intBubbleY);
            float intFraction = 0.0f;
            if (flickDistance > 0.0001f) {
                intFraction = intersectionDistance / flickDistance;
            }
            try {
                Util.Assert(intFraction >= 0.0f && intFraction <= 1.05f, "intFraction:" + intFraction + ", flickDistance:" + flickDistance);
                float intTime = flickAnimPeriod * intFraction;

                animType = DraggableHelper.AnimationType.Linear;
                flickAnimPeriod = intTime;
                targetX = (int) intBubbleX;
                targetY = (int) intBubbleY;

                tv.setTargetCenter(mTractorBeamIntersectionPoint.x, mTractorBeamIntersectionPoint.y);
            }
            catch (AssertionError exc) {
                if (animType != DraggableHelper.AnimationType.Linear) {
                    flickAnimPeriod += 0.15f;
                }
            }

        } else {
            if (animType != DraggableHelper.AnimationType.Linear) {
                flickAnimPeriod += 0.15f;
            }
        }

        // #431 - Ensure there is always >0 time to animate the flick.
        flickAnimPeriod = Math.max(0.01f, flickAnimPeriod);

        animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(Constant.BUBBLE_ANIM_TIME);
        mBadgeView.animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(Constant.BUBBLE_ANIM_TIME);

        setTargetPos(targetX, targetY, flickAnimPeriod, animType, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                BubbleTargetView.disableTractor();
                onAnimComplete();

                MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                if (tv == null) {
                    int x = mDraggableHelper.getXPos();
                    if (x != Config.mBubbleSnapLeftX && x != Config.mBubbleSnapRightX) {
                        doSnap();
                    }
                } else {
                    Constant.BubbleAction action = tv.getAction();
                    doSnapAction(action);
                }
            }

            @Override
            public void onCancel() {
                onAnimComplete();
            }
        });
    }

    public void snapToBubbleView() {
        mMode = Mode.BubbleView;
        mDraggableHelper.cancelAnimation();

        MainController.get().collapseBubbleFlow(0);

        Point bubbleRestingPoint = Settings.get().getBubbleRestingPoint();
        setTargetPos(bubbleRestingPoint.x, bubbleRestingPoint.y, 0, DraggableHelper.AnimationType.Linear, null);

        MainApplication.postEvent(getContext(), mEndCollapseTransitionEvent);
    }

    private void doAnimateToBubbleView(int animTimeMs) {
        if (mAnimActive) {
            if (mMode == Mode.BubbleView) {
                return;
            } else {
                mDraggableHelper.cancelAnimation();
            }
        }

        //StackTraceElement[] cause = Thread.currentThread().getStackTrace();
        //String log = "";
        //for (StackTraceElement i : cause) {
        //    log += i.toString() + "\n";
        //}
        //Log.d(TAG, "doAnimateToBubbleView() - " + log);

        mTouchDown = false;
        mMode = Mode.BubbleView;

        if (MainController.get().getActiveTabCount() == 0) {
            return;
        }

        if (animTimeMs == 0) {
            animTimeMs = Constant.BUBBLE_ANIM_TIME;
        }

        animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(animTimeMs);
        mBadgeView.animate().alpha(Constant.BUBBLE_MODE_ALPHA).setDuration(animTimeMs);

        float bubblePeriod = (float)animTimeMs / 1000.f;
        float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        MainController mainController = MainController.get();
        setVisibility(View.VISIBLE);
        TabView currentTab = mBubbleFlowDraggable.getCurrentTab();
        if (currentTab != null) {
            // ensure imitator image is up to date, fixes #228
            mFavicon.clearImage();
            currentTab.setImitator(this);
        }

        Point bubbleRestingPoint = Settings.get().getBubbleRestingPoint();
        setTargetPos(bubbleRestingPoint.x, bubbleRestingPoint.y, bubblePeriod, DraggableHelper.AnimationType.SmallOvershoot, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                MainApplication.postEvent(getContext(), mEndCollapseTransitionEvent);
                onAnimComplete();
            }
            @Override
            public void onCancel() {
                onAnimComplete();
            }
        });

        mainController.endAppPolling();
        mainController.collapseBubbleFlow((long) (contentPeriod * 1000));

        mBeginCollapseTransitionEvent.mPeriod = contentPeriod;
        MainApplication.postEvent(getContext(), mBeginCollapseTransitionEvent);
    }

    private void doAnimateToContentView() {
        doAnimateToContentView(true);
    }

    private void doAnimateToContentView(boolean saveBubbleRestingPoint) {
        CrashTracking.log("doAnimateToContentView()");
        if (mAnimActive) {
            if (mMode == Mode.ContentView) {
                CrashTracking.log("doAnimateToContentView() mMode == Mode.ContentView, early exit");
                return;
            } else {
                CrashTracking.log("doAnimateToContentView() cancelAnimation()");
                mDraggableHelper.cancelAnimation();
            }
        }

        if (mMode != Mode.ContentView && saveBubbleRestingPoint) {
            Settings.get().setBubbleRestingPoint(mDraggableHelper.getXPos(), mDraggableHelper.getYPos());
        }

        mTouchDown = false;
        mMode = Mode.ContentView;

        final float bubblePeriod = (float) Constant.BUBBLE_ANIM_TIME / 1000.f;
        final float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        final MainController mainController = MainController.get();
        setVisibility(View.VISIBLE);

        animate().alpha(1.0f).setDuration(Constant.BUBBLE_ANIM_TIME);
        mBadgeView.animate().alpha(1.0f).setDuration(Constant.BUBBLE_ANIM_TIME);

        int xp = (int) Config.getContentViewX(0, 1);
        int yp = Config.mContentViewBubbleY;

        setTargetPos(xp, yp, bubblePeriod, DraggableHelper.AnimationType.SmallOvershoot, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                onAnimComplete();
                int activeCount = mainController.getActiveTabCount();
                if (activeCount == 0) {
                    // Ensure we don't enter state where there are no tabs to display. Fix #448
                    MainApplication.postEvent(getContext(), new MainController.EndCollapseTransitionEvent());
                    MainApplication.postEvent(getContext(), new ExpandedActivity.MinimizeExpandedActivityEvent());
                    CrashTracking.log("doAnimateToContentView(): onAnimationComplete(): getActiveTabCount()==0");
                } else {
                    MainApplication.postEvent(getContext(), mEndExpandTransitionEvent);
                    CrashTracking.log("doAnimateToContentView(): onAnimationComplete(): getActiveTabCount():" + activeCount);
                }
            }
            @Override
            public void onCancel() {
                onAnimComplete();
                mainController.endAppPolling();
                mainController.collapseBubbleFlow((long) (contentPeriod * 1000));
            }
        });
        mainController.beginAppPolling();
        mainController.expandBubbleFlow((long) (contentPeriod * 1000), true);
    }

    public void configure(int x0, int y0, int targetX, int targetY, int targetTime, CanvasView cv)  {

        try {
            super.configure("http://blerg.com"); // the URL is not actually used...
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //setBackgroundColor(0xff00ff00);

        mMode = Mode.BubbleView;
        mAnimActive = false;
        mHasMoved = false;
        mCanvasView = cv;
        mBadgeView = (BadgeView) findViewById(R.id.badge_view);
        mBadgeView.hide();
        mBadgeView.setVisibility(View.GONE);
        mCircle = new Circle(0, 0, 1);

        int bubbleSize = getResources().getDimensionPixelSize(R.dimen.bubble_size);

        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
        windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManagerParams.x = x0;
        windowManagerParams.y = y0;
        windowManagerParams.height = bubbleSize;
        windowManagerParams.width = bubbleSize;
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowManagerParams.format = PixelFormat.TRANSPARENT;
        windowManagerParams.setTitle("LinkBubble: BubbleDraggable");

        mDraggableHelper = new DraggableHelper(this, windowManagerParams, true, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent e) {
                if (!mAnimActive) {
                    mCurrentSnapTarget = null;
                    mHasMoved = false;
                    mTouchDown = true;
                    mTouchInitialX = e.posX;
                    mTouchInitialY = e.posY;

                    animate().alpha(1.0f).setDuration(Constant.BUBBLE_ANIM_TIME);
                    mBadgeView.animate().alpha(1.0f).setDuration(Constant.BUBBLE_ANIM_TIME);

                    MainController mainController = MainController.get();
                    if (mainController != null) {
                        mainController.scheduleUpdate();
                    }

                    MainApplication.postEvent(getContext(), mBeginBubbleDragEvent);
                    CrashTracking.log("BubbleDraggable.configure(): onActionDown() - start drag");
                }
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent e) {
                if (mTouchDown) {
                    int targetX = targetX = (int) (e.rawX - Config.mBubbleWidth * 0.5);
                    int targetY = (int) (e.rawY - Config.mBubbleHeight);

                    targetX = Util.clamp(Config.mBubbleSnapLeftX, targetX, Config.mBubbleSnapRightX);
                    targetY = Util.clamp(Config.mBubbleMinY, targetY, Config.mBubbleMaxY);

                    float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
                    if (d >= Config.dpToPx(10.0f)) {
                        mHasMoved = true;
                    }

                    if (!mAnimActive) {
                        // c = null, t = null           wasn't snapping, no snap -> move
                        // c = 1, t = null              was snapping, no snap -> anim out
                        // c = null, t = 1              wasn't snapping, is snap -> anim in
                        // c = 1, t = 1                 was snapping, is snapping -> NO move

                        mCircle.Update(targetX + Config.mBubbleWidth * 0.5f, targetY + Config.mBubbleHeight * 0.5f, Config.mBubbleWidth * 0.5f);
                        BubbleTargetView tv = mCanvasView.getSnapTarget(mCircle, 1.0f);

                        if (mCurrentSnapTarget == null) {
                            if (tv == null) {
                                setTargetPos(targetX, targetY, 0.0f, DraggableHelper.AnimationType.DistanceProportion, null);
                            } else {
                                tv.beginSnapping();
                                mCurrentSnapTarget = tv;
                                mTimeOnSnapTarget = 0.0f;

                                Circle dc = tv.GetDefaultCircle();
                                int xt = (int) (0.5f + dc.mX - Config.mBubbleWidth * 0.5f);
                                int yt = (int) (0.5f + dc.mY - Config.mBubbleHeight * 0.5f);
                                setTargetPos(xt, yt, Config.ANIMATE_TO_SNAP_TIME, DraggableHelper.AnimationType.Linear, new DraggableHelper.AnimationEventListener() {
                                    @Override
                                    public void onAnimationComplete() {
                                        onAnimComplete();
                                    }
                                    @Override
                                    public void onCancel() {
                                        onAnimComplete();
                                    }
                                });
                            }
                        } else {
                            if (tv == null) {
                                setTargetPos(targetX, targetY, 0.05f, DraggableHelper.AnimationType.Linear, new DraggableHelper.AnimationEventListener() {
                                    @Override
                                    public void onAnimationComplete() {
                                        mCurrentSnapTarget.endSnapping();
                                        mCurrentSnapTarget.endLongHovering();
                                        mCurrentSnapTarget = null;
                                        mTimeOnSnapTarget = 0.f;
                                        onAnimComplete();
                                    }
                                    @Override
                                    public void onCancel() {
                                        onAnimComplete();
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent e) {
                if (mTouchDown) {
                    CrashTracking.log("BubbleDraggable.configure(): onActionUp() - end drag");
                    mDraggableHelper.cancelAnimation();

                    if (mHasMoved) {
                        if (mCurrentSnapTarget == null) {
                            float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                            float threshold = Config.dpToPx(900.0f);
                            if (v > threshold) {
                                doFlick(e.vx, e.vy);
                                CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doFlick()");
                            } else {
                                boolean doBubbleView = mMode == Mode.BubbleView ||
                                        e.posX < Config.mScreenWidth * 0.2f ||
                                        e.posX > Config.mScreenWidth * 0.8f ||
                                        e.posY > Config.mScreenHeight * 0.5f;

                                if (doBubbleView) {
                                    mMode = Mode.BubbleView;
                                }
                                MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                                if (doBubbleView) {
                                    CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doSnap()");
                                    doSnap();
                                } else {
                                    CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doAnimateToContentView() [mHasMoved==true]");
                                    doAnimateToContentView();
                                }
                            }
                        } else {
                            MainApplication.postEvent(getContext(), mEndBubbleDragEvent);
                            CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doSnapAction()");
                            doSnapAction(mCurrentSnapTarget.getAction());
                        }
                    } else {
                        MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                        if (mMode == Mode.BubbleView) {
                            CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doAnimateToContentView() [mMode == Mode.BubbleView]");
                            doAnimateToContentView();
                        } else {
                            if (mMode == Mode.ContentView && mBubbleFlowDraggable.isExpanded() == false) {
                                CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doAnimateToContentView() [mMode == Mode.ContentView]");
                                doAnimateToContentView();
                            } else {
                                CrashTracking.log("BubbleDraggable.configure(): onActionUp() - doAnimateToBubbleView()");
                                doAnimateToBubbleView(0);
                            }
                        }
                    }

                    mTouchDown = false;
                }
            }
        });

        if (mDraggableHelper.isAlive()) {
            //to do debug
            //MainController.addRootWindow(this, windowManagerParams);
            //

            slideOnScreen(x0, y0, targetX, targetY, targetTime);
        }
    }

    public void slideOnScreen(int x0, int y0, int targetX, int targetY, int targetTime) {
        setExactPos(x0, y0);
        if (targetX != x0 || targetY != y0) {
            setTargetPos(targetX, targetY, (float) targetTime / 1000.f, DraggableHelper.AnimationType.LargeOvershoot, null);
        }
        CrashTracking.log("BubbleDraggable.slideOnScreen()");
    }

    public void setBubbleFlowDraggable(BubbleFlowDraggable bubbleFlowDraggable) {
        mBubbleFlowDraggable = bubbleFlowDraggable;
    }

    public void destroy() {
        //setOnTouchListener(null);
        setOnUpdateListener(null);  // prevent memory leak
        mDraggableHelper.destroy();
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        mOnUpdateListener = onUpdateListener;
    }

    @Override
    public DraggableHelper getDraggableHelper() {
        return mDraggableHelper;
    }

    @Override
    public void update(float dt) {
        if (mTouchDown) {
            if (mCurrentSnapTarget != null) {
                mTimeOnSnapTarget += dt;
                float snapTime = mTimeOnSnapTarget - Config.ANIMATE_TO_SNAP_TIME;
                if (mCurrentSnapTarget.isLongHovering() == false && snapTime >= Config.CLOSE_ALL_BUBBLES_DELAY) {
                    mCurrentSnapTarget.beginLongHovering();
                }
                if (!mAnimActive) {
                    Circle dc = mCurrentSnapTarget.GetDefaultCircle();
                    int xt = (int) (0.5f + dc.mX - Config.mBubbleWidth * 0.5f);
                    int yt = (int) (0.5f + dc.mY - Config.mBubbleHeight * 0.5f);
                    mDraggableHelper.setTargetPos(xt, yt, 0.02f, DraggableHelper.AnimationType.Linear, null);
                }
            }
            MainController.get().scheduleUpdate();
        }

        mDraggableHelper.update(dt);

        int x = mDraggableHelper.getXPos();
        int y = mDraggableHelper.getYPos();

        mDraggableBubbleMovedEvent.mX = x;
        mDraggableBubbleMovedEvent.mY = y;

        if (mOnUpdateListener != null) {
            mOnUpdateListener.onUpdate(BubbleDraggable.this, 0);
        }
    }

    @Override
    public void onOrientationChanged() {
        if (mMode == Mode.BubbleView) {
            doAnimateToBubbleView(1);
        } else {
            switchToExpandedView();
        }
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void setTargetPos(int xp, int yp, float t, DraggableHelper.AnimationType type, DraggableHelper.AnimationEventListener listener) {
        try {
            Util.Assert(!mAnimActive, "mAnimActive:" + mAnimActive);
        }
        catch (AssertionError e) {
            e.printStackTrace();
        }
        //Util.Assert(t > 0.0f, "t:" + t);      // Don't think this happens anymore - just to catch if it does happen and investigate why.
        mAnimActive = listener != null;
        mDraggableHelper.setTargetPos(xp, yp, t, type, listener);
    }

}
