package com.linkbubble;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.ui.Prompt;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.IconCache;
import com.linkbubble.util.Tamper;
import com.linkbubble.util.Util;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.otto.Bus;
import org.mozilla.gecko.favicons.Favicons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class MainApplication extends Application {

    private Bus mBus;
    public static DatabaseHelper sDatabaseHelper;
    public static ConcurrentHashMap<String, String> sTitleHashMap = new ConcurrentHashMap<String, String>(64);
    public static Favicons sFavicons;
    public static DRM sDrm;
    public static boolean sShowingAppPickerDialog = false;
    private static long sTrialStartTime = -1;

    public IconCache mIconCache;

    public static String sLastLoadedUrl;
    public static long sLastLoadedTime;

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.initModule(this);
        Prompt.initModule(this);

        mBus = new Bus();

        sDatabaseHelper = new DatabaseHelper(this);

        Analytics.init(this);

        Favicons.attachToContext(this);
        recreateFaviconCache();

        sDrm = new DRM(this);

        initParse();
        initTrialStartTime();
    }

    static String TRIAL_START_TIME = "lb_trialStartTime";

    public static class TrialTimeStartTimeReceivedEvent {
        public TrialTimeStartTimeReceivedEvent(long startTime) {
            mStartTime = startTime;
        }
        public long mStartTime;
    }

    private void initTrialStartTime() {
        try {
            long cachedStartTime = sDrm.decryptSharedPreferencesLong(TRIAL_START_TIME, -1);
            if (cachedStartTime != -1) {
                sTrialStartTime = cachedStartTime;
                TrialTimeStartTimeReceivedEvent event = new TrialTimeStartTimeReceivedEvent(sTrialStartTime);
                postEvent(MainApplication.this, event);
                return;
            }

            final Account[] accounts = AccountManager.get(this).getAccounts();
            final String defaultEmail = Util.getDefaultEmail(accounts);
            if (defaultEmail != null) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery(Constant.DATA_TRIAL_ENTRY);
                query.whereEqualTo(Constant.DATA_TRIAL_EMAIL, defaultEmail);
                query.getFirstInBackground(new GetCallback<ParseObject>() {
                    @Override
                    public void done(ParseObject parseObject, com.parse.ParseException e) {
                        if (parseObject != null) {
                            sTrialStartTime = parseObject.getCreatedAt().getTime();
                            Log.d("Trial", "Additional run, sTrialStartTime:" + sTrialStartTime);
                        } else {
                            parseObject = new ParseObject(Constant.DATA_TRIAL_ENTRY);
                            parseObject.put(Constant.DATA_TRIAL_EMAIL, defaultEmail);
                            parseObject.saveInBackground();
                            sTrialStartTime = System.currentTimeMillis();
                            Log.d("Trial", "Initial run, sTrialStartTime:" + sTrialStartTime);
                        }

                        TrialTimeStartTimeReceivedEvent event = new TrialTimeStartTimeReceivedEvent(sTrialStartTime);
                        postEvent(MainApplication.this, event);

                        String encryptedTrialStartTime = sDrm.encryptLong(sTrialStartTime);
                        sDrm.saveToPreferences(TRIAL_START_TIME, encryptedTrialStartTime);
                    }
                });
            }
        } catch (Exception ex) {
        }
    }

    public static boolean isInTrialPeriod() {
        if (sTrialStartTime == -1) {
            return false;
        }

        long currentTimeMillis = System.currentTimeMillis();
        long timeDelay = currentTimeMillis - sTrialStartTime;
        if (timeDelay < Constant.TRIAL_TIME) {
            return true;
        }

        return false;
    }

    public static long getTrialTimeRemaining() {
        if (sTrialStartTime == -1) {
            return -1;
        }

        long currentTime = System.currentTimeMillis();
        long endTrialTime = sTrialStartTime + Constant.TRIAL_TIME;
        if (currentTime < endTrialTime) {
            return endTrialTime - currentTime;
        }

        return -1;
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

    boolean mParseInitialized = false;
    public void initParse() {
        if (mParseInitialized == false) {
            Parse.initialize(this, "S0yKk7mPfPOlNnzCwQRorwiwYZvVlNgOu4Pp1Uu3", "t1O0d847aJbZf7dg1GjqJ7sAFgdShvGMhRiuoy87");
            mParseInitialized = true;
        }
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

    public static void openLink(Context context, String url, String openedFromAppName) {
        openLink(context, url, false, true, openedFromAppName);
    }

    public static boolean openLink(Context context, String url, boolean checkLastAppLoad, boolean doLicenseCheck, String openedFromAppName) {
        long time = System.currentTimeMillis();

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
        serviceIntent.putExtra("doLicenseCheck", doLicenseCheck);
        serviceIntent.putExtra("start_time", time);
        serviceIntent.putExtra("openedFromAppName", openedFromAppName);
        context.startService(serviceIntent);

        return true;
    }

    public static void checkRestoreCurrentTabs(final Context context) {
        // Don't restore tabs if we've already got tabs open, #389
        if (MainController.get() == null) {
            final Vector<String> urls = Settings.get().loadCurrentTabs();
            int urlCount = urls.size();
            if (urlCount > 0 && DRM.allowProFeatures()) {
                String message = context.getResources().getQuantityString(R.plurals.restore_tabs_from_previous_session, urlCount, urlCount);
                Drawable icon = context.getResources().getDrawable(R.drawable.ic_action_redo_white);
                Prompt.show(message, icon, Prompt.LENGTH_SHORT, new Prompt.OnPromptEventListener() {

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
            return loadIntent(context, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name, url, urlLoadStartTime, true);
        }
        return false;
    }

    public static boolean loadIntent(Context context, String packageName, String className, String urlAsString, long urlLoadStartTime, boolean toastOnError) {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setClassName(packageName, className);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.setData(Uri.parse(urlAsString));
        try {
            context.startActivity(openIntent);
            //Log.d(TAG, "redirect to app: " + resolveInfo.loadLabel(context.getPackageManager()) + ", url:" + url);
            if (urlLoadStartTime > -1) {
                Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
            }
            return true;
        } catch (SecurityException ex) {
            openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setPackage(packageName);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openIntent.setData(Uri.parse(urlAsString));
            try {
                context.startActivity(openIntent);
                if (urlLoadStartTime > -1) {
                    Settings.get().trackLinkLoadTime(System.currentTimeMillis() - urlLoadStartTime, Settings.LinkLoadType.AppRedirectBrowser, urlAsString);
                }
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

    public static boolean handleBubbleAction(final Context context, Constant.BubbleAction action, final String urlAsString, long totalTrackedLoadTime) {
        Constant.ActionType actionType = Settings.get().getConsumeBubbleActionType(action);
        boolean result = false;
        if (actionType == Constant.ActionType.Share) {
            String consumePackageName = Settings.get().getConsumeBubblePackageName(action);
            String consumeName = Settings.get().getConsumeBubbleActivityClassName(action);

            if (consumePackageName.equals(BuildConfig.PACKAGE_NAME) && consumeName.equals(Constant.SHARE_PICKER_NAME)) {
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
                alertDialog.show();
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
            }
        } else if (actionType == Constant.ActionType.View) {
            result = MainApplication.loadIntent(context, Settings.get().getConsumeBubblePackageName(action),
                    Settings.get().getConsumeBubbleActivityClassName(action), urlAsString, -1, true);
        } else if (action == Constant.BubbleAction.Close) {
            result = true;
        }

        if (result) {
            boolean hapticFeedbackEnabled = android.provider.Settings.System.getInt(context.getContentResolver(),
                    android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
            if (hapticFeedbackEnabled) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(17);
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

    public static void showUpgradePrompt(final Context context, int stringId, String prompt) {
        showUpgradePrompt(context, context.getResources().getString(stringId), prompt);
    }

    public static void showUpgradePrompt(final Context context, String string, final String prompt) {
        Drawable icon = null;
        final ResolveInfo storeResolveInfo = getStoreViewResolveInfo(context);
        if (storeResolveInfo != null) {
            icon = storeResolveInfo.loadIcon(context.getPackageManager());
        }

        Prompt.show(string, icon, Prompt.LENGTH_LONG, new Prompt.OnPromptEventListener() {
            @Override
            public void onActionClick() {
                if (storeResolveInfo != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(BuildConfig.STORE_PRO_URL));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.setClassName(storeResolveInfo.activityInfo.packageName, storeResolveInfo.activityInfo.name);
                    context.startActivity(intent);
                    Analytics.trackUpgradePromptClicked(prompt);
                }
            }

            @Override
            public void onClose() {

            }
        });
        Analytics.trackUpgradePromptDisplayed(prompt);
    }

    // DRM state
    public static class StateChangedEvent {
        public int mState;
        public int mOldState;
        public boolean mDisplayedToast;
    }
}
