/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.linkbubble.Constant.BubbleAction;
import com.linkbubble.adblock.ABPFilterParser;
import com.linkbubble.adblock.TPFilterParser;
import com.linkbubble.adinsert.AdInserter;
import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.ui.Prompt;
import com.linkbubble.ui.SearchURLSuggestionsContainer;
import com.linkbubble.ui.SettingsActivity;
import com.linkbubble.ui.SettingsMoreActivity;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.IconCache;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import io.fabric.sdk.android.Fabric;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class MainApplication extends Application {

    private Bus mBus;
    public static DatabaseHelper sDatabaseHelper;
    public static SearchURLSuggestionsContainer sSearchURLSuggestionsContainer;
    public static ConcurrentHashMap<String, String> sTitleHashMap = new ConcurrentHashMap<String, String>(64);
    public static Favicons sFavicons;
    public static boolean sShowingAppPickerDialog = false;
    private static long sTrialStartTime = -1;

    private ABPFilterParser mABPParser = null;
    private TPFilterParser mTPParser = null;
    private AdInserter mADInserter = null;
    public boolean mAdInserterEnabled = false;

    public IconCache mIconCache;

    public static String sLastLoadedUrl;
    public static long sLastLoadedTime;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        mBus = new Bus();

        registerForBus(this, this);

        Settings.initModule(this);
        Prompt.initModule(this);

        sDatabaseHelper = new DatabaseHelper(this);
        sSearchURLSuggestionsContainer = new SearchURLSuggestionsContainer();

        Analytics.init(this);

        Favicons.attachToContext(this);
        recreateFaviconCache();

        if (Settings.get().isAdBlockEnabled()) {
            mBus.post(new SettingsMoreActivity.AdBlockTurnOnEvent());
        }
        if (Settings.get().isTrackingProtectionEnabled()) {
            mBus.post(new SettingsMoreActivity.TrackingProtectionTurnOnEvent());
        }
        // Enable ad insertion for Crashlytics builds and disable for play store builds
        ApplicationInfo appInfo = getApplicationInfo();
        if (appInfo.packageName.equals("com.brave.playstore") || appInfo.packageName.equals("com.brave.playstore.dev")) {
            mAdInserterEnabled = true;
            new DownloadAdInsertionDataAsyncTask().execute();
        }

        CrashTracking.log("MainApplication.onCreate()");
        //checkStrings();
    }

    public Bus getBus() {
        return mBus;
    }

    public void createTrackingProtectionList() {
        if (null == mTPParser) {
            mTPParser = new TPFilterParser(this);
        }
    }

    public TPFilterParser getTrackingProtectionList() {
        return mTPParser;
    }

    public void createABPParser() {
        // Lazy load ABPFilterParser so that if it is disabled we don't even read the binary data
        // to initialize the library.
        if (mABPParser == null) {
            mABPParser = new ABPFilterParser(this);
        }
    }

    public ABPFilterParser getABPParser() {
        return mABPParser;
    }

    public void createAdInsertionList() {
        if (null == mADInserter) {
            mADInserter = new AdInserter(this);
        }
    }

    public AdInserter getAdInserter() {
        return mADInserter;
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

    public static void openLink(Context context, String url, String openedFromAppName) {
        openLink(context, url, false, openedFromAppName);
    }

    public static boolean openLink(Context context, String url, boolean checkLastAppLoad, String openedFromAppName) {
        long time = System.currentTimeMillis();

        context = context.getApplicationContext();

        if (checkLastAppLoad) {
            /*
            long timeDiff = time - sLastLoadedTime;
            boolean earlyExit = false;
            if (timeDiff < 3000 && sLastLoadedUrl != null && sLastLoadedUrl.equals(url)) {
                Toast.makeText(context, "DOUBLE TAP!!", Toast.LENGTH_SHORT).show();
                earlyExit = true;
            }
            sLastLoadedUrl = url;
            sLastLoadedTime = time;

            if (earlyExit) {
                return false;
            }*/
        }

        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", "open");
        serviceIntent.putExtra("url", url);
        serviceIntent.putExtra("start_time", time);
        serviceIntent.putExtra("openedFromAppName", openedFromAppName);
        context.startService(serviceIntent);

        return true;
    }

    public static void checkRestoreCurrentTabs(Context _context) {
        // Don't restore tabs if we've already got tabs open, #389
        final Context context = _context.getApplicationContext();
        if (MainController.get() == null) {
            final Vector<String> urls = Settings.get().loadCurrentTabs();
            int urlCount = urls.size();
            if (urlCount > 0) {
                String message = context.getResources().getQuantityString(R.plurals.restore_tabs_from_previous_session, urlCount, urlCount);
                Prompt.show(message,
                        context.getResources().getString(android.R.string.ok),
                        Prompt.LENGTH_LONG,
                        new Prompt.OnPromptEventListener() {

                    boolean mOnActionClicked = false;

                    @Override
                    public void onActionClick() {
                        MainApplication.restoreLinks(context, urls.toArray(new String[urls.size()]));
                        mOnActionClicked = true;
                    }

                    @Override
                    public void onClose() {
                        if (mOnActionClicked == false) {
                            Settings.get().saveCurrentTabs(null);
                        }
                    }
                });
            }
        }
    }

    public static void restoreLinks(Context context, String [] urls) {
        context = context.getApplicationContext();
        if (urls == null || urls.length == 0) {
            return;
        }
        CrashTracking.log("MainApplication.restoreLinks(), urls.length:" + urls.length);
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
            CrashTracking.log("MainApplication.openInBrowser()");
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
            return loadIntent(context, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, url, urlLoadStartTime, true);
        }
        return false;
    }

    public static boolean loadIntent(Context context, String packageName, String className, String urlAsString, long urlLoadStartTime, boolean toastOnError) {

        Intent openIntent = new Intent(Intent.ACTION_VIEW);

        try {
            openIntent.setClassName(packageName, className);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openIntent.setData(Uri.parse(urlAsString));
            context.startActivity(openIntent);
            //Log.d(TAG, "redirect to app: " + resolveInfo.loadLabel(context.getPackageManager()) + ", url:" + url);
            if (urlLoadStartTime > -1) {
                Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
            }
            CrashTracking.log("MainApplication.loadIntent()");
            return true;
        } catch (Exception ex) {
            // We want to catch SecurityException || ActivityNotFoundException
            openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setPackage(packageName);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openIntent.setData(Uri.parse(urlAsString));
            try {
                context.startActivity(openIntent);
                if (urlLoadStartTime > -1) {
                    Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
                }
                CrashTracking.log("MainApplication.loadIntent() [2]");
                return true;
            } catch (SecurityException ex2) {
                if (toastOnError) {
                    Toast.makeText(context, R.string.unable_to_launch_app, Toast.LENGTH_SHORT).show();
                }
                return false;
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (toastOnError) {
                    Toast.makeText(context, R.string.unable_to_launch_app, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
    }

    public static boolean loadIntent(Context context, Intent intent, String urlAsString, long urlLoadStartTime) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
        //Log.d(TAG, "redirect to app: " + resolveInfo.loadLabel(context.getPackageManager()) + ", url:" + url);
        if (urlLoadStartTime > -1) {
            Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
        }
        return true;
    }

    public static boolean handleBubbleAction(final Context context, BubbleAction action, final String urlAsString, long totalTrackedLoadTime) {
        Constant.ActionType actionType = Settings.get().getConsumeBubbleActionType(action);
        boolean result = false;
        if (actionType == Constant.ActionType.Share) {
            String consumePackageName = Settings.get().getConsumeBubblePackageName(action);
            CrashTracking.log("MainApplication.handleBubbleAction() action:" + action.toString() + ", consumePackageName:" + consumePackageName);
            String consumeName = Settings.get().getConsumeBubbleActivityClassName(action);

            if (consumePackageName.equals(BuildConfig.APPLICATION_ID) && consumeName.equals(Constant.SHARE_PICKER_NAME)) {
                AlertDialog alertDialog = ActionItem.getShareAlert(context, false, new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(Intent.EXTRA_TEXT, urlAsString);
                        String title = MainApplication.sTitleHashMap != null ? MainApplication.sTitleHashMap.get(urlAsString) : null;
                        if (title != null) {
                            intent.putExtra(Intent.EXTRA_SUBJECT, title);
                        }
                        context.startActivity(intent);
                    }
                });
                Util.showThemedDialog(alertDialog);
                return true;
            }

            // TODO: Retrieve the class name below from the app in case Twitter ever change it.
            Intent intent = Util.getSendIntent(consumePackageName, consumeName, urlAsString);
            try {
                context.startActivity(intent);
                if (totalTrackedLoadTime > -1) {
                    Settings.get().trackLinkLoadTime(totalTrackedLoadTime, Settings.LinkLoadType.ShareToOtherApp, urlAsString);
                }
                result = true;
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(context, R.string.consume_activity_not_found, Toast.LENGTH_LONG).show();
            } catch (SecurityException ex) {
                Toast.makeText(context, R.string.consume_activity_security_exception, Toast.LENGTH_SHORT).show();
            }
        } else if (actionType == Constant.ActionType.View) {
            String consumePackageName = Settings.get().getConsumeBubblePackageName(action);
            CrashTracking.log("MainApplication.handleBubbleAction() action:" + action.toString() + ", consumePackageName:" + consumePackageName);
            result = MainApplication.loadIntent(context, consumePackageName,
                    Settings.get().getConsumeBubbleActivityClassName(action), urlAsString, -1, true);
        } else if (action == BubbleAction.Close || action == BubbleAction.BackButton) {
            CrashTracking.log("MainApplication.handleBubbleAction() action:" + action.toString());
            result = true;
        }

        if (result) {
            boolean hapticFeedbackEnabled = android.provider.Settings.System.getInt(context.getContentResolver(),
                    android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
            if (hapticFeedbackEnabled && action != BubbleAction.BackButton) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(10);
                }
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

    private static final String sIgnoreClassName = MainController.DraggableBubbleMovedEvent.class.getSimpleName();
    private static String sLastPostClassName = "";
    public static void postEvent(Context context, Object event) {
        MainApplication app = (MainApplication) context.getApplicationContext();
        String simpleName = event.getClass().getSimpleName();
        if (sLastPostClassName.equals(sIgnoreClassName) == false
                || sLastPostClassName.equals(sIgnoreClassName) == false) {
            CrashTracking.log("post(" + simpleName + ")");
            sLastPostClassName = simpleName;
        }
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

    public static Intent getStoreIntent(Context context, String storeProUrl) {
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(storeProUrl));
        List<ResolveInfo> infos = manager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : infos) {
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                if (info.activityInfo.packageName.equals(BuildConfig.STORE_PACKAGE)) {
                    Intent result = new Intent(Intent.ACTION_VIEW);
                    result.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                    result.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    result.setData(Uri.parse(storeProUrl));
                    return result;
                }
            }
        }

        return null;
    }

    public static void openAppStore(Context context, String url) {
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        List<ResolveInfo> infos = manager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : infos) {
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                if (info.activityInfo.packageName.equals(BuildConfig.STORE_PACKAGE)) {
                    MainApplication.loadIntent(context, info.activityInfo.packageName, info.activityInfo.name, url, -1, true);
                    return;
                }
            }
        }
    }

    // DRM state
    public static class StateChangedEvent {
        public int mState;
        public int mOldState;
        public boolean mDisplayToast;
        public boolean mDisplayedToast;
    }

    public static class CheckStateEvent {}

    @SuppressWarnings("unused")
    @Subscribe
    public void onCheckStateEvent(CheckStateEvent event) {

    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onIncognitoModeChanged(SettingsActivity.IncognitoModeChangedEvent event) {
        if (null == event.mainController) {
            return;
        }
        event.mainController.updateIncognitoMode(event.mIncognito);
    }

    class DownloadAdBlockDataAsyncTask extends AsyncTask<Void,Void,Long> {

        protected Long doInBackground(Void... params) {
            createABPParser();

            return null;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAdBlockOn(SettingsMoreActivity.AdBlockTurnOnEvent event) {
        new DownloadAdBlockDataAsyncTask().execute();;
    }

    class DownloadTrackingProtectionDataAsyncTask extends AsyncTask<Void,Void,Long> {

        protected Long doInBackground(Void... params) {
            createTrackingProtectionList();

            return null;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onTrackingProtectionOn(SettingsMoreActivity.TrackingProtectionTurnOnEvent event) {
        new DownloadTrackingProtectionDataAsyncTask().execute();
    }

    class DownloadAdInsertionDataAsyncTask extends AsyncTask<Void,Void,Long> {

        protected Long doInBackground(Void... params) {
            createAdInsertionList();

            return null;
        }
    }
/*
    private void checkStrings() {
        String blerg = "blerg";
        String[] langs = {"ar", "cs", "cs-rCZ", "da", "de", "es", "fr", "hi", "hu-rHU", "it", "ja-rJP", "nl", "pl-rPL",
                "pt-rBR", "pt-rPT", "ru", "sv", "th", "th-rTH", "tr", "zh-rCN", "zh-rTW",
                "af-rZA", "he", "id", "ko", "no", "sk-rSK", "tr-rCY", "zh-rHK" };
        for (String lang : langs) {
            Util.setLocale(this, lang);
            Log.e("langcheck", "setLocale():" + lang);
            Log.d("langcheck", String.format(getString(R.string.untrusted_certificate), blerg));
            Log.d("langcheck", String.format(getString(R.string.add_domain_error), blerg));
            Log.d("langcheck", String.format(getString(R.string.remove_default_message), blerg, blerg));
            Log.d("langcheck", String.format(getString(R.string.action_open_in_browser), blerg));
            Log.d("langcheck", String.format(getString(R.string.link_redirected), blerg));
            Log.d("langcheck", String.format(getString(R.string.long_press_unsupported_default_browser), blerg));
            Log.d("langcheck", String.format(getString(R.string.unsupported_scheme_default_browser), blerg));
            Log.d("langcheck", String.format(getString(R.string.unsupported_drop_down_default_browser), blerg));
            Log.d("langcheck", String.format(getString(R.string.requesting_location_message), blerg));
            Log.d("langcheck", String.format(getString(R.string.link_loaded_with_app), blerg));
            Log.d("langcheck", String.format(getString(R.string.undo_close_tab_title), blerg));
            Log.d("langcheck", getResources().getQuantityString(R.plurals.restore_tabs_from_previous_session, 1, 1));
            Log.d("langcheck", getResources().getQuantityString(R.plurals.restore_tabs_from_previous_session, 2, 2));
            Log.d("langcheck", String.format(getString(R.string.search_for_with), blerg, blerg));
        }
    }*/
}





