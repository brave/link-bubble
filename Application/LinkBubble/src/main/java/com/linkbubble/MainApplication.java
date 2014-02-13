package com.linkbubble;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Vibrator;
import android.widget.Toast;
import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.ui.Prompt;
import com.linkbubble.util.Tamper;
import com.squareup.otto.Bus;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class MainApplication extends Application {

    private Bus mBus;
    public static DatabaseHelper sDatabaseHelper;
    public static ConcurrentHashMap<String, String> sTitleHashMap = new ConcurrentHashMap<String, String>(64);
    public static Favicons sFavicons;
    public static DRM sDrm;

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initModule(this);
        Prompt.initModule(this);

        mBus = new Bus();

        sDatabaseHelper = new DatabaseHelper(this);

        Favicons.attachToContext(this);
        recreateFaviconCache();

        sDrm = new DRM(this);
    }

    public static void checkForProVersion(Context context) {
        if (DRM.isLicensed() == false) {
            if (sDrm != null && sDrm.mProServiceBound == false) {
                if (sDrm.bindProService(context)) {
                    sDrm.requestLicenseStatus();
                }
            }
        }
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

        sDrm.onDestroy();
        sDrm = null;

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
        if (urls == null || urls.length == 0 || Tamper.isTweaked(context)) {
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

    public static boolean openInBrowser(Context context, String urlAsString, boolean showToastIfNoBrowser) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urlAsString));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return MainApplication.openInBrowser(context, intent, showToastIfNoBrowser);
    }

    public static boolean loadResolveInfoIntent(Context context, ResolveInfo resolveInfo, String url, long urlLoadStartTime) {
        if (resolveInfo.activityInfo != null) {
            return loadIntent(context, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, url, urlLoadStartTime);
        }
        return false;
    }

    public static boolean loadIntent(Context context, String packageName, String className, String urlAsString, long urlLoadStartTime) {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setClassName(packageName, className);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.setData(Uri.parse(urlAsString));
        context.startActivity(openIntent);
        //Log.d(TAG, "redirect to app: " + resolveInfo.loadLabel(context.getPackageManager()) + ", url:" + url);
        if (urlLoadStartTime > -1) {
            Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
        }
        return true;
    }

    public static boolean handleBubbleAction(Context context, Config.BubbleAction action, String url, long totalLoadTime) {
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
            if (totalLoadTime > -1) {
                Settings.get().trackLinkLoadTime(totalLoadTime, Settings.LinkLoadType.ShareToOtherApp, url);
            }
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

    public static ResolveInfo getStoreViewResolveInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent queryIntent = new Intent();
        queryIntent.setAction(Intent.ACTION_VIEW);
        queryIntent.setData(Uri.parse(BuildConfig.STORE_PRO_URL));
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(queryIntent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName.contains(BuildConfig.STORE_PACKAGE)) {
                return resolveInfo;
            }
        }

        return null;
    }

    public static void showUpgradePrompt(final Context context, int stringId) {
        String text = context.getResources().getString(stringId);
        Drawable icon = null;
        final ResolveInfo storeResolveInfo = getStoreViewResolveInfo(context);
        if (storeResolveInfo != null) {
            icon = storeResolveInfo.loadIcon(context.getPackageManager());
        }

        Prompt.show(text, icon, Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
            @Override
            public void onClick() {
                if (storeResolveInfo != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(BuildConfig.STORE_PRO_URL));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.setClassName(storeResolveInfo.activityInfo.packageName, storeResolveInfo.activityInfo.name);
                    context.startActivity(intent);
                }
            }

            @Override
            public void onClose() {

            }
        });
    }

    // DRM state
    public static class StateChangedEvent {
        public int mState;
        public int mOldState;
    }
}
