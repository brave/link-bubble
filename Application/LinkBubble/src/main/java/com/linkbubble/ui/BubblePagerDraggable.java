package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.physics.State_ContentView;

import java.net.MalformedURLException;
import java.util.Vector;


public class BubblePagerDraggable extends BubblePagerView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;

    private static Vector<BubblePagerItemView> mBubbles = new Vector<BubblePagerItemView>();

    public interface EventHandler {
        public void onMotionEvent_Touch(BubblePagerDraggable sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubblePagerDraggable sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubblePagerDraggable sender, DraggableHelper.ReleaseEvent event);
    }

    public BubblePagerDraggable(Context context) {
        this(context, null);
    }

    public BubblePagerDraggable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubblePagerDraggable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void configure(int x0, int y0, int targetX, int targetY, float targetTime, EventHandler eh)  {

        ViewPager pager = getViewPager();
        PagerAdapter adapter = new BubblePagerAdapter(getContext());
        pager.setAdapter(adapter);
        //Necessary or the pager will only have one extra page to show
        // make this at least however many pages you can see
        pager.setOffscreenPageLimit(adapter.getCount());
        //A little space between pages
        pager.setPageMargin(15);

        //If hardware acceleration is enabled, you should also remove
        // clipping on the pager for its children.
        pager.setClipChildren(false);

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
                mEventHandler.onMotionEvent_Touch(BubblePagerDraggable.this, event);
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubblePagerDraggable.this, event);
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                //expand();
                mEventHandler.onMotionEvent_Release(BubblePagerDraggable.this, event);
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
        mWindowManager.removeView(this);
        mDraggableHelper.destroy();
    }

    public int getBubbleCount() {
        return mBubbles.size();
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
            if (windowManagerParms.x < Config.mScreenHeight * 0.5f)
                xPos = Config.mBubbleSnapLeftX;
            else
                xPos = Config.mBubbleSnapRightX;
            float yf = (float)windowManagerParms.y / (float)Config.mScreenWidth;
            yPos = (int) (yf * Config.mScreenHeight);
        }

        setExactPos(xPos, yPos);
    }

    @Override
    public void readd() {
        mWindowManager.removeView(this);
        mWindowManager.addView(this, mDraggableHelper.getWindowManagerParams());
    }

    @Override
    public ContentView getContentView() {
        return null;
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

        BubblePagerItemView bubble;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            bubble = (BubblePagerItemView) inflater.inflate(R.layout.view_bubble_pager_item, null);
            bubble.configure(url, startTime,
                    new BubblePagerItemView.BubbleFlowItemViewListener() {

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

        BubblePagerAdapter adapter = (BubblePagerAdapter) getViewPager().getAdapter();
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
