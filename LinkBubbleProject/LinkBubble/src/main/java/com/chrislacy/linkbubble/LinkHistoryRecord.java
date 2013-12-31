package com.chrislacy.linkbubble;


public class LinkHistoryRecord {

    private int mId;
    private String mUrl;
    private String mTitle;
    private long mTime;

    public LinkHistoryRecord(){}

    public LinkHistoryRecord(String title, String url, long time) {
        super();
        mTitle = title;
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
        return "LinkHistoryRecord [id=" + mId + ", title=" + mTitle + ", url=" + mUrl + "]";
    }


    public static class ChangedEvent {
        public ChangedEvent(LinkHistoryRecord linkHistoryRecord) {
            mLinkHistoryRecord = linkHistoryRecord;
        }
        LinkHistoryRecord mLinkHistoryRecord;
    }


}
