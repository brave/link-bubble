package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.Vector;

/**
 * Created by gw on 2/10/13.
 */
public class MainController implements Choreographer.FrameCallback {

    private enum Mode {
        BubbleView,
        ContentView
    }

    private abstract class State {
        public void OnEnterState() {}
        public boolean OnUpdate(float dt) { return false; }
        public void OnExitState() {}
        public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {}
        public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {}
        public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {}
        public boolean OnNewBubble(Bubble bubble) { Util.Assert(false); return false; }
        public void OnDestroyBubble(Bubble bubble) {}
        public void OnOrientationChanged() {}
        public abstract String getName();
    }

    private void doTargetAction(Canvas.BubbleAction action, String url) {
        switch (action) {
            case OpenBrowser: {
                    MainActivity.loadInBrowser(mContext, url, true);
                }
                break;
            case OpenTwitter: {
                    // TODO: Retrieve the class name below from the app in case Twitter ever change it.
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.setClassName("com.twitter.android", "com.twitter.applib.PostActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                    mContext.startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    // Idle - no bubbles
    private class IdleState extends State {
        public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
            switchState(mMoveFrontBubbleState, true);
            mMoveFrontBubbleState.OnMotionEvent_Touch(sender, e);
        }
        public boolean OnNewBubble(Bubble bubble) {
            return true;
        }
        public String getName() { return "Idle"; }
    }

    // Manually moving bubble with touch
    private class MoveFrontBubbleState extends State {
        private int mInitialX;
        private int mInitialY;
        private int mTargetX;
        private int mTargetY;
        private int mSetX;
        private int mSetY;
        private Bubble mBubble;
        private boolean mDidMove;

        public void OnEnterState() {
            mDidMove = false;
            mCanvas.fadeIn();
        }
        public void OnMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
            mBubble = sender;
            mInitialX = e.posX;
            mInitialY = e.posY;
            mTargetX = mInitialX;
            mTargetY = mInitialY;
            mSetX = -1;
            mSetY = -1;
        }
        public void OnMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
            mTargetX = mInitialX + e.dx;
            mTargetY = mInitialY + e.dy;

            mTargetX = Util.clamp(Config.mBubbleSnapLeftX, mTargetX, Config.mBubbleSnapRightX);
            mTargetY = Util.clamp(Config.mBubbleMinY, mTargetY, Config.mBubbleMaxY);

            float d = (float) Math.sqrt( (e.dx * e.dx) + (e.dy * e.dy) );
            if (d >= Config.dpToPx(10.0f)) {
                mDidMove = true;
                if (mMode == Mode.ContentView) {
                    mContentViewRoot.setVis(View.GONE);
                }
            }
        }
        public boolean OnUpdate(float dt) {

            Circle bubbleCircle = new Circle(mTargetX + Config.mBubbleWidth * 0.5f,
                                             mTargetY + Config.mBubbleHeight * 0.5f,
                                             Config.mBubbleWidth * 0.5f);
            Canvas.TargetInfo targetInfo = mCanvas.getBubbleAction(bubbleCircle);

            int targetX, targetY;

            float t = 0.04f;
            if (targetInfo.mAction == Canvas.BubbleAction.None) {
                targetX = mTargetX;
                targetY = mTargetY;
            } else {
                t = 0.1f;
                targetX = (int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f);
                targetY = (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f);
            }

            if (targetX != mSetX || targetY != mSetY) {
                mBubble.setTargetPos(targetX, targetY, t);
                mSetX = targetX;
                mSetY = targetY;
            }

            return true;
        }
        public void OnMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
            sender.clearTargetPos();

            if (!mDidMove) {
                mCanvas.fadeOut();
                if (mMode == Mode.BubbleView) {
                    mMode = Mode.ContentView;
                    Util.Assert(mSelectedBubble == null);
                    setSelectedBubble(sender);
                    updateBubbleVisibility();
                    switchState(mAnimateToModeViewState);
                } else if (sender != mSelectedBubble) {
                    setSelectedBubble(sender);
                    mContentViewRoot.switchContent(mSelectedBubble.getContentView());
                    switchState(mIdleState);
                } else {
                    mMode = Mode.BubbleView;
                    switchState(mAnimateContentViewState);
                }
            } else {

                Circle bubbleCircle = new Circle(mBubble.getXPos() + Config.mBubbleWidth * 0.5f,
                                                 mBubble.getYPos() + Config.mBubbleHeight * 0.5f,
                                                 Config.mBubbleWidth * 0.5f);
                Canvas.TargetInfo targetInfo = mCanvas.getBubbleAction(bubbleCircle);
                targetInfo.mTargetX -= Config.mBubbleWidth * 0.5f;
                targetInfo.mTargetY -= Config.mBubbleHeight * 0.5f;

                if (targetInfo.mAction != Canvas.BubbleAction.None &&
                    mBubble.getXPos() == targetInfo.mTargetX &&
                    mBubble.getYPos() == targetInfo.mTargetY) {

                    mCanvas.fadeOut();
                    String url = mBubble.getUrl();
                    destroyBubble(mBubble);
                    if (mBubbles.size() > 0) {
                        switchState(mAnimateToModeViewState);
                    } else {
                        switchState(mIdleState);
                    }

                    doTargetAction(targetInfo.mAction, url);
                }
                else {
                    float v = (float) Math.sqrt(e.vx*e.vx + e.vy*e.vy);
                    float threshold = Config.dpToPx(900.0f);
                    if (v > threshold) {
                        mFlickBubbleState.init(sender, e.vx, e.vy);
                        switchState(mFlickBubbleState);
                    } else {
                        mCanvas.fadeOut();
                        if (mMode == Mode.BubbleView) {
                            switchState(mSnapToEdgeState);
                        } else {
                            switchState(mAnimateToModeViewState);
                        }
                    }
                }
            }
        }
        public void OnOrientationChanged() {
            switchState(mIdleState);
        }
        public String getName() { return "MoveFrontBubble"; }
    }

