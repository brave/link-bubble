package com.linkbubble.articlerender;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.widget.ImageView;
import com.linkbubble.Constant;
import com.linkbubble.util.Util;
import com.linkbubble.webrender.WebRenderer;


public class ArticleRenderer {

    public interface Controller {
        public void onUrlLongClick(String url);
        public void onDownloadStart(String urlAsString);
        public boolean onBackPressed();
        public void onShowBrowserPrompt();
    }
    
    private WebView mWebView;
    private boolean mIsDestroyed = false;
    private Controller mController;

    public ArticleRenderer(Context context, Controller controller, ArticleContent articleContent, View articleRendererPlaceholder) {
        mController = controller;

        mWebView = new WebView(context);
        mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);

        mWebView.setDownloadListener(mDownloadListener);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);

        display(articleContent, false);

        Log.d("Article", "ArticleRenderer()");
    }

    public void display(ArticleContent articleContent) {
        display(articleContent, true);
    }

    private void display(ArticleContent articleContent, boolean reuse) {
        mWebView.stopLoading();
        String urlAsString = articleContent.mUrl.toString();
        mWebView.loadDataWithBaseURL(urlAsString, articleContent.mPageHtml, "text/html", "utf-8", urlAsString);
        Log.d("Article", ".display() - " + (reuse ? "REUSE" : "NEW") + ", url:" + articleContent.mUrl.toString());
    }

    public void destroy() {
        mIsDestroyed = true;
        if (mWebView != null) {
            mWebView.destroy();
            Log.d("Article", "ArticleRenderer.destroy()");
        }
    }

    public View getView() {
        return mWebView;
    }

    public void stopLoading() {
        if (mWebView != null) {
            mWebView.stopLoading();
        }
    }

    DownloadListener mDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String urlAsString, String userAgent,
                                    String contentDisposition, String mimetype,
                                    long contentLength) {
            mController.onDownloadStart(urlAsString);
        }
    };

    View.OnLongClickListener mOnWebViewLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
            //Log.d(TAG, "onLongClick type: " + hitTestResult.getType());
            switch (hitTestResult.getType()) {
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                    final String url = hitTestResult.getExtra();
                    if (url == null) {
                        return false;
                    }

                    mController.onUrlLongClick(url);
                    return true;
                }

                case WebView.HitTestResult.UNKNOWN_TYPE:
                default:
                    if (Constant.SELECT_TEXT_VIA_ACTIVITY == false) {
                        mController.onShowBrowserPrompt();
                    }
                    return false;
            }
        }
    };

    View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && mIsDestroyed == false) {
                WebView webView = (WebView) v;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK: {
                        return mController.onBackPressed();
                    }
                }
            }

            return false;
        }
    };

}
