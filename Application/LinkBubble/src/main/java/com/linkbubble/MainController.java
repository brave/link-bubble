package com.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.Draggable;
import com.linkbubble.ui.BadgeView;
import com.linkbubble.ui.BubbleDraggable;
import com.linkbubble.ui.BubbleFlowDraggable;
import com.linkbubble.ui.BubbleFlowItemView;
import com.linkbubble.ui.BubbleFlowView;
import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.ui.ContentActivity;
import com.linkbubble.ui.ContentView;
import com.linkbubble.ui.SettingsFragment;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.AppPoller;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;

/**
 * Created by gw on 2/10/13.
 */
public class MainController implements Choreographer.FrameCallback {

    private static final String TAG = "MainController";

    protected static MainController sInstance;
    private static ContentActivity sContentActivity;

    // Simple event classes used for the event bus
    public static class BeginBubbleDragEvent {
    }

    public static class EndBubbleDragEvent {
    }

    public static class BeginExpandTransitionEvent {
        public float mPeriod;
    }

    public static class BeginCollapseTransitionEvent {
        public float mPeriod;
    }

    public static class EndCollapseTransitionEvent {
    }

    public static class OrientationChangedEvent {
    }

    public static class CurrentBubbleChangedEvent {
        public BubbleFlowItemView mBubble;
    }

    public static class DraggableBubbleMovedEvent {
        public int mX, mY;
    }

    private OrientationChangedEvent mOrientationChangedEvent = new OrientationChangedEvent();
    private BeginExpandTransitionEvent mBeginExpandTransitionEvent = new BeginExpandTransitionEvent();

    // End of event bus classes

    public static MainController get() {
        return sInstance;
    }

    public static void create(Context context, EventHandler eventHandler) {
        if (sInstance != null) {
            new RuntimeException("Only one instance of MainController allowed at any one time");
        }
        sInstance = new MainController(context, eventHandler);
    }

