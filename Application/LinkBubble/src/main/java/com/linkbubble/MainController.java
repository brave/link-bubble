/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.physics.Draggable;
import com.linkbubble.ui.BubbleDraggable;
import com.linkbubble.ui.BubbleFlowDraggable;
import com.linkbubble.ui.BubbleFlowView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.ui.ExpandedActivity;
import com.linkbubble.ui.Prompt;
import com.linkbubble.ui.TabView;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.AppPoller;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

public class MainController implements Choreographer.FrameCallback {

    private static final String TAG = "MainController";

    protected static MainController sInstance;

    // Simple event classes used for the event bus
    public static class BeginBubbleDragEvent {
    }

    public static class EndBubbleDragEvent {
    }

    public static class BeginAnimateFinalTabAwayEvent {
        public TabView mTab;
    }

    public static class EndAnimateFinalTabAwayEvent {
    }

    public static class BeginExpandTransitionEvent {
        public float mPeriod;
    }

    public static class EndExpandTransitionEvent {
    }

    public static class BeginCollapseTransitionEvent {
        public float mPeriod;
    }

    public static class EndCollapseTransitionEvent {
    }

    public static class OrientationChangedEvent {
    }

    public static class CurrentTabChangedEvent {
        public CurrentTabChangedEvent() {
        }

        public CurrentTabChangedEvent(TabView tabView) {
            mTab = tabView;
        }
        public TabView mTab;
    }

    public static class CurrentTabResumeEvent {
        public CurrentTabResumeEvent() {
        }

        public CurrentTabResumeEvent(TabView tabView) {
            mTab = tabView;
        }
        public TabView mTab;
    }

    public static class CurrentTabPauseEvent {
        public CurrentTabPauseEvent() {
        }

        public CurrentTabPauseEvent(TabView tabView) {
            mTab = tabView;
        }
        public TabView mTab;
    }

    public static class DraggableBubbleMovedEvent {
        public int mX, mY;
    }

    public static class HideContentEvent {};
    public static class UnhideContentEvent {};

    public static class ScreenOnEvent {};
    public static class ScreenOffEvent {};
    public static class UserPresentEvent {};

    public static void addRootWindow(View v, WindowManager.LayoutParams lp) {
        MainController mc = get();
        if (!mc.mRootViews.contains(v)) {
            mc.mRootViews.add(v);
            if (mc.mRootWindowsVisible) {
                mc.mWindowManager.addView(v, lp);
            }
        }
    }

    public static void removeRootWindow(View v) {
        MainController mc = get();
        if (mc.mRootViews.contains(v)) {
            mc.mRootViews.remove(v);
            if (mc.mRootWindowsVisible) {
                mc.mWindowManager.removeView(v);
            }
        }
    }

    public static void updateRootWindowLayout(View v, WindowManager.LayoutParams lp) {
        MainController mc = get();
        if (mc.mRootWindowsVisible && mc.mRootViews.contains(v)) {
            mc.mWindowManager.updateViewLayout(v, lp);

        }
    }

