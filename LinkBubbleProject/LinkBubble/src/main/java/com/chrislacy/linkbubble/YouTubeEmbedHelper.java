package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public class YouTubeEmbedHelper {

    private Context mContext;
    private List<String> mEmbedIds = new ArrayList<String>();
    ResolveInfo mYouTubeResolveInfo;

    YouTubeEmbedHelper(Context context) {
        mContext = context;
        mYouTubeResolveInfo = Settings.get().getYouTubeViewResolveInfo();
    }

    void clear() {
        mEmbedIds.clear();
    }

    int size() {
        return mEmbedIds.size();
    }

    boolean onYouTubeEmbedFound(String src) {
        if (src.contains(Config.YOUTUBE_EMBED_PREFIX)) {
            String videoId = src.replace(Config.YOUTUBE_EMBED_PREFIX, "");
            if (videoId.length() > 0) {
                boolean onList = false;
                if (mEmbedIds.size() > 0) {
                    for (String s : mEmbedIds) {
                        if (s.equals(videoId)) {
                            onList = true;
                            break;
                        }
                    }
                }
                if (onList == false) {
                    mEmbedIds.add(videoId);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean loadYouTubeVideo(String id) {
        if (mYouTubeResolveInfo != null) {
            return MainApplication.loadIntent(mContext, mYouTubeResolveInfo.activityInfo.packageName,
                    mYouTubeResolveInfo.activityInfo.name, Config.YOUTUBE_WATCH_PREFIX + id, -1);
        }

        return false;
    }

    boolean onOpenInAppButtonClick() {
        if (mEmbedIds.size() > 0) {
            return loadYouTubeVideo(mEmbedIds.get(0));
        }

        return false;
    }
}