    public static void destroy() {
        if (sInstance == null) {
            new RuntimeException("No instance to destroy");
        }

        MainApplication app = (MainApplication) sInstance.mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.unregister(sInstance);

        //sInstance.mWindowManager.removeView(sInstance.mTextView);
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
    private Choreographer mChoreographer;
    protected boolean mUpdateScheduled;
    protected CanvasView mCanvasView;

    private BubbleFlowDraggable mBubbleFlowDraggable;
    private BubbleDraggable mBubbleDraggable;

    BubbleFlowView.AnimationEventListener mOnBubbleFlowExpandFinishedListener = new BubbleFlowView.AnimationEventListener() {

        @Override
        public void onAnimationEnd(BubbleFlowView sender) {

        }
    };

    BubbleFlowView.AnimationEventListener mOnBubbleFlowCollapseFinishedListener = new BubbleFlowView.AnimationEventListener() {

        @Override
        public void onAnimationEnd(BubbleFlowView sender) {
            mBubbleDraggable.setVisibility(View.VISIBLE);
            BubbleFlowItemView currentBubble = mBubbleFlowDraggable.getCurrentBubble();
            if (currentBubble != null) {
                currentBubble.setImitator(mBubbleDraggable);
            }
            mBubbleFlowDraggable.postDelayed(mSetBubbleFlowGoneRunnable, 33);
        }
    };

    Runnable mSetBubbleFlowGoneRunnable = new Runnable() {
        @Override
        public void run() {
            mBubbleFlowDraggable.setVisibility(View.GONE);
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
        Util.Assert(sInstance == null);
        sInstance = this;
        mContext = context;
        mEventHandler = eventHandler;

        mAppPoller = new AppPoller(context);
        mAppPoller.setListener(mAppPollerListener);

        /*
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
*/

        mUpdateScheduled = false;
        mChoreographer = Choreographer.getInstance();
        mCanvasView = new CanvasView(mContext);

        MainApplication app = (MainApplication) mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        updateIncognitoMode(Settings.get().isIncognitoMode());

        LayoutInflater inflater = LayoutInflater.from(mContext);

        mBubbleDraggable = (BubbleDraggable) inflater.inflate(R.layout.view_bubble_draggable, null);
        mBubbleDraggable.configure((int) (Config.mBubbleSnapLeftX - Config.mBubbleWidth), Config.BUBBLE_HOME_Y,
                Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y, 0.4f, mCanvasView);

        mBubbleDraggable.setOnUpdateListener(new BubbleDraggable.OnUpdateListener() {
            @Override
            public void onUpdate(Draggable draggable, float dt) {
                mBubbleFlowDraggable.syncWithBubble(draggable);
            }
        });

        mBubbleFlowDraggable = (BubbleFlowDraggable) inflater.inflate(R.layout.view_bubble_flow, null);
        mBubbleFlowDraggable.configure(null);
        mBubbleFlowDraggable.collapse(0, null);
        mBubbleFlowDraggable.setBubbleDraggable(mBubbleDraggable);
        mBubbleFlowDraggable.setVisibility(View.GONE);
    }

    //private TextView mTextView;
    //private WindowManager mWindowManager;
    //private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    //private int mFrameNumber;

    public void onPageLoaded(BubbleFlowItemView bubbleFlowItemView) {
        if (Settings.get().getAutoContentDisplayLinkLoaded()) {
            switch (mBubbleDraggable.getCurrentMode()) {
                case BubbleView:
                    mBubbleFlowDraggable.setCenterItem(bubbleFlowItemView);
                    mBubbleDraggable.switchToExpandedView();
                    break;
            }
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(!incognito);

        if (mBubbleFlowDraggable != null) {
            mBubbleFlowDraggable.updateIncognitoMode(incognito);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onIncognitoModeChanged(SettingsFragment.IncognitoModeChangedEvent event) {
        updateIncognitoMode(event.mIncognito);
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

    public int getBubbleCount() {
        return mBubbleFlowDraggable != null ? mBubbleFlowDraggable.getBubbleCount() : 0;
    }

    public void doFrame(long frameTimeNanos) {
        mUpdateScheduled = false;

        float dt = 1.0f / 60.0f;

        if (mBubbleFlowDraggable.update()) {
            scheduleUpdate();
        }

        mBubbleDraggable.update(dt);

        mCanvasView.update(dt);

        //mTextView.setText("S=" + mCurrentState.getName() + " F=" + mFrameNumber++);

        if (getBubbleCount() == 0 && mBubblesLoaded > 0 && !mUpdateScheduled) {
            mEventHandler.onDestroy();
        }
    }

    public void updateBackgroundColor(int color) {
        if (sContentActivity != null) {
            sContentActivity.updateBackgroundColor(color);
        }
    }

    private void showContentActivity() {
        if (sContentActivity == null && Config.USE_CONTENT_ACTIVITY) {
            Intent intent = new Intent(mContext, ContentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mContext.startActivity(intent);
        }
    }

    public void hideContentActivity() {
        if (sContentActivity != null) {
            long startTime = System.currentTimeMillis();
            sContentActivity.finish();
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "sContentActivity.finish() time=" + (endTime - startTime));
            sContentActivity = null;
        }
    }

    public void onCloseSystemDialogs() {
        switchToBubbleView();
    }

    public void onOrientationChanged() {
        Config.init(mContext);
        mBubbleDraggable.onOrientationChanged();
        MainApplication.postEvent(mContext, mOrientationChangedEvent);
    }

    public void onOpenUrl(final String url, long startTime) {
        if (Settings.get().redirectUrlToBrowser(url)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (MainApplication.loadInBrowser(mContext, intent, false)) {
                if (getBubbleCount() == 0) {
                    mEventHandler.onDestroy();
                }

                String title = String.format(mContext.getString(R.string.link_redirected), Settings.get().getDefaultBrowserLabel());
                MainApplication.saveUrlInHistory(mContext, null, url, title);
                return;
            }
        }

        final List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
        if (resolveInfos != null && resolveInfos.size() > 0 && Settings.get().getAutoContentDisplayAppRedirect()) {
            if (resolveInfos.size() == 1) {
                ResolveInfo resolveInfo = resolveInfos.get(0);
                if (resolveInfo != Settings.get().mLinkBubbleEntryActivityResolveInfo
                    && MainApplication.loadResolveInfoIntent(mContext, resolveInfo, url, startTime)) {
                    if (getBubbleCount() == 0) {
                        mEventHandler.onDestroy();
                    }

                    String title = String.format(mContext.getString(R.string.link_loaded_with_app),
                                                 resolveInfo.loadLabel(mContext.getPackageManager()));
                    MainApplication.saveUrlInHistory(mContext, resolveInfo, url, title);
                    return;
                }
            } else {
                AlertDialog dialog = ActionItem.getActionItemPickerAlert(mContext, resolveInfos, R.string.pick_default_app,
                        new ActionItem.OnActionItemSelectedListener() {
                            @Override
                            public void onSelected(ActionItem actionItem) {
                                boolean loaded = false;
                                String appPackageName = mContext.getPackageName();
                                for (ResolveInfo resolveInfo : resolveInfos) {
                                    if (resolveInfo.activityInfo.packageName.equals(actionItem.mPackageName)
                                            && resolveInfo.activityInfo.name.equals(actionItem.mActivityClassName)) {
                                        Settings.get().setDefaultApp(url, resolveInfo);

                                        // Jump out of the loop and load directly via a BubbleView below
                                        if (resolveInfo.activityInfo.packageName.equals(appPackageName)) {
                                            break;
                                        }

                                        loaded = MainApplication.loadIntent(mContext, actionItem.mPackageName,
                                                actionItem.mActivityClassName, url, -1);
                                        break;
                                    }
                                }

                                if (loaded == false) {
                                    openUrlInBubble(url, System.currentTimeMillis());
                                } else {
                                    if (getBubbleCount() == 0) {
                                        mEventHandler.onDestroy();
                                    }
                                }
                            }
                        });
                dialog.setCancelable(false);
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();

                return;
            }
        }

        openUrlInBubble(url, startTime);
    }

    protected void openUrlInBubble(String url, long startTime) {
        if (getBubbleCount() == 0) {
            mBubbleDraggable.setVisibility(View.VISIBLE);
            mBubbleDraggable.setExactPos(Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y);
        }

        mBubbleFlowDraggable.openUrlInBubble(url, startTime);
        showBadge(getBubbleCount() > 1 ? true : false);
        ++mBubblesLoaded;
    }

    public void showBadge(boolean show) {
        if (mBubbleDraggable != null) {
            mBubbleDraggable.mBadgeView.setCount(mBubbleFlowDraggable.getBubbleCount());
            if (show) {
                if (mBubbleFlowDraggable.getBubbleCount() > 1) {
                    mBubbleDraggable.mBadgeView.show();
                }
            } else {
                mBubbleDraggable.mBadgeView.hide();
            }
        }
    }

    public void onDestroyCurrentBubble() {
        mBubbleFlowDraggable.destroyCurrentBubble(true, Config.BubbleAction.None);
        if (mBubbleFlowDraggable.getBubbleCount() == 0) {

            // TODO
            //Util.Assert(false);

            /*
            STATE_KillBubble.init(mBubbleDraggable);
            switchState(STATE_KillBubble);*/
        }
    }

    public boolean destroyCurrentBubble(Config.BubbleAction action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean debug = prefs.getBoolean("debug_flick", true);

        if (debug) {
            Toast.makeText(mContext, "HIT TARGET!", 400).show();
        } else {
            mBubbleFlowDraggable.destroyCurrentBubble(false, action);
            if (mBubbleFlowDraggable.getBubbleCount() == 0) {
                removeBubbleDraggable();

                Config.BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
                Config.BUBBLE_HOME_Y = (int) (Config.mScreenHeight * 0.4f);
            }
        }

        return getBubbleCount() > 0;
    }

    public void destroyAllBubbles() {
        mBubbleFlowDraggable.destroyAllBubbles();
        removeBubbleDraggable();
    }

    private void removeBubbleDraggable() {
        mBubbleDraggable.destroy();
    }

    public void expandBubbleFlow(long time) {
        mBeginExpandTransitionEvent.mPeriod = time / 1000.0f;
        MainApplication.postEvent(mContext, mBeginExpandTransitionEvent);

        mBubbleFlowDraggable.setVisibility(View.VISIBLE);
        mBubbleFlowDraggable.expand(time, mOnBubbleFlowExpandFinishedListener);
        mBubbleDraggable.postDelayed(mSetBubbleGoneRunnable, 33);
    }

    public void collapseBubbleFlow(long time) {
        mBubbleFlowDraggable.collapse(time, mOnBubbleFlowCollapseFinishedListener);
    }

    public void switchToBubbleView() {
        mBubbleDraggable.switchToBubbleView();
    }

    public void switchToExpandedView() {
        mBubbleDraggable.switchToExpandedView();
    }

    public void beginAppPolling() {
        mAppPoller.beginAppPolling();
    }

    public void endAppPolling() {
        mAppPoller.endAppPolling();
    }

    AppPoller.AppPollerListener mAppPollerListener = new AppPoller.AppPollerListener() {
        @Override
        public void onAppChanged() {
            switchToBubbleView();
        }
    };

    public boolean showPreviousBubble() {
        /*
        if (mCurrentState instanceof State_ContentView) {
            State_ContentView contentViewState = (State_ContentView)mCurrentState;
            BubbleView activeBubble = getActiveBubble();
            if (activeBubble != null) {
                int index = activeBubble.getBubbleIndex();
                if (index > 0) {
                    for (BubbleView bubble : mBubbles) {
                        if (index-1 == bubble.getBubbleIndex()) {
                            contentViewState.setActiveBubble(bubble);
                            return true;
                        }
                    }
                }
            }
        }*/
        return false;
    }

    public boolean showNextBubble() {
        /*
        if (mCurrentState instanceof State_ContentView) {
            State_ContentView contentViewState = (State_ContentView)mCurrentState;
            BubbleView activeBubble = getActiveBubble();
            if (activeBubble != null) {
                int index = activeBubble.getBubbleIndex();
                for (BubbleView bubble : mBubbles) {
                    if (index+1 == bubble.getBubbleIndex()) {
                        contentViewState.setActiveBubble(bubble);
                        return true;
                    }
                }
            }
        }*/
        return false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onContentActivityResumed(ContentActivity.ContentActivityResumedEvent event) {
        sContentActivity = event.mActivity;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onContentActivityPaused(ContentActivity.ContentActivityPausedEvent event) {
        sContentActivity = null;
    }
}
