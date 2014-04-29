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
import java.util.List;

public class SnacktoryWebViewRenderer extends WebRenderer {

    private static final String TAG = "SnacktoryRenderer";

    private GetPageAsTextTask mGetPageAsTextTask;
    private WebView mWebView;
    private View mTouchInterceptorView;
    private long mLastWebViewTouchUpTime = -1;
    private String mLastWebViewTouchDownUrl;
    private TouchIconTransformation mTouchIconTransformation;
    private Boolean mIsDestroyed = false;
    private boolean mDoDropDownCheck;

    public SnacktoryWebViewRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mWebView = new WebView(context);
        mWebView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mWebView);

        mTouchInterceptorView = new View(context);
        mTouchInterceptorView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        mTouchInterceptorView.setWillNotDraw(true);
        mTouchInterceptorView.setOnTouchListener(mWebViewOnTouchListener);

        ViewGroup parent = (ViewGroup)mWebView.getParent();
        int index = parent.indexOfChild(mWebView);
        parent.addView(mTouchInterceptorView, index+1);

        //mWebView.setWebViewClient(mWebViewClient);
        mWebView.setOnLongClickListener(mOnWebViewLongClickListener);
        mWebView.setOnKeyListener(mOnKeyListener);
    }

    @Override
    public void destroy() {
        mIsDestroyed = true;
        mWebView.destroy();
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void updateIncognitoMode(boolean incognito) {

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
    }

    @Override
    public void hidePopups() {

    }

    @Override
    public void resetPageInspector() {

    }

    @Override
    public void runPageInspector() {

    }

    @Override
    public YouTubeEmbedHelper getPageInspectorYouTubeEmbedHelper() {
        return null;
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
            String pageHtml = "<html><body>";

            String title = result.getTitle();
            if (title != null) {
                pageHtml += "<h1>" + title + "</h1>";
            }

            String html = result.getHtml();
            if (html != null) {
                pageHtml += html;
            }

            String urlAsString = result.getCanonicalUrl();
            if (urlAsString == null) {
                urlAsString = result.getUrl();
            }

            try {
                setUrl(urlAsString);

                //mWebView.loadUrl(urlAsString);
                //mWebView.stopLoading();

                mWebView.loadData(pageHtml, "text/html", "utf-8");

                if (title != null) {
                    mController.onReceivedTitle(urlAsString, title);
                }
                mController.onProgressChanged(100, urlAsString);
                mController.onPageFinished(urlAsString);

                String faviconUrl = result.getFaviconUrl();
                Log.d(TAG, "faviconUrl:" + faviconUrl);
                if (faviconUrl != null) {
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
                    mController.onShowBrowserPrompt();
                    return false;
            }
        }
    };

    private View.OnTouchListener mWebViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastWebViewTouchDownUrl = mUrl.toString();
                    //Log.d(TAG, "[urlstack] WebView - MotionEvent.ACTION_DOWN");
                    break;

                case MotionEvent.ACTION_UP:
                    mLastWebViewTouchUpTime = System.currentTimeMillis();
                    //Log.d(TAG, "[urlstack] WebView - MotionEvent.ACTION_UP");
                    break;
            }
            // Forcibly pass along to the WebView. This ensures we receive the ACTION_UP event above.
            mWebView.onTouchEvent(event);
            return true;
        }
    };

    WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView wView, final String urlAsString) {
            boolean viaInput = false;
            if (mLastWebViewTouchUpTime > -1) {
                long touchUpTimeDelta = System.currentTimeMillis() - mLastWebViewTouchUpTime;
                // this value needs to be largish
                if (touchUpTimeDelta < 1500) {
                    // If the url has changed since the use pressed their finger down, a redirect has likely occurred,
                    // in which case we don't update the Url Stack
                    if (mLastWebViewTouchDownUrl.equals(mUrl.toString())) {
                        viaInput = true;
                    }
                    mLastWebViewTouchUpTime = -1;

                }
            }

            if (viaInput) {
                mDoDropDownCheck = true;
            }

            return mController.shouldOverrideUrlLoading(urlAsString, viaInput);
        }
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mController.onReceivedError();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onPageStarted(WebView view, String urlAsString, Bitmap favIcon) {
            mDoDropDownCheck = true;
            mController.onPageStarted(urlAsString, favIcon);
        }

        @Override
        public void onPageFinished(WebView webView, String urlAsString) {
            mController.onPageFinished(urlAsString);
        }
    };


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
