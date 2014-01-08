package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.physics.DraggableHelper;
import com.linkbubble.physics.Draggable;
import com.squareup.picasso.Transformation;
import org.mozilla.gecko.favicons.Favicons;
import org.mozilla.gecko.favicons.LoadFaviconTask;
import org.mozilla.gecko.favicons.OnFaviconLoadedListener;

import java.net.MalformedURLException;
import java.net.URL;

public class BubbleView extends FrameLayout  {

    private BadgeView mBadgeView;
    protected ImageView mFavicon;
    protected ImageView mAdditionalFaviconView;
    protected WindowManager mWindowManager;

    protected ProgressIndicator mProgressIndicator;

    protected URL mUrl;

    protected int mFaviconLoadId;


    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void configure(String url) throws MalformedURLException {
        mUrl = new URL(url);
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

    public URL getUrl() {
        return mUrl;
    }

    public Drawable getFavicon() {
        return mFavicon.getDrawable();
    }

    public void setAdditionalFaviconView(ImageView imageView) {
        mAdditionalFaviconView = imageView;
    }


    public void setFaviconLoadId(int faviconLoadId) {
        mFaviconLoadId = faviconLoadId;
    }

    public int getFaviconLoadId() {
        return mFaviconLoadId;
    }

    protected String getDefaultFaviconUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
    }

    protected void loadFavicon() {
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

    protected FaviconTransformation mFaviconTransformation = new FaviconTransformation();

    protected void onPageLoaded(ContentView.PageLoadInfo info) {
        showProgressBar(false, 0);

        if (info != null && info.bmp != null) {
            onReceivedIcon(info.bmp);
        }
    }

    protected void onReceivedIcon(Bitmap favicon) {
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