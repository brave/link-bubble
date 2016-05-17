/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
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
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.VerticalGestureListener;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class BubbleFlowDraggable extends BubbleFlowView implements Draggable {

    private DraggableHelper mDraggableHelper;
    private EventHandler mEventHandler;
    private int mBubbleFlowWidth;
    private int mBubbleFlowHeight;
    private TabView mCurrentTab;
    public BubbleDraggable mBubbleDraggable;
    private HashSet<OpenUrlSettings> mUrlsToOpen;
    private ReentrantReadWriteLock mUrlsToOpenLock;
    private Point mTempSize = new Point();
    private boolean mDestroyed = true;

    private MainController.CurrentTabChangedEvent mCurrentTabChangedEvent = new MainController.CurrentTabChangedEvent();
    private MainController.CurrentTabResumeEvent mCurrentTabResumeEvent = new MainController.CurrentTabResumeEvent();
    private MainController.CurrentTabPauseEvent mCurrentTabPauseEvent = new MainController.CurrentTabPauseEvent();

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleFlowDraggable sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleFlowDraggable sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleFlowDraggable sender, DraggableHelper.ReleaseEvent event);
    }

    public static class OpenUrlSettings {
        OpenUrlSettings(String url, long urlLoadStartTime, boolean setAsCurrentTab, boolean hasShownAppPicker,
                        boolean performEmptyClick, boolean openedFromItself) {
            mUrl = url;
            mUrlLoadStartTime = urlLoadStartTime;
            mSetAsCurrentTab = setAsCurrentTab;
            mHasShownAppPicker = hasShownAppPicker;
            mPerformEmptyClick = performEmptyClick;
            mOpenedFromItself = openedFromItself;
        }

        String mUrl;
        long mUrlLoadStartTime;
        boolean mSetAsCurrentTab;
        boolean mHasShownAppPicker;
        boolean mPerformEmptyClick;
        boolean mOpenedFromItself;
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

    public boolean isDragging() {
        return false;
    }

    public void configure(EventHandler eventHandler, boolean addRootWindow)  {
        mUrlsToOpen = new HashSet<OpenUrlSettings>();
        mUrlsToOpenLock = new ReentrantReadWriteLock();
        mBubbleFlowWidth = Config.mScreenWidth;
        mBubbleFlowHeight = getResources().getDimensionPixelSize(R.dimen.bubble_pager_height);

        configure(mBubbleFlowWidth,
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_width),
                getResources().getDimensionPixelSize(R.dimen.bubble_pager_item_height));

        setBubbleFlowViewListener(new BubbleFlowView.Listener() {
            @Override
            public void onCenterItemClicked(BubbleFlowView sender, View view) {
                try {
                    MainController.get().switchToBubbleView();
                }
                catch (NullPointerException exc) {
                    CrashTracking.logHandledException(exc);
                }
            }

            @Override
            public void onCenterItemLongClicked(BubbleFlowView sender, View view) {
                if (view instanceof TabView) {
                    MainController mainController = MainController.get();
                    if (mainController.getActiveTabCount() != 0) {
                        mainController.startDraggingFromContentView();
                    }
                }
            }

            @Override
            public void onCenterItemSwiped(VerticalGestureListener.GestureDirection gestureDirection) {
                // TODO: Implement me
            }

            @Override
            public void onCenterItemChanged(BubbleFlowView sender, View view) {
                setCurrentTab((TabView) view, true);
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

        mDraggableHelper = new DraggableHelper(this, windowManagerParams, false, new DraggableHelper.OnTouchActionEventListener() {

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
            public void onActionUp(DraggableHelper.ReleaseEvent event, boolean startDelay) {
                if (mEventHandler != null) {
                    mEventHandler.onMotionEvent_Release(BubbleFlowDraggable.this, event);
                }
            }
        });

        mEventHandler = eventHandler;

        if (mDraggableHelper.isAlive() && addRootWindow) {
            MainController.addRootWindow(this, windowManagerParams);

            setExactPos(0, 0);
        }
        mDestroyed = false;
        StartActivity();
    }

    public void StartActivity() {
        new StartBubblesEvent(getContext()).execute();
    }

    class StartBubblesEvent extends AsyncTask<Void,Void,Long> {
        Context mContext;

        StartBubblesEvent(Context context) {
            super();
            mContext = context;
        }
        protected Long doInBackground(Void... params) {

            synchronized (MainApplication.mActivitySharedLock) {
                if (MainApplication.mActivityIsUp || mDestroyed) {
                    return null;
                }
                Intent intent1 = new Intent(mContext, BubbleFlowActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                mContext.startActivity(intent1);

                try {
                    MainApplication.mActivitySharedLock.wait();
                    if (mDestroyed) {
                        return null;
                    }
                    MainApplication.mActivityIsUp = true;

                    try {
                        mUrlsToOpenLock.writeLock().lock();
                        for (OpenUrlSettings urlToOpen : mUrlsToOpen) {
                            Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
                            intent.putExtra("command", BubbleFlowActivity.OPEN_URL);
                            intent.putExtra("url", urlToOpen.mUrl);
                            intent.putExtra("urlStartTime", urlToOpen.mUrlLoadStartTime);
                            intent.putExtra("hasShownAppPicker", urlToOpen.mHasShownAppPicker);
                            intent.putExtra("performEmptyClick", urlToOpen.mPerformEmptyClick);
                            intent.putExtra("setAsCurrentTab", urlToOpen.mSetAsCurrentTab);
                            intent.putExtra("openedFromItself", urlToOpen.mOpenedFromItself);
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        }
                        mUrlsToOpen.clear();
                    }
                    finally {
                        mUrlsToOpenLock.writeLock().unlock();
                    }
                }
                catch (InterruptedException exc) {
                }
            }

            return null;
        }
    }

    private void passUrlToActivity(OpenUrlSettings urlToOpen) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.OPEN_URL);
        intent.putExtra("url", urlToOpen.mUrl);
        intent.putExtra("urlStartTime", urlToOpen.mUrlLoadStartTime);
        intent.putExtra("hasShownAppPicker", urlToOpen.mHasShownAppPicker);
        intent.putExtra("performEmptyClick", urlToOpen.mPerformEmptyClick);
        intent.putExtra("setAsCurrentTab", urlToOpen.mSetAsCurrentTab);
        intent.putExtra("openedFromItself", urlToOpen.mOpenedFromItself);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    public void setTabAsActive(TabView tabView) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.SET_TAB_AS_ACTIVE);
        intent.putExtra("url", tabView.getUrl().toString());
        intent.putExtra("index", getIndexOfView(tabView));
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    @Override
    public void collapse(long time, AnimationEventListener animationEventListener) {
        if (null != mCurrentTab && null != mCurrentTab.mContentView) {
            mCurrentTab.mContentView.collapse();
        }

        super.collapse(time, animationEventListener);
    }

    @Override
    void configure(int width, int itemWidth, int itemHeight) {
        mBubbleFlowWidth = Config.mScreenWidth;

        super.configure(width, itemWidth, itemHeight);

        if (mDraggableHelper != null && mDraggableHelper.getWindowManagerParams() != null) {
            WindowManager.LayoutParams windowManagerParams = mDraggableHelper.getWindowManagerParams();
            windowManagerParams.width = width;
            windowManagerParams.x = 0;
            windowManagerParams.y = 0;
            windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;

            setExactPos(0, 0);
        }
    }

    public void destroy() {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.DESTROY_ACTIVITY);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
        mDestroyed = true;
        synchronized (MainApplication.mActivitySharedLock) {
            MainApplication.mActivitySharedLock.notify();
        }

        mDraggableHelper.destroy();
    }

    public void nextTab() {
        int tabCount = getActiveTabCount();
        TabView currentTab = getCurrentTab();
        if (currentTab != null) {
            int tabIndex = getIndexOfView(currentTab);
            int nextTabIndex = tabIndex + 1;

            if (nextTabIndex < tabCount) {
                setCenterIndex(nextTabIndex);
            }
        }
    }

    public void previousTab() {
        int tabCount = getActiveTabCount();
        TabView currentTab = getCurrentTab();
        if (currentTab != null) {
            int tabIndex = getIndexOfView(currentTab);
            int nextTabIndex = tabIndex - 1;

            if (nextTabIndex >= 0) {
                setCenterIndex(nextTabIndex);
            }
        }
    }

    // The number of items currently actively managed by the BubbleFlowView.
    // Note: it's possible for getActiveTabCount() to equal 0, but getVisibleTabCount() to be > 0.
    public int getActiveTabCount() {
        return mViews.size();
    }

    public boolean isUrlActive(String urlAsString) {
        for (View v : mViews) {
            TabView tabView = (TabView)v;
            if (tabView.getUrl().toString().equals(urlAsString)) {
                return true;
            }
        }

        return false;
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

        CrashTracking.log("BubbleFlowDraggable.expand(): time:" + time);

        if (isExpanded() == false && mCurrentTab != null) {
            // Ensure the centerIndex matches the current bubble. This should only *NOT* be the case when
            // restoring with N Bubbles from a previous session and the user clicks to expand the BubbleFlowView.
            int currentTabIndex = getIndexOfView(mCurrentTab);
            int centerIndex = getCenterIndex();
            if (centerIndex > -1 && currentTabIndex != centerIndex && isAnimatingToCenterIndex() == false) {
                setCenterIndex(currentTabIndex, false);
            }
        }

        if (mViews.size() > 0) {
            Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
            intent.putExtra("command", BubbleFlowActivity.EXPAND);
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
            bm.sendBroadcast(intent);
        }

        if (super.expand(time, animationEventListener)) {
            int centerIndex = getCenterIndex();
            if (centerIndex > -1) {
                setCurrentTab((TabView) mViews.get(centerIndex), true);
            }
            return true;
        }


        return false;
    }

    public TabView getCurrentTab() {
        return mCurrentTab;
    }

    public void setCurrentTabAsActive() {
        if (null != mCurrentTab) {
            setCurrentTab(mCurrentTab, true);
        }
    }

    public void setCurrentTab(TabView tab, boolean sendIntentToActivity) {
        mCurrentTabResumeEvent.mTab = tab;
        MainApplication.postEvent(getContext(), mCurrentTabResumeEvent);
        if (mCurrentTab == tab) {
            if (null != mCurrentTab) {
                ContentView contentView = mCurrentTab.getContentView();
                if (null != contentView) {
                    contentView.setTabAsActive();
                    setTabAsActive(tab);
                }
            }
            return;
        }

        if (mCurrentTab != null) {
            mCurrentTab.setImitator(null);
        }
        mCurrentTabPauseEvent.mTab = mCurrentTab;
        MainApplication.postEvent(getContext(), mCurrentTabPauseEvent);
        mCurrentTab = tab;
        mCurrentTabChangedEvent.mTab = tab;
        MainApplication.postEvent(getContext(), mCurrentTabChangedEvent);
        if (mCurrentTab != null) {
            mCurrentTab.setImitator(mBubbleDraggable);
            ContentView contentView = mCurrentTab.getContentView();
            if (null != contentView) {
                contentView.setTabAsActive();
                if (sendIntentToActivity) {
                    setTabAsActive(tab);
                }
            }
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

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(mTempSize);
        configure(mTempSize.x, mItemWidth, mItemHeight);
        updatePositions();
        updateScales(getScrollX());

        setExactPos(0, 0);
        if (null != mCurrentTab) {
            mCurrentTab.getContentView().onOrientationChanged();
        }
    }

    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void createTabView(ContentView view, BubbleFlowDraggable.OpenUrlSettings openUrlSettings, MainController controller) {
        TabView tabView;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            tabView = (TabView) inflater.inflate(R.layout.view_tab, null);
            tabView.mContentView = view;
            tabView.configure(openUrlSettings.mUrl, openUrlSettings.mUrlLoadStartTime, openUrlSettings.mHasShownAppPicker,
                    openUrlSettings.mPerformEmptyClick, false, controller);
        } catch (MalformedURLException e) {
            // TODO: Inform the user somehow?
            return;
        }

        add(tabView, mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView);

        mBubbleDraggable.mBadgeView.setCount(getActiveTabCount());
        if (openUrlSettings.mSetAsCurrentTab) {
            setCurrentTab(tabView, false);
        }

        controller.afterTabLoaded(tabView, openUrlSettings.mUrlLoadStartTime, openUrlSettings.mHasShownAppPicker,
                openUrlSettings.mOpenedFromItself);
    }

    public void openUrlInTab(String url, long urlLoadStartTime, boolean setAsCurrentTab, boolean hasShownAppPicker,
                                boolean performEmptyClick, boolean openedFromItself) {

        try {
            mUrlsToOpenLock.writeLock().lock();
            OpenUrlSettings openUrlSettings = new OpenUrlSettings(url, urlLoadStartTime, setAsCurrentTab, hasShownAppPicker,
                    performEmptyClick, openedFromItself);
            if (!MainApplication.mActivityIsUp) {
                mUrlsToOpen.add(openUrlSettings);
            }
            else {
                passUrlToActivity(openUrlSettings);
            }
        }
        finally {
            mUrlsToOpenLock.writeLock().unlock();
        }
        if (!MainApplication.mActivityIsUp) {
            StartActivity();
        }
    }

    public void restoreTabInActivity(String url) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.RESTORE_TAB);
        intent.putExtra("url", url);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    public void restoreTab(TabView tabView) {
        restoreTabInActivity(tabView.getUrl().toString());
        add(tabView, mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView);

        mBubbleDraggable.mBadgeView.setCount(getActiveTabCount());

        saveCurrentTabs();

        if (getActiveTabCount() == 1) {
            setCurrentTab(tabView, true);
        }

        tabView.mWasRestored = true;
    }

    @Override
    protected void remove(final int index, boolean animateOff, boolean removeFromList, final OnRemovedListener onRemovedListener) {
        if (index < 0 || index >= mViews.size()) {
            return;
        }
        TabView tab = (TabView) mViews.get(index);

        OnRemovedListener internalOnRemoveListener = new OnRemovedListener() {
            @Override
            public void onRemoved(View view) {
                if (onRemovedListener != null) {
                    onRemovedListener.onRemoved(view);
                }

                if (getActiveTabCount() == 0 && getVisibleTabCount() == 0) {
                    MainApplication.postEvent(getContext(), new MainController.EndAnimateFinalTabAwayEvent());
                }
            }
        };

        super.remove(index, animateOff, removeFromList, internalOnRemoveListener);
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
            // Tabs are now destroyed after a time so the Undo close tab functionality works.
            //((TabView)view).destroy();

            ((TabView)view).getContentView().onRemoved();
        }
    };

    public TabView getTabByNotification(int notificationId) {
        if (mViews != null) {
            for (View view : mViews) {
                TabView tabView = ((TabView)view);
                if (tabView.getContentView().getArticleNotificationId() == notificationId) {
                    return tabView;
                }
            }
        }

        return null;
    }

    public void setCurrentTabByNotification(int notificationId, boolean contentViewShowing) {
        TabView tabView = getTabByNotification(notificationId);
        if (tabView != null) {
            int currentTabIndex = getIndexOfView(tabView);
            if (currentTabIndex > -1) {
                int centerIndex = getCenterIndex();
                Log.d("blerg", "centerIndex:" + centerIndex + ", currentTabIndex:" + currentTabIndex);
                if (contentViewShowing) {
                    if (centerIndex != currentTabIndex) {
                        setCenterIndex(currentTabIndex, true);
                    }
                } else {
                    if (centerIndex > -1 && currentTabIndex != centerIndex && isAnimatingToCenterIndex() == false) {
                        setCenterIndex(currentTabIndex, false);
                    }
                }
                setCurrentTab(tabView, true);
            }
        }
    }

    public void preCloseTabInActivity(TabView tab) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.PRE_CLOSE_VIEW);
        intent.putExtra("url", tab.getUrl().toString());
        intent.putExtra("index", getIndexOfView(tab));
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    public void closeTabInActivity(String url) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.CLOSE_VIEW);
        intent.putExtra("url", url);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    private void closeTab(TabView tab, boolean animateRemove, boolean removeFromList, boolean closeAllBubbels) {
        int index = mViews.indexOf(tab);
        if (index == -1) {
            return;
        }

        String url = tab.getUrl().toString();
        if (removeFromList == true && url.equals(Constant.WELCOME_MESSAGE_URL)) {
            Settings.get().setWelcomeMessageDisplayed(true);
        }

        preCloseTabInActivity(tab);
        remove(index, animateRemove, removeFromList, mOnTabRemovedListener);

        // Don't do this if we're animating the final tab off, as the setCurrentTab() call messes with the
        // CanvasView.mContentView, which has already been forcible set above in remove() via BeginAnimateFinalTabAwayEvent.
        boolean animatingFinalTabOff = getActiveTabCount() == 0 && getVisibleTabCount() > 0 && mSlideOffAnimationPlaying;
        if (mCurrentTab == tab && animatingFinalTabOff == false) {
            TabView newCurrentTab = null;
            int viewsCount = mViews.size();
            if (viewsCount > 0) {
                if (viewsCount == 1) {
                    newCurrentTab = (TabView) mViews.get(0);
                } else if (index < viewsCount) {
                    newCurrentTab = (TabView) mViews.get(index);
                } else {
                    if (index > 0) {
                        newCurrentTab = (TabView) mViews.get(index-1);
                    } else {
                        newCurrentTab = (TabView) mViews.get(0);
                    }
                }
            }
            if (!closeAllBubbels) {
                setCurrentTab(newCurrentTab, true);
            }
        }
    }

    private void postClosedTab(boolean removeFromCurrentTabs) {
        if (removeFromCurrentTabs) {
            saveCurrentTabs();
        }
    }

    public void closeTab(TabView tabView, boolean animateRemove, Constant.BubbleAction action, long totalTrackedLoadTime) {
        if (tabView != null) {
            String url = tabView.getUrl().toString();
            closeTab(tabView, animateRemove, true, false);
            postClosedTab(true);
            if (action != Constant.BubbleAction.None) {
                MainApplication.handleBubbleAction(getContext(), action, url, totalTrackedLoadTime);
            }
        }
    }

    public void closeAllBubbles(boolean removeFromCurrentTabs) {
        int closeCount = 0;
        for (View view : mViews) {
            closeTab(((TabView) view), false, false, true);
            ((TabView) view).destroy();
            closeCount++;
        }

        CrashTracking.log("closeAllbubbles(): closeCount:" + closeCount);

        mViews.clear();
        postClosedTab(removeFromCurrentTabs);
    }

    public void updateIncognitoMode(boolean incognito) {
        for (View view : mViews) {
            ((TabView)view).updateIncognitoMode(incognito);
        }
    }

    public void saveCurrentTabs() {
        Settings.get().saveCurrentTabs(mViews);
        CrashTracking.log("saveCurrentTabs()");
    }
}
