package com.linkbubble.articlerender;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import com.linkbubble.util.Util;


public class ArticleRenderer {

    private WebView mWebView;

    public ArticleRenderer(Context context, ArticleContent articleContent, View articleRendererPlaceholder) {
        mWebView = new WebView(context);
        mWebView.setLayoutParams(articleRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(articleRendererPlaceholder, mWebView);

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

}
