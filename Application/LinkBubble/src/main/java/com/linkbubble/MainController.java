package com.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.linkbubble.physics.ControllerState;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.physics.State_AnimateToContentView;
import com.linkbubble.physics.State_BubbleView;
import com.linkbubble.physics.State_ContentView;
import com.linkbubble.physics.State_Flick_BubbleView;
import com.linkbubble.physics.State_Flick_ContentView;
import com.linkbubble.physics.State_KillBubble;
import com.linkbubble.physics.State_SnapToEdge;
import com.linkbubble.ui.BadgeView;
import com.linkbubble.ui.BubbleLegacyView;
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
public abstract class MainController implements Choreographer.FrameCallback {

    private static final String TAG = "MainController";

    protected static MainController sInstance;
    private static ContentActivity sContentActivity;

    public static MainController get() {
        return sInstance;
    }

    public static void destroy() {
        if (sInstance == null) {
            new RuntimeException("No instance to destroy");
        }

        MainApplication app = (MainApplication) sInstance.mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.unregister(sInstance);

        //mWindowManager.removeView(mTextView);
        sInstance.mCanvasView.destroy();
        sInstance.mChoreographer.removeFrameCallback(sInstance);
        sInstance.endAppPolling();
        sInstance = null;
    }

    public abstract ContentView getActiveContentView();

    public interface EventHandler {
        public void onDestroy();
    }

    public State_BubbleView STATE_BubbleView;
    public State_SnapToEdge STATE_SnapToEdge;
    public State_AnimateToContentView STATE_AnimateToContentView;
    public State_ContentView STATE_ContentView;
    public State_AnimateToBubbleView STATE_AnimateToBubbleView;
    public State_Flick_ContentView STATE_Flick_ContentView;
    public State_Flick_BubbleView STATE_Flick_BubbleView;
    public State_KillBubble STATE_KillBubble;

    protected ControllerState mCurrentState;
    protected EventHandler mEventHandler;
    protected int mBubblesLoaded;
    private AppPoller mAppPoller;

    protected Context mContext;
    private Choreographer mChoreographer;
    protected boolean mUpdateScheduled;
    protected static Vector<Draggable> mDraggables = new Vector<Draggable>();
    protected CanvasView mCanvasView;
    protected BadgeView mBadgeView;
    protected Draggable mFrontDraggable;


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
        mWindowManager.addView(mTextView, mWindowManagerParams);*/

        mUpdateScheduled = false;
        mChoreographer = Choreographer.getInstance();
        mCanvasView = new CanvasView(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mBadgeView = (BadgeView) inflater.inflate(R.layout.view_badge, null);

        MainApplication app = (MainApplication) mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        STATE_BubbleView = new State_BubbleView(mCanvasView, mBadgeView);
        STATE_SnapToEdge = new State_SnapToEdge(mCanvasView);
        STATE_AnimateToContentView = new State_AnimateToContentView(mCanvasView);
        STATE_ContentView = new State_ContentView(mCanvasView);
        STATE_AnimateToBubbleView = new State_AnimateToBubbleView(mCanvasView);
        STATE_Flick_ContentView = new State_Flick_ContentView(mCanvasView);
        STATE_Flick_BubbleView = new State_Flick_BubbleView(mCanvasView);
        STATE_KillBubble = new State_KillBubble(mCanvasView);

        updateIncognitoMode(Settings.get().isIncognitoMode());
        switchState(STATE_BubbleView);
    }

    protected void doTargetAction(Config.BubbleAction action, String url) {

        switch (action) {
            case ConsumeRight:
            case ConsumeLeft: {
                MainApplication.handleBubbleAction(mContext, action, url);
                break;
            }
            default:
                break;
        }
    }

    //private TextView mTextView;
    //private WindowManager mWindowManager;
    //private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    //private int mFrameNumber;

    public void onPageLoaded() {
        mCurrentState.onPageLoaded();
    }

    public boolean isStateActive(Class<?> clazz) {
        if (mCurrentState != null) {
            return mCurrentState.getClass() == clazz;
        }
        return false;
    }

    public abstract boolean destroyBubble(Draggable draggable, Config.BubbleAction action);

    public void setAllDraggablePositions(Draggable ref) {
        if (ref != null) {
            // Force all bubbles to be where the moved one ended up
            int bubbleCount = mDraggables.size();
            int xPos = ref.getDraggableHelper().getXPos();
            int yPos = ref.getDraggableHelper().getYPos();
            for (int i=0 ; i < bubbleCount ; ++i) {
                Draggable draggable = mDraggables.get(i);
                if (draggable != ref) {
                    draggable.getDraggableHelper().setExactPos(xPos, yPos);
                }
            }
        }
    }

    public abstract void updateIncognitoMode(boolean incognito);

    @Subscribe
    public void onIncognitoModeChanged(SettingsFragment.IncognitoModeChangedEvent event) {
        updateIncognitoMode(event.mIncognito);
    }

    public void scheduleUpdate() {
        if (!mUpdateScheduled) {
            mUpdateScheduled = true;
            mChoreographer.postFrameCallback(this);
        }
    }

    public void switchState(ControllerState newState) {
        //Util.Assert(newState != sMainController.mCurrentState);
        if (mCurrentState != null) {
            mCurrentState.onExitState();
        }
        mCurrentState = newState;
        mCurrentState.onEnterState();
        scheduleUpdate();
    }

    public void showBubblePager(boolean show) {}

    public void showContentView(ContentView contentView) {
        mCanvasView.setContentView(contentView);
        mCanvasView.showContentView();
        mCanvasView.setContentViewTranslation(0.0f);
    }

    public abstract int getBubbleCount();

    public int getDraggableCount() {
        return mDraggables.size();
    }

    public Draggable getDraggable(int index) {
        return mDraggables.get(index);
    }

    public abstract void doFrame(long frameTimeNanos);

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
        if (mCurrentState != null) {
            mCurrentState.onCloseDialog();
            switchState(STATE_AnimateToBubbleView);
        }
    }

    public void onOrientationChanged() {
        Config.init(mContext);
        mCanvasView.onOrientationChanged();
        boolean contentView = mCurrentState.onOrientationChanged();
        for (int i=0 ; i < mDraggables.size() ; ++i) {
            mDraggables.get(i).onOrientationChanged(contentView);
        }
    }

    public Draggable getActiveDraggable() {
        Util.Assert(mFrontDraggable != null);
        return mFrontDraggable;
    }

    public void setActiveDraggable(Draggable draggable) {
        mFrontDraggable = draggable;
        Util.Assert(mFrontDraggable != null);
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

    protected abstract void openUrlInBubble(String url, long startTime);

    public void beginAppPolling() {
        mAppPoller.beginAppPolling();
    }

    public void endAppPolling() {
        mAppPoller.endAppPolling();
    }

    AppPoller.AppPollerListener mAppPollerListener = new AppPoller.AppPollerListener() {
        @Override
        public void onAppChanged() {
            if (mCurrentState != null && mCurrentState instanceof State_AnimateToBubbleView == false) {
                switchState(STATE_AnimateToBubbleView);
            }
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
