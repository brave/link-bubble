package com.linkbubble.physics;

import android.content.Context;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.Config;
import com.linkbubble.MainController;
import com.linkbubble.util.Util;

import java.util.Vector;

/**
 * Created by gw on 18/11/13.
 */
public class State_AnimateToContentView extends ControllerState {

    private class DraggableInfo {
        public float mPosX;
        public float mPosY;
        public float mDistanceX;
        public float mDistanceY;
        public float mTargetX;
        public float mTargetY;
    }

    private OvershootInterpolator mInterpolator = new OvershootInterpolator(0.5f);
    private float mTime;
    private float mBubblePeriod;
    private float mContentPeriod;
    private Vector<DraggableInfo> mDraggableInfo = new Vector<DraggableInfo>();

    public State_AnimateToContentView() {
    }

    @Override
    public void onEnterState() {
        mDraggableInfo.clear();
        mTime = 0.0f;
        mBubblePeriod = (float)Constant.BUBBLE_ANIM_TIME / 1000.f;
        mContentPeriod = mBubblePeriod * 0.666667f;      // 0.66667 is the normalized t value when f = 1.0f for overshoot interpolator of 0.5 tension

        MainController mainController = MainController.get();
        DraggableInfo draggableInfo = new DraggableInfo();
        Draggable draggable = mainController.getBubbleDraggable();
        draggable.getDraggableView().setVisibility(View.VISIBLE);

        draggableInfo.mPosX = (float) draggable.getDraggableHelper().getXPos();
        draggableInfo.mPosY = (float) draggable.getDraggableHelper().getYPos();

        draggableInfo.mTargetX = Config.getContentViewX(0, 1);
        draggableInfo.mTargetY = Config.mContentViewBubbleY;
        draggableInfo.mDistanceX = draggableInfo.mTargetX - draggableInfo.mPosX;
        draggableInfo.mDistanceY = draggableInfo.mTargetY - draggableInfo.mPosY;
        mDraggableInfo.add(draggableInfo);

        mainController.beginAppPolling();
        mainController.expandBubbleFlow((long) (mContentPeriod * 1000));
    }

    @Override
    public boolean onUpdate(float dt) {
        float f = mInterpolator.getInterpolation(mTime / mBubblePeriod);
        //Log.e("GapTech", "t=" + mTime / mBubblePeriod + ", f=" + f);
        mTime += dt;

        MainController mainController = MainController.get();
        DraggableInfo draggableInfo = mDraggableInfo.get(0);
        Draggable draggable = mainController.getBubbleDraggable();

        float x = draggableInfo.mPosX + draggableInfo.mDistanceX * f;
        float y = draggableInfo.mPosY + draggableInfo.mDistanceY * f;

        if (mTime >= mBubblePeriod) {
            x = draggableInfo.mTargetX;
            y = draggableInfo.mTargetY;
        }

        draggable.getDraggableHelper().setExactPos((int) x, (int) y);

        float t = Util.clamp(0.0f, 1.0f - mTime / mContentPeriod, 1.0f);

        if (mTime >= mBubblePeriod && mTime >= mContentPeriod) {
            mainController.switchState(mainController.STATE_ContentView);
        }

        return true;
    }

    @Override
    public void onExitState() {
    }

    @Override
    public void onTouchActionDown(Draggable sender, DraggableHelper.TouchEvent e) {
    }

    @Override
    public void onTouchActionMove(Draggable sender, DraggableHelper.MoveEvent e) {
    }

    @Override
    public void onTouchActionRelease(Draggable sender, DraggableHelper.ReleaseEvent e) {
    }

    @Override
    public boolean onNewDraggable(Draggable draggable) {
        Util.Assert(false);
        return false;
    }

    @Override
    public void onDestroyDraggable(Draggable draggable) {
        Util.Assert(false);
    }

    @Override
    public boolean onOrientationChanged() {
        MainController mainController = MainController.get();
        mainController.switchState(mainController.STATE_ContentView);
        return true;
    }

    @Override
    public void onCloseDialog() {
    }

    @Override
    public String getName() {
        return "AnimateToContentView";
    }
}
