package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.DraggableHelper;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;


public class BubbleLegacyView extends BubbleView implements Draggable {


    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleLegacyView sender, DraggableHelper.TouchEvent event);
        public void onMotionEvent_Move(BubbleLegacyView sender, DraggableHelper.MoveEvent event);
        public void onMotionEvent_Release(BubbleLegacyView sender, DraggableHelper.ReleaseEvent event);
        public void onDestroyDraggable(Draggable sender);
        public void onMinimizeBubbles();
    }

    private DraggableHelper mDraggableHelper;
    private EventHandler mEventHandler;
    protected WindowManager mWindowManager;
    protected ContentView mContentView;

    private boolean mRecordHistory;
    protected int mBubbleIndex;

    public BubbleLegacyView(Context context) {
        this(context, null);
    }

    public BubbleLegacyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleLegacyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public void configure(String url, int x0, int y0, int targetX, int targetY, float targetTime, long startTime,
                          EventHandler eh) throws MalformedURLException {
        mEventHandler = eh;
        mRecordHistory = Settings.get().isIncognitoMode() ? false : true;

        super.configure(url);

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();
        windowManagerParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManagerParams.x = x0;
        windowManagerParams.y = y0;
        int bubbleSize = getResources().getDimensionPixelSize(R.dimen.bubble_size);
        windowManagerParams.height = bubbleSize;
        windowManagerParams.width = bubbleSize;
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowManagerParams.format = PixelFormat.TRANSPARENT;
        windowManagerParams.setTitle("LinkBubble: BubbleLegacyView");

        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(this, url, startTime, new ContentView.EventHandler() {

            @Override
            public void onDestroyBubble() {
                mEventHandler.onDestroyDraggable(BubbleLegacyView.this);
            }

            @Override
            public void onMinimizeBubbles() {
                mEventHandler.onMinimizeBubbles();
            }

            @Override
            public void onPageLoading(String url) {
                BubbleLegacyView.this.onPageLoading(url);
            }

            @Override
            public void onProgressChanged(int progress) {
                BubbleLegacyView.this.onProgressChanged(progress);
            }

            @Override
            public void onPageLoaded(ContentView.PageLoadInfo info) {
                BubbleLegacyView.this.onPageLoaded(info);

                if (mRecordHistory && info != null && info.url != null) {
                    MainApplication.saveUrlInHistory(getContext(), null, info.url, info.mHost, info.title);
                }

                MainController.get().onPageLoaded(BubbleLegacyView.this);
            }

            @Override
            public void onReceivedIcon(Bitmap favicon) {
                BubbleLegacyView.this.onReceivedIcon(favicon);
            }
        });

        mDraggableHelper = new DraggableHelper(this, mWindowManager, windowManagerParams, new DraggableHelper.OnTouchActionEventListener() {

            @Override
            public void onActionDown(DraggableHelper.TouchEvent event) {
                mEventHandler.onMotionEvent_Touch(BubbleLegacyView.this, event);
            }

            @Override
            public void onActionMove(DraggableHelper.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubbleLegacyView.this, event);
            }

            @Override
            public void onActionUp(DraggableHelper.ReleaseEvent event) {
                mEventHandler.onMotionEvent_Release(BubbleLegacyView.this, event);
            }
        });

        setVisibility(GONE);

        if (mDraggableHelper.isAlive()) {
            mWindowManager.addView(this, windowManagerParams);

            setExactPos(x0, y0);
            if (targetX != x0 || targetY != y0) {
                setTargetPos(targetX, targetY, targetTime, true);
            }
        }
    }

    public void destroy() {

        setOnTouchListener(null);

        if (mContentView != null) {
            mContentView.destroy();
        }

        mDraggableHelper.destroy();
    }


    public void onOrientationChanged(boolean contentViewMode) {
        clearTargetPos();

        int xPos, yPos;

        if (contentViewMode) {
            xPos = (int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount());
            yPos = Config.mContentViewBubbleY;
        } else {
            WindowManager.LayoutParams windowManagerParms = mDraggableHelper.getWindowManagerParams();
            if (windowManagerParms.x < Config.mScreenHeight * 0.5f)
                xPos = Config.mBubbleSnapLeftX;
            else
                xPos = Config.mBubbleSnapRightX;
            float yf = (float)windowManagerParms.y / (float)Config.mScreenWidth;
            yPos = (int) (yf * Config.mScreenHeight);
        }

        setExactPos(xPos, yPos);
    }

    public void readd() {
        ProgressIndicator progressIndicator = getProgressIndicator();
        boolean showing = progressIndicator.isIndicatorShowing();
        int progress = progressIndicator.getProgress();
        URL url = progressIndicator.getUrl();

        mWindowManager.removeView(this);
        mWindowManager.addView(this, mDraggableHelper.getWindowManagerParams());
        if (url != null) {
            progressIndicator.setProgress(showing, progress, url);
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

    @Override
    public ContentView getContentView() {
        return mContentView;
    }

    @Override
    public DraggableHelper getDraggableHelper() {
        return mDraggableHelper;
    }

    @Override
    public BubbleLegacyView getBubbleLegacyView() {
        return this;
    }

    @Override
    public View getDraggableView() {
        return this;
    }

    public int getXPos() {
        return mDraggableHelper.getXPos();
    }

    public int getYPos() {
        return mDraggableHelper.getYPos();
    }

    public void setExactPos(int x, int y) {
        mDraggableHelper.setExactPos(x, y);
    }

    public void setTargetPos(int x, int y, float t, boolean overshoot) {
        mDraggableHelper.setTargetPos(x, y, t, overshoot);
    }

    public void clearTargetPos() {
        mDraggableHelper.clearTargetPos();

        if (mContentView != null) {
            mContentView.setMarkerX((int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount()));
        }
    }

    public boolean isSnapping() {
        return mDraggableHelper.isSnapping();
    }

    @Override
    public void update(float dt, boolean contentView) {
        if (mDraggableHelper.update(dt, contentView)) {
            if (contentView) {
                mContentView.setMarkerX(mDraggableHelper.getXPos());
            }
        }
    }

    public void setBubbleIndex(int i) {
        mBubbleIndex = i;
        mContentView.setMarkerX((int) Config.getContentViewX(i, MainController.get().getBubbleCount()));
    }

    public int getBubbleIndex() {
        return mBubbleIndex;
    }
}
