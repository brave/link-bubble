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
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
//import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.util.Util;
import com.linkbubble.util.VerticalGestureListener;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Vector;


public class BubbleFlowDraggable extends BubbleFlowView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;
    private int mBubbleFlowHeight;
    private BubbleFlowItemView mCurrentBubble;
    private BubbleDraggable mBubbleDraggable;
    private Point mTempSize = new Point();

    private static Vector<BubbleFlowItemView> mBubbles = new Vector<BubbleFlowItemView>();

    private MainController.CurrentBubbleChangedEvent mCurrentBubbleChangedEvent = new MainController.CurrentBubbleChangedEvent();

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
                if (view instanceof BubbleFlowItemView) {
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
                setCurrentBubble((BubbleFlowItemView)view);
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
        super.configure(width, itemWidth, itemHeight);

        if (mDraggableHelper != null && mDraggableHelper.getWindowManagerParams() != null) {
            mDraggableHelper.getWindowManagerParams().width = width;
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
    public boolean expand() {
        if (super.expand()) {
            postExpand();
            return true;
        }

        return false;
    }

    @Override
    public boolean expand(long time, final AnimationEventListener animationEventListener) {
        if (super.expand(time, animationEventListener)) {
            postExpand();
            return true;
        }

        return false;
    }

    private void postExpand() {
        int centerIndex = getCenterIndex();
        if (centerIndex > -1) {
            setCurrentBubble((BubbleFlowItemView) mViews.get(centerIndex));
        }
    }

    public BubbleFlowItemView getCurrentBubble() {
        return mCurrentBubble;
    }

    private void setCurrentBubble(BubbleFlowItemView bubble) {
        if (mCurrentBubble == bubble) {
            return;
        }
        
        if (mCurrentBubble != null) {
            mCurrentBubble.setImitator(null);
        }
        mCurrentBubble = bubble;
        mCurrentBubbleChangedEvent.mBubble = bubble;
        MainApplication.postEvent(getContext(), mCurrentBubbleChangedEvent);
        if (mCurrentBubble != null) {
            mCurrentBubble.setImitator(mBubbleDraggable);
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

    public void openUrlInBubble(String url, long startTime) {
        BubbleFlowItemView bubble;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            bubble = (BubbleFlowItemView) inflater.inflate(R.layout.view_bubble_flow_item, null);
            bubble.configure(url, startTime,
                    new BubbleFlowItemView.BubbleFlowItemViewListener() {

                        @Override
                        public void onDestroyBubble() {
                            MainController.get().onDestroyCurrentBubble();
                        }

                        @Override
                        public void onMinimizeBubbles() {
                            MainController.get().switchToBubbleView();
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
            setCurrentBubble(bubble);
        }

        mBubbles.add(bubble);
        mBubbleDraggable.mBadgeView.setCount(mBubbles.size());

        Settings.get().saveCurrentBubbles(mBubbles);

        add(bubble, true);
    }

    private void destroyBubble(BubbleFlowItemView bubble, boolean animateRemove, boolean removeFromList) {
        int index = mViews.indexOf(bubble);
        if (index == -1) {
            return;
        }
        remove(index, animateRemove);

        if (removeFromList) {
            mBubbles.remove(bubble);
        }
        bubble.destroy();

        if (mCurrentBubble == bubble) {
            BubbleFlowItemView newCurrentBubble = null;
            int viewsCount = mViews.size();
            if (viewsCount > 0) {
                if (viewsCount == 1) {
                    newCurrentBubble = (BubbleFlowItemView) mViews.get(0);
                } else if (index < viewsCount) {
                    newCurrentBubble = (BubbleFlowItemView) mViews.get(index);
                } else {
                    if (index > 0) {
                        newCurrentBubble = (BubbleFlowItemView) mViews.get(index-1);
                    } else {
                        newCurrentBubble = (BubbleFlowItemView) mViews.get(0);
                    }
                }
            }
            setCurrentBubble(newCurrentBubble);
        }
    }

    private void postDestroyedBubble() {
        Settings.get().saveCurrentBubbles(mBubbles);
    }

    public void destroyCurrentBubble(boolean animateRemove, Config.BubbleAction action) {
        BubbleFlowItemView currentBubble = getCurrentBubble();
        String url = currentBubble.getUrl().toString();
        destroyBubble(currentBubble, animateRemove, true);
        postDestroyedBubble();
        if (action != Config.BubbleAction.None) {
            MainApplication.handleBubbleAction(getContext(), action, url);
        }
    }

    public void destroyAllBubbles() {
        Iterator<BubbleFlowItemView> iterator = mBubbles.iterator();
        while (iterator.hasNext()) {
            BubbleFlowItemView item = iterator.next();
            destroyBubble(item, false, false);
            iterator.remove();
        }

        mBubbles.clear();
        postDestroyedBubble();
    }

    public void updateIncognitoMode(boolean incognito) {
        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).updateIncognitoMode(incognito);
        }
    }

}
