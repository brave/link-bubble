package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.physics.Circle;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.Draggable;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class BubbleFlowView extends FancyCoverFlow implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleFlowView sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleFlowView sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleFlowView sender, DraggableHelper.ReleaseEvent event);
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
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
        windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManagerParams.x = x0;
        windowManagerParams.y = y0;
        int bubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_flow_height);
        windowManagerParams.height = bubbleFlowHeight;
        int bubbleFlowWidth = getResources().getDimensionPixelSize(R.dimen.bubble_flow_width);
        windowManagerParams.width = bubbleFlowWidth;
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowManagerParams.format = PixelFormat.TRANSPARENT;
        windowManagerParams.setTitle("LinkBubble: BubbleFlowView");

        mDraggableHelper = new DraggableHelper(this, mWindowManager, windowManagerParams, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent event) {
                mEventHandler.onMotionEvent_Touch(BubbleFlowView.this, event);
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubbleFlowView.this, event);
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                mEventHandler.onMotionEvent_Release(BubbleFlowView.this, event);
            }
        });

        mEventHandler = eh;

        if (mDraggableHelper.isAlive()) {
            mWindowManager.addView(this, windowManagerParams);

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
        mDraggableHelper.destroy();
    }

    public void readd() {
        mWindowManager.removeView(this);
        mWindowManager.addView(this, mDraggableHelper.getWindowManagerParams());
    }

    public boolean isSnapping() {
        return mDraggableHelper.isSnapping();
    }

    @Override
    public DraggableHelper getDraggableHelper() {
        return mDraggableHelper;
    }

    @Override
    public BubbleView getBubbleView() {
        return null;
    }

    @Override
    public View getDraggableView() {
        return this;
    }

    @Override
    public void update(float dt, boolean contentView) {
        if (mDraggableHelper.update(dt, contentView)) {
            if (contentView) {
                //mContentView.setMarkerX(mDraggableHelper.getXPos());
            }
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
        return mDraggableHelper.getXPos();
    }

    public int getYPos() {
        return mDraggableHelper.getYPos();
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
            WindowManager.LayoutParams windowManagerParms = mDraggableHelper.getWindowManagerParams();
            if (windowManagerParms.x < Config.mScreenHeight * 0.5f)
                xPos = Config.mBubbleSnapLeftX;
            else
                xPos = Config.mBubbleSnapRightX;
            float yf = (float)windowManagerParms.y / (float)Config.mScreenWidth;
            yPos = (int) (yf * Config.mScreenHeight);
        }

        setExactPos(xPos, yPos);
    }


    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void setTargetPos(int x, int y, float t, boolean overshoot) {
        mDraggableHelper.setTargetPos(x, y, t, overshoot);
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
