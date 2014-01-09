package com.linkbubble;

import android.content.Context;
import android.view.LayoutInflater;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.ui.BubbleFlowAdapter;
import com.linkbubble.ui.BubbleFlowView;


public class MainControllerNew extends MainController {

    public static void create(Context context, EventHandler eventHandler) {
        if (sInstance != null) {
            new RuntimeException("Only one instance of MainController allowed at any one time");
        }
        sInstance = new MainControllerNew(context, eventHandler);
    }

    private BubbleFlowView mBubbleFlowView;

    protected MainControllerNew(Context context, EventHandler eventHandler) {
        super(context, eventHandler);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mBubbleFlowView = (BubbleFlowView) inflater.inflate(R.layout.view_bubble_flow, null);
        BubbleFlowAdapter bubbleFlowAdapter = new BubbleFlowAdapter(context, false);
        mBubbleFlowView.setAdapter(bubbleFlowAdapter);
        int bubbleFlowViewX = (Config.mScreenWidth - context.getResources().getDimensionPixelSize(R.dimen.bubble_flow_width)) / 2;
        mBubbleFlowView.configure(bubbleFlowViewX, 0, bubbleFlowViewX, 0, 0.f, new BubbleFlowView.EventHandler() {
            @Override
            public void onMotionEvent_Touch(BubbleFlowView sender, DraggableHelper.TouchEvent event) {
                mCurrentState.onTouchActionDown(sender, event);
            }

            @Override
            public void onMotionEvent_Move(BubbleFlowView sender, DraggableHelper.MoveEvent event) {
                mCurrentState.onTouchActionMove(sender, event);
            }

            @Override
            public void onMotionEvent_Release(BubbleFlowView sender, DraggableHelper.ReleaseEvent event) {
                mCurrentState.onTouchActionRelease(sender, event);
            }
        });
        mDraggables.add(mBubbleFlowView);
        setActiveDraggable(mBubbleFlowView);
    }

    @Override
    public boolean destroyBubble(Draggable draggable, Config.BubbleAction action) {
        return false;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {
        CookieSyncManager.createInstance(mContext);
        CookieManager.getInstance().setAcceptCookie(!incognito);

        if (mBubbleFlowView != null) {
            mBubbleFlowView.updateIncognitoMode(incognito);
        }
    }

    @Override
    public int getBubbleCount() {
        return mBubbleFlowView != null ? mBubbleFlowView.getBubbleCount() : 0;
    }

    @Override
    protected void openUrlInBubble(String url, long startTime) {
        mBubbleFlowView.openUrlInBubble(url, startTime);
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

        if (mBubbleFlowView != null) {
            mBubbleFlowView.update(dt, mCurrentState == STATE_ContentView);
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
