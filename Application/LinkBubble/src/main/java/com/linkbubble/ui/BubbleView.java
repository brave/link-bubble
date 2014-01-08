package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.Draggable;
import com.linkbubble.physics.Circle;
import com.squareup.picasso.Transformation;
import org.mozilla.gecko.favicons.Favicons;
import org.mozilla.gecko.favicons.LoadFaviconTask;
import org.mozilla.gecko.favicons.OnFaviconLoadedListener;

import java.net.MalformedURLException;
import java.net.URL;

public class BubbleView extends FrameLayout {

    private Draggable mDraggable;
    private BadgeView mBadgeView;
    private ImageView mFavicon;
    private ImageView mAdditionalFaviconView;
    protected WindowManager mWindowManager;
    private EventHandler mEventHandler;
    private ProgressIndicator mProgressIndicator;

    private URL mUrl;
    private ContentView mContentView;
    private boolean mRecordHistory;
    private int mFaviconLoadId;
    private int mBubbleIndex;


    public interface EventHandler {
        public void onMotionEvent_Touch(BubbleView sender, Draggable.TouchEvent event);
        public void onMotionEvent_Move(BubbleView sender, Draggable.MoveEvent event);
        public void onMotionEvent_Release(BubbleView sender, Draggable.ReleaseEvent event);
        public void onDestroyBubble(BubbleView sender);
        public void onMinimizeBubbles();
    }

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void attachBadge(BadgeView badgeView) {
        if (mBadgeView == null) {
            mBadgeView = badgeView;

            int badgeMargin = getResources().getDimensionPixelSize(R.dimen.badge_margin);
            int badgeSize = getResources().getDimensionPixelSize(R.dimen.badge_size);
            FrameLayout.LayoutParams lp = new LayoutParams(badgeSize, badgeSize);
            lp.gravity = Gravity.TOP|Gravity.RIGHT;
            lp.leftMargin = badgeMargin;
            lp.rightMargin = badgeMargin;
            lp.topMargin = badgeMargin;
            addView(mBadgeView, lp);
        }
    }

