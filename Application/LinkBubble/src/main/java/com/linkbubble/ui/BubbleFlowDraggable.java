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
    //to do debug
    public BubbleDraggable mBubbleDraggable;
    public Object mActivitySharedLock = new Object();
    public static boolean mActivityIsUp = false;
    private HashSet<OpenUrlSettings> mUrlsToOpen;
    private ReentrantReadWriteLock mUrlsToOpenLock;
    //
    private Point mTempSize = new Point();

    private MainController.CurrentTabChangedEvent mCurrentTabChangedEvent = new MainController.CurrentTabChangedEvent();
    private MainController.CurrentTabResumeEvent mCurrentTabResumeEvent = new MainController.CurrentTabResumeEvent();
    private MainController.CurrentTabPauseEvent mCurrentTabPauseEvent = new MainController.CurrentTabPauseEvent();

    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleFlowDraggable sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleFlowDraggable sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleFlowDraggable sender, DraggableHelper.ReleaseEvent event);
    }

    //to do debug
    public static class OpenUrlSettings {
        OpenUrlSettings(String url, long urlLoadStartTime, boolean setAsCurrentTab, boolean hasShownAppPicker,
                        boolean performEmptyClick) {
            mUrl = url;
            mUrlLoadStartTime = urlLoadStartTime;
            mSetAsCurrentTab = setAsCurrentTab;
            mHasShownAppPicker = hasShownAppPicker;
            mPerformEmptyClick = performEmptyClick;
        }

        String mUrl;
        long mUrlLoadStartTime;
        boolean mSetAsCurrentTab;
        boolean mHasShownAppPicker;
        boolean mPerformEmptyClick;
    }
    //

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
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
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

        new StartBubblesEvent(getContext()).execute();
    }

    class StartBubblesEvent extends AsyncTask<Void,Void,Long> {
        Context mContext;

        StartBubblesEvent(Context context) {
            super();
            mContext = context;
        }
        protected Long doInBackground(Void... params) {
            //to do debug
            Intent intent1 = new Intent(mContext, BubbleFlowActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent1);

            synchronized (mActivitySharedLock) {
                try {
                    mActivitySharedLock.wait();
                    BubbleFlowDraggable.mActivityIsUp = true;

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
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                        }
                        mUrlsToOpen.clear();
                    }
                    finally {
                        mUrlsToOpenLock.writeLock().unlock();
                    }
                    /*Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
                    intent.putExtra("url", "http://macworld.com");
                    LocalBroadcastManager bm = LocalBroadcastManager.getInstance(mContext);
                    bm.sendBroadcast(intent);

                    Intent intentActivity = new Intent(mContext, BubbleFlowActivity.class);
                    intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intentActivity);*/
                }
                catch (InterruptedException exc) {
                }
            }
            //

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
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    public void setTabAsActive(TabView tabView) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.SET_TAB_AS_ACTIVE);
        intent.putExtra("url", tabView.getUrl().toString());
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    @Override
    public void collapse(long time, AnimationEventListener animationEventListener) {
        /*if (mViews.size() > 0) {
            Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
            intent.putExtra("command", BubbleFlowActivity.COLLAPSE);
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
            bm.sendBroadcast(intent);

            Intent intentActivity = new Intent(getContext(), BubbleFlowActivity.class);
            intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intentActivity);
        }*/

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
        //setOnTouchListener(null);
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.DESTROY_ACTIVITY);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);

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
            /*Intent intentActivity = new Intent(getContext(), BubbleFlowActivity.class);
            intentActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            getContext().startActivity(intentActivity);*/

            Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
            intent.putExtra("command", BubbleFlowActivity.EXPAND);
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
            bm.sendBroadcast(intent);
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

    public void setCurrentTabAsActive() {
        if (null != mCurrentTab) {
            setCurrentTab(mCurrentTab);
        }
    }

    //to do debug
    public void setCurrentTab(TabView tab) {
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
                setTabAsActive(tab);
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
        //controller.addBubble(tabView, false);
        if (openUrlSettings.mSetAsCurrentTab) {
            setCurrentTab(tabView);
        }
    }

    public TabView openUrlInTab(String url, long urlLoadStartTime, boolean setAsCurrentTab, boolean hasShownAppPicker,
                                boolean performEmptyClick) {

        try {
            mUrlsToOpenLock.writeLock().lock();
            OpenUrlSettings openUrlSettings = new OpenUrlSettings(url, urlLoadStartTime, setAsCurrentTab, hasShownAppPicker,
                    performEmptyClick);
            if (!mActivityIsUp) {
                mUrlsToOpen.add(openUrlSettings);
            }
            else {
                passUrlToActivity(openUrlSettings);
            }
        }
        finally {
            mUrlsToOpenLock.writeLock().unlock();
        }

        /*try {
            do {
                Thread.sleep(10);
            } while (null == mActivityMessageHandler);
        }
        catch (InterruptedException e) {
        }*/

        //mActivityMessageHandler.openUrl(url);
        /*ActivityInfo[] list;
        try {
            list = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), PackageManager.GET_ACTIVITIES).activities;
            for(int i = 0;i< list.length;i++)
            {
                System.out.println("List of running activities"+list[i].name);

            }
        }
        catch (PackageManager.NameNotFoundException e) {

        }*/

//        Intent intent2 = new Intent(getContext(), BubbleFlowActivity.class);
//        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK/* | Intent.FLAG_ACTIVITY_CLEAR_TOP /*| Intent.FLAG_ACTIVITY_MULTIPLE_TASK*/);
//        intent2.putExtra("host", "http://slashdot.org");
//        getContext().startActivity(intent2);
        //

        /*try {
            list = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), PackageManager.GET_ACTIVITIES).activities;
            for(int i = 0;i< list.length;i++)
            {
                System.out.println("List of running activities"+list[i].name);

            }
        }
        catch (PackageManager.NameNotFoundException e) {

        }*/
        //

        /*TabView tabView;
        try {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            tabView = (TabView) inflater.inflate(R.layout.view_tab, null);
            // Stub
            tabView.configure(getContext().getString(R.string.empty_bubble_page), urlLoadStartTime, hasShownAppPicker, performEmptyClick, true);
            //tabView.configure(url, urlLoadStartTime, hasShownAppPicker, performEmptyClick, true);
        } catch (MalformedURLException e) {
            // TODO: Inform the user somehow?
            return null;
        }*/

        // Only insert next to current Bubble when in ContentView mode. Ensures links opened when app is
        // minimized are added to the end.
        /*add(tabView, mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView);

        mBubbleDraggable.mBadgeView.setCount(getActiveTabCount());

        if (setAsCurrentTab) {
            setCurrentTab(tabView);
        }

        saveCurrentTabs();*/
        return new TabView(getContext());//tabView;

        //return tabView;
    }

    public void restoreTab(TabView tabView) {
        add(tabView, mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView);

        mBubbleDraggable.mBadgeView.setCount(getActiveTabCount());

        saveCurrentTabs();

        if (getActiveTabCount() == 1) {
            setCurrentTab(tabView);
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
                setCurrentTab(tabView);
            }
        }
    }

    private void closeTabInActivity(String url) {
        Intent intent = new Intent(BubbleFlowActivity.ACTIVITY_INTENT_NAME);
        intent.putExtra("command", BubbleFlowActivity.CLOSE_VIEW);
        intent.putExtra("url", url);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.sendBroadcast(intent);
    }

    private void closeTab(TabView tab, boolean animateRemove, boolean removeFromList) {
        int index = mViews.indexOf(tab);
        if (index == -1) {
            return;
        }

        String url = tab.getUrl().toString();
        if (removeFromList == true && url.equals(Constant.WELCOME_MESSAGE_URL)) {
            Settings.get().setWelcomeMessageDisplayed(true);
        }

        closeTabInActivity(url);
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
            setCurrentTab(newCurrentTab);
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
            closeTab(tabView, animateRemove, true);
            postClosedTab(true);
            if (action != Constant.BubbleAction.None) {
                MainApplication.handleBubbleAction(getContext(), action, url, totalTrackedLoadTime);
            }
        }
    }

    public void closeAllBubbles(boolean removeFromCurrentTabs) {
        int closeCount = 0;
        for (View view : mViews) {
            closeTab(((TabView) view), false, false);
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
