package com.linkbubble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;
import com.squareup.picasso.Transformation;

import org.mozilla.gecko.favicons.Favicons;
import org.mozilla.gecko.favicons.LoadFaviconTask;
import org.mozilla.gecko.favicons.OnFaviconLoadedListener;
import org.mozilla.gecko.widget.FaviconView;

import java.net.MalformedURLException;
import java.net.URL;

public class BubbleView extends FrameLayout  {

    private static final String TAG = "BubbleView";
    private static final boolean DEBUG = false;

    public interface OnApplyFaviconListener {
        boolean applyFavicon(String faviconURL);
    }

    protected FaviconView mFavicon;
    protected int mFaviconLoadId;
    private ProgressIndicatorView mProgressIndicator;
    protected URL mUrl;
    private BubbleView mImitator;       //
    private OnApplyFaviconListener mOnApplyFaviconListener;

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void configure() {
        mFavicon = (FaviconView) findViewById(R.id.favicon);
        mFavicon.setOnPaletteChangeListener(mOnPaletteChangeListener);
        mFavicon.mFavicons = MainApplication.sFavicons;
        mProgressIndicator = (ProgressIndicatorView) findViewById(R.id.progressIndicator);
        showProgressBar(0);
    }

    void configure(String url) throws MalformedURLException {
        mUrl = new URL(url);

        configure();
    }

    void setOnApplyFaviconListener(OnApplyFaviconListener onApplyFaviconListener) {
        mOnApplyFaviconListener = onApplyFaviconListener;
    }

    public URL getUrl() {
        return mUrl;
    }

    public Drawable getFavicon() {
        return mFavicon.getDrawable();
    }

    private void setFavicon(Bitmap bitmap, String faviconUrl) {
        mFavicon.updateImage(bitmap, faviconUrl, mThemeColor == null);
        mFavicon.setTag(faviconUrl);
    }

    private void setFallbackFavicon() {
        mFavicon.showDefaultFavicon();
        mFavicon.setTag(null);
    }

    public void setImitator(BubbleView bubbleView) {
        mImitator = bubbleView;
        if (mImitator != null) {
            String tag = (String)mFavicon.getTag();
            Drawable drawable = mFavicon.getDrawable();
            if (tag != null && drawable != null) {
                mImitator.setFavicon(((BitmapDrawable)drawable).getBitmap(), tag);
            } else {
                mImitator.setFallbackFavicon();
            }
            mImitator.mProgressIndicator.setProgress(mProgressIndicator.getProgress(), mUrl);
        }
    }

    public void setFaviconLoadId(int faviconLoadId) {
        mFaviconLoadId = faviconLoadId;
    }

    public int getFaviconLoadId() {
        return mFaviconLoadId;
    }

    protected void loadFavicon() {
        maybeCancelFaviconLoad();

        final int tabFaviconSize = getResources().getDimensionPixelSize(R.dimen.browser_toolbar_favicon_size);

        //int flags = (tab.isPrivate() || tab.getErrorType() != Tab.ErrorType.NONE) ? 0 : LoadFaviconTask.FLAG_PERSIST;
        int flags = Settings.get().isIncognitoMode() ? 0 : LoadFaviconTask.FLAG_PERSIST;
        flags |= Favicons.FLAG_OFFLINE_NO_CACHE;
        String faviconUrl = Util.getDefaultFaviconUrl(mUrl);
        int faviconLoadIdBefore = mFaviconLoadId;
        int id = MainApplication.sFavicons.getFaviconForSize(mUrl.toString(), faviconUrl, Integer.MAX_VALUE, flags, mOnFaviconLoadedListener);

        // If the favicon is cached, mOnFaviconLoadedListener.onFaviconLoaded() will be called before this check is reached,
        // and this call will have already set mFaviconLoadId. Thus only act on the id return value if the value was not already changed
        if (faviconLoadIdBefore == mFaviconLoadId) {
            setFaviconLoadId(id);
            if (id != Favicons.LOADED) {
                setFallbackFavicon();
                if (mImitator != null) {
                    mImitator.setFallbackFavicon();
                }
                if (DEBUG) {
                    Log.d(TAG, "[favicon] loadFavicon: setImageBitmap() FALLBACK for " + faviconUrl);
                }
            }
        }
    }

    OnFaviconLoadedListener mOnFaviconLoadedListener = new OnFaviconLoadedListener() {
        @Override
        public void onFaviconLoaded(String url, String faviconURL, Bitmap favicon) {
            if (favicon != null) {
                if (mOnApplyFaviconListener != null && mOnApplyFaviconListener.applyFavicon(faviconURL) == false) {
                    return;
                }
                // Note: don't upsize favicon because Favicons.getFaviconForSize() already does this
                setFavicon(favicon, faviconURL);
                setFaviconLoadId(Favicons.LOADED);
                if (mImitator != null) {
                    mImitator.setFavicon(favicon, faviconURL);
                    mImitator.setFaviconLoadId(Favicons.LOADED);
                }
                if (DEBUG) {
                    Log.d(TAG, "[favicon] mOnFaviconLoadedListener: setImageBitmap() size:" + favicon.getWidth() + " for " + faviconURL);
                }
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
        MainApplication.sFavicons.cancelFaviconLoad(faviconLoadId);
        setFaviconLoadId(Favicons.NOT_LOADING);
    }


    private static class FaviconTransformation implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int w = source.getWidth();
            int h = source.getHeight();

            int reqW = Math.min(Constant.DESIRED_FAVICON_SIZE, w*2);
            int reqH = Math.min(Constant.DESIRED_FAVICON_SIZE, h*2);

            if (w != reqW || h != reqH) {
                w = reqW;
                h = reqH;

                Bitmap result = source;
                try {
                    result = Bitmap.createScaledBitmap(source, w, h, true);
                } catch (OutOfMemoryError e) {

                }

                return result;
            }

            return source;
        }

        @Override
        public String key() { return "faviconTransformation()"; }
    }

