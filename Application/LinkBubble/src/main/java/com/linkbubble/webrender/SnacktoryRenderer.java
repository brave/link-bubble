package com.linkbubble.webrender;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import com.linkbubble.Constant;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SnacktoryRenderer extends WebViewRenderer {

    // **Broken links
    //
    // [nothing displays]:
    //  * http://www.bostonglobe.com/sports/2014/04/28/the-donald-sterling-profile-not-pretty-picture/jZx4v3EWUFdLYh9c289ODL/story.html


    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM dd, yyyy");

    private GetPageAsTextTask mGetPageAsTextTask;

    public SnacktoryRenderer(Context context, Controller controller, View webRendererPlaceholder, String tag) {
        super(context, controller, webRendererPlaceholder, tag);

        mMode = Mode.Web;
    }

    @Override
    public void loadUrl(URL url, Mode mode) {
        String urlAsString = url.toString();
        Log.d(TAG, "loadUrl() - " + urlAsString);

        if (mGetPageAsTextTask != null) {
            mGetPageAsTextTask.cancel(true);
        }

        mMode = mode;

        if (mMode == Mode.Web) {
            super.loadUrl(url, mMode);
            return;
        }

        mGetPageAsTextTask = new GetPageAsTextTask();
        mGetPageAsTextTask.execute(urlAsString);

        // This is only called by Snacktory renderer so that the loading animations start at the point the page HTML commences.
        // Not needed for other Renderers given onPageStarted() will be called.
        mController.onLoadUrl(urlAsString);
    }

    @Override
    public void reload() {
        if (mMode == Mode.Web) {
            super.reload();
            return;
        }

        loadUrl(getUrl(), mMode);
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
                Log.d(TAG, "GetPageAsTextTask().doInBackground(): url:" + url);
                HtmlFetcher fetcher = new HtmlFetcher();
                result = fetcher.fetchAndExtract(url, 30 * 1000, true);
                //String text = result.getText();
                //String title = result.getTitle();
                //String imageUrl = result.getImageUrl();
                //Log.d(TAG, "title: " + title + ", text: " + text + ", imageUrl:" + imageUrl);
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
                setUrl(urlAsString);
                url = getUrl();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String text = result.getText();
            if (text.isEmpty()) {
                Log.d(TAG, "No text found for - forcing to Web mode");
                loadUrl(url, Mode.Web);
                return;
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

            //Log.d(TAG, "pageHtml:" + pageHtml);
            mWebView.loadDataWithBaseURL(urlAsString, pageHtml, "text/html", "utf-8", urlAsString);
            //mWebView.loadData(pageHtml, "text/html", "utf-8");

            if (title != null) {
                mController.onReceivedTitle(urlAsString, title);
            }
            mController.onProgressChanged(100, urlAsString);
            //mController.onPageFinished(urlAsString);
        }
    }
}
