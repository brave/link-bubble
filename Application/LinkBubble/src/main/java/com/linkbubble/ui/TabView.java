package com.linkbubble.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.ScaleUpAnimHelper;
import com.linkbubble.util.Util;

import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;

public class TabView extends BubbleView {

    private ContentView mContentView;
    private ImageView mBackIndicatorView;
    private ScaleUpAnimHelper mBackIndicatorAnimHelper;

    public boolean mWasRestored;

    public TabView(Context context) {
        this(context, null);
    }

    public TabView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void configure(String url, long urlLoadStartTime, boolean hasShownAppPicker) throws MalformedURLException {
        super.configure(url);

        mBackIndicatorView = (ImageView) findViewById(R.id.back_indicator);
        if (Settings.get().getDarkThemeEnabled()) {
            mBackIndicatorView.setBackgroundResource(R.drawable.badge_plate_dark);
            mBackIndicatorView.setImageResource(R.drawable.ic_action_arrow_left_white);
        } else {
            mBackIndicatorView.setBackgroundResource(R.drawable.badge_plate);
            mBackIndicatorView.setImageResource(R.drawable.ic_action_arrow_left);
        }
        mBackIndicatorAnimHelper = new ScaleUpAnimHelper(mBackIndicatorView, 1.0f);
        mBackIndicatorAnimHelper.hide();

        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(mUrl.toString(), this, urlLoadStartTime, hasShownAppPicker, new ContentView.EventHandler() {

            @Override
            public void onPageLoading(URL url) {
                boolean setDefaultFavicon = true;

                URL previousUrl = mUrl;
                mUrl = url;

                showProgressBar(0);

                if (previousUrl != null && previousUrl.getHost().equals(mUrl.getHost()) && mFaviconLoadId == Favicons.LOADED) {
                    setDefaultFavicon = false;
                } else {
                    loadFavicon();
                    if (mFaviconLoadId == Favicons.LOADED || mFaviconLoadId == Favicons.NOT_LOADING) {
                        setDefaultFavicon = false;
                    }
                }

                if (setDefaultFavicon) {
                    setDefaultFavicon();
                }
            }

            @Override
            public void onProgressChanged(int progress) {
                showProgressBar(progress);
            }

            @Override
            public void onPageLoaded(boolean withError) {
                TabView.this.onPageLoaded(withError);
            }

            @Override
            public boolean onReceivedIcon(Bitmap favicon) {
                return TabView.this.onReceivedIcon(favicon, false);
            }

            @Override
            public void setDefaultFavicon() {
                TabView.this.onReceivedIcon(null, true);
            }

            @Override
            public void onCanGoBackChanged(boolean canGoBack) {
                if (canGoBack) {
                    mBackIndicatorAnimHelper.show();
                } else {
                    mBackIndicatorAnimHelper.hide();
                }
            }

            @Override
            public boolean hasHighQualityFavicon() {
                String tag = (String)mFavicon.getTag();
                Drawable drawable = mFavicon.getDrawable();
                if (tag != null && drawable != null && drawable instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
                    if (bitmap != null && bitmap.getWidth() >= Constant.DESIRED_FAVICON_SIZE) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onThemeColor(Integer color) {
                TabView.this.onThemeColor(color);
            }
        });

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // TODO: How does this code path actually get hit?
                // GW: Let me know if you hit this code path.
                //Util.Assert(false);
                //MainController mainController = MainController.get();
                //mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }
        });

        setOnApplyFaviconListener(new OnApplyFaviconListener() {
            @Override
            public boolean applyFavicon(String faviconURL) {
                URL currentUrl = mContentView.getUrl();
                if (currentUrl != null) {
                    String currentFaviconUrl = Util.getDefaultFaviconUrl(currentUrl);
                    if (faviconURL != null && faviconURL.equals(currentFaviconUrl)) {
                        return true;
                    }
                    //Log.d("blerg", "Ignoring favicon " + faviconURL + " in favor of " + currentFaviconUrl);
                }

                return false;
            }
        });
    }

    public void destroy() {
        // Will be null
        if (mContentView != null) {
            mContentView.destroy();
        }
    }

    @Override
    protected void onPageLoaded(boolean withError) {
        super.onPageLoaded(withError);
        if (MainController.get() != null) {
            MainController.get().onPageLoaded(this, withError);
        }
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public long getTotalTrackedLoadTime() {
        return mContentView.getTotalTrackedLoadTime();
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

    @Override
    void setProgressColor(int color) {
        super.setProgressColor(color);
        mContentView.setFaviconColor(color);
    }

}
