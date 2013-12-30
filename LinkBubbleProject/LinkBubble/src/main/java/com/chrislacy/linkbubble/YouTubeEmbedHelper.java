package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class YouTubeEmbedHelper {

    private Context mContext;
    private List<String> mEmbedIds = new ArrayList<String>();

    private static class EmbedInfo {
        String mTitle;
        String mThumbnailUrl;
        String mId;
    }
    private List<EmbedInfo> mEmbedInfo = new ArrayList<EmbedInfo>();

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

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_loading, null);

        TextView textView = (TextView) view.findViewById(R.id.loading_text);
        textView.setText(R.string.loading_youtube_embed_info);

        builder.setView(view);
        builder.setIcon(0);

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        new DownloadYouTubeEmbedInfo(alertDialog).execute(null, null, null);

        return alertDialog;
    }

    //https://www.googleapis.com/youtube/v3/videos?id=7lCDEYXw3mM,CevxZvSJLk8&key=AIzaSyChiS6yef7AIe5p0JvJGnHrHmmimehIuDs&part=snippet&fields=items(snippet(title,thumbnails(default)))

    private class DownloadYouTubeEmbedInfo extends AsyncTask<Void, Void, Void> {
        private AlertDialog mLoadingAlertDialog;

        DownloadYouTubeEmbedInfo(AlertDialog loadingAlertDialog) {
            super();
            mLoadingAlertDialog = loadingAlertDialog;
        }

        protected Void doInBackground(Void... arg) {

            String idsAsString = "";
            for (String id : mEmbedIds) {
                if (idsAsString.length() > 0) {
                    idsAsString += ",";
                }
                idsAsString += id;
            }

            String url = "https://www.googleapis.com/youtube/v3/videos?id=" + idsAsString + "&key=" + Config.YOUTUBE_API_KEY +
                    "&part=snippet&fields=items(id,snippet(title,thumbnails(default)))";

            String jsonAsString = Util.downloadJSONAsString(url, 5000);

            mEmbedInfo.clear();

            try {
                JSONObject jsonObject = new JSONObject(jsonAsString);
                Object itemsObject = jsonObject.get("items");
                if (itemsObject instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray)itemsObject;
                    for (int i = 0; i < jsonArray.length(); ++i) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        EmbedInfo embedInfo = new EmbedInfo();
                        embedInfo.mId = item.getString("id");
                        JSONObject snippet = item.getJSONObject("snippet");
                        if (snippet != null) {
                            embedInfo.mTitle = snippet.getString("title");
                            JSONObject thumbnails = snippet.getJSONObject("thumbnails");
                            if (thumbnails != null) {
                                JSONObject defaultEntry = thumbnails.getJSONObject("default");
                                if (defaultEntry != null) {
                                    embedInfo.mThumbnailUrl = defaultEntry.getString("url");
                                    mEmbedInfo.add(embedInfo);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            if (mEmbedInfo.size() > 0) {
                mLoadingAlertDialog.dismiss();
                mLoadingAlertDialog = null;

                ListView listView = new ListView(mContext);
                ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1);
                for (EmbedInfo embedInfo : mEmbedInfo) {
                    listAdapter.add(embedInfo.mTitle);
                }
                listView.setAdapter(listAdapter);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setView(listView);
                builder.setIcon(0);
                builder.setTitle(R.string.title_youtube_embed_to_load);

                AlertDialog alertDialog = builder.create();
                alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alertDialog.show();
            }
        }
    };
}
