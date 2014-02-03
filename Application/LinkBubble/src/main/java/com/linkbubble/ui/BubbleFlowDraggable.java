package com.linkbubble.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
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
//import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.util.VerticalGestureListener;

import java.net.MalformedURLException;


public class BubbleFlowDraggable extends BubbleFlowView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;
    private int mBubbleFlowHeight;
    private TabView mCurrentTab;
    private BubbleDraggable mBubbleDraggable;
    private Point mTempSize = new Point();

    private MainController.CurrentTabChangedEvent mCurrentTabChangedEvent = new MainController.CurrentTabChangedEvent();

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

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        mBubbleFlowWidth = Config.mScreenWidth;
        mBubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_pager_height);

        configure(mBubbleFlowWidth,
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));

        setBubbleFlowViewListener(new BubbleFlowView.Listener() {
            @Override
            public void onCenterItemClicked(BubbleFlowView sender, View view) {
                MainController.get().switchToBubbleView();
            }

            @Override
            public void onCenterItemLongClicked(BubbleFlowView sender, View view) {
                if (view instanceof TabView) {
                    //shrink(Constant.BUBBLE_ANIM_TIME);
                    MainController.get().startDraggingFromContentView();
                }
            }

            @Override
            public void onCenterItemSwiped(VerticalGestureListener.GestureDirection gestureDirection) {
                // TODO: Implement me
            }

            @Override
            public void onCenterItemChanged(BubbleFlowView sender, View view) {
                setCurrentTab((TabView) view);
            }
        });

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

    @Override
    void configure(int width, int itemWidth, int itemHeight) {
        mBubbleFlowWidth = Config.mScreenWidth;

        super.configure(width, itemWidth, itemHeight);

        if (mDraggableHelper != null && mDraggableHelper.getWindowManagerParams() != null) {
            //mWindowManager.removeView(this);

            WindowManager.LayoutParams windowManagerParams = mDraggableHelper.getWindowManagerParams();
            windowManagerParams.width = width;
            windowManagerParams.x = 0;
            windowManagerParams.y = 0;
            windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;

            //mWindowManager.addView(this, windowManagerParams);
            setExactPos(0, 0);
        }
    }

    public void destroy() {
        //setOnTouchListener(null);
        //mWindowManager.removeView(this);
        mDraggableHelper.destroy();
    }

    // The number of items currently actively managed by the BubbleFlowView.
    // Note: it's possible for getActiveTabCount() to equal 0, but getVisibleTabCount() to be > 0.
    public int getActiveTabCount() {
        return mViews.size();
    }

    // The number of items being drawn on the BubbleFlowView.
    // Note: It's possible for getVisibleTabCount() to be greater than getActiveTabCount() in the event an item is animating off.
    // Eg, when Back is pressed to dismiss the last Bubble.
    // This function does NOT return the number of items currently fitting on the current width of the screen.
    public int getVisibleTabCount() {
        return mContent.getChildCount();
    }

    @Override
    public boolean expand(long time, final AnimationEventListener animationEventListener) {

        if (isExpanded() == false && mCurrentTab != null) {
            // Ensure the centerIndex matches the current bubble. This should only *NOT* be the case when
            // restoring with N Bubbles from a previous session and the user clicks to expand the BubbleFlowView.
            int currentBubbleIndex = getIndexOfView(mCurrentTab);
            int centerIndex = getCenterIndex();
            if (centerIndex > -1 && currentBubbleIndex != centerIndex && isAnimatingToCenterIndex() == false) {
                setCenterIndex(currentBubbleIndex, false);
            }
        }

        if (super.expand(time, animationEventListener)) {
            int centerIndex = getCenterIndex();
            if (centerIndex > -1) {
                setCurrentTab((TabView) mViews.get(centerIndex));
            }
            return true;
        }

        return false;
    }

    public TabView getCurrentTab() {
        return mCurrentTab;
    }

    private void setCurrentTab(TabView tab) {
        if (mCurrentTab == tab) {
            return;
        }

        if (mCurrentTab != null) {
            mCurrentTab.setImitator(null);
        }
        mCurrentTab = tab;
        mCurrentTabChangedEvent.mTab = tab;
        MainApplication.postEvent(getContext(), mCurrentTabChangedEvent);
        if (mCurrentTab != null) {
            mCurrentTab.setImitator(mBubbleDraggable);
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
    public void update(float dt) {
        mDraggableHelper.update(dt);
    }

    public void syncWithBubble(Draggable draggable) {
        WindowManager.LayoutParams draggableParams = draggable.getDraggableHelper().getWindowManagerParams();

        int xOffset = (draggableParams.width - mBubbleFlowWidth) / 2;
        int yOffset = (draggableParams.height - mBubbleFlowHeight) / 2;

        mDraggableHelper.setExactPos(draggableParams.x + xOffset, draggableParams.y + yOffset);
    }

    @Override
    public void onOrientationChanged() {
        clearTargetPos();

        mWindowManager.getDefaultDisplay().getSize(mTempSize);
        configure(mTempSize.x, mItemWidth, mItemHeight);
        updatePositions();
        updateScales(getScrollX());

        setExactPos(0, 0);
    }

    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public TabView openUrlInBubble(String url, long startTime, boolean setAsCurrentBubble, boolean hasShownAppPicker) {
        TabView bubble;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            bubble = (TabView) inflater.inflate(R.layout.view_tab, null);
            bubble.configure(url, startTime, hasShownAppPicker);
        } catch (MalformedURLException e) {
            // TODO: Inform the user somehow?
            return null;
        }

        // Only insert next to current Bubble when in ContentView mode. Ensures links opened when app is
        // minimized are added to the end.
        add(bubble, mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView);

        mBubbleDraggable.mBadgeView.setCount(getActiveTabCount());

        if (setAsCurrentBubble) {
            setCurrentTab(bubble);
        }

        Settings.get().saveCurrentBubbles(mViews);
        return bubble;
    }

    @Override
    protected void remove(final int index, boolean animateOff, boolean removeFromList, OnRemovedListener onRemovedListener) {
        if (index < 0 || index >= mViews.size()) {
            return;
        }
        TabView tab = (TabView) mViews.get(index);
        super.remove(index, animateOff, removeFromList, onRemovedListener);
        if (animateOff && mSlideOffAnimationPlaying) {
            // Kick off an update so as to ensure BubbleFlowView.update() is always called when animating items off screen (see #189)
            MainController.get().scheduleUpdate();
            if (getActiveTabCount() == 0 && getVisibleTabCount() > 0) {
                // Bit of a hack, but we need to ensure CanvasView has a valid mContentView, so use the one currently being killed.
                // This is perfectly safe, because the view doesn't get destroyed until it has animated off screen.
                MainController.BeginAnimateFinalTabAwayEvent event = new MainController.BeginAnimateFinalTabAwayEvent();
                event.mTab = tab;
                MainApplication.postEvent(getContext(), event);
            }
        }
    }

    private OnRemovedListener mOnTabRemovedListener = new OnRemovedListener() {

        @Override
        public void onRemoved(View view) {
            ((TabView)view).destroy();
        }
    };

    private void destroyBubble(TabView tab, boolean animateRemove, boolean removeFromList) {
        int index = mViews.indexOf(tab);
        if (index == -1) {
            return;
        }
        remove(index, animateRemove, removeFromList, mOnTabRemovedListener);

        // Don't do this if we're animating the final tab off, as the setCurrentTab() call messes with the
        // CanvasView.mContentView, which has already been forcible set above in remove() via BeginAnimateFinalTabAwayEvent.
        boolean animatingFinalTabOff = getActiveTabCount() == 0 && getVisibleTabCount() > 0 && mSlideOffAnimationPlaying;
        if (mCurrentTab == tab && animatingFinalTabOff == false) {
            TabView newCurrentBubble = null;
            int viewsCount = mViews.size();
            if (viewsCount > 0) {
                if (viewsCount == 1) {
                    newCurrentBubble = (TabView) mViews.get(0);
                } else if (index < viewsCount) {
                    newCurrentBubble = (TabView) mViews.get(index);
                } else {
                    if (index > 0) {
                        newCurrentBubble = (TabView) mViews.get(index-1);
                    } else {
                        newCurrentBubble = (TabView) mViews.get(0);
                    }
                }
            }
            setCurrentTab(newCurrentBubble);
        }
    }

    private void postDestroyedBubble(boolean removeFromCurrentTabs) {
        if (removeFromCurrentTabs) {
            Settings.get().saveCurrentBubbles(mViews);
        }
    }

    public void destroyCurrentTab(boolean animateRemove, Config.BubbleAction action) {
        TabView currentTab = getCurrentTab();
        String url = currentTab.getUrl().toString();
        destroyBubble(currentTab, animateRemove, true);
        postDestroyedBubble(true);
        if (action != Config.BubbleAction.None) {
            MainApplication.handleBubbleAction(getContext(), action, url);
        }
    }

    public void destroyAllBubbles(boolean removeFromCurrentTabs) {
        for (View view : mViews) {
            destroyBubble(((TabView) view), false, false);
        }

        mViews.clear();
        postDestroyedBubble(removeFromCurrentTabs);
    }

    public void updateIncognitoMode(boolean incognito) {
        for (View view : mViews) {
            ((TabView)view).updateIncognitoMode(incognito);
        }
    }

}
