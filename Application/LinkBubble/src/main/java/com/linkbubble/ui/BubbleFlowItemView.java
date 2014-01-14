package com.linkbubble.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;

public class BubbleFlowItemView extends BubbleView {

    public interface BubbleFlowItemViewListener {
        public void onDestroyBubble();
        public void onMinimizeBubbles();
        public void onPageLoaded(ContentView.PageLoadInfo info);
    }

    protected ContentView mContentView;
    private BubbleFlowItemViewListener mListener;

    public BubbleFlowItemView(Context context) {
        this(context, null);
    }

    public BubbleFlowItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleFlowItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void configure(String url, long startTime, BubbleFlowItemViewListener listener) throws MalformedURLException {
        super.configure(url);

        mListener = listener;
        mContentView = (ContentView)inflate(getContext(), R.layout.view_content, null);
        mContentView.configure(this, mUrl.toString(), startTime, new ContentView.EventHandler() {

            @Override
            public void onDestroyBubble() {
                mListener.onDestroyBubble();

            }

            @Override
            public void onMinimizeBubbles() {
                mListener.onMinimizeBubbles();

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
                BubbleFlowItemView.this.onPageLoaded(info);

                mListener.onPageLoaded(info);
            }

            @Override
            public void onReceivedIcon(Bitmap favicon) {
                BubbleFlowItemView.this.onReceivedIcon(favicon);
            }
        });
        mContentView.setMarkerX(Config.mScreenCenterX);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MainController mainController = MainController.get();
                mainController.getActiveDraggable().readd();
                mainController.switchState(mainController.STATE_AnimateToBubbleView);
            }
        });
    }

    void destroy() {
        // Will be null
        if (mContentView != null) {
            mContentView.destroy();
        }
    }

    public ContentView getContentView() {
        return mContentView;
    }

    public void updateIncognitoMode(boolean incognito) {
        mContentView.updateIncognitoMode(incognito);
    }

}
