package com.linkbubble.articlerender;


import android.content.Context;
import android.util.Log;
import com.linkbubble.R;
import de.jetwick.snacktory.JResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArticleContent {

    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM dd, yyyy");

    String mPageHtml;
    String mTitle;
    public String mText;
    public URL mUrl;

    public static ArticleContent extract(Context context, JResult result) {
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

        boolean isTablet = context.getResources().getBoolean(R.bool.is_tablet);

        String bodyHMargin;
        String titleTopMargin;
        String titleFontSize;
        if (isTablet) {
            bodyHMargin = "24px";
            titleTopMargin = "32px";
            titleFontSize = "150%";
        } else {
            bodyHMargin = "12px";
            titleTopMargin = "24px";
            titleFontSize = "130%";
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


}
