package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ResolveInfo;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.ProgressBar;

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
        int size = mEmbedIds.size();
        if (size == 1) {
            return loadYouTubeVideo(mEmbedIds.get(0));
        } else if (size > 1) {
            getMultipleEmbedsDialog().show();
            return true;
        }

        return false;
    }

    private AlertDialog getMultipleEmbedsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        final ProgressBar progressBar = new ProgressBar(mContext);

        builder.setView(progressBar);
        builder.setIcon(0);
        builder.setTitle(R.string.title_youtube_embed_to_load);

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        return alertDialog;
    }
}
