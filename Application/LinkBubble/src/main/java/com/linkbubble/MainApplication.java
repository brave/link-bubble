package com.linkbubble;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.ui.Prompt;
import com.squareup.otto.Bus;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;


public class MainApplication extends Application {

    private Bus mBus;
    public static DatabaseHelper sDatabaseHelper;
    public static ConcurrentHashMap<String, String> sTitleHashMap = new ConcurrentHashMap<String, String>(64);
    public static Favicons sFavicons;


    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initModule(this);
        Prompt.initModule(this);

        mBus = new Bus();

        sDatabaseHelper = new DatabaseHelper(this);

        Favicons.attachToContext(this);
        recreateFaviconCache();
    }

    public Bus getBus() {
        return mBus;
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        Prompt.deinitModule();
        Settings.deinitModule();

        sFavicons.close();
        sFavicons = null;

        super.onTerminate();
    }

    public static void recreateFaviconCache() {
        if (sFavicons != null) {
            sFavicons.close();
        }

        sFavicons = new Favicons();
    }

    public static void openLink(Context context, String url) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", "open");
        serviceIntent.putExtra("url", url);
        serviceIntent.putExtra("start_time", System.currentTimeMillis());
        context.startService(serviceIntent);
    }

    public static void restoreLinks(Context context, String [] urls) {
        if (urls == null || urls.length == 0) {
            return;
        }
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", "restore");
        serviceIntent.putExtra("urls", urls);
        serviceIntent.putExtra("start_time", System.currentTimeMillis());
        context.startService(serviceIntent);
    }

    public static boolean openInBrowser(Context context, Intent intent, boolean showToastIfNoBrowser) {
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
        boolean result = false;
        if (actionType == Config.ActionType.Share) {
            // TODO: Retrieve the class name below from the app in case Twitter ever change it.
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setClassName(Settings.get().getConsumeBubblePackageName(action),
                    Settings.get().getConsumeBubbleActivityClassName(action));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, url);
            context.startActivity(intent);
            result = true;
        } else if (actionType == Config.ActionType.View) {
            result = MainApplication.loadIntent(context, Settings.get().getConsumeBubblePackageName(action),
                    Settings.get().getConsumeBubbleActivityClassName(action), url, -1);
        } else if (action == Config.BubbleAction.Close) {
            result = true;
        }

        if (result) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(17);
            }
        }

        return result;
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
        sDatabaseHelper.addHistoryRecord(historyRecord);
        app.getBus().post(new HistoryRecord.ChangedEvent(historyRecord));
    }

    public static void postEvent(Context context, Object event) {
        MainApplication app = (MainApplication) context.getApplicationContext();
        app.getBus().post(event);
    }

    public static void registerForBus(Context context, Object object) {
        MainApplication app = (MainApplication) context.getApplicationContext();
        app.getBus().register(object);
    }

    public static void unregisterForBus(Context context, Object object) {
        MainApplication app = (MainApplication) context.getApplicationContext();
        app.getBus().unregister(object);
    }

    public static void copyLinkToClipboard(Context context, String urlAsString, int string) {
        ClipboardManager clipboardManager = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            ClipData clipData = ClipData.newPlainText("url", urlAsString);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
        }
    }
}
