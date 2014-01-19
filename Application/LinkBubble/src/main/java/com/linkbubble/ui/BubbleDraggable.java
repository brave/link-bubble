package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
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

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private OnUpdateListener mOnUpdateListener;
    public BadgeView mBadgeView;
    private CanvasView mCanvasView;

    private MainController.BeginBubbleDragEvent mBeginBubbleDragEvent = new MainController.BeginBubbleDragEvent();
    private MainController.DraggableBubbleMovedEvent mDraggableBubbleMovedEvent = new MainController.DraggableBubbleMovedEvent();
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();
    private MainController.BeginCollapseTransitionEvent mBeginCollapseTransitionEvent = new MainController.BeginCollapseTransitionEvent();
    private MainController.EndCollapseTransitionEvent mEndCollapseTransitionEvent = new MainController.EndCollapseTransitionEvent();

    // Physics state
    private enum Mode {
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

    public BubbleDraggable(Context context) {
        this(context, null);
    }

    public BubbleDraggable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleDraggable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void onAnimComplete() {
        Util.Assert(mAnimActive == true);
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
        });
    }

    public void switchToBubbleView() {
        doAnimateToBubbleView();
    }

    public void switchToExpandedView() {
        doAnimateToContentView();
    }

    private void doAnimateToBubbleView() {
        if (mMode == Mode.BubbleView)
            return;

        mMode = Mode.BubbleView;

        if (MainController.get().getBubbleCount() == 0) {
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
        });

        mainController.endAppPolling();
        mainController.collapseBubbleFlow((long) (contentPeriod * 1000));

        mBeginCollapseTransitionEvent.mPeriod = contentPeriod;
        MainApplication.postEvent(getContext(), mBeginCollapseTransitionEvent);
    }

    private void doAnimateToContentView() {
        if (mMode == Mode.ContentView)
            return;

        mMode = Mode.ContentView;

        float bubblePeriod = (float) Constant.BUBBLE_ANIM_TIME / 1000.f;
        float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        MainController mainController = MainController.get();
        setVisibility(View.VISIBLE);

        int xp = (int) Config.getContentViewX(0, 1);
        int yp = Config.mContentViewBubbleY;

        setTargetPos(xp, yp, bubblePeriod, DraggableHelper.AnimationType.SmallOvershoot, new DraggableHelper.AnimationEventListener() {
            @Override
            public void onAnimationComplete() {
                onAnimComplete();
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

                    setTargetPos(targetX, targetY, 0.02f, DraggableHelper.AnimationType.Linear, null);
                }
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent e) {
                if (mTouchDown) {
                    clearTargetPos();

                    MainController mainController = MainController.get();

                    MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                    if (mHasMoved) {

                        if (mCurrentSnapTarget == null) {
                            if (mMode == Mode.ContentView) {
                                doAnimateToContentView();
                            } else {
                                doSnap();
                            }
                        } else {
                            if (mCurrentSnapTarget.getAction() == Config.BubbleAction.Destroy) {
                                mainController.destroyAllBubbles();
                                mMode = Mode.BubbleView;
                            } else {
                                if (mainController.destroyCurrentBubble(mCurrentSnapTarget.getAction())) {
                                    doAnimateToBubbleView();
                                } else {
                                    mMode = Mode.BubbleView;
                                }
                            }
                        }

/*                        if (mCurrentSnapTarget == null) {
                            float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                            float threshold = Config.dpToPx(900.0f);
                            if (v > threshold) {
                                mainController.STATE_Flick_BubbleView.init(sender, e.vx, e.vy);
                                mainController.switchState(mainController.STATE_Flick_BubbleView);
                                mainController.hideContentActivity();
                                endDragEvent = false;
                            } else {
                                mainController.STATE_SnapToEdge.init(sender);
                                mainController.switchState(mainController.STATE_SnapToEdge);
                            }
                        } else {
                            if (mCurrentSnapTarget.getAction() == Config.BubbleAction.Destroy) {
                                mainController.destroyAllBubbles();
                                mainController.switchState(mainController.STATE_BubbleView);
                            } else {
                                if (mainController.destroyCurrentBubble(snapTarget.getAction())) {
                                    mainController.switchState(mainController.STATE_AnimateToBubbleView);
                                } else {
                                    mainController.switchState(mainController.STATE_BubbleView);
                                }
                            }
                        }*/
                    } else {
                        if (mMode == Mode.BubbleView) {
                            doAnimateToContentView();
                        } else {
                            doAnimateToBubbleView();
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
        switchToBubbleView();
    }

    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void setTargetPos(int xp, int yp, float t, DraggableHelper.AnimationType type, DraggableHelper.AnimationEventListener listener) {
        Util.Assert(!mAnimActive);

        if (listener == null) {
            Circle bubbleCircle = new Circle(xp + Config.mBubbleWidth * 0.5f, yp + Config.mBubbleHeight * 0.5f, Config.mBubbleWidth * 0.5f);
            BubbleTargetView tv = mCanvasView.getSnapTarget(bubbleCircle);

            if (tv != null) {

                if (tv != mCurrentSnapTarget) {
                    mCurrentSnapTarget = tv;
                    mCurrentSnapTarget.beginSnapping();
                }

                Circle c = tv.GetDefaultCircle();
                int xt = (int) (c.mX - Config.mBubbleWidth * 0.5f);
                int yt = (int) (c.mY - Config.mBubbleHeight * 0.5f);
                mDraggableHelper.setTargetPos(xt, yt, 0.3f, DraggableHelper.AnimationType.LargeOvershoot, null);
            } else {

                if (mCurrentSnapTarget != null) {
                    mCurrentSnapTarget.endSnapping();
                    mCurrentSnapTarget = null;
                }

                if (t == 0.0f) {
                    mDraggableHelper.clearTargetPos();
                    mDraggableHelper.setExactPos(xp, yp);
                } else {
                    mDraggableHelper.setTargetPos(xp, yp, t, type, null);
                }
            }
        } else {
            mAnimActive = true;
            mDraggableHelper.setTargetPos(xp, yp, t, type, listener);
        }
    }

}