    // Flick bubble with velocity after release
    private class FlickBubbleState extends State {
        private Bubble mBubble;
        private Canvas.TargetInfo mTargetInfo;

        private OvershootInterpolator mOvershootInterpolator = new OvershootInterpolator(1.5f);
        private LinearInterpolator mLinearInterpolator = new LinearInterpolator();
        private float mTime;
        private float mPeriod;
        private float mInitialX;
        private float mInitialY;
        private float mTargetX;
        private float mTargetY;
        private boolean mLinear;

        public void init(Bubble bubble, float vx, float vy) {
            mTargetInfo = null;
            mBubble = bubble;

            mInitialX = bubble.getXPos();
            mInitialY = bubble.getYPos();
            mTime = 0.0f;
            mPeriod = 0.0f;
            mLinear = true;

            if (Math.abs(vx) < 0.1f) {
                mTargetX = mInitialX;

                if (vy > 0.0f) {
                    mTargetY = Config.mBubbleMaxY;
                } else {
                    mTargetY = Config.mBubbleMinY;
                }
            } else {

                if (vx > 0.0f) {
                    mTargetX = Config.mBubbleSnapRightX;
                } else {
                    mTargetX = Config.mBubbleSnapLeftX;
                }

                float m = vy / vx;

                mTargetY = m * (mTargetX - mInitialX) + mInitialY;

                if (mTargetY < Config.mBubbleMinY) {
                    mTargetY = Config.mBubbleMinY;
                    mTargetX = mInitialX + (mTargetY - mInitialY) / m;
                } else if (mTargetY > Config.mBubbleMaxY) {
                    mTargetY = Config.mBubbleMaxY;
                    mTargetX = mInitialX + (mTargetY - mInitialY) / m;
                } else {
                    mLinear = false;
                    mPeriod += 0.2f;
                }
            }

            float dx = mTargetX - mInitialX;
            float dy = mTargetY - mInitialY;
            float d = (float) Math.sqrt(dx*dx + dy*dy);

            float v = (float) Math.sqrt(vx*vx + vy*vy);

            mPeriod += d/v;
        }
        public void OnExitState() {
            mCanvas.fadeOut();
        }
        public boolean OnUpdate(float dt) {

            if (mTargetInfo == null) {
                float tf = mTime / mPeriod;
                float f = (mLinear ? mLinearInterpolator.getInterpolation(tf) : mOvershootInterpolator.getInterpolation(tf));
                mTime += dt;

                float x = mInitialX + (mTargetX - mInitialX) * f;
                float y = mInitialY + (mTargetY - mInitialY) * f;

                Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                        y + Config.mBubbleHeight * 0.5f,
                        Config.mBubbleWidth * 0.5f);

                Canvas.TargetInfo ti = mCanvas.getBubbleAction(bubbleCircle);
                switch (ti.mAction) {
                    case Destroy:
                    case OpenBrowser:
                    case OpenTwitter:
                        ti.mTargetX = (int) (0.5f + ti.mTargetX - Config.mBubbleWidth * 0.5f);
                        ti.mTargetY = (int) (0.5f + ti.mTargetY - Config.mBubbleHeight * 0.5f);
                        mTargetInfo = ti;
                        mBubble.setTargetPos(ti.mTargetX, ti.mTargetY, 0.04f);
                        break;
                    default:
                        if (mTime >= mPeriod) {
                            x = mTargetX;
                            y = mTargetY;

                            if (mMode == Mode.ContentView) {
                                switchState(mAnimateToModeViewState);
                            } else if (x == Config.mBubbleSnapLeftX || x == Config.mBubbleSnapRightX) {
                                switchState(mIdleState);
                            } else {
                                switchState(mSnapToEdgeState);
                            }
                        }
                        mBubble.setExactPos((int) x, (int) y);
                        break;
                }
            } else {
                if (mBubble.getXPos() == mTargetInfo.mTargetX &&
                    mBubble.getYPos() == mTargetInfo.mTargetY) {

                    String url = mBubble.getUrl();
                    destroyBubble(mBubble);
                    if (mBubbles.size() > 0) {
                        switchState(mAnimateToModeViewState);
                    } else {
                        switchState(mIdleState);
                    }

                    doTargetAction(mTargetInfo.mAction, url);
                }
            }

