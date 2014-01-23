package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.physics.Circle;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.util.Util;

import java.net.MalformedURLException;


public class BubbleDraggable extends BubbleView implements Draggable {

    private static final String TAG = "BubbleDraggable";

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private OnUpdateListener mOnUpdateListener;
    public BadgeView mBadgeView;
    private CanvasView mCanvasView;
    private BubbleFlowDraggable mBubbleFlowDraggable;

    private MainController.BeginBubbleDragEvent mBeginBubbleDragEvent = new MainController.BeginBubbleDragEvent();
    private MainController.DraggableBubbleMovedEvent mDraggableBubbleMovedEvent = new MainController.DraggableBubbleMovedEvent();
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();
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
    private boolean mFlickActive;
    private float mTimeOnSnapTarget;

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
        Util.Assert(mAnimActive);
        mAnimActive = false;
    }

    private void doSnap() {
        int xp = mDraggableHelper.getXPos();
        int yp = mDraggableHelper.getYPos();

        if (xp < Config.mScreenCenterX) {
            xp = Config.mBubbleSnapLeftX;
        } else {
            xp = Config.mBubbleSnapRightX;
        }

        Config.BUBBLE_HOME_X = xp;
        Config.BUBBLE_HOME_Y = yp;

        setTargetPos(xp, yp, 0.5f, DraggableHelper.AnimationType.MediumOvershoot, new DraggableHelper.AnimationEventListener() {
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

    public void switchToBubbleView() {
        doAnimateToBubbleView();
    }

    public void switchToExpandedView() {
        doAnimateToContentView();
    }

    private void doSnapAction(Config.BubbleAction action) {
        MainController mainController = MainController.get();

        float snapTime = mTimeOnSnapTarget - Config.ANIMATE_TO_SNAP_TIME;
        if (action == Config.BubbleAction.Destroy && snapTime >= Config.DESTROY_ALL_BUBBLES_DELAY) {
            mainController.destroyAllBubbles();
            mMode = Mode.BubbleView;
        } else {
            if (mainController.destroyCurrentBubble(action, false)) {
                doAnimateToContentView();
            } else {
                mMode = Mode.BubbleView;
            }
        }
    }

    private void doFlick(float vx, float vy) {
        DraggableHelper.AnimationType animType = DraggableHelper.AnimationType.Linear;
        float period = 0.0f;
        mFlickActive = true;
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
                period += 0.15f;
            }
        }

        float dx = targetX - initialX;
        float dy = targetY - initialY;
        float d = (float) Math.sqrt(dx*dx + dy*dy);

        float v = (float) Math.sqrt(vx*vx + vy*vy);

        period += d/v;
        period = Util.clamp(0.05f, period, 0.5f);

        setTargetPos(targetX, targetY, period, animType, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                mFlickActive = false;
                onAnimComplete();

                MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                if (mMode == Mode.ContentView) {
                    doAnimateToContentView();
                } else {
                    int x = mDraggableHelper.getXPos();
                    if (x != Config.mBubbleSnapLeftX && x != Config.mBubbleSnapRightX) {
                        doSnap();
                    }
                }
            }
            @Override
            public void onCancel() {
                onAnimComplete();
            }
        });
    }

    private void doAnimateToBubbleView() {
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
            throw new RuntimeException("Should be at least 1 bubble active to enter the AnimateToBubbleView state");
        }
        float bubblePeriod = (float)Constant.BUBBLE_ANIM_TIME / 1000.f;
        float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        MainController mainController = MainController.get();
        setVisibility(View.VISIBLE);

        int xp = Config.BUBBLE_HOME_X;
        int yp = Config.BUBBLE_HOME_Y;

        setTargetPos(xp, yp, bubblePeriod, DraggableHelper.AnimationType.SmallOvershoot, new DraggableHelper.AnimationEventListener() {
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
        if (mAnimActive) {
            if (mMode == Mode.ContentView) {
                return;
            } else {
                mDraggableHelper.cancelAnimation();
            }
        }

        mTouchDown = false;
        mMode = Mode.ContentView;

        final float bubblePeriod = (float) Constant.BUBBLE_ANIM_TIME / 1000.f;
        final float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        final MainController mainController = MainController.get();
        setVisibility(View.VISIBLE);

        int xp = (int) Config.getContentViewX(0, 1);
        int yp = Config.mContentViewBubbleY;

        setTargetPos(xp, yp, bubblePeriod, DraggableHelper.AnimationType.SmallOvershoot, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                onAnimComplete();
            }
            @Override
            public void onCancel() {
                onAnimComplete();
                mainController.endAppPolling();
                mainController.collapseBubbleFlow((long) (contentPeriod * 1000));
            }
        });
        mainController.beginAppPolling();
        mainController.expandBubbleFlow((long) (contentPeriod * 1000));
    }

    public void configure(int x0, int y0, int targetX, int targetY, float targetTime, CanvasView cv)  {

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

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

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

        mDraggableHelper = new DraggableHelper(this, mWindowManager, windowManagerParams, true, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent e) {
                if (!mAnimActive) {
                    mCurrentSnapTarget = null;
                    mHasMoved = false;
                    mTouchDown = true;
                    mTouchInitialX = e.posX;
                    mTouchInitialY = e.posY;

                    MainController mainController = MainController.get();
                    mainController.scheduleUpdate();

                    MainApplication.postEvent(getContext(), mBeginBubbleDragEvent);
                }
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent e) {
                if (mTouchDown) {
                    int targetX = mTouchInitialX + e.dx;
                    int targetY = mTouchInitialY + e.dy;

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

                        Circle c = new Circle(targetX + Config.mBubbleWidth * 0.5f, targetY + Config.mBubbleHeight * 0.5f, Config.mBubbleWidth * 0.5f);
                        BubbleTargetView tv = mCanvasView.getSnapTarget(c);

                        if (mCurrentSnapTarget == null) {
                            if (tv == null) {
                                setTargetPos(targetX, targetY, 0.02f, DraggableHelper.AnimationType.Linear, null);
                            } else {
                                tv.beginSnapping();
                                mCurrentSnapTarget = tv;
                                mTimeOnSnapTarget = 0.0f;

                                Circle dc = tv.GetDefaultCircle();
                                int xt = (int) (dc.mX - Config.mBubbleWidth * 0.5f);
                                int yt = (int) (dc.mY - Config.mBubbleHeight * 0.5f);
                                setTargetPos(xt, yt, Config.ANIMATE_TO_SNAP_TIME, DraggableHelper.AnimationType.MediumOvershoot, new DraggableHelper.AnimationEventListener() {
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
                                setTargetPos(targetX, targetY, 0.02f, DraggableHelper.AnimationType.Linear, new DraggableHelper.AnimationEventListener() {
                                    @Override
                                    public void onAnimationComplete() {
                                        mCurrentSnapTarget.endSnapping();
                                        mCurrentSnapTarget = null;
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
                    mDraggableHelper.cancelAnimation();

                    MainController mainController = MainController.get();

                    if (mHasMoved) {

                        if (mCurrentSnapTarget == null) {
                            if (mMode == Mode.ContentView) {
                                MainApplication.postEvent(getContext(), mEndBubbleDragEvent);
                                doAnimateToContentView();
                            } else {
                                float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                                float threshold = Config.dpToPx(900.0f);
                                if (v > threshold) {
                                    doFlick(e.vx, e.vy);
                                } else {
                                    MainApplication.postEvent(getContext(), mEndBubbleDragEvent);
                                    doSnap();
                                }
                            }
                        } else {
                            MainApplication.postEvent(getContext(), mEndBubbleDragEvent);
                            doSnapAction(mCurrentSnapTarget.getAction());
                        }
                    } else {
                        MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                        if (mMode == Mode.BubbleView) {
                            doAnimateToContentView();
                        } else {
                            if (mMode == Mode.ContentView && mBubbleFlowDraggable.isExpanded() == false) {
                                doAnimateToContentView();
                            } else {
                                doAnimateToBubbleView();
                            }
                        }
                    }

                    mTouchDown = false;
                }
            }
        });

        if (mDraggableHelper.isAlive()) {
            mWindowManager.addView(this, windowManagerParams);

            setExactPos(x0, y0);
            if (targetX != x0 || targetY != y0) {
                setTargetPos(targetX, targetY, targetTime, DraggableHelper.AnimationType.LargeOvershoot, null);
            }
        }
    }

    public void setBubbleFlowDraggable(BubbleFlowDraggable bubbleFlowDraggable) {
        mBubbleFlowDraggable = bubbleFlowDraggable;
    }

    public void destroy() {
        //setOnTouchListener(null);
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

        if (mFlickActive && mCurrentSnapTarget == null) {
            int x = mDraggableHelper.getXPos();
            int y = mDraggableHelper.getYPos();

            Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f, y + Config.mBubbleHeight * 0.5f, Config.mBubbleWidth * 0.5f);
            BubbleTargetView tv = mCanvasView.getSnapTarget(bubbleCircle);

            if (tv != null) {
                mCurrentSnapTarget = tv;
                mTimeOnSnapTarget = 0.0f;
                mCurrentSnapTarget.beginSnapping();

                Circle c = tv.GetDefaultCircle();
                int xt = (int) (c.mX - Config.mBubbleWidth * 0.5f);
                int yt = (int) (c.mY - Config.mBubbleHeight * 0.5f);

                mDraggableHelper.cancelAnimation();

                setTargetPos(xt, yt, 0.3f, DraggableHelper.AnimationType.LargeOvershoot, new DraggableHelper.AnimationEventListener() {
                    @Override
                    public void onAnimationComplete() {
                        MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                        onAnimComplete();
                        mFlickActive = false;

                        Config.BubbleAction action = mCurrentSnapTarget.getAction();
                        mCurrentSnapTarget.endSnapping();
                        mCurrentSnapTarget = null;

                        doSnapAction(action);
                    }
                    @Override
                    public void onCancel() {
                        onAnimComplete();
                    }
                });
            }
        }

        if (mTouchDown) {
            if (mCurrentSnapTarget != null) {
                mTimeOnSnapTarget += dt;
            }
            MainController.get().scheduleUpdate();
        }

        mDraggableHelper.update(dt);

        int x = mDraggableHelper.getXPos();
        int y = mDraggableHelper.getYPos();

        mDraggableBubbleMovedEvent.mX = x;
        mDraggableBubbleMovedEvent.mY = y;
        MainApplication.postEvent(getContext(), mDraggableBubbleMovedEvent);

        if (mOnUpdateListener != null) {
            mOnUpdateListener.onUpdate(this, dt);
        }
    }

    @Override
    public void onOrientationChanged() {
        if (mMode == Mode.BubbleView) {
            switchToBubbleView();
        } else {
            switchToExpandedView();
        }
    }

    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void setTargetPos(int xp, int yp, float t, DraggableHelper.AnimationType type, DraggableHelper.AnimationEventListener listener) {
        Util.Assert(!mAnimActive);
        Util.Assert(t > 0.0f);      // Don't think this happens anymore - just to catch if it does happen and investigate why.
        mAnimActive = listener != null;
        mDraggableHelper.setTargetPos(xp, yp, t, type, listener);
    }

}
