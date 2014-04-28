package com.linkbubble.webrender;


import android.content.Context;
import android.util.Log;
import android.view.View;
import com.google.gson.annotations.SerializedName;
import com.linkbubble.Constant;
import com.linkbubble.util.YouTubeEmbedHelper;
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
        @SerializedName("text") String mText;
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
    }

    interface DiffBotService {

        @GET("/v2/article?token=" + Constant.DIFFBOT_KEY)
        void getArticle(@Query("url") String url, Callback<Article> callback);

    }

    static RestAdapter sRestAdapter;
    static DiffBotService sDiffBotService;

    public DiffBotRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder);

        if (sDiffBotService == null) {
            sRestAdapter = new RestAdapter.Builder().setEndpoint(API_URL).build();

            sDiffBotService = sRestAdapter.create(DiffBotService.class);
        }
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
                Log.d(TAG, "success\ntitle:" + article.mTitle + "\ntext:" + article.mText);
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