    public void detachBadge() {
        if (mBadgeView != null) {
            removeView(mBadgeView);
            mBadgeView = null;
        }
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

    public void setBubbleIndex(int i) {
        mBubbleIndex = i;
        mContentView.setMarkerX((int) Config.getContentViewX(i, MainController.get().getBubbleCount()));
    }

    public int getBubbleIndex() {
        return mBubbleIndex;
    }

    public int getXPos() {
        return mDraggable.getXPos();
    }

    public int getYPos() {
        return mDraggable.getYPos();
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public void OnOrientationChanged(boolean contentViewMode) {
        clearTargetPos();

        int xPos, yPos;

        if (contentViewMode) {
            xPos = (int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount());
            yPos = Config.mContentViewBubbleY;
        } else {
            WindowManager.LayoutParams windowManagerParms = mDraggable.getWindowManagerParams();
            if (windowManagerParms.x < Config.mScreenHeight * 0.5f)
                xPos = Config.mBubbleSnapLeftX;
            else
                xPos = Config.mBubbleSnapRightX;
            float yf = (float)windowManagerParms.y / (float)Config.mScreenWidth;
            yPos = (int) (yf * Config.mScreenHeight);
        }

        setExactPos(xPos, yPos);
    }

    public void clearTargetPos() {
        mDraggable.clearTargetPos();

        if (mContentView != null) {
            mContentView.setMarkerX((int) Config.getContentViewX(mBubbleIndex, MainController.get().getBubbleCount()));
        }
    }

    public void setExactPos(int x, int y) {
        mDraggable.setExactPos(x, y);
    }

    public void setTargetPos(int x, int y, float t, boolean overshoot) {
        mDraggable.setTargetPos(x, y, t, overshoot);
    }

    public CanvasView.TargetInfo getTargetInfo(CanvasView canvasView, int x, int y) {
        Circle bubbleCircle = new Circle(x + Config.mBubbleWidth * 0.5f,
                y + Config.mBubbleHeight * 0.5f,
                Config.mBubbleWidth * 0.5f);
        CanvasView.TargetInfo targetInfo = canvasView.getBubbleAction(bubbleCircle);
        return targetInfo;
    }

    public Config.BubbleAction doSnap(CanvasView canvasView, int targetX, int targetY) {
        CanvasView.TargetInfo targetInfo = getTargetInfo(canvasView, targetX, targetY);

        if (targetInfo.mAction != Config.BubbleAction.None) {
            setTargetPos((int) (targetInfo.mTargetX - Config.mBubbleWidth * 0.5f),
                         (int) (targetInfo.mTargetY - Config.mBubbleHeight * 0.5f),
                         0.3f, true);
        } else {
            setTargetPos(targetX, targetY, 0.02f, false);
        }

        return targetInfo.mAction;
    }

    public boolean isSnapping() {
        return mDraggable.isSnapping();
    }

    public void update(float dt, boolean contentView) {
        if (mDraggable.update(dt, contentView)) {
            if (contentView) {
                mContentView.setMarkerX(mDraggable.getXPos());
            }
        }
    }

    public void destroy() {
        setOnTouchListener(null);
        // Will be null 
        if (mContentView != null) {
            mContentView.destroy();
        }
        mDraggable.destroy();
    }

    public URL getUrl() {
        return mUrl;
    }

    public Drawable getFavicon() {
        return mFavicon.getDrawable();
    }

    public void setAdditionalFaviconView(ImageView imageView) {
        mAdditionalFaviconView = imageView;
    }

    public void readd() {
        boolean showing = mProgressIndicator.isIndicatorShowing();
        int progress = mProgressIndicator.getProgress();
        URL url = mProgressIndicator.getUrl();

        mWindowManager.removeView(this);
        mWindowManager.addView(this, mDraggable.getWindowManagerParams());
        if (url != null) {
            mProgressIndicator.setProgress(showing, progress, url);
        }
    }

    public void setFaviconLoadId(int faviconLoadId) {
        mFaviconLoadId = faviconLoadId;
    }

    public int getFaviconLoadId() {
        return mFaviconLoadId;
    }

    private String getDefaultFaviconUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
    }

    private void loadFavicon() {
        maybeCancelFaviconLoad();

        final int tabFaviconSize = getResources().getDimensionPixelSize(R.dimen.browser_toolbar_favicon_size);

        //int flags = (tab.isPrivate() || tab.getErrorType() != Tab.ErrorType.NONE) ? 0 : LoadFaviconTask.FLAG_PERSIST;
        int flags = Settings.get().isIncognitoMode() ? 0 : LoadFaviconTask.FLAG_PERSIST;
        String faviconUrl = getDefaultFaviconUrl(mUrl);
        int faviconLoadIdBefore = mFaviconLoadId;
        int id = Favicons.getFaviconForSize(mUrl.toString(), faviconUrl, Integer.MAX_VALUE, flags, mOnFaviconLoadedListener);

        // If the favicon is cached, mOnFaviconLoadedListener.onFaviconLoaded() will be called before this check is reached,
        // and this call will have already set mFaviconLoadId. Thus only act on the id return value if the value was not already changed
        if (faviconLoadIdBefore == mFaviconLoadId) {
            setFaviconLoadId(id);
            if (id != Favicons.LOADED) {
                mFavicon.setImageResource(R.drawable.fallback_favicon);
            }
        }
    }

    OnFaviconLoadedListener mOnFaviconLoadedListener = new OnFaviconLoadedListener() {
        @Override
        public void onFaviconLoaded(String url, String faviconURL, Bitmap favicon) {
            if (favicon != null) {
                // Note: don't upsize favicon because Favicons.getFaviconForSize() already does this
                mFavicon.setImageBitmap(favicon);
                setFaviconLoadId(Favicons.LOADED);
            }
        }
    };

    private void maybeCancelFaviconLoad() {
        int faviconLoadId = getFaviconLoadId();

        if (Favicons.NOT_LOADING == faviconLoadId) {
            return;
        }

        // Cancel load task and reset favicon load state if it wasn't already
        // in NOT_LOADING state.
        Favicons.cancelFaviconLoad(faviconLoadId);
        setFaviconLoadId(Favicons.NOT_LOADING);
    }

    public void configure(String url, int x0, int y0, int targetX, int targetY, float targetTime, long startTime,
                  EventHandler eh) throws MalformedURLException {
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
        windowManagerParams.setTitle("LinkBubble: Bubble");

        mDraggable = new Draggable(this, mWindowManager, windowManagerParams, new Draggable.OnTouchActionEventListener() {

            @Override
            public void onActionDown(Draggable.TouchEvent event) {
                mEventHandler.onMotionEvent_Touch(BubbleView.this, event);
            }

            @Override
            public void onActionMove(Draggable.MoveEvent event) {
                mEventHandler.onMotionEvent_Move(BubbleView.this, event);
            }

            @Override
            public void onActionUp(Draggable.ReleaseEvent event) {
                mEventHandler.onMotionEvent_Release(BubbleView.this, event);
            }
        });

        mEventHandler = eh;
        mUrl = new URL(url);
        mRecordHistory = Settings.get().isIncognitoMode() ? false : true;

        mFavicon = (ImageView) findViewById(R.id.favicon);
        mProgressIndicator = (ProgressIndicator) findViewById(R.id.progressIndicator);
        showProgressBar(true, 0);

        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(BubbleView.this, mUrl.toString(), startTime, new ContentView.EventHandler() {

            @Override
            public void onDestroyBubble() {
                mEventHandler.onDestroyBubble(BubbleView.this);
            }

            @Override
            public void onMinimizeBubbles() {
                mEventHandler.onMinimizeBubbles();
            }

            @Override
            public void onPageLoading(String url) {
                showProgressBar(true, 0);

                boolean setDefaultFavicon = true;

                try {
                    // TODO: remove this allocation
                    URL previousUrl = mUrl;
                    mUrl = new URL(url);

                    if (previousUrl != null && previousUrl.getHost().equals(mUrl.getHost()) && mFaviconLoadId == Favicons.LOADED) {
                        setDefaultFavicon = false;
                    } else {
                        loadFavicon();
                        if (mFaviconLoadId == Favicons.LOADED || mFaviconLoadId == Favicons.NOT_LOADING) {
                            setDefaultFavicon = false;
                        }
                        /*
                        String faviconUrl = "http://" + mUrl.getHost() + "/favicon.ico";
                        //String faviconUrl = "http://1.gravatar.com/blavatar/f8748081423ce49bd3ecb267cd4effc7?s=16";
                        Picasso.with(getContext()).cancelRequest(mFavicon);
                        Picasso.with(getContext())
                                .load(faviconUrl)
                                .transform(mFaviconTransformation)
                                .placeholder(R.drawable.fallback_favicon)
                                .into(mFavicon, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        if (mAdditionalFaviconView != null) {
                                            mAdditionalFaviconView.setImageDrawable(mFavicon.getDrawable());
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        onReceivedIcon(null);
                                    }
                                });
                        */
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                if (setDefaultFavicon) {
                    onReceivedIcon(null);
                }
            }

            @Override
            public void onProgressChanged(int progress) {
                showProgressBar(true, progress);
            }

            @Override
            public void onPageLoaded(ContentView.PageLoadInfo info) {
                showProgressBar(false, 0);

                if (info != null && info.bmp != null) {
                    onReceivedIcon(info.bmp);
                }

                if (mRecordHistory && info != null && info.url != null) {
                    MainApplication.saveUrlInHistory(getContext(), null, info.url, info.mHost, info.title);
                }

                MainController.get().onPageLoaded(BubbleView.this);
            }

            @Override
            public void onReceivedIcon(Bitmap favicon) {
                if (favicon == null) {
                    mFaviconLoadId = Favicons.NOT_LOADING;
                    mFavicon.setImageResource(R.drawable.fallback_favicon);
                } else {
                    MainApplication mainApplication = (MainApplication) getContext().getApplicationContext();
                    String faviconUrl = getDefaultFaviconUrl(mUrl);
                    if(mainApplication.mDatabaseHelper.faviconExists(faviconUrl) == false) {
                        mainApplication.mDatabaseHelper.addFaviconForUrl(faviconUrl, favicon, mUrl.toString());
                    }

                    favicon = mFaviconTransformation.transform(favicon);

                    mFaviconLoadId = Favicons.LOADED;
                    mFavicon.setImageBitmap(favicon);
                    if (mAdditionalFaviconView != null) {
                        mAdditionalFaviconView.setImageBitmap(favicon);
                    }
                }

                mFavicon.setVisibility(VISIBLE);
                showProgressBar(false, 0);
            }
        });

        setVisibility(GONE);


        if (mDraggable.isAlive()) {
            mWindowManager.addView(this, windowManagerParams);

            setExactPos(x0, y0);
            if (targetX != x0 || targetY != y0) {
                setTargetPos(targetX, targetY, targetTime, true);
            }
        }
    }

    private static class FaviconTransformation implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int w = source.getWidth();
            int h = source.getHeight();

            int reqW = Math.min((int) (Config.mBubbleWidth * 0.5f), w*2);
            int reqH = Math.min((int) (Config.mBubbleHeight * 0.5f), h*2);

            if (w != reqW || h != reqH) {
                w = reqW;
                h = reqH;

                Bitmap result = Bitmap.createScaledBitmap(source, w, h, true);
                return result;
            }

            return source;
        }

        @Override
        public String key() { return "faviconTransformation()"; }
    }

    private FaviconTransformation mFaviconTransformation = new FaviconTransformation();

    /*
    Handler mHandler = new Handler();
    float mTempProgress = 0.f;
    Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressIndicator.setProgress((int) mTempProgress);
            mTempProgress += .3f;
            if (mTempProgress >= 100) {
                mTempProgress -= 100;
            }
            mHandler.postDelayed(mProgressRunnable, 33);
        }
    };*/

    void showProgressBar(boolean show, int progress) {
        mProgressIndicator.setProgress(show, progress, mUrl);
    }
}