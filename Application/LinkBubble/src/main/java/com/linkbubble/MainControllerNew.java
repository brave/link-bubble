package com.linkbubble;

import android.content.Context;
import android.view.LayoutInflater;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.ui.BubbleDraggable;
import com.linkbubble.ui.BubblePagerDraggable;


public class MainControllerNew extends MainController {

    public static void create(Context context, EventHandler eventHandler) {
        if (sInstance != null) {
            new RuntimeException("Only one instance of MainController allowed at any one time");
        }
        sInstance = new MainControllerNew(context, eventHandler);
    }

    private BubblePagerDraggable mBubblePagerDraggable;
    private BubbleDraggable mBubbleDraggable;

    protected MainControllerNew(Context context, EventHandler eventHandler) {
        super(context, eventHandler);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mBubblePagerDraggable = (BubblePagerDraggable) inflater.inflate(R.layout.view_bubble_pager, null);

        int bubbleFlowViewX = (Config.mScreenWidth - context.getResources().getDimensionPixelSize(R.dimen.bubble_pager_width)) / 2;
        mBubblePagerDraggable.configure(bubbleFlowViewX, 0, bubbleFlowViewX, 0, 0.f, null);

        mBubbleDraggable = (BubbleDraggable) inflater.inflate(R.layout.view_bubble_draggable, null);
        mBubbleDraggable.configure((int) (Config.mBubbleSnapLeftX - Config.mBubbleWidth), Config.BUBBLE_HOME_Y,
                Config.BUBBLE_HOME_X, Config.BUBBLE_HOME_Y, 0.4f, new BubbleDraggable.EventHandler() {
            @Override
            public void onMotionEvent_Touch(BubbleDraggable sender, DraggableHelper.TouchEvent event) {
                mCurrentState.onTouchActionDown(sender, event);
            }

            @Override
            public void onMotionEvent_Move(BubbleDraggable sender, DraggableHelper.MoveEvent event) {
                mCurrentState.onTouchActionMove(sender, event);
            }

            @Override
            public void onMotionEvent_Release(BubbleDraggable sender, DraggableHelper.ReleaseEvent event) {
                mCurrentState.onTouchActionRelease(sender, event);
            }
        });

        mBubbleDraggable.setOnUpdateListener(new BubbleDraggable.OnUpdateListener() {
            @Override
            public void onUpdate(Draggable draggable, float dt, boolean contentView) {
                mBubblePagerDraggable.syncWithBubble(draggable);
            }
        });

        mDraggables.add(mBubbleDraggable);
        setActiveDraggable(mBubbleDraggable);
    }

    @Override
    public boolean destroyBubble(Draggable draggable, Config.BubbleAction action) {
        return false;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(!incognito);

        if (mBubblePagerDraggable != null) {
            mBubblePagerDraggable.updateIncognitoMode(incognito);
        }
    }

    @Override
    public int getBubbleCount() {
        return mBubblePagerDraggable != null ? mBubblePagerDraggable.getBubbleCount() : 0;
    }

    @Override
    protected void openUrlInBubble(String url, long startTime) {
        mBubblePagerDraggable.openUrlInBubble(url, startTime);
        ++mBubblesLoaded;
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
}
