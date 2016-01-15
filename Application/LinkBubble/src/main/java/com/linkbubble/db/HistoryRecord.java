/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.db;


public class HistoryRecord {

    private int mId;
    private String mUrl;
    private String mHost;
    private String mTitle;
    private long mTime;

    public HistoryRecord(){}

    public HistoryRecord(String title, String url, String host, long time) {
        super();
        mTitle = title;
        mHost = host;
        mUrl = url;
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

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    @Override
    public String toString() {
        return "HistoryRecord [id=" + mId + ", title=" + mTitle + ", url=" + mUrl + "]";
    }


    public static class ChangedEvent {
        public ChangedEvent(HistoryRecord historyRecord) {
            mHistoryRecord = historyRecord;
        }
        public HistoryRecord mHistoryRecord;
    }


}
