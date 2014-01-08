package com.linkbubble.physics;

import com.linkbubble.ui.BubbleView;
import com.linkbubble.ui.CanvasView;
import com.linkbubble.Config;
import com.linkbubble.ui.ContentView;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

/**
 * Created by gw on 18/11/13.
 */
public class State_KillBubble extends ControllerState {

    private CanvasView mCanvasView;
    private float mTime;
    private float mPeriod;

    private DraggableItem mBubble;
    private float mBubbleY0;

    public State_KillBubble(CanvasView canvasView) {
        mCanvasView = canvasView;
    }

    public void init(DraggableItem bubble) {
        Util.Assert(mBubble == null);
        mBubble = bubble;
        mBubbleY0 = mBubble.getDraggableHelper().getYPos();
    }

    @Override
    public void onEnterState() {
        Util.Assert(mBubble != null);
        mCanvasView.fadeOutTargets();
        ContentView contentView = mCanvasView.getContentView();
        if (contentView != null) {
            contentView.onAnimateOffscreen();
        }
        mTime = 0.0f;
        mPeriod = 0.3f;
        mCanvasView.setContentViewTranslation(0.0f);

        MainController.get().endAppPolling();
    }

    @Override
    public boolean onUpdate(float dt) {
        float t = mTime / mPeriod;
        mTime += dt;

        float dy = t * Config.mScreenHeight;

        mBubble.getDraggableHelper().setExactPos(mBubble.getDraggableHelper().getXPos(), (int) (dy + mBubbleY0));

        mCanvasView.setContentViewTranslation(dy);

        if (mTime >= mPeriod) {
            MainController mainController = MainController.get();
            mainController.switchState(mainController.STATE_BubbleView);
            mainController.hideContentActivity();
        }

        return true;
    }

    @Override
    public void onExitState() {
        mCanvasView.setContentViewTranslation(Config.mScreenHeight - Config.mContentOffset);
        mCanvasView.setContentView(null);
        MainController.get().destroyBubble(mBubble, Config.BubbleAction.Destroy);
        mBubble = null;
    }

    @Override
    public void onTouchActionDown(DraggableItem sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(DraggableItem sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(DraggableItem sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean onNewDraggable(DraggableItem draggableItem) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyDraggable(DraggableItem draggableItem) {
    }

    @Override
    public boolean onOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_BubbleView);
        return false;
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "KillBubble";
    }
}
