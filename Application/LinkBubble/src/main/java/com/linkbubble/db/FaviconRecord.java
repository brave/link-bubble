/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.db;

import android.graphics.Bitmap;


public class FaviconRecord {

    private int mId;
    private String mUrl;
    private String mPageUrl;
    private Bitmap mFavicon;
    private long mTime;

    public FaviconRecord(){}

    public FaviconRecord(String url, String pageUrl, Bitmap favicon, long time) {
        super();
        mUrl = url;
        mPageUrl = pageUrl;
        mFavicon = favicon;
        mTime = time;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getPageUrl() {
        return mPageUrl;
    }

    public void setPageUrl(String pageUrl) {
        mPageUrl = pageUrl;
    }

    public Bitmap getFavicon() {
        return mFavicon;
    }

    public void setFavicon(Bitmap bitmap) {
        mFavicon = bitmap;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    @Override
    public String toString() {
        return "HistoryRecord [id=" + mId + ", mUrl=" + mUrl + ", mPageUrl=" + mPageUrl + "]";
    }

}
