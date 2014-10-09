package com.linkbubble.util;


import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.ui.Prompt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tamper {

    private static class CheckTamperTask extends AsyncTask<Void, Void, Boolean> {

        Context mContext;
        Listener mListener;

        CheckTamperTask(Context context, Listener listener) {
            super();
            mContext = context;
            mListener = listener;
        }

        protected Boolean doInBackground(Void... arg) {

            // This only should happen on a page change, in which case, abort
            if (isCancelled()) {
                return false;
            }

            boolean result = TamperImpl.isTweaked(mContext);

            if (isCancelled()) {
                return false;
            }

            return result;
        }

        protected void onPostExecute(Boolean result) {
            if (result.booleanValue() == true && isCancelled() == false) {
                String text = mContext.getResources().getString(R.string.tampered_apk);
                Drawable icon = null;
                final ResolveInfo storeResolveInfo = MainApplication.getStoreViewResolveInfo(mContext);
                if (storeResolveInfo != null) {
                    icon = storeResolveInfo.loadIcon(mContext.getPackageManager());
                }
                Prompt.show(text, icon, Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
                    @Override
                    public void onActionClick() {
                        MainApplication.openAppStore(mContext, BuildConfig.STORE_FREE_URL);
                    }

                    @Override
                    public void onClose() {
                    }
                });
                if (mListener != null) {
                    mListener.onTweaked();
                }
            }
        }
    }

    public interface Listener {
        public void onTweaked();
    }

    public static void checkForTamper(Context context, Listener listener) {
        new CheckTamperTask(context, listener).execute();
    }

}