    protected FaviconTransformation mFaviconTransformation = new FaviconTransformation();

    protected void onPageLoaded(boolean withError) {
        showProgressBar(100);
    }

    protected boolean onReceivedIcon(Bitmap favicon, boolean forceSet) {
        boolean appliedFavicon = false;
        if (favicon == null) {
            // Don't update if an image already exists. Optimization as the fallback favicon is already set via loadFavicon()
            if (mFavicon.getDrawable() == null || forceSet) {
                mFaviconLoadId = Favicons.NOT_LOADING;
                setFallbackFavicon();
                if (mImitator != null) {
                    mImitator.mFaviconLoadId = Favicons.NOT_LOADING;
                    mImitator.setFallbackFavicon();
                }
                if (DEBUG) {
                    Log.d(TAG, "[favicon] onReceivedIcon: setImageBitmap() FALLBACK on host " + mUrl.getHost());
                }
            }
        } else {
            MainApplication mainApplication = (MainApplication) getContext().getApplicationContext();
            String faviconUrl = Util.getDefaultFaviconUrl(mUrl);
            String faviconTag = mFavicon.getTag() instanceof String ? (String)mFavicon.getTag() : null;

            // We will ignore this if we are already using a larger icon which was retrieved as a TouchIcon specified
            // in the page URL. Technically this is probably incorrect behaviour for a browser,
            // but the larger icons look better, so I'm going with it.
            boolean applyFavicon = true;
            if (faviconTag != null && faviconTag.equals(faviconUrl)) {
                Drawable currentFavicon = mFavicon.getDrawable();
                if (currentFavicon != null && currentFavicon instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable)currentFavicon;
                    if (bitmapDrawable.getBitmap().getWidth() > favicon.getWidth()) {
                        applyFavicon = false;
                    }
                }
            }

            if (applyFavicon || forceSet) {
                if(MainApplication.sDatabaseHelper.faviconExists(faviconUrl, favicon) == false) {
                    MainApplication.sDatabaseHelper.addFaviconForUrl(faviconUrl, favicon, mUrl.toString());
                }

                favicon = mFaviconTransformation.transform(favicon);

                mFaviconLoadId = Favicons.LOADED;
                setFavicon(favicon, faviconUrl);
                if (mImitator != null) {
                    mImitator.mFaviconLoadId = Favicons.LOADED;
                    mImitator.setFavicon(favicon, faviconUrl);
                }
                if (DEBUG) {
                    Log.d(TAG, "[favicon] onReceivedIcon: setImageBitmap() size:" + favicon.getWidth() + " on host " + mUrl.getHost());
                }
                appliedFavicon = true;
            }
        }

        mFavicon.setVisibility(VISIBLE);
        return appliedFavicon;
    }

    Integer mThemeColor;
    public void onThemeColor(Integer color) {
        mThemeColor = color;
        mProgressIndicator.setColor(color);

        if (mImitator != null) {
            mImitator.onThemeColor(color);
        }
    }

    public void onProgressChanged(int progress) {
        showProgressBar(progress);
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

    void showProgressBar(int progress) {
        mProgressIndicator.setProgress(progress, mUrl);
        if (mImitator != null) {
            mImitator.mProgressIndicator.setProgress(progress, mUrl);
        }
    }

    FaviconView.OnPaletteChangeListener mOnPaletteChangeListener = new FaviconView.OnPaletteChangeListener() {

        @Override
        public void onPaletteChange(Palette palette) {
            if (mThemeColor != null) {
                return;
            }
            if (palette == null) {
                setProgressColor(Settings.get().getThemedDefaultProgressColor());
                return;
            }
            if (palette.getDarkVibrantSwatch() != null) {
                setProgressColor(palette.getDarkVibrantSwatch().getRgb());
            } else {
                if (palette.getDarkMutedSwatch() != null) {
                    setProgressColor(palette.getDarkMutedSwatch().getRgb());
                } else {
                    if (palette.getMutedSwatch() != null) {
                        setProgressColor(palette.getMutedSwatch().getRgb());
                    } else {
                        if (palette.getVibrantSwatch() != null) {
                            setProgressColor(palette.getVibrantSwatch().getRgb());
                        } else {
                            setProgressColor(Settings.get().getThemedDefaultProgressColor());
                        }
                    }
                }
            }
        }
    };

    void setProgressColor(int color) {
        mProgressIndicator.setColor(color);
    }
}