package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by gw on 2/10/13.
 */
public class MainController implements Choreographer.FrameCallback {

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
    public static State_Flick STATE_Flick;

    private ControllerState mCurrentState;

    private Context mContext;
    private Choreographer mChoreographer;
    private boolean mUpdateScheduled;
    private static Vector<Bubble> mBubbles = new Vector<Bubble>();
    private Canvas mCanvas;
    private Badge mBadge;
    private static MainController sMainController;

    private boolean mEnabled;

    private TextView mTextView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    private int mFrameNumber;

/*
    private void destroyBubble(Bubble bubble, Config.BubbleAction action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean debug = prefs.getBoolean("debug_flick", true);

        if (debug) {
            Toast.makeText(mContext, "HIT TARGET!", 400).show();
        } else {
            Util.Assert(false);
            String url = bubble.getUrl();

            bubble.destroy();
            int bubbleIndex = mBubbles.indexOf(bubble);
            mBubbles.remove(bubble);

            for (int i=0 ; i < mBubbles.size() ; ++i) {
                mBubbles.get(i).setBubbleIndex(i);
            }

            if (mBubbles.size() > 0) {
                int nextBubbleIndex = Util.clamp(0, bubbleIndex, mBubbles.size()-1);
                Bubble nextBubble = mBubbles.get(nextBubbleIndex);
                mBadge.attach(nextBubble);
                if (mMode == Mode.ContentView) {
                    mContentViewRoot.hide();
                } else {
                    nextBubble.setExactPos(bubble.getXPos(), bubble.getYPos());
                }
                setSelectedBubble(nextBubble);
            } else {
                if (mMode == Mode.ContentView) {
                    mContentViewRoot.hide();
                }
                mBadge.attach(null);
                mMode = Mode.BubbleView;
                setSelectedBubble(null);

                mBubbleHomeX = Config.mBubbleSnapLeftX;
                mBubbleHomeY = (int) (Config.mScreenHeight * 0.4f);
            }

            updateBubbleVisibility();
            mCurrentState.OnDestroyBubble(bubble);

            doTargetAction(action, url);
        }

        Util.Assert(false);
        if (mBubbles.size() > 0) {
            switchState(mAnimateToModeViewState);
        } else {
            switchState(mIdleState);
        }
    }*/

    public static void setAllBubblePositions(Bubble ref) {
        // Force all bubbles to be where the moved one ended up
        int bubbleCount = mBubbles.size();
        for (int i=0 ; i < bubbleCount-1 ; ++i) {
            Bubble b = mBubbles.get(i);
            if (b != ref) {
                b.setExactPos(ref.getXPos(), ref.getYPos());
            }
        }
    }

    public MainController(Context context) {
        Util.Assert(sMainController == null);
        sMainController = this;
        mContext = context;
        mEnabled = true;

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
        mWindowManager.addView(mTextView, mWindowManagerParams);

        mUpdateScheduled = false;
        mChoreographer = Choreographer.getInstance();
        mCanvas = new Canvas(context);
        mBadge = new Badge(context);

        Config.BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
        Config.BUBBLE_HOME_Y = (int) (Config.mScreenHeight * 0.4f);

        STATE_BubbleView = new State_BubbleView(mCanvas, mBadge);
        STATE_SnapToEdge = new State_SnapToEdge();
        STATE_AnimateToContentView = new State_AnimateToContentView(mCanvas);
        STATE_ContentView = new State_ContentView(mCanvas);
        STATE_AnimateToBubbleView = new State_AnimateToBubbleView(mCanvas);
        STATE_Flick = new State_Flick(mCanvas);

        switchState(STATE_BubbleView);
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
        Util.Assert(newState != sMainController.mCurrentState);
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

        mTextView.setText("S=" + mCurrentState.getName() + " F=" + mFrameNumber++);
    }

    public void onCloseSystemDialogs() {
        if (mCurrentState != null) {
            //mCurrentState.OnCloseDialog();
        }
    }

    /*
    public void enable() {
        mEnabled = true;
        updateBubbleVisibility();
        mCanvas.enable(true);
    }

    public void disable() {
        mEnabled = false;
        updateBubbleVisibility();
        mCanvas.enable(false);
    }*/

    public void onOrientationChanged() {
        //Util.Assert(false);
        /*
        Config.init(mContext);

        mBubbleHomeX = Config.mBubbleSnapLeftX;
        mBubbleHomeY = (int) (Config.mScreenHeight * 0.4f);

        if (mBubbles.size() > 0) {
            Bubble b = getFrontBubble();

            int x = b.getXPos();
            if (x < Config.mScreenCenterX) {
                x = Config.mBubbleSnapLeftX;
            } else {
                x = Config.mBubbleSnapRightX;
            }

            int y = b.getYPos();
            y = Util.clamp(Config.mBubbleMinY, y, Config.mBubbleMaxY);

            b.setExactPos(x, y);
        }

        mCanvas.onOrientationChanged();
        mCurrentState.OnOrientationChanged();*/
    }

    public void onOpenUrl(String url, boolean recordHistory) {
        if (mBubbles.size() < Config.MAX_BUBBLES) {
            Bubble bubble = new Bubble(mContext, url, Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y, recordHistory, mBubbles.size(), new Bubble.EventHandler() {
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
                    Util.Assert(false);
                    //Util.Assert(mCurrentState == mIdleState);
                    //Util.Assert(mMode == Mode.ContentView);
                    //mMode = Mode.BubbleView;
                    //switchState(mAnimateToModeViewState);
                }
            });

            mCurrentState.OnNewBubble(bubble);
            mBubbles.add(bubble);

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
