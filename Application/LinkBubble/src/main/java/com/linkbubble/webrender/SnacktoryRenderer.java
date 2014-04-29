package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
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

public class SnacktoryRenderer extends WebRenderer {

    private static final String TAG = "SnacktoryRenderer";

    private GetPageAsTextTask mGetPageAsTextTask;
    private View mView;
    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mBodyTextView;
    private TouchIconTransformation mTouchIconTransformation;

    public SnacktoryRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        mView = LayoutInflater.from(context).inflate(R.layout.view_cleanview, null);
        mView.setLayoutParams(webRendererPlaceholder.getLayoutParams());
        Util.replaceViewAtPosition(webRendererPlaceholder, mView);

        mImageView = (ImageView) mView.findViewById(R.id.image_view);
        mTitleTextView = (TextView) mView.findViewById(R.id.title_text);
        mBodyTextView = (TextView) mView.findViewById(R.id.body_text);
    }

    @Override
    public void destroy() {

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
            String title = result.getTitle();
            if (title != null) {
                mTitleTextView.setVisibility(View.VISIBLE);
                mTitleTextView.setText(title);
            } else {
                mTitleTextView.setVisibility(View.GONE);
            }

            String text = result.getText();
            if (text != null) {
                mBodyTextView.setVisibility(View.VISIBLE);
                mBodyTextView.setText(text);
            } else {
                mBodyTextView.setVisibility(View.GONE);
            }

            String imageUrl = result.getImageUrl();
            if (imageUrl != null) {
                mImageView.setVisibility(View.VISIBLE);
                Picasso.with(mContext).load(imageUrl).into(mImageView);
            } else {
                mImageView.setVisibility(View.GONE);
            }

            String urlAsString = result.getCanonicalUrl();
            if (urlAsString == null) {
                urlAsString = result.getUrl();
            }

            try {
                setUrl(urlAsString);

                if (title != null) {
                    mController.onReceivedTitle(urlAsString, title);
                }
                mController.onProgressChanged(100, urlAsString);
                mController.onPageFinished(urlAsString);

                String faviconUrl = result.getFaviconUrl();
                Log.d(TAG, "faviconUrl:" + faviconUrl);
                if (faviconUrl != null) {
                    if (mTouchIconTransformation == null) {
                        mTouchIconTransformation = new TouchIconTransformation(SnacktoryRenderer.this);
                    }
                    mTouchIconTransformation.setPageUrl(urlAsString);
                    Picasso.with(mContext).load(faviconUrl).transform(mTouchIconTransformation).fetch();
                }
            } catch (MalformedURLException ex) {

            }
        }
    }


    private static class TouchIconTransformation implements Transformation {

        private WeakReference<SnacktoryRenderer> mRenderer;
        String mPageUrl = null;

        TouchIconTransformation(SnacktoryRenderer renderer) {
            mRenderer = new WeakReference<SnacktoryRenderer>(renderer);
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
                SnacktoryRenderer renderer = mRenderer.get();
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
