package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;

import java.net.MalformedURLException;


public class BubbleDraggable extends BubbleView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private OnUpdateListener mOnUpdateListener;
    public BadgeView mBadgeView;

    private MainController.BeginBubbleDragEvent mBeginBubbleDragEvent = new MainController.BeginBubbleDragEvent();
    private MainController.EndBubbleDragEvent mEndBubbleDragEvent = new MainController.EndBubbleDragEvent();

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleDraggable sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleDraggable sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleDraggable sender, DraggableHelper.ReleaseEvent event);
    }

    public BubbleDraggable(Context context) {
        this(context, null);
    }

    public BubbleDraggable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleDraggable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void configure(int x0, int y0, int targetX, int targetY, float targetTime, EventHandler eh)  {

        try {
            super.configure("http://blerg.com"); // the URL is not actually used...
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //setBackgroundColor(0xff00ff00);

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
            public void onActionDown(DraggableHelper.TouchEvent event) {
                //collapse();
                MainApplication.postEvent(getContext(), mBeginBubbleDragEvent);

                mEventHandler.onMotionEvent_Touch(BubbleDraggable.this, event);
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubbleDraggable.this, event);
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                //expand();
                MainApplication.postEvent(getContext(), mEndBubbleDragEvent);

                mEventHandler.onMotionEvent_Release(BubbleDraggable.this, event);
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
    public View getDraggableView() {
        return this;
    }

    @Override
    public void update(float dt) {
        mDraggableHelper.update(dt);

        if (mOnUpdateListener != null) {
            mOnUpdateListener.onUpdate(this, dt);
        }
    }

    @Override
    public void onOrientationChanged(boolean contentViewMode) {
        clearTargetPos();

        int xPos, yPos;

        if (contentViewMode) {
            //xPos = (int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount());
            xPos = (int) Config.getContentViewX(0, MainController.get().getBubbleCount());
            yPos = Config.mContentViewBubbleY;
        } else {
            WindowManager.LayoutParams windowManagerParms = mDraggableHelper.getWindowManagerParams();
            if (windowManagerParms.x < Config.mScreenHeight * 0.5f) {
                xPos = Config.mBubbleSnapLeftX;
            } else {
                xPos = Config.mBubbleSnapRightX;
            }
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

}