    private void enableRootWindows() {
        if (!mRootWindowsVisible) {
            for (View v : mRootViews) {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) v.getLayoutParams();
                //lp.alpha = 1.0f;
                //mWindowManager.updateViewLayout(v, lp);
                mWindowManager.addView(v, lp);
                // Hack to ensure BubbleFlowDraggable doesn't display in Bubble mode, fix #457
                if (v instanceof BubbleFlowView) {
                    ((BubbleFlowView)v).forceCollapseEnd();
                    ((BubbleFlowDraggable)v).setCurrentTabAsActive();
                }
            }
            mRootWindowsVisible = true;
        }
    }

    private void disableRootWindows() {
        if (mRootWindowsVisible) {
            for (View v : mRootViews) {
                //WindowManager.LayoutParams lp = (WindowManager.LayoutParams) v.getLayoutParams();
                //lp.alpha = 0.5f;
                //mWindowManager.updateViewLayout(v, lp);
                // Hack to ensure BubbleFlowDraggable doesn't display in Bubble mode, fix #457
                if (v instanceof BubbleFlowView) {
                    ((BubbleFlowView)v).forceCollapseEnd();
                }
                mWindowManager.removeView(v);
            }
            mRootWindowsVisible = false;
        }
    }

    private Vector<View> mRootViews = new Vector<View>();
    private boolean mRootWindowsVisible = true;
    private WindowManager mWindowManager;

    private OrientationChangedEvent mOrientationChangedEvent = new OrientationChangedEvent();
    private BeginExpandTransitionEvent mBeginExpandTransitionEvent = new BeginExpandTransitionEvent();
    private ExpandedActivity.MinimizeExpandedActivityEvent mMinimizeExpandedActivityEvent = new ExpandedActivity.MinimizeExpandedActivityEvent();

    private UserPresentEvent mUserPresentEvent = new UserPresentEvent();
    private ScreenOnEvent mScreenOnEvent = new ScreenOnEvent();
    private ScreenOffEvent mScreenOffEvent = new ScreenOffEvent();
    private boolean mScreenOn = true;
    private java.util.Timer mTimer = null;

    private static class OpenUrlInfo {
        String mUrlAsString;
        long mStartTime;

        OpenUrlInfo(String url, long startTime) {
            mUrlAsString = url;
            mStartTime = startTime;
        }
    };

    private ArrayList<OpenUrlInfo> mOpenUrlInfos = new ArrayList<OpenUrlInfo>();

    // End of event bus classes

    public static MainController get() {
        return sInstance;
    }

    public static void create(Context context, EventHandler eventHandler) {
        if (sInstance != null) {
            throw new RuntimeException("Only one instance of MainController allowed at any one time");
        }
        sInstance = new MainController(context, eventHandler);
    }

    public static void destroy() {
        if (sInstance == null) {
            throw new RuntimeException("No instance to destroy");
        }

        Settings.get().saveData();

        MainApplication app = (MainApplication) sInstance.mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.unregister(sInstance);

        if (Settings.get().isIncognitoMode()) {
            CookieManager cookieManager = CookieManager.getInstance();
            if (cookieManager != null && cookieManager.hasCookies()) {
                cookieManager.removeAllCookie();
            }
        }

        if (Constant.PROFILE_FPS) {
            sInstance.mWindowManager.removeView(sInstance.mTextView);
        }
        sInstance.mBubbleDraggable.destroy();
        sInstance.mBubbleFlowDraggable.destroy();
        sInstance.mCanvasView.destroy();
        sInstance.mChoreographer.removeFrameCallback(sInstance);
        sInstance.endAppPolling();
        sInstance = null;
    }

    public interface EventHandler {
        public void onDestroy();
    }

    protected EventHandler mEventHandler;
    protected int mBubblesLoaded;
    private AppPoller mAppPoller;

    protected Context mContext;
    private String mAppPackageName;
    private Choreographer mChoreographer;
    protected boolean mUpdateScheduled;
    protected CanvasView mCanvasView;

    private BubbleFlowDraggable mBubbleFlowDraggable;
    private BubbleDraggable mBubbleDraggable;

    private long mPreviousFrameTime;

    // false if the user has forcibilty minimized the Bubbles from ContentView. Set back to true once a new link is loaded.
    private boolean mCanAutoDisplayLink;

    BubbleFlowView.AnimationEventListener mOnBubbleFlowExpandFinishedListener = new BubbleFlowView.AnimationEventListener() {

        @Override
        public void onAnimationEnd(BubbleFlowView sender) {
            TabView currentTab = ((BubbleFlowDraggable)sender).getCurrentTab();
            if (currentTab != null && currentTab.getContentView() != null) {
                currentTab.getContentView().saveLoadTime();
            }
        }
    };

    BubbleFlowView.AnimationEventListener mOnBubbleFlowCollapseFinishedListener = new BubbleFlowView.AnimationEventListener() {

        @Override
        public void onAnimationEnd(BubbleFlowView sender) {
            onBubbleFlowCollapseFinished();
        }
    };

    private void onBubbleFlowCollapseFinished() {
        mBubbleDraggable.setVisibility(View.VISIBLE);
        TabView tab = mBubbleFlowDraggable.getCurrentTab();
        if (tab != null) {
            tab.setImitator(mBubbleDraggable);
        }
        mSetBubbleFlowGone = true;
        mBubbleFlowDraggable.postDelayed(mSetBubbleFlowGoneRunnable, 33);
    }

    private boolean mSetBubbleFlowGone = false;
    Runnable mSetBubbleFlowGoneRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSetBubbleFlowGone) {
                mBubbleFlowDraggable.setVisibility(View.GONE);
            }
        }
    };

    Runnable mSetBubbleGoneRunnable = new Runnable() {
        @Override
        public void run() {
            mBubbleDraggable.setVisibility(View.GONE);
        }
    };

    /*
     * Pass all the input along to mBubbleDraggable
     */
    BubbleFlowView.TouchInterceptor mBubbleFlowTouchInterceptor = new BubbleFlowView.TouchInterceptor() {

        @Override
        public boolean onTouchActionDown(MotionEvent event) {
            return mBubbleDraggable.getDraggableHelper().onTouchActionDown(event);
        }

        @Override
        public boolean onTouchActionMove(MotionEvent event) {
            return mBubbleDraggable.getDraggableHelper().onTouchActionMove(event);
        }

        @Override
        public boolean onTouchActionUp(MotionEvent event) {
            boolean result = mBubbleDraggable.getDraggableHelper().onTouchActionUp(event);
            mBubbleFlowDraggable.setTouchInterceptor(null);
            return result;
        }
    };

    protected MainController(Context context, EventHandler eventHandler) {
        Util.Assert(sInstance == null, "non-null instance");
        sInstance = this;
        mContext = context;
        mAppPackageName = mContext.getPackageName();
        mEventHandler = eventHandler;

        mAppPoller = new AppPoller(context);
        mAppPoller.setListener(mAppPollerListener);

        mCanAutoDisplayLink = true;

        mCanDisplay = true;

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (Constant.PROFILE_FPS) {
            mTextView = new TextView(mContext);
            mTextView.setTextColor(0xff00ffff);
            mTextView.setTextSize(32.0f);
            mWindowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
            mWindowManagerParams.x = 500;
            mWindowManagerParams.y = 16;
            mWindowManagerParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            mWindowManagerParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mWindowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            mWindowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            mWindowManagerParams.format = PixelFormat.TRANSPARENT;
            mWindowManagerParams.setTitle("LinkBubble: Debug Text");
            mWindowManager.addView(mTextView, mWindowManagerParams);
        }

        mUpdateScheduled = false;
        mChoreographer = Choreographer.getInstance();
        mCanvasView = new CanvasView(mContext);

        MainApplication app = (MainApplication) mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        updateIncognitoMode(Settings.get().isIncognitoMode());

        LayoutInflater inflater = LayoutInflater.from(mContext);

        mBubbleDraggable = (BubbleDraggable) inflater.inflate(R.layout.view_bubble_draggable, null);
        Point bubbleRestingPoint = Settings.get().getBubbleRestingPoint();
        int fromX = Settings.get().getBubbleStartingX(bubbleRestingPoint);
        mBubbleDraggable.configure(fromX, bubbleRestingPoint.y, bubbleRestingPoint.x, bubbleRestingPoint.y,
                Constant.BUBBLE_SLIDE_ON_SCREEN_TIME, mCanvasView);

        mBubbleDraggable.setOnUpdateListener(new BubbleDraggable.OnUpdateListener() {
            @Override
            public void onUpdate(Draggable draggable, float dt) {
                if (!draggable.isDragging()) {
                    mBubbleFlowDraggable.syncWithBubble(draggable);
                }
            }
        });

        mBubbleFlowDraggable = (BubbleFlowDraggable) inflater.inflate(R.layout.view_bubble_flow, null);
        mBubbleFlowDraggable.configure(null);
        mBubbleFlowDraggable.collapse(0, null);
        mBubbleFlowDraggable.setBubbleDraggable(mBubbleDraggable);
        mBubbleFlowDraggable.setVisibility(View.GONE);

        mBubbleDraggable.setBubbleFlowDraggable(mBubbleFlowDraggable);
    }

    /*
     * Begin the destruction process.
     */
    public void finish() {
        mEventHandler.onDestroy();
    }

    private TextView mTextView;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();

    public void onPageLoaded(TabView tab, boolean withError) {
        // Ensure this is not an edge case where the Tab has already been destroyed, re #254
        if (getActiveTabCount() == 0 || isTabActive(tab) == false) {
            return;
        }

        // Debounce the saving call so we don't attempt to save after every concurrent page load, causing potential problems with the database connection.
        if (mTimer != null) {
            try {
                mTimer.cancel();
                mTimer.purge();
            }
            catch (NullPointerException exc) {
                // We can have a crash here sometimes when several pages loaded at one time, some of threads will do those calls in any case
            }
        }

        mTimer = new java.util.Timer();
        java.util.TimerTask tt = new java.util.TimerTask() {
            @Override
            public void run() {
                mTimer = null;
                saveCurrentTabs();
            };
        };
        mTimer.schedule(tt, 200);
    }

    public void autoContentDisplayLinkLoaded(TabView tab) {
        // Ensure this is not an edge case where the Tab has already been destroyed, re #254
        if (getActiveTabCount() == 0 || isTabActive(tab) == false) {
            return;
        }

        if (Settings.get().getAutoContentDisplayLinkLoaded()) {
            displayTab(tab);
        }
    }

    public boolean displayTab(TabView tab) {
        if (!mBubbleDraggable.isDragging() && mCanAutoDisplayLink) {
            switch (mBubbleDraggable.getCurrentMode()) {
                case BubbleView:
                    mBubbleFlowDraggable.setCenterItem(tab);
                    mBubbleDraggable.switchToExpandedView();
                    return true;
            }
        }

        return false;
    }

    public void saveCurrentTabs() {
        if (mBubbleFlowDraggable != null) {
            mBubbleFlowDraggable.saveCurrentTabs();
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(true);

        if (mBubbleFlowDraggable != null) {
            mBubbleFlowDraggable.updateIncognitoMode(incognito);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndCollapseTransition(EndCollapseTransitionEvent e) {
        showBadge(true);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onBeginExpandTransition(MainController.BeginExpandTransitionEvent e) {
        showBadge(false);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndExpandTransition(MainController.EndExpandTransitionEvent e) {
        if (Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
            showExpandedActivity();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onExpandedActivityReadyEvent(ExpandedActivity.ExpandedActivityReadyEvent event) {
        if (mDeferredExpandBubbleFlowTime > -1) {
            doExpandBubbleFlow(mDeferredExpandBubbleFlowTime, mDeferredExpandBubbleFlowHideDraggable);
            mDeferredExpandBubbleFlowTime = -1;
            mDeferredExpandBubbleFlowHideDraggable = false;
        }
    }

    static public long sStartExpandedActivityTime = -1;

    public void showExpandedActivity() {
        Log.e(TAG, "showExpandedActivity()");
        sStartExpandedActivityTime = System.currentTimeMillis();
        Intent intent = new Intent(mContext, ExpandedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.getApplicationContext().startActivity(intent);
    }

    public void scheduleUpdate() {
        if (!mUpdateScheduled) {
            mUpdateScheduled = true;
            mChoreographer.postFrameCallback(this);
        }
    }

    // TODO: think of a better name
    public void startDraggingFromContentView() {
        // When we start dragging, configure the BubbleFlowView to pass all its input to our TouchInterceptor so we
        // can re-route it to the BubbleDraggable. This is a bit messy, but necessary so as to cleanly using the same
        // MotionEvent chain for the BubbleFlowDraggable and BubbleDraggable so the items visually sync up.
        mBubbleFlowDraggable.setTouchInterceptor(mBubbleFlowTouchInterceptor);
        mBubbleFlowDraggable.collapse(Constant.BUBBLE_ANIM_TIME, mOnBubbleFlowCollapseFinishedListener);
        mBubbleDraggable.setVisibility(View.VISIBLE);
    }

    public BubbleDraggable getBubbleDraggable() {
        return mBubbleDraggable;
    }

    public int getActiveTabCount() {
        return mBubbleFlowDraggable != null ? mBubbleFlowDraggable.getActiveTabCount() : 0;
    }

    public int getVisibleTabCount() {
        return mBubbleFlowDraggable != null ? mBubbleFlowDraggable.getVisibleTabCount() : 0;
    }

    public boolean isUrlActive(String urlAsString) {
        return mBubbleFlowDraggable != null ? mBubbleFlowDraggable.isUrlActive(urlAsString) : false;
    }

    public boolean wasUrlRecentlyLoaded(String urlAsString, long urlLoadStartTime) {
        for (OpenUrlInfo openUrlInfo : mOpenUrlInfos) {
            long delta = urlLoadStartTime - openUrlInfo.mStartTime;
            if (openUrlInfo.mUrlAsString.equals(urlAsString) && delta < 7 * 1000) {
                //Log.d("blerg", "urlAsString:" + urlAsString + ", openUrlInfo.mUrlAsString:" + openUrlInfo.mUrlAsString + ", delta: " + delta);
                if (mBubbleFlowDraggable != null && mBubbleFlowDraggable.isUrlActive(urlAsString)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getTabIndex(TabView tab) {
        return mBubbleFlowDraggable != null ? mBubbleFlowDraggable.getIndexOfView(tab) : -1;
    }

    public boolean isTabActive(TabView tab) {
        int index = getTabIndex(tab);
        if (index > -1) {
            return true;
        }
        return false;
    }

    private static final int MAX_SAMPLE_COUNT = 60 * 10;
    private static final float MAX_VALID_TIME = 10.0f / 60.0f;
    private float [] mSamples = new float[MAX_SAMPLE_COUNT];
    private int mSampleCount = 0;

    public void doFrame(long frameTimeNanos) {
        mUpdateScheduled = false;

        //if (mHiddenByUser == true) {
        //    return;
        //}

        float t0 = mPreviousFrameTime / 1000000000.0f;
        float t1 = frameTimeNanos / 1000000000.0f;
        float t = t1 - t0;
        mPreviousFrameTime = frameTimeNanos;
        float dt;
        if (Constant.DYNAMIC_ANIM_STEP) {
            dt = Util.clamp(0.0f, t, 3.0f / 60.0f);
        } else {
            dt = 1.0f / 60.0f;
        }

        if (mBubbleFlowDraggable.update()) {
            scheduleUpdate();
        }

        mBubbleDraggable.update(dt);

        mCanvasView.update(dt);

        if (getActiveTabCount() == 0 && mBubblesLoaded > 0 && !mUpdateScheduled) {
            // Will be non-zero in the event a link has been dismissed by a user, but its TabView
            // instance is still animating off screen. In that case, keep triggering an update so that when the
            // item finishes, we are ready to call onDestroy().
            if (mBubbleFlowDraggable.getVisibleTabCount() == 0 && Prompt.isShowing() == false) {
                finish();
            } else {
                scheduleUpdate();
            }
        }

        if (mHiddenByUser == false) {
            updateKeyguardLocked();
        }

        if (Constant.PROFILE_FPS) {
            if (t < MAX_VALID_TIME) {
                mSamples[mSampleCount % MAX_SAMPLE_COUNT] = t;
                ++mSampleCount;
            }

            float total = 0.0f;
            float worst = 0.0f;
            float best = 99999999.0f;
            int badFrames = 0;
            int frameCount = Math.min(mSampleCount, MAX_SAMPLE_COUNT);
            for (int i = 0; i < frameCount; ++i) {
                total += mSamples[i];
                worst = Math.max(worst, mSamples[i]);
                best = Math.min(best, mSamples[i]);
                if (mSamples[i] > 1.5f / 60.0f) {
                    ++badFrames;
                }
            }

            String sbest = String.format("%.2f", 1000.0f * best);
            String sworst = String.format("%.2f", 1000.0f * worst);
            String savg = String.format("%.2f", 1000.0f * total / frameCount);
            String badpc = String.format("%.2f", 100.0f * badFrames / frameCount);
            String s = "Best=" + sbest + "\nWorst=" + sworst + "\nAvg=" + savg + "\nBad=" + badFrames + "\nBad %=" + badpc + "%";

            mTextView.setSingleLine(false);
            mTextView.setText(s);
            scheduleUpdate();
        }
    }

    public void onCloseSystemDialogs() {
        long delta = System.currentTimeMillis() - mLastOpenTabFromNotificationTime;
        // Intent.ACTION_CLOSE_SYSTEM_DIALOGS gets triggered when NotificationOpenTabActivity is instantiated. Ignore that to stop minimizing...
        if (delta < 200) {
            return;
        }
        switchToBubbleView();
    }

    // Before this version select elements would crash WebView in a background service
    // STABLE_SELECT_WEBVIEW_VERSIONCODE used to be set to 249007650 because drop downs
    // worked on some devices but not all.  For example it works on a Samsung Galaxy S4 but not
    // on Nexus 6P.  Hopefully the next update will work across all devices.
    public static final long STABLE_SELECT_WEBVIEW_VERSIONCODE = Long.MAX_VALUE;

    static private long mVersionNumber = 0;

    public long getWebviewVersion(Context context) {
        if (mVersionNumber != 0) {
            return mVersionNumber;
        }

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo("com.google.android.webview", 0);
            mVersionNumber =  pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            mVersionNumber = -1;
        }
        return mVersionNumber;
    }

    public boolean hasStableWebViewForSelects(Context context) {
        return getWebviewVersion(context) >= STABLE_SELECT_WEBVIEW_VERSIONCODE;
    }

    public void onOrientationChanged() {
        Config.init(mContext);
        Settings.get().onOrientationChange();
        mBubbleDraggable.onOrientationChanged();
        mBubbleFlowDraggable.onOrientationChanged();
        MainApplication.postEvent(mContext, mOrientationChangedEvent);
    }

    private boolean handleResolveInfo(ResolveInfo resolveInfo, String urlAsString, long urlLoadStartTime) {
        if (Settings.get().didRecentlyRedirectToApp(urlAsString)) {
            return false;
        }

        boolean isLinkBubble = resolveInfo.activityInfo != null
                && resolveInfo.activityInfo.packageName.equals(mAppPackageName);
        if (isLinkBubble == false && MainApplication.loadResolveInfoIntent(mContext, resolveInfo, urlAsString, -1)) {
            if (getActiveTabCount() == 0 && Prompt.isShowing() == false) {
                finish();
            }

            String title = String.format(mContext.getString(R.string.link_loaded_with_app),
                    resolveInfo.loadLabel(mContext.getPackageManager()));
            MainApplication.saveUrlInHistory(mContext, resolveInfo, urlAsString, title);
            Settings.get().addRedirectToApp(urlAsString);
            Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectInstant, urlAsString);
            return true;
        }

        return false;
    }

    public TabView openUrl(final String urlAsString, long urlLoadStartTime, final boolean setAsCurrentTab,
                           String openedFromAppName) {

        Analytics.trackOpenUrl(openedFromAppName);

        if (wasUrlRecentlyLoaded(urlAsString, urlLoadStartTime) && !urlAsString.equals(mContext.getString(R.string.empty_bubble_page))) {
            Toast.makeText(mContext, R.string.duplicate_link_will_not_be_loaded, Toast.LENGTH_SHORT).show();
            return null;
        }

        URL url;
        try {
            url = new URL(urlAsString);
        } catch (MalformedURLException e) { // If this is not a valid scheme, back out. #271
            Toast.makeText(mContext, mContext.getString(R.string.unsupported_scheme), Toast.LENGTH_SHORT).show();
            if (getActiveTabCount() == 0 && Prompt.isShowing() == false) {
                finish();
            }
            return null;
        }

        if (Settings.get().redirectUrlToBrowser(url)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(urlAsString));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (MainApplication.openInBrowser(mContext, intent, false)) {
                if (getActiveTabCount() == 0 && Prompt.isShowing() == false) {
                    finish();
                }

                String title = String.format(mContext.getString(R.string.link_redirected), Settings.get().getDefaultBrowserLabel());
                MainApplication.saveUrlInHistory(mContext, null, urlAsString, title);
                return null;
            }
        }

        boolean showAppPicker = false;

        PackageManager packageManager = mContext.getPackageManager();
        String urlString = urlAsString.toString();
        List<ResolveInfo> tempResolveInfos = new ArrayList<>();
        if (!urlString.equals(mContext.getString(R.string.empty_bubble_page))) {
            tempResolveInfos = Settings.get().getAppsThatHandleUrl(urlString, packageManager);
        }
        final List<ResolveInfo> resolveInfos = tempResolveInfos;
        ResolveInfo defaultAppResolveInfo = Settings.get().getDefaultAppForUrl(url, resolveInfos);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            if (defaultAppResolveInfo != null) {
                if (handleResolveInfo(defaultAppResolveInfo, urlAsString, urlLoadStartTime)) {
                    return null;
                }
            } else if (resolveInfos.size() == 1) {
                if (handleResolveInfo(resolveInfos.get(0), urlAsString, urlLoadStartTime)) {
                    return null;
                }
            } else {
                // If LinkBubble is a valid resolve target, do not show other options to open the content.
                for (ResolveInfo info : resolveInfos) {
                    if (info.activityInfo.packageName.startsWith("com.linkbubble.playstore")
                            || info.activityInfo.packageName.startsWith("com.brave.playstore")) {
                        showAppPicker = false;
                        break;
                    } else {
                        showAppPicker = true;
                    }
                }
            }
        }

        boolean openedFromItself = false;
        if (null != openedFromAppName && (openedFromAppName.equals(Analytics.OPENED_URL_FROM_NEW_TAB)
                || openedFromAppName.equals(Analytics.OPENED_URL_FROM_HISTORY))) {
            showAppPicker = true;
            openedFromItself = true;
        }
        mCanAutoDisplayLink = true;
        final TabView result = openUrlInTab(urlAsString, urlLoadStartTime, setAsCurrentTab, showAppPicker,
                !(null == openedFromAppName ? false : openedFromAppName.equals(Analytics.OPENED_URL_FROM_MAIN_NEW_TAB)));

        // Show app picker after creating the tab to load so that we have the instance to close if redirecting to an app, re #292.
        if (!openedFromItself && showAppPicker && MainApplication.sShowingAppPickerDialog == false && 0 != resolveInfos.size()) {
            AlertDialog dialog = ActionItem.getActionItemPickerAlert(mContext, resolveInfos, R.string.pick_default_app,
                    new ActionItem.OnActionItemDefaultSelectedListener() {
                        @Override
                        public void onSelected(ActionItem actionItem, boolean always) {
                            boolean loaded = false;
                            for (ResolveInfo resolveInfo : resolveInfos) {
                                if (resolveInfo.activityInfo.packageName.equals(actionItem.mPackageName)
                                        && resolveInfo.activityInfo.name.equals(actionItem.mActivityClassName)) {
                                    if (always) {
                                        Settings.get().setDefaultApp(urlAsString, resolveInfo);
                                    }

                                    // Jump out of the loop and load directly via a BubbleView below
                                    if (resolveInfo.activityInfo.packageName.equals(mAppPackageName)) {
                                        break;
                                    }

                                    loaded = MainApplication.loadIntent(mContext, actionItem.mPackageName,
                                            actionItem.mActivityClassName, urlAsString, -1, true);
                                    break;
                                }
                            }

                            if (loaded) {
                                Settings.get().addRedirectToApp(urlAsString);
                                closeTab(result, contentViewShowing(), false);
                                if (getActiveTabCount() == 0 && Prompt.isShowing() == false) {
                                    finish();
                                }
                                // L_WATCH: L currently lacks getRecentTasks(), so minimize here
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                                    MainController.get().switchToBubbleView();
                                }
                            }
                        }
                    });

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    MainApplication.sShowingAppPickerDialog = false;
                }
            });

            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            Util.showThemedDialog(dialog);
            MainApplication.sShowingAppPickerDialog = true;
        }

        return result;
    }

    protected TabView openUrlInTab(String url, long urlLoadStartTime, boolean setAsCurrentTab, boolean hasShownAppPicker,
                                   boolean performEmptyClick) {
        setHiddenByUser(false);

        if (getActiveTabCount() == 0) {
            mBubbleDraggable.setVisibility(View.VISIBLE);
            collapseBubbleFlow(0);
            mBubbleFlowDraggable.setVisibility(View.GONE);
            // Only do this snap if ContentView is showing. No longer obliterates slide-in animation
            if (contentViewShowing()) {
                mBubbleDraggable.snapToBubbleView();
            } else {
                Point bubbleRestingPoint = Settings.get().getBubbleRestingPoint();
                int fromX = Settings.get().getBubbleStartingX(bubbleRestingPoint);
                mBubbleDraggable.slideOnScreen(fromX, bubbleRestingPoint.y, bubbleRestingPoint.x, bubbleRestingPoint.y,
                                                Constant.BUBBLE_SLIDE_ON_SCREEN_TIME);
            }
        }

        TabView result = mBubbleFlowDraggable.openUrlInTab(url, urlLoadStartTime, setAsCurrentTab, hasShownAppPicker, performEmptyClick);
        showBadge(getActiveTabCount() > 1 ? true : false);
        ++mBubblesLoaded;

        mOpenUrlInfos.add(new OpenUrlInfo(url, urlLoadStartTime));

        return result;
    }

    protected void restoreTab(TabView tab) {
        mBubbleFlowDraggable.restoreTab(tab);
        // Only do this if there's just 1 tab open. Fix #446
        if (getActiveTabCount() == 1) {
            // If the bubble was closed when in BubbleView mode, forcibly reset to Bubble mode
            if (mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.BubbleView) {
                mBubbleDraggable.setVisibility(View.VISIBLE);
                collapseBubbleFlow(0);
                mBubbleFlowDraggable.setVisibility(View.GONE);

                // Ensure CanvasView has a valid ContentView
                CurrentTabChangedEvent event = new CurrentTabChangedEvent();
                event.mTab = tab;
                MainApplication.postEvent(mContext, event);

                mBubbleDraggable.snapToBubbleView();
            } else {
                final float bubblePeriod = (float) Constant.BUBBLE_ANIM_TIME / 1000.f;
                final float contentPeriod = bubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension
                expandBubbleFlow((long) (contentPeriod * 1000), false);
                if (Constant.ACTIVITY_WEBVIEW_RENDERING == false) {
                    // No need to do this if above is true because it's already done
                    showExpandedActivity();
                }
            }
        } else {
            showBadge(true);
        }
        ++mBubblesLoaded;
    }

    public void showBadge(boolean show) {
        if (mBubbleDraggable != null) {
            int tabCount = mBubbleFlowDraggable.getActiveTabCount();
            mBubbleDraggable.mBadgeView.setCount(tabCount);
            if (show) {
                if (tabCount > 1 && mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.BubbleView) {
                    mBubbleDraggable.mBadgeView.show();
                }
            } else {
                mBubbleDraggable.mBadgeView.hide();
            }
        }
    }

    public boolean contentViewShowing() {
        return mBubbleDraggable != null && mBubbleDraggable.getCurrentMode() == BubbleDraggable.Mode.ContentView;
    }

    private long mLastOpenTabFromNotificationTime = -1;
    public void openTabFromNotification(int notificationId) {
        mLastOpenTabFromNotificationTime = System.currentTimeMillis();
        if (mBubbleFlowDraggable != null) {
            boolean contentViewShowing = contentViewShowing();
            mBubbleFlowDraggable.setCurrentTabByNotification(notificationId, contentViewShowing);
            if (!contentViewShowing) {
                mBubbleDraggable.switchToExpandedView();
            }
        }
    }

    public void startFileBrowser(String[] acceptTypes, ValueCallback<Uri[]> filePathCallback) {
        MainApplication.postEvent(mContext,
                new ExpandedActivity.ShowFileBrowserEvent(acceptTypes, filePathCallback));
    }

    public boolean closeTab(int notificationId) {
        if (mBubbleFlowDraggable != null) {
            TabView tabView = mBubbleFlowDraggable.getTabByNotification(notificationId);
            if (tabView != null) {
                return closeTab(tabView, contentViewShowing() && mScreenOn, true);
            }
        }
        return false;
    }

    public boolean closeCurrentTab(Constant.BubbleAction action, boolean animateOff) {
        if (mBubbleFlowDraggable != null) {
            return closeTab(mBubbleFlowDraggable.getCurrentTab(), action, animateOff, true);
        }

        return false;
    }

    public boolean closeTab(TabView tabView, boolean animateOff, boolean canShowUndoPrompt) {
        return closeTab(tabView, Constant.BubbleAction.Close, animateOff, canShowUndoPrompt);
    }

    public boolean closeTab(TabView tabView, Constant.BubbleAction action, boolean animateOff, boolean canShowUndoPrompt) {

        if (tabView == null) {
            CrashTracking.log("closeTab attempt on null tabView");
            return false;
        }

        // If the tab is already closing, do nothing. Otherwise we could end up in a weird state,
        // where we attempt to show multiple prompts and crashing upon tab restore.
        if (null == tabView || tabView.mIsClosing == true) {
            CrashTracking.log("Ignoring duplicate tabView close request");
            return false;
        }
        if (null != tabView) {
            tabView.mIsClosing = true;
        }
        else {
            CrashTracking.log("attempt to access on null tabView");
            return false;
        }

        boolean contentViewShowing = contentViewShowing();
        CrashTracking.log("MainController.closeTab(): action:" + action.toString() + ", contentViewShowing:" + contentViewShowing
                + ", visibleTabCount:" + getVisibleTabCount() + ", activeTabCount:" + getActiveTabCount() + ", canShowUndoPrompt:" + canShowUndoPrompt
                + ", animateOff:" + animateOff + ", canShowUndoPrompt:" + canShowUndoPrompt);
        if (mBubbleFlowDraggable != null) {
            mBubbleFlowDraggable.closeTab(tabView, animateOff, action, tabView != null ? tabView.getTotalTrackedLoadTime() : -1);
        }

        int activeTabCount = getActiveTabCount();
        showBadge(activeTabCount > 1 ? true : false);
        if (activeTabCount == 0) {
            hideBubbleDraggable();
            // Ensure BubbleFlowDraggable gets at least 1 update in the event items are animating off screen. See #237.
            scheduleUpdate();

            MainApplication.postEvent(mContext, mMinimizeExpandedActivityEvent);
        }

        if (tabView == null) {
            CrashTracking.logHandledException(new RuntimeException("tabView = null"));
        } else {
            if (canShowUndoPrompt && Settings.get().getShowUndoCloseTab()) {
                showClosePrompt(tabView);
            } else {
                destroyTabOnDelay(tabView);
            }
        }

        return getActiveTabCount() > 0;
    }

    private void destroyTabOnDelay(final TabView tabView) {
        mBubbleDraggable.postDelayed(new Runnable() {
            @Override
            public void run() {
                tabView.destroy();
            }
        }, 500);
    }

    private void showClosePrompt(final TabView tabView) {
        String title = null;
        if (tabView.getUrl() != null && MainApplication.sTitleHashMap != null) {
            String urlAsString = tabView.getUrl().toString();
            title = MainApplication.sTitleHashMap.get(urlAsString);
        }
        String message;
        if (title != null) {
            message = String.format(mContext.getResources().getString(R.string.undo_close_tab_title), title);
        } else {
            message = mContext.getResources().getString(R.string.undo_close_tab_no_title);
        }
        tabView.mWasRestored = false;
        Prompt.show(message,
                mContext.getResources().getString(R.string.action_undo).toUpperCase(),
                Prompt.LENGTH_SHORT,
                true,
                new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                        if (tabView.mWasRestored == false) {
                            tabView.mIsClosing = false;
                            restoreTab(tabView);
                            tabView.getContentView().onRestored();
                        }
                    }

                    @Override
                    public void onClose() {
                        if (tabView.mWasRestored == false) {
                            tabView.destroy();
                        }
                    }
                }
        );
    }

    public void closeAllBubbles() {
        closeAllBubbles(true);
    }

    public void closeAllBubbles(boolean removeFromCurrentTabs) {
        mBubbleFlowDraggable.closeAllBubbles(removeFromCurrentTabs);
        hideBubbleDraggable();
    }

    private void hideBubbleDraggable() {
        mBubbleDraggable.setVisibility(View.GONE);
    }

    private long mDeferredExpandBubbleFlowTime = -1;
    private boolean mDeferredExpandBubbleFlowHideDraggable;

    public void expandBubbleFlow(long time, boolean hideDraggable) {
        if (Constant.ACTIVITY_WEBVIEW_RENDERING) {
            mDeferredExpandBubbleFlowTime = time;
            mDeferredExpandBubbleFlowHideDraggable = hideDraggable;
            showExpandedActivity();
        } else {
            doExpandBubbleFlow(time, hideDraggable);
        }
    }

    private void doExpandBubbleFlow(long time, boolean hideDraggable) {
        mBeginExpandTransitionEvent.mPeriod = time / 1000.0f;

        mBubbleFlowDraggable.setVisibility(View.VISIBLE);
        mSetBubbleFlowGone = false; // cancel any pending operation to set visibility to GONE (see #190)
        mBubbleFlowDraggable.expand(time, mOnBubbleFlowExpandFinishedListener);

        MainApplication.postEvent(mContext, mBeginExpandTransitionEvent);

        if (hideDraggable) {
            mBubbleDraggable.postDelayed(mSetBubbleGoneRunnable, 33);
        }
    }

    public void collapseBubbleFlow(long time) {
        mBubbleFlowDraggable.collapse(time, mOnBubbleFlowCollapseFinishedListener);
    }

    public void switchToBubbleView() {
        mCanAutoDisplayLink = false;
        if (MainController.get().getActiveTabCount() > 0) {
            mBubbleDraggable.switchToBubbleView();
        } else {
            // If there's no tabs, ensuring pressing Home will cause the CanvasView to go away. Fix #448
            MainApplication.postEvent(mContext, new MainController.EndCollapseTransitionEvent());
        }
    }

    public void switchToExpandedView() {
        mBubbleDraggable.switchToExpandedView();
    }

    public void beginAppPolling() {
        if (mAppPoller != null) {
            mAppPoller.beginAppPolling();
        }
    }

    public void endAppPolling() {
        if (mAppPoller != null) {
            mAppPoller.endAppPolling();
        }
    }

    AppPoller.AppPollerListener mAppPollerListener = new AppPoller.AppPollerListener() {
        @Override
        public void onAppChanged() {
            switchToBubbleView();
        }
    };

    public void showPreviousBubble() {
        mBubbleFlowDraggable.previousTab();
    }

    public void showNextBubble() {
        mBubbleFlowDraggable.nextTab();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStateChangedEvent(MainApplication.StateChangedEvent event) {
        closeAllBubbles(false);
        final Vector<String> urls = Settings.get().loadCurrentTabs();
        if (urls.size() > 0) {
            for (String url : urls) {
                MainApplication.openLink(mContext, url, null);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEndAnimateFinalTabAway(MainController.EndAnimateFinalTabAwayEvent event) {
        mBubbleFlowDraggable.setVisibility(View.GONE);
    }

    public boolean reloadAllTabs(Context context) {
        CrashTracking.log("MainController.reloadAllTabs()");
        boolean reloaded = false;
        closeAllBubbles(false);
        final Vector<String> urls = Settings.get().loadCurrentTabs();
        if (urls.size() > 0) {
            for (String url : urls) {
                MainApplication.openLink(context.getApplicationContext(), url, null);
                reloaded = true;
            }
        }

        return reloaded;
    }

    private boolean mHiddenByUser = false;
    public void setHiddenByUser(boolean hiddenByUser) {
        if (mHiddenByUser != hiddenByUser) {
            mHiddenByUser = hiddenByUser;
            if (mHiddenByUser) {
                switch (mBubbleDraggable.getCurrentMode()) {
                    case ContentView:
                        mBubbleDraggable.snapToBubbleView();
                        break;
                }
                MainApplication.postEvent(mContext, new HideContentEvent());
                MainApplication.postEvent(mContext, new MainService.ShowUnhideNotificationEvent());
            } else {
                MainApplication.postEvent(mContext, new CurrentTabChangedEvent(mBubbleFlowDraggable.getCurrentTab()));
                MainApplication.postEvent(mContext, new MainService.ShowDefaultNotificationEvent());
                MainApplication.postEvent(mContext, new UnhideContentEvent());
            }
            setCanDisplay(!mHiddenByUser);
        }
    }

    private boolean mCanDisplay;
    //private static final String SCREEN_LOCK_TAG = "screenlock";

    private void setCanDisplay(boolean canDisplay) {
        if (canDisplay == mCanDisplay) {
            return;
        }
        //Log.d(SCREEN_LOCK_TAG, "*** setCanDisplay() - old:" + mCanDisplay + ", new:" + canDisplay);
        mCanDisplay = canDisplay;
        if (canDisplay) {
            enableRootWindows();
        } else {
            disableRootWindows();
        }
    }

    private void updateKeyguardLocked() {
        KeyguardManager keyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            boolean isLocked = keyguardManager.isKeyguardLocked();
            //Log.d(SCREEN_LOCK_TAG, "keyguardManager.isKeyguardLocked():" + mCanDisplay);
            setCanDisplay(!isLocked);
        }
    }

    void updateScreenState(String action) {
        //Log.d(SCREEN_LOCK_TAG, "---" + action);
        CrashTracking.log("BubbleFlowView - updateScreenState(): " + action);

        if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            mScreenOn = false;
            setCanDisplay(false);
            MainApplication.postEvent(mContext, mScreenOffEvent);
        } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            updateKeyguardLocked();
            mScreenOn = true;
            MainApplication.postEvent(mContext, mScreenOnEvent);
        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            setCanDisplay(mHiddenByUser ? false : true);
            MainApplication.postEvent(mContext, mUserPresentEvent);
        }
    }


    public boolean isScreenOn() {
        return mScreenOn;
    }

    public TabView getCurrentTab() {
        TabView tab = mBubbleFlowDraggable.getCurrentTab();
        return tab;
    }

    public static void doCrash() {
        throw new RuntimeException("Forced Exception");
    }
}
