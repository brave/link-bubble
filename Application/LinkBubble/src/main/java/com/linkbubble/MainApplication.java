package com.linkbubble;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.squareup.otto.Bus;

import java.net.MalformedURLException;
import java.net.URL;


public class MainApplication extends Application {

    private Bus mBus;
    public MainDatabaseHelper mDatabaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initModule(this);

        mBus = new Bus();

        mDatabaseHelper = new MainDatabaseHelper(this);
    }

    public Bus getBus() {
        return mBus;
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        Settings.deinitModule();

        super.onTerminate();
    }

    public static void openLink(Context context, String url) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("url", url);
        serviceIntent.putExtra("start_time", System.currentTimeMillis());
        context.startService(serviceIntent);
    }

    public static boolean loadInBrowser(Context context, Intent intent, boolean showToastIfNoBrowser) {
        boolean activityStarted = false;
        ComponentName defaultBrowserComponentName = Settings.get().getDefaultBrowserComponentName(context);
        if (defaultBrowserComponentName != null) {
            intent.setComponent(defaultBrowserComponentName);
            context.startActivity(intent);
            activityStarted = true;
        }

        if (activityStarted == false && showToastIfNoBrowser) {
            Toast.makeText(context, R.string.no_default_browser, Toast.LENGTH_LONG).show();
        }
        return activityStarted;
    }

    public static boolean loadResolveInfoIntent(Context context, ResolveInfo resolveInfo, String url, long startTime) {
        if (resolveInfo.activityInfo != null) {
            return loadIntent(context, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, url, startTime);
        }
        return false;
    }

    public static boolean loadIntent(Context context, String packageName, String className, String url, long startTime) {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setClassName(packageName, className);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.setData(Uri.parse(url));
        context.startActivity(openIntent);
        //Log.d(TAG, "redirect to app: " + resolveInfo.loadLabel(context.getPackageManager()) + ", url:" + url);
        if (startTime > -1) {
            Log.d("LoadTime", "Saved " + ((System.currentTimeMillis()-startTime)/1000) + " seconds.");
        }
        return true;
    }

    public static boolean handleBubbleAction(Context context, Config.BubbleAction action, String url) {
        Config.ActionType actionType = Settings.get().getConsumeBubbleActionType(action);
        if (actionType == Config.ActionType.Share) {
            // TODO: Retrieve the class name below from the app in case Twitter ever change it.
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setClassName(Settings.get().getConsumeBubblePackageName(action),
                    Settings.get().getConsumeBubbleActivityClassName(action));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, url);
            context.startActivity(intent);
            return true;
        } else if (actionType == Config.ActionType.View) {
            return MainApplication.loadIntent(context, Settings.get().getConsumeBubblePackageName(action),
                    Settings.get().getConsumeBubbleActivityClassName(action), url, -1);
        }
        return false;
    }

    public static void saveUrlInHistory(Context context, ResolveInfo resolveInfo, String url, String title) {
        saveUrlInHistory(context, resolveInfo, url, null, title);
    }

    public static void saveUrlInHistory(Context context, ResolveInfo resolveInfo, String url, String host, String title) {

        if (host == null) {
            try {
            URL _url = new URL(url);
            host = _url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        HistoryRecord historyRecord = new HistoryRecord(title, url, host, System.currentTimeMillis());

        MainApplication app = (MainApplication) context.getApplicationContext();

        app.mDatabaseHelper.addHistoryRecord(historyRecord);
        app.getBus().post(new HistoryRecord.ChangedEvent(historyRecord));

    }
}
