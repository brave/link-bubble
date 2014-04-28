package com.linkbubble.webrender;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.annotations.SerializedName;
import com.linkbubble.Constant;
import com.linkbubble.R;
import com.linkbubble.util.Util;
import com.linkbubble.util.YouTubeEmbedHelper;
import com.squareup.picasso.Picasso;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

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

        sDiffBotService.getArticle(urlAsString, new Callback<Article>() {

            @Override
            public void success(Article article, Response response) {
                Log.d(TAG, "success\ntitle:" + article.mTitle + "\ntext:" + article.mBody);

                if (article != null) {
                    if (article.mTitle != null) {
                        mTitleTextView.setVisibility(View.VISIBLE);
                        mTitleTextView.setText(article.mTitle);
                    } else {
                        mTitleTextView.setVisibility(View.GONE);
                    }

                    if (article.mBody != null) {
                        mBodyTextView.setVisibility(View.VISIBLE);
                        mBodyTextView.setText(article.mBody);
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
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "failure");
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
}
