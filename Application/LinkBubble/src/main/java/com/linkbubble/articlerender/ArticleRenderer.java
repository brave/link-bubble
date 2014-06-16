package com.linkbubble.articlerender;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import com.linkbubble.util.Util;


public class ArticleRenderer {

    private WebView mWebView;

    public ArticleRenderer(Context context, ArticleContent articleContent, View articleRendererPlaceholder) {
        if (mWebView == null) {
            mWebView = new WebView(context);
            mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
            Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);
        }

        display(articleContent);
    }

    public void display(ArticleContent articleContent) {
        mWebView.stopLoading();
        String urlAsString = articleContent.mUrl.toString();
        mWebView.loadDataWithBaseURL(urlAsString, articleContent.mPageHtml, "text/html", "utf-8", urlAsString);
    }

    public void destroy() {
        if (mWebView != null) {
            mWebView.destroy();
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

}
