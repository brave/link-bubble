package com.linkbubble.articlerender;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.linkbubble.Config;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

public class ArticleContent {

    private static final String TAG = "ArticleContent";

    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM dd, yyyy");

    String mPageHtml;
    String mTitle;
    public String mText;
    public URL mUrl;

    public interface OnFinishedListener {
        public void onFinished(ArticleContent articleContent);
    }

    static public BuildContentTask fetchArticleContent(String url, String pageHtml, OnFinishedListener onFinishedListener) {
        BuildContentTask task = new BuildContentTask(onFinishedListener);
        task.execute(url, pageHtml);
        return task;
    }

    public static ArticleContent extract(JResult result) {
        ArticleContent articleModeContent = new ArticleContent();

        String urlAsString = result.getCanonicalUrl();
        if (urlAsString == null || urlAsString.isEmpty()) {
            urlAsString = result.getUrl();
        }
        try {
            articleModeContent.mUrl = new URL(urlAsString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return articleModeContent;
        }

        articleModeContent.mText = result.getText();
        if (articleModeContent.mText.isEmpty()) {
            return articleModeContent;
        }

        String bodyHMargin;
        String titleTopMargin;
        String titleFontSize;
        if (Config.sIsTablet) {
            bodyHMargin = "24px";
            titleTopMargin = "32px";
            titleFontSize = "150%";
        } else {
            bodyHMargin = "12px";
            titleTopMargin = "24px";
            titleFontSize = "130%";
        }

        String textColor =  String.format("#%06X", 0xFFFFFF & Settings.get().getThemedTextColor());
        String bgColor =  String.format("#%06X", 0xFFFFFF & Settings.get().getThemedContentViewColor());
        String linkColor =  String.format("#%06X", 0xFFFFFF & Settings.get().getThemedLinkColor());

        String headHtml =
                "  <head>\n" +
                        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0, height=device-height\"/>\n" +
                        "    <link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>\n" +
                        "    <style type=\"text/css\">\n" +
                        "      body { background-color: " + bgColor + "; color: " + textColor + ";}\n" +
                        "      p, div { font-family: 'Roboto', sans-serif; font-size: 16px; line-height: 160%;}\n" +
                        "      a { text-decoration: none; color: " + linkColor + "}\n" +
                        "      #lbInfo { width:100%; min-height:28px; margin:0 auto; padding-bottom: 20px;}\n" +
                        "      #lbInfoL { float:left; width:70%; }\n" +
                        "      #lbInfoR { float:right; width:30%; }\n" +
                        "    </style>" +
                        "    </style>";

        String bodyHtml = "<body >\n" +
                "    <div style=\"margin:0px " + bodyHMargin + " 0px " + bodyHMargin + "\">\n";

        String title = result.getTitle();
        if (title != null) {
            headHtml += "<title>" + title + "</title>";
            bodyHtml += "<p style=\"font-size:" + titleFontSize + ";line-height:120%;font-weight:bold;margin:" + titleTopMargin + " 0px 12px 0px\">" + title + "</p>";
        }

        String authorName = result.getAuthorName();
        Date publishedDate = result.getDate();

        String leftString = "";
        String rightString = "";

        if (authorName != null) {
            leftString = "<span class=\"nowrap\"><b>" + authorName + "</b>,</span> ";
        }
        if (articleModeContent.mUrl != null) {
            leftString += "<span class=\"nowrap\"><a href=\"" + articleModeContent.mUrl.getProtocol()
                    + "://" + articleModeContent.mUrl.getHost() + "\">" + (articleModeContent.mUrl.getHost().replace("www.", "")) + "</a></span>";
        }

        Log.d("info", "urlHost:" + articleModeContent.mUrl.getHost() + ", authorName: " + authorName);

        if (publishedDate != null) {
            rightString = "<span style=\"float:right\">" + sDateFormat.format(publishedDate) + "</span>";
        }

        bodyHtml += "<hr style=\"border: 0;height: 0; border-top: 1px solid rgba(0, 0, 0, 0.1); border-bottom: 1px solid rgba(255, 255, 255, 0.3);\">"
                + "<div id=\"lbInfo\"><div id=\"lbInfoL\">" + leftString + "</div><div id=\"lbInfoR\">" + rightString + "</div></div>";

        String html = result.getHtml();
        if (html != null) {
            bodyHtml += html;
        }

        headHtml += "</head>";
        bodyHtml += " </div>\n" +
                "    </div>\n" +
                "    <br><br><br>" +
                "  </body>\n";

        //mWebView.loadUrl(urlAsString);
        //mWebView.stopLoading();

        String pageHtml = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n" + headHtml + bodyHtml + "</html>";

        articleModeContent.mPageHtml = pageHtml;
        articleModeContent.mTitle = title;

        return articleModeContent;
    }

    // **Broken links
    //
    // [nothing displays]:
    //  * http://www.bostonglobe.com/sports/2014/04/28/the-donald-sterling-profile-not-pretty-picture/jZx4v3EWUFdLYh9c289ODL/story.html

    static public class BuildContentTask extends AsyncTask<String, JResult, JResult> {
        OnFinishedListener mOnFinishedListener;

        public BuildContentTask(OnFinishedListener onFetched) {
            super();
            mOnFinishedListener = onFetched;
        }

        protected JResult doInBackground(String... data) {

            JResult result = null;
            String url = data[0];
            String pageHtml = data[1];
            try {
                Log.d(TAG, "BuildContentTask().doInBackground(): url:" + url);
                HtmlFetcher fetcher = new HtmlFetcher();
                result = fetcher.extract(url, pageHtml);
            } catch (Exception ex) {
                Log.d(TAG, ex.getLocalizedMessage(), ex);
            }

            return isCancelled() ? null : result;
        }

        protected void onPostExecute(JResult result) {
            if (result == null) {
                mOnFinishedListener.onFinished(null);
                return;
            }

            ArticleContent articleContent = ArticleContent.extract(result);

            if (articleContent.mUrl == null || articleContent.mText.isEmpty()) {
                mOnFinishedListener.onFinished(null);
                return;
            }

            mOnFinishedListener.onFinished(articleContent);
        }
    }

    public static boolean tryForArticleContent(URL url) {
        String path = url.getPath();
        if (path.equals("/") || path.equals("/m/") || path.equals("/mobile/")) {
            Log.d(TAG, "ignore path for url: " + url.toString());
            return false;
        }

        // Ignore the media sites
        String host = url.getHost();
        if (host.contains("google.com") || host.equals("imgur.com") || host.equals("instagram.com") || host.equals("linkbubble.com")
                || host.equals("reddit.com") || host.equals("twitter.com") || host.equals("vine.co") || host.equals("vimeo.com")
                || host.equals("youtube.com")) {
            Log.d(TAG, "ignore host for url: " + url.toString());
            return false;
        }

        // Ignore the Link Bubble welcome page
        if (path.equals("/linkbubble/welcome.html")) {
            return false;
        }

        return true;
    }
}
