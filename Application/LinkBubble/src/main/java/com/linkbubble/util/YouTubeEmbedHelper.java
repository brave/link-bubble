/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.linkbubble.Config;
import com.linkbubble.ConfigAPIs;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
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
    private DownloadYouTubeEmbedInfoTask mCurrentDownloadTask;

    public ResolveInfo mYouTubeResolveInfo;

    public YouTubeEmbedHelper(Context context) {
        mContext = context;
        mYouTubeResolveInfo = Settings.get().getYouTubeViewResolveInfo();
    }

    public void clear() {
        mEmbedIds.clear();
        if (mCurrentDownloadTask != null) {
            synchronized (mCurrentDownloadTask) {
                if (mCurrentDownloadTask != null) {
                    mCurrentDownloadTask.cancel(true);
                }
                mCurrentDownloadTask = null;
            }
        }
    }

    public int size() {
        return mEmbedIds.size();
    }

    /*
     * Known YouTube embed URLs:
        * http://www.youtube.com/embed/oSAW1tSNIa4?version=3&rel=1&fs=1&showsearch=0&showinfo=1&iv_load_policy=1&wmode=transparent
        * https://www.youtube.com/embed/q1dpQKntj_w
     */
    public boolean onEmbeds(String[] strings) {
        if (strings == null || strings.length == 0) {
            return false;
        }

        boolean listChanged = false;

        for (String string : strings) {
            int prefixStartIndex = string.indexOf(Config.YOUTUBE_EMBED_PREFIX);
            if (prefixStartIndex > -1) {
                URL url;
                try {
                    url = new URL(string);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    break;
                }

                String path = url.getPath();
                int pathStartIndex = path.indexOf(Config.YOUTUBE_EMBED_PATH_SUFFIX);
                if (pathStartIndex > -1) {
                    String videoId = path.substring(pathStartIndex + Config.YOUTUBE_EMBED_PATH_SUFFIX.length());
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
                            listChanged = true;
                        }
                    }
                }
            }
        }

        if (listChanged) {
            if (mCurrentDownloadTask != null) {
                synchronized (mCurrentDownloadTask) {
                    if (mCurrentDownloadTask != null) {
                        mCurrentDownloadTask.cancel(true);
                    }
                    mCurrentDownloadTask = new DownloadYouTubeEmbedInfoTask(false, null);
                    mCurrentDownloadTask.execute(null, null, null);
                }
            } else {
                mCurrentDownloadTask = new DownloadYouTubeEmbedInfoTask(false, null);
                mCurrentDownloadTask.execute(null, null, null);
            }
        }

        return mEmbedIds.size() > 0;
    }

    private boolean loadYouTubeVideo(String id) {
        if (mYouTubeResolveInfo != null) {
            if (MainApplication.loadIntent(mContext, mYouTubeResolveInfo.activityInfo.packageName,
                    mYouTubeResolveInfo.activityInfo.name, Config.YOUTUBE_WATCH_PREFIX + id, -1, true)) {
                // L_WATCH: L currently lacks getRecentTasks(), so minimize here
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    MainController.get().switchToBubbleView(false);
                }
                return true;
            }
        }

        return false;
    }

    public boolean onOpenInAppButtonClick() {
        int size = mEmbedIds.size();
        if (size == 1) {
            return loadYouTubeVideo(mEmbedIds.get(0));
        } else if (size > 1) {
            Util.showThemedDialog(getMultipleEmbedsDialog());
            return true;
        }

        return false;
    }

    private boolean embedInfoMatchesIds() {
        if (mEmbedIds.size() == mEmbedInfo.size()) {
            for (String id : mEmbedIds) {
                boolean found = false;
                for (EmbedInfo info : mEmbedInfo) {
                    if (info.mId.equals(id)) {
                        found = true;
                        break;
                    }
                }

                if (found == false) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private AlertDialog getMultipleEmbedsDialog() {
        if (embedInfoMatchesIds()) {
            return getEmbedResultsDialog();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.view_loading, null);

            TextView textView = (TextView) view.findViewById(R.id.loading_text);
            textView.setText(R.string.loading_youtube_embed_info);

            builder.setView(view);
            builder.setIcon(0);

            AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

            if (mCurrentDownloadTask != null) {
                synchronized (mCurrentDownloadTask) {
                    if (mCurrentDownloadTask != null) {
                        mCurrentDownloadTask.cancel(true);
                    }
                    mCurrentDownloadTask = new DownloadYouTubeEmbedInfoTask(true, alertDialog);
                    mCurrentDownloadTask.execute(null, null, null);
                }
            } else {
                mCurrentDownloadTask = new DownloadYouTubeEmbedInfoTask(true, alertDialog);
                mCurrentDownloadTask.execute(null, null, null);
            }

            return alertDialog;
        }
    }

    AlertDialog getEmbedResultsDialog() {
        if (mEmbedInfo.size() > 0) {
            ListView listView = new ListView(mContext);
            listView.setAdapter(new EmbedItemAdapter());

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setView(listView);
            builder.setIcon(mYouTubeResolveInfo.loadIcon(mContext.getPackageManager()));
            builder.setTitle(R.string.title_youtube_embed_to_load);

            final AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    EmbedInfo embedInfo = (EmbedInfo) view.getTag();
                    if (embedInfo != null) {
                        loadYouTubeVideo(embedInfo.mId);
                    }
                    alertDialog.dismiss();
                }
            });

            return alertDialog;
        } else {
            final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
            alertDialog.setTitle(R.string.youtube_embed_error_title);
            alertDialog.setMessage(mContext.getString(R.string.youtube_embed_error_summary));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(R.string.action_ok), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }

            });
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            return alertDialog;
        }
    }

    private class DownloadYouTubeEmbedInfoTask extends AsyncTask<Void, Void, Boolean> {
        private AlertDialog mLoadingAlertDialog;
        private boolean mShowResultsOnCompletion;

        DownloadYouTubeEmbedInfoTask(boolean showResultsOnCompletion, AlertDialog loadingAlertDialog) {
            super();
            mShowResultsOnCompletion = showResultsOnCompletion;
            mLoadingAlertDialog = loadingAlertDialog;
        }

        protected Boolean doInBackground(Void... arg) {

            // This only should happen on a page change, in which case, abort
            if (mEmbedIds.size() == 0 || isCancelled()) {
                return false;
            }

            if (mCurrentDownloadTask != null) {
                synchronized (mCurrentDownloadTask) {
                    if (mCurrentDownloadTask != this) {
                        return false;
                    }
                }
            }

            String idsAsString = "";
            for (String id : mEmbedIds) {
                if (idsAsString.length() > 0) {
                    idsAsString += ",";
                }
                idsAsString += id;
            }

            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            String thumbnailsArg;
            if (Config.isLowMemoryDevice()
                || connectivityManager.isActiveNetworkMetered()
                || NetworkConnectivity.isConnectedFast(mContext) == false) {
                thumbnailsArg = Config.YOUTUBE_API_THUMBNAILS_LOW_QUALITY;
            } else {
                thumbnailsArg = Config.YOUTUBE_API_THUMBNAILS_HIGH_QUALITY;
            }

            String url = "https://www.googleapis.com/youtube/v3/videos?id=" + idsAsString + "&key=" + ConfigAPIs.YOUTUBE_API_KEY +
                    "&part=snippet&fields=items(id,snippet(title," + thumbnailsArg + "))";

            mEmbedInfo.clear();
            String jsonAsString = Util.downloadJSONAsString(url, 5000);
            if (jsonAsString != null) {
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
                                    try {
                                        JSONObject mediumEntry = thumbnails.getJSONObject("medium");
                                        if (mediumEntry != null) {
                                            embedInfo.mThumbnailUrl = mediumEntry.getString("url");
                                            mEmbedInfo.add(embedInfo);
                                        }
                                    } catch (JSONException e) {
                                      // ignore "org.json.JSONException: No value for medium"
                                    }

                                    // Fallback to checking for default...
                                    if (embedInfo.mThumbnailUrl == null) {
                                        JSONObject defaultEntry = thumbnails.getJSONObject("default");
                                        if (defaultEntry != null) {
                                            embedInfo.mThumbnailUrl = defaultEntry.getString("url");
                                            mEmbedInfo.add(embedInfo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (mCurrentDownloadTask != null) {
                synchronized (mCurrentDownloadTask) {
                    if (mCurrentDownloadTask != this) {
                        return false;
                    }
                }
            }

            if (isCancelled()) {
                return false;
            }

            return mEmbedInfo.size() > 0 ? true : false;
        }

        protected void onPostExecute(Boolean result) {
            if (result.booleanValue() == true && mShowResultsOnCompletion && isCancelled() == false) {
                mLoadingAlertDialog.dismiss();
                mLoadingAlertDialog = null;

                Util.showThemedDialog(getEmbedResultsDialog());
            }
        }
    }

    private class EmbedItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mEmbedInfo.size();
        }

        @Override
        public Object getItem(int position) {
            return mEmbedInfo.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.view_youtube_embed_item, null);
            }

            EmbedInfo embedInfo = mEmbedInfo.get(position);

            ImageView imageView = (ImageView)convertView.findViewById(R.id.image);
            if (embedInfo.mThumbnailUrl != null) {
                Picasso.with(mContext).load(embedInfo.mThumbnailUrl).into(imageView);
            }

            TextView textView = (TextView)convertView.findViewById(R.id.text);
            textView.setText(embedInfo.mTitle);

            convertView.setTag(embedInfo);

            return convertView;
        }
    }
}
