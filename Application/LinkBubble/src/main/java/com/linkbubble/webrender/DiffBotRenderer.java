package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
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

public class DiffBotRenderer extends WebRenderer {

    private static final String TAG = "DiffBotRenderer";
    private static final String API_URL = "http://api.diffbot.com";

    @SuppressWarnings("unused")
    static class Article {
        @SerializedName("author") String mAuthor;
        @SerializedName("text") String mBody;
        @SerializedName("title") String mTitle;
        @SerializedName("type") String mType;
        @SerializedName("url") String mUrl;
        @SerializedName("resolved_url") String mResolvedUrl;
        @SerializedName("icon") String mIconUrl;
        @SerializedName("html") String mHtml;
        @SerializedName("date") String mDate;

        static class Image {
            @SerializedName("primary") boolean mPrimary;
            @SerializedName("caption") String mCaption;
            @SerializedName("url") String mUrl;
        }
        @SerializedName("images") List<Image> mImages;

        Image getPrimaryImage() {
            if (mImages != null && mImages.size() > 0) {
                for (Image image : mImages) {
                    if (image.mPrimary) {
                        return image;
                    }
                }
            }

            return null;
        }

        String getUrlAsString() {
            if (mResolvedUrl != null) {
                return mResolvedUrl;
            }

            return mUrl;
        }
    }

    interface DiffBotService {

        @GET("/v2/article?token=" + Constant.DIFFBOT_KEY)
        void getArticle(@Query("url") String url, Callback<Article> callback);

    }

    static RestAdapter sRestAdapter;
    static DiffBotService sDiffBotService;

    private View mView;
    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mBodyTextView;
    private TouchIconTransformation mTouchIconTransformation;

    public DiffBotRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        if (sDiffBotService == null) {
            sRestAdapter = new RestAdapter.Builder().setEndpoint(API_URL).build();

            sDiffBotService = sRestAdapter.create(DiffBotService.class);
        }

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

        sDiffBotService.getArticle(urlAsString, new Callback<Article>() {

            @Override
            public void success(Article article, Response response) {
                Log.d(TAG, "success\ntitle:" + article.mTitle + "\nbody: " + article.mBody + "\nhtml:" + article.mHtml + "\nmIconUrl:" + article.mIconUrl);

                if (article != null) {
                    if (article.mTitle != null) {
                        mTitleTextView.setVisibility(View.VISIBLE);
                        mTitleTextView.setText(article.mTitle);
                    } else {
                        mTitleTextView.setVisibility(View.GONE);
                    }

                    if (article.mBody != null) {
                        mBodyTextView.setVisibility(View.VISIBLE);
                        //mBodyTextView.setText(article.mBody);
                        mBodyTextView.setText(Html.fromHtml(article.mHtml));
                        mBodyTextView.setMovementMethod(LinkMovementMethod.getInstance());
                        SafeUrlSpan.fixUrlSpans(mBodyTextView);
                    } else {
                        mBodyTextView.setVisibility(View.GONE);
                    }

                    Article.Image primaryImage = article.getPrimaryImage();
                    if (primaryImage != null) {
                        mImageView.setVisibility(View.VISIBLE);
                        Picasso.with(mContext).load(primaryImage.mUrl).into(mImageView);
                    } else {
                        mImageView.setVisibility(View.GONE);
                    }

                    String urlAsString = article.getUrlAsString();

                    try {
                        setUrl(urlAsString);

                        mController.onReceivedTitle(urlAsString, article.mTitle);
                        mController.onProgressChanged(100, urlAsString);
                        mController.onPageFinished(urlAsString);

                        if (article.mIconUrl != null) {
                            if (mTouchIconTransformation == null) {
                                mTouchIconTransformation = new TouchIconTransformation(DiffBotRenderer.this);
                            }
                            mTouchIconTransformation.setPageUrl(urlAsString);
                            Picasso.with(mContext).load(article.mIconUrl).transform(mTouchIconTransformation).fetch();
                        }
                    } catch (MalformedURLException ex) {

                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "failure:" + error.getLocalizedMessage());
            }
        });
    }

    @Override
    public void reload() {

    }

    @Override
    public void stopLoading() {

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

    private static class TouchIconTransformation implements Transformation {

        private WeakReference<DiffBotRenderer> mRenderer;
        String mPageUrl = null;

        TouchIconTransformation(DiffBotRenderer renderer) {
            mRenderer = new WeakReference<DiffBotRenderer>(renderer);
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
                DiffBotRenderer renderer = mRenderer.get();
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
