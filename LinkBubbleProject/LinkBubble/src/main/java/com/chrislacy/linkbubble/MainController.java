package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Vector;

/**
 * Created by gw on 2/10/13.
 */
public class MainController implements Choreographer.FrameCallback {

    public interface EventHandler {
        public void onDestroy();
    }

    private void doTargetAction(Config.BubbleAction action, String url) {

        switch (action) {
            case ConsumeRight:
            case ConsumeLeft: {
                Config.ActionType actionType = Settings.get().getConsumeBubbleActionType(action);
                if (actionType == Config.ActionType.Share) {
                    // TODO: Retrieve the class name below from the app in case Twitter ever change it.
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.setClassName(Settings.get().getConsumeBubblePackageName(action),
                                        Settings.get().getConsumeBubbleActivityClassName(action));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                    mContext.startActivity(intent);
                } else if (actionType == Config.ActionType.View) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(Settings.get().getConsumeBubblePackageName(action),
                                        Settings.get().getConsumeBubbleActivityClassName(action));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    intent.setData(Uri.parse(url));
                    mContext.startActivity(intent);
                }
                break;
            }
            default:
                break;
        }
    }

    public static State_BubbleView STATE_BubbleView;
    public static State_SnapToEdge STATE_SnapToEdge;
    public static State_AnimateToContentView STATE_AnimateToContentView;
    public static State_ContentView STATE_ContentView;
    public static State_AnimateToBubbleView STATE_AnimateToBubbleView;
    public static State_Flick_ContentView STATE_Flick_ContentView;
    public static State_Flick_BubbleView STATE_Flick_BubbleView;

    private ControllerState mCurrentState;
    private EventHandler mEventHandler;
    private int mBubblesLoaded;

    private Context mContext;
    private Choreographer mChoreographer;
    private boolean mUpdateScheduled;
    private static Vector<Bubble> mBubbles = new Vector<Bubble>();
    private Canvas mCanvas;
    private Badge mBadge;
    private static MainController sMainController;

    //private TextView mTextView;
    //private WindowManager mWindowManager;
    //private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    //private int mFrameNumber;

    public static void onPageLoaded(Bubble bubble) {
        sMainController.mCurrentState.OnPageLoaded(bubble);
    }

    public static boolean destroyBubble(Bubble bubble, Config.BubbleAction action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sMainController.mContext);
        boolean debug = prefs.getBoolean("debug_flick", true);

        if (debug) {
            Toast.makeText(sMainController.mContext, "HIT TARGET!", 400).show();
        } else {
            String url = bubble.getUrl();

            bubble.destroy();
            int bubbleIndex = mBubbles.indexOf(bubble);
            Util.Assert(bubbleIndex >= 0 && bubbleIndex < mBubbles.size());
            mBubbles.remove(bubble);

            for (int i=0 ; i < mBubbles.size() ; ++i) {
                mBubbles.get(i).setBubbleIndex(i);
            }

            if (mBubbles.size() > 0) {
                int nextBubbleIndex = Util.clamp(0, bubbleIndex, mBubbles.size()-1);
                Bubble nextBubble = mBubbles.get(nextBubbleIndex);
                sMainController.mBadge.attach(nextBubble);
                sMainController.mBadge.setBubbleCount(mBubbles.size());

                nextBubble.setVisibility(View.VISIBLE);
            } else {
                sMainController.mBadge.attach(null);

                Config.BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
                Config.BUBBLE_HOME_Y = (int) (Config.mScreenHeight * 0.4f);
            }

            sMainController.mCurrentState.OnDestroyBubble(bubble);

            sMainController.doTargetAction(action, url);
        }

        return mBubbles.size() > 0;
    }

    public static void setAllBubblePositions(Bubble ref) {
        if (ref != null) {
            // Force all bubbles to be where the moved one ended up
            int bubbleCount = mBubbles.size();
            for (int i=0 ; i < bubbleCount ; ++i) {
                Bubble b = mBubbles.get(i);
                if (b != ref) {
                    b.setExactPos(ref.getXPos(), ref.getYPos());
                }
            }
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(!incognito);

        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).updateIncognitoMode(incognito);
        }
    }

    @Subscribe
    public void onIncognitoModeChanged(SettingsFragment.IncognitoModeChangedEvent event) {
        updateIncognitoMode(event.mIncognito);
    }

    public MainController(Context context, EventHandler eh) {
        Util.Assert(sMainController == null);
        sMainController = this;
        mContext = context;
        mEventHandler = eh;

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
        mCanvas = new Canvas(mContext);
        mBadge = new Badge(mContext);

        MainApplication app = (MainApplication) mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        STATE_BubbleView = new State_BubbleView(mCanvas, mBadge);
        STATE_SnapToEdge = new State_SnapToEdge(mCanvas);
        STATE_AnimateToContentView = new State_AnimateToContentView(mCanvas);
        STATE_ContentView = new State_ContentView(mCanvas);
        STATE_AnimateToBubbleView = new State_AnimateToBubbleView(mCanvas);
        STATE_Flick_ContentView = new State_Flick_ContentView(mCanvas);
        STATE_Flick_BubbleView = new State_Flick_BubbleView(mCanvas);

        updateIncognitoMode(Settings.get().isIncognitoMode());
        switchState(STATE_BubbleView);
    }

    public void destroy() {
        MainApplication app = (MainApplication) mContext.getApplicationContext();
        Bus bus = app.getBus();
        bus.unregister(this);

        //mWindowManager.removeView(mTextView);
        mCanvas.destroy();
        mChoreographer.removeFrameCallback(this);
        sMainController = null;
    }

    public static void scheduleUpdate() {
        Util.Assert(sMainController != null);
        if (!sMainController.mUpdateScheduled) {
            sMainController.mUpdateScheduled = true;
            sMainController.mChoreographer.postFrameCallback(sMainController);
        }
    }

    public static void switchState(ControllerState newState) {
        Util.Assert(sMainController != null);
        //Util.Assert(newState != sMainController.mCurrentState);
        if (sMainController.mCurrentState != null) {
            sMainController.mCurrentState.OnExitState();
        }
        sMainController.mCurrentState = newState;
        sMainController.mCurrentState.OnEnterState();
        scheduleUpdate();
    }

    public static int getBubbleCount() {
        return mBubbles.size();
    }

    public static Bubble getBubble(int index) {
        return mBubbles.get(index);
    }

    public void doFrame(long frameTimeNanos) {
        mUpdateScheduled = false;

        float dt = 1.0f / 60.0f;

        int bubbleCount = mBubbles.size();
        for (int i=0 ; i < bubbleCount ; ++i) {
            Bubble b = mBubbles.get(i);
            b.update(dt);
        }

        Bubble frontBubble = null;
        if (mBubbles.size() > 0) {
            frontBubble = mBubbles.lastElement();
        }
        mCanvas.update(dt, frontBubble);

        if (mCurrentState.OnUpdate(dt)) {
            scheduleUpdate();
        }

        //mTextView.setText("S=" + mCurrentState.getName() + " F=" + mFrameNumber++);

        if (mCurrentState == STATE_BubbleView && mBubbles.size() == 0 &&
                mBubblesLoaded > 0 && !mUpdateScheduled) {
            sMainController.mEventHandler.onDestroy();
        }
    }

    public void onCloseSystemDialogs() {
        if (mCurrentState != null) {
            mCurrentState.OnCloseDialog();
            switchState(STATE_AnimateToBubbleView);
        }
    }

    public void onOrientationChanged() {
        Config.init(mContext);
        mCanvas.onOrientationChanged();
        boolean contentView = mCurrentState.OnOrientationChanged();
        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).OnOrientationChanged(contentView);
        }
    }

    public void onOpenUrl(final String url, boolean recordHistory, long startTime) {
        final List<ResolveInfo> resolveInfos = Settings.get().getAppsThatHandleUrl(url);
        if (resolveInfos != null && resolveInfos.size() > 0 && Settings.get().autoLoadContent()) {
            if (resolveInfos.size() == 1) {
                if (MainApplication.loadResolveInfoIntent(mContext, resolveInfos.get(0), url, startTime)) {
                    return;
                }
            } else {
                AlertDialog dialog = ActionItem.getActionItemPickerAlert(mContext, resolveInfos, R.string.pick_default_app,
                        new ActionItem.OnActionItemSelectedListener() {
                            @Override
                            public void onSelected(ActionItem actionItem) {
                                for (ResolveInfo resolveInfo : resolveInfos) {
                                    if (resolveInfo.activityInfo.packageName.equals(actionItem.mPackageName)
                                            && resolveInfo.activityInfo.name.equals(actionItem.mActivityClassName)) {
                                        MainApplication.loadIntent(mContext, actionItem.mPackageName,
                                                actionItem.mActivityClassName, url, -1);
                                        break;
                                    }
                                }

                            }
                        });
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                dialog.show();
                return;
            }
        }

        if (mBubbles.size() < Config.MAX_BUBBLES) {
            Bubble bubble = new Bubble(mContext, url, Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y, startTime, recordHistory,
                    mBubbles.size(), new Bubble.EventHandler() {
                @Override
                public void onMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
                    mCurrentState.OnMotionEvent_Touch(sender, e);
                }

                @Override
                public void onMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
                    mCurrentState.OnMotionEvent_Move(sender, e);
                }

                @Override
                public void onMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
                    mCurrentState.OnMotionEvent_Release(sender, e);
                }

                @Override
                public void onSharedLink(Bubble sender) {
                    switchState(STATE_AnimateToBubbleView);
                }
            });

            mCurrentState.OnNewBubble(bubble);
            mBubbles.add(bubble);
            ++mBubblesLoaded;

            int bubbleCount = mBubbles.size();

            mBadge.attach(bubble);
            mBadge.setBubbleCount(bubbleCount);

            for (int i=0 ; i < bubbleCount ; ++i) {
                Bubble b = mBubbles.get(i);
                int vis = View.VISIBLE;
                if (i != bubbleCount-1)
                    vis = View.GONE;
                b.setVisibility(vis);
            }
        }
    }
}
