package com.linkbubble;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.State_AnimateToBubbleView;
import com.linkbubble.ui.BubbleLegacyView;
import com.linkbubble.util.Util;

import java.net.MalformedURLException;
import java.util.Vector;

public class MainControllerLegacy extends MainController {

    public static void create(Context context, EventHandler eventHandler) {
        if (sInstance != null) {
            new RuntimeException("Only one instance of MainController allowed at any one time");
        }
        sInstance = new MainControllerLegacy(context, eventHandler);
    }

    private static Vector<BubbleLegacyView> mBubbles = new Vector<BubbleLegacyView>();

    private MainControllerLegacy(Context context, EventHandler eventHandler) {
        super(context, eventHandler);
    }

    @Override
    public boolean destroyBubble(Draggable draggable, Config.BubbleAction action) {
        return destroyBubble(draggable.getBubbleLegacyView(), action);
    }

    public boolean destroyBubble(BubbleLegacyView bubble, Config.BubbleAction action) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean debug = prefs.getBoolean("debug_flick", true);

        if (debug) {
            Toast.makeText(mContext, "HIT TARGET!", 400).show();
        } else {
            String url = bubble.getUrl().toString();

            bubble.destroy();

            {
            int bubbleIndex = mBubbles.indexOf(bubble);
            Util.Assert(bubbleIndex >= 0 && bubbleIndex < mBubbles.size());
            mBubbles.remove(bubble);
            }

            int draggableIndex = mDraggables.indexOf(bubble);
            Util.Assert(draggableIndex >= 0 && draggableIndex < mDraggables.size());
            mDraggables.remove(bubble);

            Settings.get().saveCurrentBubblesLegacy(mBubbles);

            for (int i=0 ; i < mBubbles.size() ; ++i) {
                mBubbles.get(i).setBubbleIndex(i);
            }

            if (mBubbles.size() > 0) {
                int nextBubbleIndex = Util.clamp(0, draggableIndex, mBubbles.size()-1);
                BubbleLegacyView nextBubble = mBubbles.get(nextBubbleIndex);
                mFrontDraggable = nextBubble;
                mBadgeView.attach(nextBubble);
                mBadgeView.setBubbleCount(mBubbles.size());

                nextBubble.setVisibility(View.VISIBLE);
            } else {
                hideContentActivity();
                mBadgeView.attach(null);
                mFrontDraggable = null;

                Config.BUBBLE_HOME_X = Config.mBubbleSnapLeftX;
                Config.BUBBLE_HOME_Y = (int) (Config.mScreenHeight * 0.4f);
            }

            //((MainApplication)mContext.getApplicationContext()).getBus().post(new BubbleRemovedEvent(bubble));

            mCurrentState.onDestroyDraggable(bubble);

            doTargetAction(action, url);
        }

        return getBubbleCount() > 0;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(!incognito);

