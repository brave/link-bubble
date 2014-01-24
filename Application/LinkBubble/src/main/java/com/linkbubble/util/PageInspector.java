package com.linkbubble.util;


import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.linkbubble.Config;

import java.net.MalformedURLException;
import java.net.URL;

public class PageInspector {

    private static final String TAG = "PageInspector";

    private static final String JS_VARIABLE = "LinkBubble";
    private static final String UNKNOWN_TAG = "unknown";        // the tag Chrome/WebView uses for unknown elements

    private static final String JS_YOUTUBE_EMBED_CHECK =
            "{\n" +
            "    var elems = document.getElementsByTagName('*'), i;\n" +
            "    var resultArray = null;\n" +
            "    var resultCount = 0;\n" +
            "    for (i in elems) {\n" +
            "       var elem = elems[i];\n" +
            "       if (elem.src != null && elem.src.indexOf(\"" + Config.YOUTUBE_EMBED_PREFIX + "\") != -1) {\n" +
            //"           //console.log(\"found embed: \" + elem.src);\n" +
            "           if (resultArray == null) {\n" +
            "               resultArray = new Array();\n" +
            //"           console.log(\"allocate array\");\n" +
            "           }\n" +
            "           resultArray[resultCount] = elem.src;\n" +
            "           resultCount++;\n" +
            "       }\n" +
            "    }\n" +
            "    if (resultCount > 0) {\n" +
            "       " + JS_VARIABLE + ".onYouTubeEmbeds(resultArray.toString());\n" +
            "    }\n" +
            "}";

    private static final String JS_FAVICON_CHECK =
            "{\n" +
            "  var links = document.head.getElementsByTagName('link');\n" +
            "  var linksArray = null;\n" +
            "  var linksCount = 0;\n" +
            "  for(var link in links){\n" +
            "      if(links.hasOwnProperty(link)){\n" +
            "          var l = links[link];\n" +
            "          if (l.rel != null && l.rel.indexOf(\"apple-touch-icon\") != -1) {\n" +
            "            if (linksArray == null) {\n" +
            "              linksArray = new Array();\n" +
            "            }\n" +
            "            var s = \"@@@\" + l.rel + \",\" + l.href + \",\" + l.sizes + \"###\";\n" +
            "            linksArray[linksCount] = s;\n" +
            "            linksCount++;\n" +
            "          }\n" +
            "      }\n" +
            "  }\n" +
            "  if (linksCount > 0) {" +
            "  " + JS_VARIABLE + ".onFaviconLinks(linksArray.toString());\n" +
            "  }\n" +
            "}";

    private static final String JS_EMBED = "javascript:(function() {\n" +
            JS_FAVICON_CHECK +
            JS_YOUTUBE_EMBED_CHECK +
            "})();";

    private Context mContext;
    private JSEmbedHandler mJSEmbedHandler;
    private YouTubeEmbedHelper mYouTubeEmbedHelper;
    private OnItemFoundListener mOnItemFoundListener;

    private static final int MAX_FAVICON_ENTRIES = 4;
    private FaviconEntry[] mFaviconEntries = new FaviconEntry[MAX_FAVICON_ENTRIES];
    private int mFaviconEntryCount = 0;
    private String mLastFaviconResultString = null;

    public interface OnItemFoundListener {
        void onYouTubeEmbeds();
    }

    public PageInspector(Context context, WebView webView, OnItemFoundListener listener) {
        mContext = context;
        mJSEmbedHandler = new JSEmbedHandler();
        mOnItemFoundListener = listener;
        webView.addJavascriptInterface(mJSEmbedHandler, JS_VARIABLE);
    }

    public void run(WebView webView) {
        webView.loadUrl(JS_EMBED);
    }

    public void reset() {
        if (mYouTubeEmbedHelper != null) {
            mYouTubeEmbedHelper.clear();
        }
    }

    public YouTubeEmbedHelper getYouTubeEmbedHelper() {
        return mYouTubeEmbedHelper;
    }

    private static class FaviconEntry {
        String mRel;
        URL mUrl;
        int mSize;

        public String toString() {
            return "rel:" + mRel + ", url:" + mUrl.toString() + ", size:" + mSize;
        }
    };

    // For security reasons, all callbacks should be in a self contained class
    private class JSEmbedHandler {

        @JavascriptInterface
        public void onFaviconLinks(String string) {
            if (mLastFaviconResultString != null && mLastFaviconResultString.equals(string)) {
                return;
            }
            mLastFaviconResultString = string;

            if (string == null || string.length() == 0) {
                return;
            }

            Log.d(TAG, "onFaviconLinks() - " + string);

            mFaviconEntryCount = 0;

            String[] items = string.split("@@@");
            for (String s : items) {
                if (mFaviconEntryCount == MAX_FAVICON_ENTRIES) {
                    break;
                }
                s = s.replace("###", "");
                if (s.length() > 0) {
                    String[] vars = s.split(",");
                    if (vars == null || vars.length < 2) {
                        continue;
                    }

                    String rel = vars[0];
                    if (rel.equals(UNKNOWN_TAG)) {
                        continue;
                    }
                    String href = vars[1];
                    if (href.equals(UNKNOWN_TAG)) {
                        continue;
                    }
                    int size = -1;
                    String sizes = vars.length > 2 ? vars[2] : null;
                    if (sizes != null && sizes.equals(UNKNOWN_TAG) == false) {
                        String[] splitSizes = sizes.split("x");     // specified as 'sizes="128x128"' (http://goo.gl/tGV50j)
                        if (splitSizes.length > 0) {
                            try {
                                // just pick the first one...
                                size = Integer.valueOf(splitSizes[0]);
                            } catch (NumberFormatException e) {
                                // do nothing...
                            }
                        }
                    }

                    try {
                        FaviconEntry faviconEntry = mFaviconEntries[mFaviconEntryCount];
                        if (faviconEntry == null) {
                            faviconEntry = new FaviconEntry();
                            mFaviconEntries[mFaviconEntryCount] = faviconEntry;
                        }
                        faviconEntry.mRel = rel;
                        faviconEntry.mUrl = new URL(href);
                        faviconEntry.mSize = size;
                        mFaviconEntryCount++;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (int i = 0; i < mFaviconEntryCount; i++) {
                Log.d(TAG, "Favicon entry:" + mFaviconEntries[i].toString());
            }
        }

        @JavascriptInterface
        public void onYouTubeEmbeds(String string) {
            Log.d(TAG, "onYouTubeEmbeds() - " + string);

            if (string == null || string.length() == 0) {
                return;
            }

            if (mYouTubeEmbedHelper == null) {
                mYouTubeEmbedHelper = new YouTubeEmbedHelper(mContext);
            }

            String[] strings = string.split(",");
            if (mYouTubeEmbedHelper.onEmbeds(strings)) {
                if (mOnItemFoundListener != null) {
                    mOnItemFoundListener.onYouTubeEmbeds();
                }
            }
        }
    };


}
