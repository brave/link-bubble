package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.annotations.SerializedName;
import com.linkbubble.Constant;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.util.SafeUrlSpan;
import com.linkbubble.util.Util;
import com.linkbubble.util.YouTubeEmbedHelper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.ImageResult;
import de.jetwick.snacktory.JResult;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SnacktoryWebViewRenderer extends WebViewRenderer {

    private static final String TAG = "SnacktoryRenderer";

    private GetPageAsTextTask mGetPageAsTextTask;
    private TouchIconTransformation mTouchIconTransformation;

    public SnacktoryWebViewRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder, tag);
    }

    @Override
    protected WebView newWebView(Context context) {
        return new WebView(context) {

            public String getUrl() {
                return SnacktoryWebViewRenderer.this.getUrl().toString();
            }

        };
    }

    @Override
    protected void webChromeClientOnProgressChanged(WebView webView, int progress) {
        super.webChromeClientOnProgressChanged(webView, progress);
        if (progress == 100) {
            mController.onPageFinished(getUrl().toString());
        }
    }

    @Override
    public void loadUrl(String urlAsString) {

        Log.d(TAG, "loadUrl() - " + urlAsString);

        if (mGetPageAsTextTask != null) {
            mGetPageAsTextTask.cancel(true);
        }

        mGetPageAsTextTask = new GetPageAsTextTask();
        mGetPageAsTextTask.execute(getUrl().toString());
    }

    @Override
    public void reload() {
        if (mGetPageAsTextTask != null) {
            mGetPageAsTextTask.cancel(true);
        }

        mGetPageAsTextTask = new GetPageAsTextTask();
        mGetPageAsTextTask.execute(getUrl().toString());
    }

    @Override
    public void stopLoading() {
        if (mGetPageAsTextTask != null) {
            mGetPageAsTextTask.cancel(true);
        }

        super.stopLoading();
    }

    private class GetPageAsTextTask extends AsyncTask<String, JResult, JResult> {
        protected JResult doInBackground(String... urls) {

            JResult result = null;
            String url = urls[0];
            try {
                HtmlFetcher fetcher = new HtmlFetcher();
                result = fetcher.fetchAndExtract(url, 30 * 1000, true);

                String text = result.getText();
                String title = result.getTitle();
                String imageUrl = result.getImageUrl();
                Log.d(TAG, "title: " + title + ", text: " + text + ", imageUrl:" + imageUrl);
            } catch (Exception ex) {
                Log.d(TAG, ex.getLocalizedMessage(), ex);
            }
            return result;
        }

        protected void onPostExecute(JResult result) {
            String urlAsString = result.getCanonicalUrl();
            if (urlAsString == null) {
                urlAsString = result.getUrl();
            }
            URL url = null;
            try {
                url = new URL(urlAsString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String headHtml =
                    "  <head>\n" +
                    "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0, height=device-height\"/>\n" +
                    "    <link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>\n" +
                    "    <style type=\"text/css\">\n" +
                    "      p, div { font-family: 'Roboto', sans-serif; font-size: 16px; color:#333; line-height: 160%; }\n" +
                    "      a { text-decoration: none; }\n" +
                    "      #lbInfo { width:100%; min-height:28px; margin:0 auto; padding-bottom: 20px;}\n" +
                    "      #lbInfoL { float:left; width:70%; }\n" +
                    "      #lbInfoR { float:right; width:30%; }\n" +
                    "    </style>" +
                    "    </style>";

            String bodyHtml = "<body >\n" +
                    "    <div style=\"margin:0px 24px 0px 24px\">\n";

            String title = result.getTitle();
            if (title != null) {
                headHtml += "<title>" + title + "</title>";
                bodyHtml += "<p style=\"font-size:150%;line-height:120%;font-weight:bold;margin:32px 0px 12px 0px\">" + title + "</p>";
            }

            String authorName = result.getAuthorName();
            Date publishedDate = result.getDate();

            String leftString = "";
            String rightString = "";

            if (authorName != null) {
                leftString = "<span class=\"nowrap\">by <b>" + authorName + "</b>,</span> ";
            }
            if (url != null) {
                leftString += "<span class=\"nowrap\"><a href=\"" + url.getProtocol() + "://" + url.getHost() + "\">" + (url.getHost().replace("www.", "")) + "</a></span>";
            }

            Log.d("info", "urlHost:" + url.getHost() + ", authorName: " + authorName);

            if (publishedDate != null) {
                Format formatter = new SimpleDateFormat("MMMM dd, yyyy");
                rightString = "<span style=\"float:right\">" + formatter.format(publishedDate) + "</span>";
            }

            bodyHtml += "<hr style=\"border: 0;height: 0; border-top: 1px solid rgba(0, 0, 0, 0.1); border-bottom: 1px solid rgba(255, 255, 255, 0.3);\">"
                    + "<div id=\"lbInfo\"><div id=\"lbInfoL\">" + leftString + "</div><div id=\"lbInfoR\">" + rightString + "</div></div>";

            String html = result.getHtml();
            if (html != null) {
                bodyHtml += html;
            }

            try {
                setUrl(urlAsString);

                headHtml += "</head>";
                bodyHtml += " </div>\n" +
                        "    </div>\n" +
                        "    <br><br><br>" +
                        "  </body>\n";

                //mWebView.loadUrl(urlAsString);
                //mWebView.stopLoading();

                String pageHtml = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + headHtml + bodyHtml + "</html>";

                //Log.d(TAG, "pageHtml:" + pageHtml);
                mWebView.loadDataWithBaseURL(urlAsString, pageHtml, "text/html", "utf-8", urlAsString);
                //mWebView.loadData(pageHtml, "text/html", "utf-8");

                if (title != null) {
                    mController.onReceivedTitle(urlAsString, title);
                }
                mController.onProgressChanged(100, urlAsString);
                //mController.onPageFinished(urlAsString);

                String faviconUrl = result.getFaviconUrl();
                //Log.d(TAG, "faviconUrl:" + faviconUrl);
                if (faviconUrl != null && faviconUrl.isEmpty() == false) {
                    if (mTouchIconTransformation == null) {
                        mTouchIconTransformation = new TouchIconTransformation(SnacktoryWebViewRenderer.this);
                    }
                    mTouchIconTransformation.setPageUrl(urlAsString);
                    Picasso.with(mContext).load(faviconUrl).transform(mTouchIconTransformation).fetch();
                }
            } catch (MalformedURLException ex) {

            }
        }
    }

    private static class TouchIconTransformation implements Transformation {

        private WeakReference<SnacktoryWebViewRenderer> mRenderer;
        String mPageUrl = null;

        TouchIconTransformation(SnacktoryWebViewRenderer renderer) {
            mRenderer = new WeakReference<SnacktoryWebViewRenderer>(renderer);
        }

        void setPageUrl(String pageUrl) {
            mPageUrl = pageUrl;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            int w = source.getWidth();

            Bitmap result = source;
            if (w > Constant.TOUCH_ICON_MAX_SIZE) {
                try {
                    result = Bitmap.createScaledBitmap(source, Constant.TOUCH_ICON_MAX_SIZE, Constant.TOUCH_ICON_MAX_SIZE, true);
                } catch (OutOfMemoryError e) {

                }
            }

            if (result != null && mRenderer != null) {
                SnacktoryWebViewRenderer renderer = mRenderer.get();
                if (renderer != null && renderer.mController != null) {
                    renderer.mController.onPageInspectorTouchIconLoaded(result, mPageUrl);
                }
            }

            // return null. No need for Picasso to cache this, as we're already doing so elsewhere
            return null;
        }

        @Override
        public String key() { return "faviconTransformation()"; }
    }
}