        for (int i=0 ; i < mBubbles.size() ; ++i) {
            mBubbles.get(i).updateIncognitoMode(incognito);
        }
    }

    @Override
    public int getBubbleCount() {
        return mBubbles.size();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        mUpdateScheduled = false;

        float dt = 1.0f / 60.0f;

        int draggableCount = mDraggables.size();
        for (int i=0 ; i < draggableCount ; ++i) {
            Draggable draggable = mDraggables.get(i);
            draggable.update(dt, mCurrentState == STATE_ContentView);
        }

        Draggable frontDraggable = null;
        if (getBubbleCount() > 0) {
            frontDraggable = getActiveDraggable();
        }
        mCanvasView.update(dt, frontDraggable);

        if (mCurrentState.onUpdate(dt)) {
            scheduleUpdate();
        }

        //mTextView.setText("S=" + mCurrentState.getName() + " F=" + mFrameNumber++);

        if (mCurrentState == STATE_BubbleView && mDraggables.size() == 0 &&
                mBubblesLoaded > 0 && !mUpdateScheduled) {
            mEventHandler.onDestroy();
        }
    }

    @Override
    protected void openUrlInBubble(String url, long startTime) {
        if (mDraggables.size() < Config.MAX_BUBBLES) {

            int x, targetX, y, targetY;
            float time;

            int bubbleIndex = mDraggables.size();

            if (mCurrentState == STATE_ContentView) {
                x = (int) Config.getContentViewX(bubbleIndex, getBubbleCount()+1);
                y = (int) -Config.mBubbleHeight;
                targetX = x;
                targetY = Config.mContentViewBubbleY;
                time = 0.4f;
            } else {
                if (bubbleIndex == 0) {
                    x = (int) (Config.mBubbleSnapLeftX - Config.mBubbleWidth);
                    y = Config.BUBBLE_HOME_Y;
                    targetX = Config.BUBBLE_HOME_X;
                    targetY = y;
                    time = 0.4f;
                } else {
                    x = Config.BUBBLE_HOME_X;
                    y = Config.BUBBLE_HOME_Y;
                    targetX = x;
                    targetY = y;
                    time = 0.0f;
                }
            }

            Draggable draggable = null;
            BubbleLegacyView bubble;
            try {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                bubble = (BubbleLegacyView) inflater.inflate(R.layout.view_bubble_legacy, null);
                draggable = bubble;
                bubble.configure(url, x, y, targetX, targetY, time, startTime,
                        new BubbleLegacyView.EventHandler() {
                            @Override
                            public void onMotionEvent_Touch(BubbleLegacyView sender, DraggableHelper.TouchEvent e) {
                                mCurrentState.onTouchActionDown(sender, e);
                                //showContentActivity();
                            }

                            @Override
                            public void onMotionEvent_Move(BubbleLegacyView sender, DraggableHelper.MoveEvent e) {
                                mCurrentState.onTouchActionMove(sender, e);
                            }

                            @Override
                            public void onMotionEvent_Release(BubbleLegacyView sender, DraggableHelper.ReleaseEvent e) {
                                mCurrentState.onTouchActionRelease(sender, e);
                                //if (mCurrentState instanceof State_SnapToEdge) {
                                //    hideContentActivity();
                                //}
                            }

                            @Override
                            public void onDestroyDraggable(Draggable sender) {
                                if (mDraggables.size() > 1) {
                                    BubbleLegacyView bubbleView = sender.getBubbleLegacyView();
                                    int bubbleIndex = bubbleView.getBubbleIndex();
                                    destroyBubble(sender, Config.BubbleAction.Destroy);
                                    int nextBubbleIndex = Util.clamp(0, bubbleIndex, mDraggables.size()-1);
                                    Draggable nextBubble = mDraggables.get(nextBubbleIndex);
                                    STATE_ContentView.setActiveBubble(nextBubble);
                                } else {
                                    STATE_KillBubble.init(sender);
                                    switchState(STATE_KillBubble);
                                }
                            }

                            @Override
                            public void onMinimizeBubbles() {
                                if (mCurrentState != null && mCurrentState instanceof State_AnimateToBubbleView == false) {
                                    switchState(STATE_AnimateToBubbleView);
                                }
                            }

                        });
            } catch (MalformedURLException e) {
                // TODO: Inform the user somehow?
                return;
            }

            mCurrentState.onNewDraggable(draggable);
            mDraggables.add(draggable);
            mBubbles.add(bubble);
            ++mBubblesLoaded;

            for (int i=0 ; i < mBubbles.size() ; ++i) {
                mBubbles.get(i).setBubbleIndex(i);
            }

            Settings.get().saveCurrentBubblesLegacy(mBubbles);

            mBadgeView.attach(bubble);
            mBadgeView.setBubbleCount(mBubbles.size());
            int draggableCount = mDraggables.size();
            if (mCurrentState == STATE_ContentView) {
                draggable.getDraggableView().setVisibility(View.VISIBLE);
                for (int i=0 ; i < draggableCount ; ++i) {
                    Draggable draggableItem = mDraggables.get(i);
                    if (draggableItem != bubble) {
                        draggableItem.getDraggableHelper().setTargetPos((int)Config.getContentViewX(draggableItem.getBubbleLegacyView().getBubbleIndex(),
                                getBubbleCount()), draggableItem.getDraggableHelper().getYPos(), 0.2f, false);
                    }
                }
            } else {
                mFrontDraggable = bubble;
                for (int i=0 ; i < draggableCount ; ++i) {
                    Draggable draggableItem = mDraggables.get(i);
                    int vis = View.VISIBLE;
                    if (draggableItem != mFrontDraggable) {
                        vis = View.GONE;
                    }
                    draggableItem.getDraggableView().setVisibility(vis);
                }
            }

            //((MainApplication)mContext.getApplicationContext()).getBus().post(new BubbleAddedEvent(bubble));
        }
    }
}
