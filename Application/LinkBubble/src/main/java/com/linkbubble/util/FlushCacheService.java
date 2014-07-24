package com.linkbubble.util;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.linkbubble.Constant;
import com.linkbubble.MainController;
import com.linkbubble.Settings;

import java.io.File;

public class FlushCacheService extends Service {

    private static final String TAG = "FlushCacheService";

    public static void doCheck(Context context) {
        // Check if we should flush the WebView cache
        if (Settings.get().canFlushWebViewCache()) {
            Intent serviceIntent = new Intent(context, FlushCacheService.class);
            context.startService(serviceIntent);
            Log.d(TAG, "startService()");
        }
    }

    private Handler mHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashTracking.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (MainController.get() == null) {
                    if (Settings.get().canFlushWebViewCache()) {
                        flushWebViewCache(FlushCacheService.this);
                        Settings.get().updateLastFlushWebViewCacheTime();
                        //Log.d(TAG, "FlushCacheService emptying cache...");
                    }
                } else {
                    //Log.d(TAG, "FlushCacheService does nothing..., mainControler != null");
                }
                stopSelf();
            }
        }, 5000);
        //Log.d(TAG, "FlushCacheService runnable on delay...");

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //Log.d(TAG, "EmptyWebViewCacheService.onDestroy()");
        super.onDestroy();
    }

    /*
     * Delete all WebView related intermediate data
     */
    private static void flushWebViewCache(Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (s.equals("app_webview")
                        || s.equals("app_icons")
                        || s.equals("cache")
                        || s.equals("CachedGeoposition.db")) {
                    deleteDir(new File(appDir, s), null);
                    //Log.e(TAG, "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
                } else if (s.equals("databases")) {
                    deleteDir(new File(appDir, s), "webview");
                    //Log.e(TAG, "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
                }
            }
        }
    }

    private static boolean deleteDir(File dir, String matchSubset) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                if (matchSubset != null) {
                    if (children[i].contains(matchSubset) == false) {
                        continue;
                    }
                }
                boolean success = deleteDir(new File(dir, children[i]), matchSubset);
                //Log.d(TAG, "-- folder:" + children[i]);
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }
}
