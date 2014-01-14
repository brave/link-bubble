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
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.physics.State_ContentView;

import java.net.MalformedURLException;
import java.util.Vector;


public class BubbleFlowDraggable extends BubbleFlowView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;
    private int mBubbleFlowHeight;
    private BubbleFlowItemView mCurrentBubble;
    private BubbleDraggable mBubbleDraggable;

    private static Vector<BubbleFlowItemView> mBubbles = new Vector<BubbleFlowItemView>();

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleFlowDraggable sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleFlowDraggable sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleFlowDraggable sender, DraggableHelper.ReleaseEvent event);
    }

    public BubbleFlowDraggable(Context context) {
        this(context, null);
    }

    public BubbleFlowDraggable(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowDraggable(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void configure(EventHandler eventHandler)  {

        super.configure(getResources().getDimensionPixelSize(R.dimen.bubble_pager_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));

        setBubbleFlowViewListener(new BubbleFlowView.Listener() {
            @Override
            public void onCenterItemClicked(View view) {
                MainController mainController = MainController.get();
                mainController.getActiveDraggable().readd();
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }

            @Override
            public void onCenterItemChanged(View view) {
                if (view instanceof BubbleFlowItemView) {
                    MainController.get().showContentView(((BubbleFlowItemView)view).getContentView());
                }
            }
        });

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        mBubbleFlowWidth = getResources().getDimensionPixelSize(R.dimen.bubble_pager_width);
        mBubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_pager_height);

        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
        windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManagerParams.x = 0;
        windowManagerParams.y = 0;
        windowManagerParams.height = mBubbleFlowHeight;
        windowManagerParams.width = mBubbleFlowWidth;
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowManagerParams.format = PixelFormat.TRANSPARENT;
        windowManagerParams.setTitle("LinkBubble: BubbleFlowView");

        mDraggableHelper = new DraggableHelper(this, mWindowManager, windowManagerParams, false, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent event) {
                if (mEventHandler != null) {
                    mEventHandler.onMotionEvent_Touch(BubbleFlowDraggable.this, event);
                }
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                if (mEventHandler != null) {
                    mEventHandler.onMotionEvent_Move(BubbleFlowDraggable.this, event);
                }
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                if (mEventHandler != null) {
                    mEventHandler.onMotionEvent_Release(BubbleFlowDraggable.this, event);
                }
            }
        });

        mEventHandler = eventHandler;

        if (mDraggableHelper.isAlive()) {
            mWindowManager.addView(this, windowManagerParams);

            setExactPos(0, 0);
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

    public BubbleFlowItemView getCurrentBubble() {
        return mCurrentBubble;
    }

    private void setCurrentBubble(BubbleFlowItemView bubble, boolean showContentView) {
        if (mCurrentBubble != null) {
            mCurrentBubble.setImitator(null);
        }
        mCurrentBubble = bubble;
        if (mCurrentBubble != null) {
            mCurrentBubble.setImitator(mBubbleDraggable);
        }
        if (showContentView) {
            MainController.get().showContentView(bubble.getContentView());
        }
    }

    public void setBubbleDraggable(BubbleDraggable bubbleDraggable) {
        mBubbleDraggable = bubbleDraggable;
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

    public void syncWithBubble(Draggable draggable) {
        WindowManager.LayoutParams draggableParams = draggable.getDraggableHelper().getWindowManagerParams();

        int xOffset = (draggableParams.width - mBubbleFlowWidth) / 2;
        int yOffset = (draggableParams.height - mBubbleFlowHeight) / 2;

        mDraggableHelper.setExactPos(draggableParams.x + xOffset, draggableParams.y + yOffset);
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

    public ContentView getContentView() {
        return mBubbles != null && mBubbles.size() > 0 ? mBubbles.get(0).getContentView() : null;
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
                            if (info != null && info.url != null) {
                                MainApplication.saveUrlInHistory(getContext(), null, info.url, info.mHost, info.title);
                            }

                            MainController.get().onPageLoaded();
                        }

                    });
        } catch (MalformedURLException e) {
            // TODO: Inform the user somehow?
            return;
        }

        if (mCurrentBubble == null) {
            setCurrentBubble(bubble, false);
        }

        mBubbles.add(bubble);
        mBubbleDraggable.mBadgeView.setCount(mBubbles.size());

        Settings.get().saveCurrentBubbles(mBubbles);

        add(bubble);
    }

    private void destroyBubble(BubbleFlowItemView bubble) {
        mBubbles.remove(bubble);
        bubble.destroy();
    }

    private void postDestroyedBubble() {
        /*
        mBubbleDraggable.mBadgeView.setCount(mBubbles.size());

        getViewPager().getAdapter().notifyDataSetChanged();

        int bubbleCount = mBubbles.size();
        int currentItem = getViewPager().getCurrentItem();
        if (currentItem >= bubbleCount) {
            if (bubbleCount > 0) {
                setCurrentBubble(mBubbles.get(bubbleCount-1), true);
            } else {
                setCurrentBubble(null, false);
            }
        } else {
            if (bubbleCount == 0) {
                setCurrentBubble(null, true);
            } else {
                setCurrentBubble(mBubbles.get(currentItem), true);
            }
        }*/

        Settings.get().saveCurrentBubbles(mBubbles);
    }

    public void destroyCurrentBubble() {
        BubbleFlowItemView currentBubble = getCurrentBubble();
        destroyBubble(currentBubble);
        postDestroyedBubble();
    }

    public void destroyAllBubbles() {
        for (BubbleFlowItemView bubble : mBubbles) {
            destroyBubble(bubble);
        }
        mBubbles.clear();
        postDestroyedBubble();
    }

    public void updateIncognitoMode(boolean incognito) {
        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).updateIncognitoMode(incognito);
        }
    }

    public void show() {
        setVisibility(View.VISIBLE);
        //getViewPager().getAdapter().notifyDataSetChanged();
    }

    public void hide() {
        setVisibility(View.GONE);
    }
}