            return true;
        }
        public String getName() { return "FlickBubble"; }
    }

    private class SnapToEdgeState extends State {
        private float mPosX;
        private float mDistanceX;
        private OvershootInterpolator mInterpolator = new OvershootInterpolator(1.5f);
        private float mTime;
        private float mPeriod;

        public void OnEnterState() {
            mTime = 0.0f;
            mPeriod = 0.3f;

            mPosX = (float) getFrontBubble().getXPos();
            if (mPosX < Config.mScreenCenterX) {
                mDistanceX = Config.mBubbleSnapLeftX - mPosX;
            } else {
                mDistanceX = Config.mBubbleSnapRightX - mPosX;
            }
        }
        public boolean OnUpdate(float dt) {
            float f = mInterpolator.getInterpolation(mTime / mPeriod);
            mTime += dt;

            Bubble frontBubble = getFrontBubble();

            float x = mPosX + mDistanceX * f;
            float y = (float) frontBubble.getYPos();

            if (mTime >= mPeriod) {
                x = Util.clamp(Config.mBubbleSnapLeftX, x, Config.mBubbleSnapRightX);
                setAllBubblePositions((int)x, (int)y);
                switchState(mIdleState);
            }

            mBubbleHomeX = (int) x;
            mBubbleHomeY = (int) y;

            getFrontBubble().setExactPos(mBubbleHomeX, mBubbleHomeY);
            return true;
        }
        public String getName() { return "SnapToEdge"; }
    }

    private class AnimateToModeViewState extends State {
        private class BubbleInfo {
            public float mPosX;
            public float mPosY;
            public float mDistanceX;
            public float mDistanceY;
            public float mTargetX;
            public float mTargetY;
        }

        private OvershootInterpolator mInterpolator = new OvershootInterpolator(0.5f);
        private float mTime;
        private float mPeriod;
        private Vector<BubbleInfo> mBubbleInfo = new Vector<BubbleInfo>();

        public void OnEnterState() {
            mBubbleInfo.clear();
            mTime = 0.0f;
            mPeriod = 0.3f;

            int bubbleCount = mBubbles.size();
            for (int i=0 ; i < bubbleCount ; ++i) {
                BubbleInfo bi = new BubbleInfo();
                Bubble b = mBubbles.get(i);
                bi.mPosX = (float) b.getXPos();
                bi.mPosY = (float) b.getYPos();
                if (mMode == Mode.BubbleView) {
                    bi.mTargetX = mBubbleHomeX;
                    bi.mTargetY = mBubbleHomeY;
                } else {
                    Util.Assert(mMode == Mode.ContentView);
                    bi.mTargetX = Config.getContentViewX(i);
                    bi.mTargetY = Config.mContentViewBubbleY;
                }
                bi.mDistanceX = bi.mTargetX - bi.mPosX;
                bi.mDistanceY = bi.mTargetY - bi.mPosY;
                mBubbleInfo.add(bi);
            }
        }
        public boolean OnUpdate(float dt) {
            float f = mInterpolator.getInterpolation(mTime / mPeriod);
            mTime += dt;

            int bubbleCount = mBubbles.size();
            for (int i=0 ; i < bubbleCount ; ++i) {
                BubbleInfo bi = mBubbleInfo.get(i);
                Bubble b = mBubbles.get(i);

                float x = bi.mPosX + bi.mDistanceX * f;
                float y = bi.mPosY + bi.mDistanceY * f;

                if (mTime >= mPeriod) {
                    x = bi.mTargetX;
                    y = bi.mTargetY;
                }

                b.setExactPos((int) x, (int) y);
            }

            if (mTime >= mPeriod) {
                updateBubbleVisibility();
                if (mMode == Mode.ContentView) {
                    mContentViewRoot.setVis(View.VISIBLE);
                    switchState(mAnimateContentViewState);
                } else {
                    switchState(mIdleState);
                }
            }

            return true;
        }
        public String getName() { return "AnimateToModeView"; }
    }

    private class AnimateContentViewState extends State {
        private float mTime;
        private float mPeriod;

        public void setPivot(float xp) {
            mContentViewRoot.setPivot(xp, 0.0f);
        }

        public void OnEnterState() {
            mPeriod = 0.3f;
            mTime = 0.0f;
            setPivot(mSelectedBubble.getXPos() + Config.mBubbleWidth * 0.5f);

            if (mMode == Mode.ContentView) {
                mContentViewRoot.setScale(0.0f, 0.0f);
                mContentViewRoot.show(mSelectedBubble.getContentView());
            } else {
                mContentViewRoot.setScale(1.0f, 1.0f);
            }

            mContentViewRoot.enableWebView(false);
        }
        public void OnExitState() {
            mContentViewRoot.setScale(1.0f, 1.0f);
            mContentViewRoot.enableWebView(true);
        }
        public boolean OnUpdate(float dt) {
            if (mTime >= mPeriod) {
                if (mMode == Mode.BubbleView) {
                    switchState(mAnimateToModeViewState);
                    mContentViewRoot.hide();

                    Util.Assert(mSelectedBubble != null);
                    setSelectedBubble(null);
                } else {
                    switchState(mIdleState);
                }
            }

            float scale = mTime / mPeriod;
            if (mMode == Mode.BubbleView)
                scale = 1.0f - scale;
            mContentViewRoot.setScale(scale, scale);

            mTime += dt;
            return true;
        }
        public String getName() { return "AnimateContentView"; }
    }

    private IdleState mIdleState = new IdleState();
    private MoveFrontBubbleState mMoveFrontBubbleState = new MoveFrontBubbleState();
    private FlickBubbleState mFlickBubbleState = new FlickBubbleState();
    private SnapToEdgeState mSnapToEdgeState = new SnapToEdgeState();
    private AnimateToModeViewState mAnimateToModeViewState = new AnimateToModeViewState();
    private AnimateContentViewState mAnimateContentViewState = new AnimateContentViewState();

    private Context mContext;
    private Choreographer mChoreographer;
    private boolean mUpdateScheduled;
    private State mCurrentState;
    private Vector<Bubble> mBubbles = new Vector<Bubble>();
    private Vector<Bubble> mPendingBubbles = new Vector<Bubble>();
    private Canvas mCanvas;
    private Badge mBadge;
    private static MainController sMainController;
    private boolean mTouchDown;
    private boolean mAllowTouchEvents;
    private int mBubbleHomeX;
    private int mBubbleHomeY;
    private Mode mMode;
    private boolean mEnabled;

    //private TextView mTextView;
    //private WindowManager mWindowManager;
    //private WindowManager.LayoutParams mWindowManagerParams = new WindowManager.LayoutParams();
    //private int mFrameNumber;

    private Bubble mSelectedBubble;
    private ContentViewRoot mContentViewRoot;

    private void setSelectedBubble(Bubble b) {
        mSelectedBubble = b;
    }

    private Bubble getFrontBubble() {
        Util.Assert(mBubbles.size() > 0);
        return mBubbles.lastElement();
    }

    private void setAllBubblePositions(int x, int y) {
        Bubble frontBubble = getFrontBubble();

        // Force all bubbles to be where the moved one ended up
        int bubbleCount = mBubbles.size();
        for (int i=0 ; i < bubbleCount-1 ; ++i) {
            Bubble b = mBubbles.get(i);
            Util.Assert(b != frontBubble);
            b.setExactPos(x, y);
        }
    }

    private void destroyBubble(Bubble bubble) {
        bubble.destroy();
        int bubbleIndex = mBubbles.indexOf(bubble);
        mBubbles.remove(bubble);

        if (mBubbles.size() > 0) {
            int nextBubbleIndex = Util.clamp(0, bubbleIndex, mBubbles.size()-1);
            Bubble nextBubble = mBubbles.get(nextBubbleIndex);
            mBadge.attach(nextBubble);
            if (mMode == Mode.ContentView) {
                //mContentViewRoot.switchContent(nextBubble.getContentView());
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
    }

    public MainController(Context context) {
        Util.Assert(sMainController == null);
        sMainController = this;
        mContext = context;
        mMode = Mode.BubbleView;
        mContentViewRoot = new ContentViewRoot(context);
        mAllowTouchEvents = true;
        mEnabled = true;

        /*mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
        mWindowManager.addView(mTextView, mWindowManagerParams);*/

        mUpdateScheduled = false;
        mChoreographer = Choreographer.getInstance();
        mCanvas = new Canvas(context);
        mBadge = new Badge(context);

        mBubbleHomeX = Config.mBubbleSnapLeftX;
        mBubbleHomeY = (int) (Config.mScreenHeight * 0.4f);

        switchState(mIdleState);
    }

    public static void scheduleUpdate() {
        Util.Assert(sMainController != null);
        if (!sMainController.mUpdateScheduled) {
            sMainController.mUpdateScheduled = true;
            sMainController.mChoreographer.postFrameCallback(sMainController);
        }
    }

    private void switchState(State newState) {
        switchState(newState, false);
    }

    private void switchState(State newState, boolean allowTouchDown) {
        if (mTouchDown && !allowTouchDown) {
            mAllowTouchEvents = false;
        } else {
            mAllowTouchEvents = true;
        }
        Util.Assert(newState != mCurrentState);
        if (mCurrentState != null) {
            mCurrentState.OnExitState();
        }
        mCurrentState = newState;
        mCurrentState.OnEnterState();
        scheduleUpdate();
    }

    public void doFrame(long frameTimeNanos) {
        mUpdateScheduled = false;

        float dt = 1.0f / 60.0f;

        int bubbleCount = mBubbles.size();
        for (int i=0 ; i < bubbleCount ; ++i) {
            Bubble b = mBubbles.get(i);
            b.update(dt);
        }

        mCanvas.update(dt);

        if (mCurrentState.OnUpdate(dt)) {
            scheduleUpdate();
        }

        //String modeString = (mMode == Mode.BubbleView ? "BubbleView" : "ContentView");
        //mTextView.setText("M=" + modeString + " S=" + mCurrentState.getName() + " F=" + mFrameNumber++);
    }

    private void updateBubbleVisibility() {
        int bubbleCount = mBubbles.size();

        mBadge.setBubbleCount(bubbleCount);
        if (mMode == Mode.BubbleView && mEnabled)
            mBadge.show();
        else
            mBadge.hide();

        for (int i=0 ; i < bubbleCount ; ++i) {
            Bubble b = mBubbles.get(i);
            int vis = View.VISIBLE;
            if (!mEnabled || (mMode == Mode.BubbleView && i != bubbleCount-1))
                vis = View.GONE;
            b.setVisibility(vis);
        }
    }

    public void enable() {
        mEnabled = true;
        updateBubbleVisibility();
        mCanvas.enable(true);
        mContentViewRoot.enable(true);
    }

    public void disable() {
        mEnabled = false;
        updateBubbleVisibility();
        mCanvas.enable(false);
        mContentViewRoot.enable(false);
    }

    public void onOrientationChanged() {
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
        mCurrentState.OnOrientationChanged();
    }

    public void onOpenUrl(String url, boolean recordHistory) {
        if (mBubbles.size() < Config.MAX_BUBBLES) {
            Bubble bubble = new Bubble(mContext, url, mBubbleHomeX, mBubbleHomeY, recordHistory, new Bubble.EventHandler() {
                @Override
                public void onMotionEvent_Touch(Bubble sender, Bubble.TouchEvent e) {
                    Util.Assert(!mTouchDown);
                    mTouchDown = true;
                    mCurrentState.OnMotionEvent_Touch(sender, e);
                }

                @Override
                public void onMotionEvent_Move(Bubble sender, Bubble.MoveEvent e) {
                    Util.Assert(mTouchDown);
                    if (mAllowTouchEvents) {
                        mCurrentState.OnMotionEvent_Move(sender, e);
                    }
                }

                @Override
                public void onMotionEvent_Release(Bubble sender, Bubble.ReleaseEvent e) {
                    Util.Assert(mTouchDown);
                    mTouchDown = false;
                    if (mAllowTouchEvents) {
                        mCurrentState.OnMotionEvent_Release(sender, e);
                    }
                    mAllowTouchEvents = true;
                }

                @Override
                public void onCloseClicked(Bubble sender) {
                    destroyBubble(sender);
                    if (mBubbles.size() > 0) {
                        switchState(mAnimateToModeViewState);
                    }
                }

                @Override
                public void onSharedLink(Bubble sender) {
                    Util.Assert(mCurrentState == mIdleState);
                    Util.Assert(mMode == Mode.ContentView);
                    mMode = Mode.BubbleView;
                    switchState(mAnimateContentViewState);
                }
            });

            if (mCurrentState.OnNewBubble(bubble)) {
                mBubbles.add(bubble);
                mBadge.attach(bubble);
                updateBubbleVisibility();
            } else {
                mPendingBubbles.add(bubble);
            }
        }
    }
}
