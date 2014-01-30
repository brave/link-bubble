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
import com.linkbubble.util.ScaleUpAnimHelper;
import com.linkbubble.util.Util;

import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;

public class TabView extends BubbleView {

    private ContentView mContentView;
    private ImageView mBackIndicatorView;
    private ScaleUpAnimHelper mBackIndicatorAnimHelper;
    private int mLastBackStackSize = -1;

    public TabView(Context context) {
        this(context, null);
    }

    public TabView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void configure(String url, long startTime, boolean hasShownAppPicker) throws MalformedURLException {
        super.configure(url);

        mBackIndicatorView = (ImageView) findViewById(R.id.back_indicator);
        mBackIndicatorAnimHelper = new ScaleUpAnimHelper(mBackIndicatorView);
        mBackIndicatorAnimHelper.hide();

        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(mUrl.toString(), startTime, hasShownAppPicker, new ContentView.EventHandler() {

            @Override
            public void onPageLoading(URL url) {
                showProgressBar(true, 0);

                boolean setDefaultFavicon = true;

                URL previousUrl = mUrl;
                mUrl = url;

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
                showProgressBar(true, progress);
            }

            @Override
            public void onPageLoaded() {
                TabView.this.onPageLoaded();
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
            public void onBackStackSizeChanged(int size) {
                if (size == 0 && mLastBackStackSize > 0) {
                    mBackIndicatorAnimHelper.hide();
                } else if (size > 0 && mLastBackStackSize == 0) {
                    mBackIndicatorAnimHelper.show();
                }
                mLastBackStackSize = size;
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
        });

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // TODO: How does this code path actually get hit?
                // GW: Let me know if you hit this code path.
                Util.Assert(false);
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
                    if (faviconURL.equals(currentFaviconUrl)) {
                        return true;
                    }
                    //Log.d("blerg", "Ignoring favicon " + faviconURL + " in favor of " + currentFaviconUrl);
                }

                return false;
            }
        });
    }

    void destroy() {
        // Will be null
        if (mContentView != null) {
            mContentView.destroy();
        }
    }

    @Override
    protected void onPageLoaded() {
        super.onPageLoaded();
        MainController.get().onPageLoaded(this);
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

}
