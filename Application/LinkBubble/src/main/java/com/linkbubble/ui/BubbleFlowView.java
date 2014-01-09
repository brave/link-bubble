package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Circle;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.physics.State_ContentView;
import com.linkbubble.physics.State_SnapToEdge;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.MalformedURLException;
import java.util.Vector;

public class BubbleFlowView extends FancyCoverFlow implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;

    private static Vector<BubbleFlowItemView> mBubbles = new Vector<BubbleFlowItemView>();

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

        setBackgroundColor(0x33ff0000);
    }

    public void configure(int x0, int y0, int targetX, int targetY, float targetTime, EventHandler eh)  {
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        mBubbleFlowWidth = getResources().getDimensionPixelSize(R.dimen.bubble_flow_width);

        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
        windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManagerParams.x = x0;
        windowManagerParams.y = y0;
        int bubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_flow_height);
        windowManagerParams.height = bubbleFlowHeight;
        windowManagerParams.width = mBubbleFlowWidth;
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowManagerParams.format = PixelFormat.TRANSPARENT;
        windowManagerParams.setTitle("LinkBubble: BubbleFlowView");

        mDraggableHelper = new DraggableHelper(this, mWindowManager, windowManagerParams, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent event) {
                //collapse();
                mEventHandler.onMotionEvent_Touch(BubbleFlowView.this, event);
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubbleFlowView.this, event);
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                //expand();
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

    public int getBubbleCount() {
        return mBubbles.size();
    }

    public void readd() {
        mWindowManager.removeView(this);
        mWindowManager.addView(this, mDraggableHelper.getWindowManagerParams());
    }

    @Override
    public ContentView getContentView() {
        return null;
    }

    public boolean isSnapping() {
        return mDraggableHelper.isSnapping();
    }

    @Override
    public DraggableHelper getDraggableHelper() {
        return mDraggableHelper;
    }

    @Override
    public BubbleLegacyView getBubbleLegacyView() {
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

    public int getXPos() {
        return mDraggableHelper.getXPos();
    }

    public int getYPos() {
        return mDraggableHelper.getYPos();
    }

    public void expand() {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        lp.width = mBubbleFlowWidth;
        setLayoutParams(lp);
    }

    void collapse() {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        lp.width = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        setLayoutParams(lp);
    }

    public void onOrientationChanged(boolean contentViewMode) {
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

    public void openUrlInBubble(String url, long startTime) {

        int x, targetX, y, targetY;
        float time;

        int bubbleIndex = mBubbles.size();

        if (MainController.get().isStateActive(State_ContentView.class)) {
            x = (int) Config.getContentViewX(bubbleIndex, getBubbleCount()+1);
            y = (int) -Config.mBubbleHeight;
            targetX = x;
            targetY = Config.mContentViewBubbleY;
            time = 0.4f;
        } else {
            if (bubbleIndex == 0) {
                x = (int) (Config.mBubbleSnapLeftX - Config.mBubbleWidth);
                y = Config.BUBBLE_HOME_Y;
                targetX = Config.BUBBLE_HOME_X;
                targetY = y;
                time = 0.4f;
            } else {
                x = Config.BUBBLE_HOME_X;
                y = Config.BUBBLE_HOME_Y;
                targetX = x;
                targetY = y;
                time = 0.0f;
            }
        }

        BubbleFlowItemView bubble;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            bubble = (BubbleFlowItemView) inflater.inflate(R.layout.view_bubble_flow_item, null);
            bubble.configure(url, startTime,
                    new BubbleFlowItemView.BubbleFlowItemViewListener() {

                        @Override
                        public void onDestroyBubble() {

                        }

                        @Override
                        public void onMinimizeBubbles() {
                            if (MainController.get().isStateActive(State_AnimateToBubbleView.class) == false) {
                                MainController.get().switchState(MainController.get().STATE_AnimateToBubbleView);
                            }
                        }

                        @Override
                        public void onPageLoaded(ContentView.PageLoadInfo info) {

                        }

                    });
        } catch (MalformedURLException e) {
            // TODO: Inform the user somehow?
            return;
        }

        mBubbles.add(bubble);

        Settings.get().saveCurrentBubbles(mBubbles);

        // BFV_CHANGE:
        /*
        mBadgeView.attach(bubble);
        mBadgeView.setBubbleCount(mBubbles.size());
        int draggableCount = mDraggables.size();
        if (MainController.get().isStateActive(State_ContentView.class)) {
            draggable.getDraggableView().setVisibility(View.VISIBLE);
            for (int i=0 ; i < draggableCount ; ++i) {
                Draggable draggableItem = mDraggables.get(i);
                if (draggableItem != bubble) {
                    draggableItem.getDraggableHelper().setTargetPos((int)Config.getContentViewX(draggableItem.getBubbleView().getBubbleIndex(),
                            getBubbleCount()), draggableItem.getDraggableHelper().getYPos(), 0.2f, false);
                }
            }
        } else {
            mFrontBubble = bubble;
            for (int i=0 ; i < draggableCount ; ++i) {
                Draggable draggableItem = mDraggables.get(i);
                int vis = View.VISIBLE;
                if (draggableItem != mFrontBubble) {
                    vis = View.GONE;
                }
                draggableItem.getDraggableView().setVisibility(vis);
            }
        }*/

        BubbleFlowAdapter adapter = (BubbleFlowAdapter)getAdapter();
        if (adapter.mBubbles == null) {
            adapter.setBubbles(mBubbles);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).updateIncognitoMode(incognito);
        }
    }

}
