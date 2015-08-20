package com.linkbubble;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.linkbubble.ui.TabView;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

public class Settings {

    public static final String PREFERENCE_ENABLED = "preference_enabled";
    public static final String PREFERENCE_CHECK_FOR_YOUTUBE_EMBEDS = "preference_scan_for_youtube_embeds";
    public static final String PREFERENCE_IGNORE_LINKS_FROM = "preference_ignore_links_from";

    public static final String PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED = "preference_auto_content_display_link_loaded";
    public static final boolean PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED_DEFAULT = false;
    public static final String PREFERENCE_SHOW_UNDO_CLOSE_TAB = "preference_show_undo_close_tab_prompt";

    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE = "preference_left_consume_bubble";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_left_consume_bubble_package_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_left_consume_bubble_activity_class_name";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL = "preference_left_consume_bubble_label";
    public static final String PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE = "preference_left_consume_bubble_type";

    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE = "preference_right_consume_bubble";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME = "preference_right_consume_bubble_package_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME = "preference_right_consume_bubble_activity_class_name";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL = "preference_right_consume_bubble_label";
    public static final String PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE = "preference_right_consume_bubble_type";

    //public static final String PREFERENCE_LINK_DOUBLE_TAP = "preference_double_tap";
    //public static final String PREFERENCE_LINK_DOUBLE_TAP_PACKAGE_NAME = "preference_double_tap_package_name";
    //public static final String PREFERENCE_LINK_DOUBLE_TAP_ACTIVITY_CLASS_NAME = "preference_double_tap_activity_class_name";
    //public static final String PREFERENCE_LINK_DOUBLE_TAP_LABEL = "preference_double_tap_bubble_label";
    //public static final String PREFERENCE_LINK_DOUBLE_TAP_TYPE = "preference_double_tap_bubble_type";

    public static final String PREFERENCE_DEFAULT_BROWSER = "preference_default_browser";
    public static final String PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME = "preference_default_browser_package_name";
    public static final String PREFERENCE_DEFAULT_BROWSER_LABEL = "preference_default_browser_bubble_label";

    public static final String KEY_ARTICLE_MODE_PREFERENCE = "preference_article_mode";
    public static final String KEY_ARTICLE_MODE_ON_WEAR_PREFERENCE = "preference_reading_mode_on_wear";

    public static final String PREFERENCE_THEME_DARK = "preference_theme_dark";
    public static final String PREFERENCE_COLORED_PROGRESS_INDICATOR = "preference_colored_progress_indicator";

    public static final String PREFERENCE_CURRENT_TABS = "preference_current_bubbles";
    public static final String PREFERENCE_DEFAULT_APPS = "preference_default_apps";
    public static final String PREFERENCE_FALLBACK_REDIRECT_HOSTS = "preference_redirect_hosts";
    public static final String PREFERENCE_THEME_TOOLBAR = "preference_theme_toolbar";

    public static final String PREFERENCE_AUTO_ARTICLE_MODE = "preference_auto_article_mode";
    public static final String PREFERENCE_INCOGNITO_MODE = "preference_incognito";
    public static final String PREFERENCE_WEBVIEW_BATTERY_SAVING_MODE = "preference_webview_battery_save";

    public static final String PREFERENCE_WEBVIEW_TEXT_ZOOM = "preference_webview_text_zoom2";
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN = 50;
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_DEFAULT = 100;
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_MAX = 250;

    public static final String PREFERENCE_USER_AGENT = "preference_user_agent";

    private static final String SAY_THANKS_CLICKED = "say_thanks_clicked";

    private static final String DEFAULT_APPS_MAP_KEY_HOST = "host";
    private static final String DEFAULT_APPS_MAP_KEY_COMPONENT = "component";

    private static final String BUBBLE_RESTING_X = "bubble_resting_x";
    private static final String BUBBLE_RESTING_Y = "bubble_resting_y";

    private static final String WELCOME_MESSAGE_DISPLAYED = "welcome_message_displayed";
    private static final String TERMS_ACCEPTED = "terms_accepted";
    private static final String LAST_FLUSH_WEBVIEW_CACHE_TIME = "last_flush_cache_time";

    public enum WebViewBatterySaveMode {
        Aggressive,
        Default,
        Off,
    }

    enum ColorTheme {
        Light,
        Dark,
        Palette,
    }

    /*
	 *
	 */
    public static void initModule(Context context) {
        mInstance = new Settings(context);
    }

    /*
     *
     */
    public static void deinitModule() {
        mInstance = null;
    }

    /*
     *
     */
    public static Settings get()	{
        return mInstance;
    }

    private static Settings mInstance = null;

    public static class OnConsumeBubblesChangedEvent {}

    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private TreeMap<String, String> mDefaultAppsMap = new TreeMap<String, String>();
    private List<Intent> mBrowsers;
    private List<String> mBrowserPackageNames;
    private ResolveInfo mYouTubeViewResolveInfo;
    public ResolveInfo mLinkBubbleEntryActivityResolveInfo;
    private boolean mCheckedForYouTubeResolveInfo = false;
    private List<String> mIgnoreLinksFromPackageNames;
    private WebViewBatterySaveMode mWebViewBatterySaveMode;
    // The point to save
    private Point mBubbleRestingPoint = new Point();
    // The point used as the return value. Required so we don't overwrite the desired point in landscape mode
    private Point mTempBubbleRestingPoint = new Point();

    private HashSet<String> mFallbackRedirectHosts = new HashSet<>();

    static public int COLOR_TEXT_DARK;
    static public int COLOR_TEXT_LIGHT;
    static public int COLOR_CONTENT_VIEW_DARK;
    static public int COLOR_CONTENT_VIEW_LIGHT;
    static public int COLOR_PROGRESS_DARK;
    static public int COLOR_PROGRESS_LIGHT;
    static public int COLOR_WHITE;
    static public int COLOR_BLACK;

    Settings(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        //mDownloadHandlerComponentName = new ComponentName(mContext, DownloadHandlerActivity.class);

        COLOR_WHITE = mContext.getResources().getColor(android.R.color.white);
        COLOR_BLACK = mContext.getResources().getColor(android.R.color.black);
        COLOR_TEXT_DARK  = mContext.getResources().getColor(R.color.color_text_dark);
        COLOR_TEXT_LIGHT = mContext.getResources().getColor(R.color.color_text_light);
        COLOR_CONTENT_VIEW_DARK = mContext.getResources().getColor(R.color.color_content_view_dark);
        COLOR_CONTENT_VIEW_LIGHT = mContext.getResources().getColor(R.color.color_content_view_light);
        COLOR_PROGRESS_DARK = mContext.getResources().getColor(R.color.color_progress_default_dark);
        COLOR_PROGRESS_LIGHT = mContext.getResources().getColor(R.color.color_progress_default_light);

        checkForVersionUpgrade();

        setDefaultRightConsumeBubble();
        setDefaultLeftConsumeBubble();

        if (mSharedPreferences.getBoolean("first_run", true)) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean("first_run", false);
            editor.putLong(LAST_FLUSH_WEBVIEW_CACHE_TIME, System.currentTimeMillis());
            editor.commit();

            PackageManager packageManager = mContext.getPackageManager();

            configureDefaultApp(packageManager, "https://www.youtube.com/watch?v=_Aj-PRdU7xA", "com.google.android.youtube");
            configureDefaultApp(packageManager, "https://plus.google.com/+LinkBubble/posts/RdMoBbbjPUi", "com.google.android.apps.plus");
            configureDefaultApp(packageManager, "https://play.google.com/store/apps/details?id=com.linkbubble.playstore&hl=en", "com.android.vending");
            configureDefaultApp(packageManager, "https://maps.google.com/maps/ms?msid=212078515518849153944.000434d59f7fc56a57668", "com.google.android.apps.maps");
            saveDefaultApps();
        } else {
            // This option is being added in 1.3. For people upgrading from older versions, set the value to force a clear very soon
            if (mSharedPreferences.getLong(LAST_FLUSH_WEBVIEW_CACHE_TIME, -1) == -1) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putLong(LAST_FLUSH_WEBVIEW_CACHE_TIME, System.currentTimeMillis() - Constant.EMPTY_WEBVIEW_CACHE_INTERVAL);
                editor.apply();
            }
        }

        configureDefaultApps(mSharedPreferences.getString(PREFERENCE_DEFAULT_APPS, null));
        mBubbleRestingPoint.set(-1, -1);

        loadLinkLoadStats();
        loadRecentAppRedirects();
        loadIgnoreLinksFromPackageNames();

        setWebViewBatterySaveMode(mSharedPreferences.getString(PREFERENCE_WEBVIEW_BATTERY_SAVING_MODE, "default"));

        HashSet<String> defaultRedirects = new HashSet<>();
        defaultRedirects.add("accounts.google.com");
        configureFallbackRedirectHosts(mSharedPreferences.getStringSet(PREFERENCE_FALLBACK_REDIRECT_HOSTS, defaultRedirects));
    }

    private void checkForVersionUpgrade() {
        final String key = "lastUpgradeVersion";
        int lastUpgradeVersion = mSharedPreferences.getInt(key, -1);
        int upgradeVersionToSet = -1;
        if (lastUpgradeVersion < 1) {
            // Remove all defaults to TapPath
            String defaultAppsAsString = mSharedPreferences.getString(PREFERENCE_DEFAULT_APPS, null);

            try {
                if (defaultAppsAsString != null) {
                    JSONArray defaultApps = new JSONArray(defaultAppsAsString);
                    mDefaultAppsMap.clear();

                    boolean tapPathFound = false;

                    for (int i = 0; i < defaultApps.length(); i++) {
                        try {
                            JSONObject object = defaultApps.getJSONObject(i);
                            String host = object.getString(DEFAULT_APPS_MAP_KEY_HOST);
                            String flattenedName = object.getString(DEFAULT_APPS_MAP_KEY_COMPONENT);
                            if (flattenedName.contains(BuildConfig.TAP_PATH_PACKAGE_NAME) == false) {
                                mDefaultAppsMap.put(host, flattenedName);
                            } else {
                                tapPathFound = true;
                                //Log.d("blerg", "ignore " + host + ", " + flattenedName);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (tapPathFound) {
                        saveDefaultApps();
                    }
                }
            } catch (JSONException e) {
                mDefaultAppsMap.clear();
            }

            upgradeVersionToSet = 1;
        }

        if (upgradeVersionToSet > -1) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putInt(key, upgradeVersionToSet);
            editor.apply();
        }
    }

    private void configureDefaultApp(PackageManager packageManager, String urlAsString, String desiredPackageName) {
        try {
            URL url = new URL(urlAsString);
            final List<ResolveInfo> resolveInfos = getAppsThatHandleUrl(url.toString(), packageManager);

            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo != null) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    if (packageName.equals(BuildConfig.APPLICATION_ID) == false && packageName.contains(desiredPackageName)) {
                        setDefaultApp(url.toString(), resolveInfo, false);
                        return;
                    }
                }
            }
        } catch (MalformedURLException ex) {

        }
    }

    public void saveData() {
        saveLinkLoadStats();
        saveRecentAppRedirects();
    }

    public void onOrientationChange() {
        int bubbleRestingX = mBubbleRestingPoint.x;
        if (bubbleRestingX == -1) {
            bubbleRestingX = mSharedPreferences.getInt(BUBBLE_RESTING_X, -1);
            if (bubbleRestingX == -1) {
                bubbleRestingX = Config.mBubbleSnapLeftX;
            }
        }
        if (bubbleRestingX < Config.mScreenCenterX) {
            bubbleRestingX = Config.mBubbleSnapLeftX;
        } else {
            bubbleRestingX = Config.mBubbleSnapRightX;
        }

        int bubbleRestingY = mBubbleRestingPoint.y;
        if (bubbleRestingY == -1) {
            bubbleRestingY = mSharedPreferences.getInt(BUBBLE_RESTING_Y, -1);
            if (bubbleRestingY == -1) {
                bubbleRestingY = (int) (Config.mScreenHeight * 0.35f);
            }
        }

        mBubbleRestingPoint.set(bubbleRestingX, bubbleRestingY);
    }

    public void updateBrowsers() {
        if (mBrowsers == null) {
            mBrowsers = new Vector<Intent>();
            mBrowserPackageNames = new ArrayList<String>();
        } else {
            mBrowsers.clear();
            mBrowserPackageNames.clear();
        }
        PackageManager packageManager = mContext.getPackageManager();
        Intent queryIntent = new Intent();
        queryIntent.setAction(Intent.ACTION_VIEW);
        queryIntent.setData(Uri.parse("http://www.fdasfjsadfdsfas.com"));        // Something stupid that no non-browser app will handle
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(queryIntent, PackageManager.GET_RESOLVED_FILTER);
        String fallbackDefaultBrowserPackageName = null;
        String fallbackDefaultBrowserActivityClassName = null;
        for (ResolveInfo resolveInfo : resolveInfos) {
            IntentFilter filter = resolveInfo.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                // Ignore LinkBubble from this list
                if (resolveInfo.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID)) {
                    mLinkBubbleEntryActivityResolveInfo = resolveInfo;
                } else if (Util.isValidBrowserPackageName(resolveInfo.activityInfo.packageName)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    mBrowsers.add(intent);
                    mBrowserPackageNames.add(resolveInfo.activityInfo.packageName);
                    if (fallbackDefaultBrowserPackageName == null) {
                        fallbackDefaultBrowserPackageName = resolveInfo.activityInfo.packageName;
                        fallbackDefaultBrowserActivityClassName = resolveInfo.activityInfo.name;
                    } else if (resolveInfo.activityInfo.packageName.equals("com.android.chrome")) {
                        fallbackDefaultBrowserPackageName = resolveInfo.activityInfo.packageName;
                        fallbackDefaultBrowserActivityClassName = resolveInfo.activityInfo.name;
                    }
                }
            }
        }

        String defaultBrowserPackage = mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, null);
        String rightConsumeBubblePackageName = mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        String leftConsumeBubblePackageName = mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);

        if (fallbackDefaultBrowserPackageName != null) {
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(fallbackDefaultBrowserPackageName, 0);
                String defaultBrowserLabel = packageManager.getApplicationLabel(applicationInfo).toString();

                if (defaultBrowserPackage == null || !doesPackageExist(packageManager, defaultBrowserPackage)) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString(PREFERENCE_DEFAULT_BROWSER_LABEL, defaultBrowserLabel);
                    editor.putString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, fallbackDefaultBrowserPackageName);
                    editor.commit();
                }
                if (leftConsumeBubblePackageName != null && !doesPackageExist(packageManager, leftConsumeBubblePackageName)) {
                    setConsumeBubble(Constant.BubbleAction.ConsumeLeft, Constant.ActionType.View,
                            defaultBrowserLabel,
                            fallbackDefaultBrowserPackageName, fallbackDefaultBrowserActivityClassName);
                }

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean doesPackageExist(PackageManager pm, String targetPackage) {
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private ResolveInfo findResolveInfoForPackageName(List<ResolveInfo> resolveInfos, String packageName) {
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                return resolveInfo;
            }
        }

        return null;
    }

    public ResolveInfo getYouTubeViewResolveInfo() {
        if (mCheckedForYouTubeResolveInfo == false) {
            PackageManager packageManager = mContext.getPackageManager();
            Intent queryIntent = new Intent();
            queryIntent.setAction(Intent.ACTION_VIEW);
            queryIntent.setData(Uri.parse("http://www.youtube.com/watch?v=jNQXAC9IVRw"));
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(queryIntent, PackageManager.GET_RESOLVED_FILTER);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.packageName.contains("com.google.android.youtube")) {
                    mYouTubeViewResolveInfo = resolveInfo;
                    break;
                }
            }
            mCheckedForYouTubeResolveInfo = true;
        }

        return mYouTubeViewResolveInfo;
    }

    private void setDefaultRightConsumeBubble() {
        PackageManager packageManager = mContext.getPackageManager();
        String rightConsumeBubblePackageName = mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        if (rightConsumeBubblePackageName == null
                || (rightConsumeBubblePackageName != null
                    && !rightConsumeBubblePackageName.equals(BuildConfig.APPLICATION_ID)
                    && !doesPackageExist(packageManager, rightConsumeBubblePackageName))) {
            setConsumeBubble(Constant.BubbleAction.ConsumeRight, Constant.ActionType.Share,
                    mContext.getResources().getString(R.string.share_picker_label),
                    BuildConfig.APPLICATION_ID, Constant.SHARE_PICKER_NAME);
        }
    }

    private void setDefaultLeftConsumeBubble() {
        String leftConsumeBubblePackageName = mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        if (leftConsumeBubblePackageName == null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);

            if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, Constant.POCKET_PACKAGE_NAME), packageManager) == false) {
                if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.instapaper.android"), packageManager) == false) {
                    if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.facebook.katana"), packageManager) == false) {
                        if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.twitter.android"), packageManager) == false) {
                            if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.google.android.apps.plus"), packageManager) == false) {
                                if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.google.android.gm"), packageManager) == false) {
                                    // Can't imagine *none* of the above apps will not be installed too often, but if so, fall back to the first item in the list...
                                    setDefaultLeftConsumeBubble(resolveInfos.get(0), packageManager);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean setDefaultLeftConsumeBubble(ResolveInfo resolveInfo, PackageManager packageManager) {
        if (resolveInfo != null) {
            setConsumeBubble(Constant.BubbleAction.ConsumeLeft, Constant.ActionType.Share,
                    resolveInfo.loadLabel(packageManager).toString(),
                    resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            return true;
        }
        return false;
    }

    public List<Intent> getBrowsers() {
        if (mBrowsers == null) {
            updateBrowsers();
        }
        return mBrowsers;
    }

    public List<String> getBrowserPackageNames() {
        if (mBrowserPackageNames == null) {
            updateBrowsers();
        }

        return mBrowserPackageNames;
    }

    public void setDefaultBrowser(String label, String packageName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_DEFAULT_BROWSER_LABEL, label);
        editor.putString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, packageName);
        editor.commit();
    }

    public String getDefaultBrowserLabel() {
        return mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_LABEL, null);
    }

    String getDefaultBrowserPackageName() {
        return mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, null);
    }

    ComponentName getDefaultBrowserComponentName(Context context) {
        String defaultBrowserPackageName = getDefaultBrowserPackageName();
        if (defaultBrowserPackageName != null) {
            Intent browserIntent = context.getPackageManager().getLaunchIntentForPackage(defaultBrowserPackageName);
            if (browserIntent != null) {
                return browserIntent.getComponent();
            }
        }
        return null;
    }

    public Drawable getDefaultBrowserIcon(Context context) {
        ComponentName componentName = getDefaultBrowserComponentName(context);
        if (componentName != null) {
            try {
                return context.getPackageManager().getActivityIcon(componentName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }

        return null;
    }

    public void setConsumeBubble(Constant.BubbleAction action, Constant.ActionType type, String label, String packageName, String activityClassName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        switch (action) {
            case ConsumeLeft:
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                editor.putString(PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE, type.name());
                break;

            case ConsumeRight:
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, label);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, packageName);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, activityClassName);
                editor.putString(PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE, type.name());
                break;

            //case LinkDoubleTap:
            //    editor.putString(PREFERENCE_LINK_DOUBLE_TAP_LABEL, label);
            //    editor.putString(PREFERENCE_LINK_DOUBLE_TAP_PACKAGE_NAME, packageName);
            //    editor.putString(PREFERENCE_LINK_DOUBLE_TAP_ACTIVITY_CLASS_NAME, activityClassName);
            //    editor.putString(PREFERENCE_LINK_DOUBLE_TAP_TYPE, type.name());
            //    break;

        }
        editor.commit();

        MainApplication.postEvent(mContext, new OnConsumeBubblesChangedEvent());
    }

    public String getConsumeBubbleLabel(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, null);

            //case LinkDoubleTap:
            //    return mSharedPreferences.getString(PREFERENCE_LINK_DOUBLE_TAP_LABEL, mContext.getString(R.string.not_set));
        }
        return null;
    }

    String getConsumeBubblePackageName(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);

            //case LinkDoubleTap:
            //    return mSharedPreferences.getString(PREFERENCE_LINK_DOUBLE_TAP_PACKAGE_NAME, null);
        }
        return null;
    }

    String getConsumeBubbleActivityClassName(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);

            //case LinkDoubleTap:
            //    return mSharedPreferences.getString(PREFERENCE_LINK_DOUBLE_TAP_ACTIVITY_CLASS_NAME, null);
        }
        return null;
    }

    Constant.ActionType getConsumeBubbleActionType(Constant.BubbleAction action) {
        String actionTypeAsString = null;
        switch (action) {
            case ConsumeLeft:
                actionTypeAsString = mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_TYPE, null);
                break;

            case ConsumeRight:
                actionTypeAsString = mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_TYPE, null);
                break;

            //case LinkDoubleTap:
            //    actionTypeAsString = mSharedPreferences.getString(PREFERENCE_LINK_DOUBLE_TAP_TYPE, null);
            //    break;
        }

        if (actionTypeAsString != null) {
            if (actionTypeAsString.equals(Constant.ActionType.Share.name())) {
                return Constant.ActionType.Share;
            } else if (actionTypeAsString.equals(Constant.ActionType.View.name())) {
                return Constant.ActionType.View;
            }
        }

        return Constant.ActionType.Unknown;
    }

    public Drawable getConsumeBubbleIcon(Constant.BubbleAction action) {
        return getConsumeBubbleIcon(action, true);
    }

    public Drawable getConsumeBubbleIcon(Constant.BubbleAction action, boolean whiteShareIcon) {
        PackageManager packageManager = mContext.getPackageManager();
        try {
            String packageName = getConsumeBubblePackageName(action);
            String name = getConsumeBubbleActivityClassName(action);
            if (packageName != null && name != null) {
                if (name.equals(Constant.SHARE_PICKER_NAME)) {
                    return mContext.getResources().getDrawable(whiteShareIcon ? R.drawable.ic_share_white_24dp : R.drawable.ic_share_grey600_24dp);
                }
                ComponentName componentName = new ComponentName(packageName, name);
                return packageManager.getActivityIcon(componentName);
            }
        } catch (OutOfMemoryError ex) {
        } catch (PackageManager.NameNotFoundException e) {
        }
        return mContext.getResources().getDrawable(R.drawable.ic_launcher);
    }

    private void loadIgnoreLinksFromPackageNames() {
        if (mIgnoreLinksFromPackageNames == null) {
            mIgnoreLinksFromPackageNames = new ArrayList<String>();
        }
        mIgnoreLinksFromPackageNames.clear();

        // Ignore this on L. Hopefully Google reverse this decision...
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            String string = mSharedPreferences.getString(PREFERENCE_IGNORE_LINKS_FROM, null);
            if (string != null) {
                String[] split = string.split(",");
                if (split != null && split.length > 0) {
                    for (String s : split) {
                        mIgnoreLinksFromPackageNames.add(s);
                    }
                }
            }
        }
    }

    public void setIgnoreLinksFromPackageNames(ArrayList<String> packageNames) {

        mIgnoreLinksFromPackageNames.clear();

        String result = "";
        if (packageNames != null && packageNames.size() > 0) {
            for (String packageName : packageNames) {
                result += packageName + ",";
                mIgnoreLinksFromPackageNames.add(packageName);
            }
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_IGNORE_LINKS_FROM, result);
        editor.commit();
    }

    public List<String> getIgnoreLinksFromPackageNames() {
        return mIgnoreLinksFromPackageNames;
    }

    public boolean ignoreLinkFromPackageName(String packageName) {
        for (String ignore : mIgnoreLinksFromPackageNames) {
            if (ignore.equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    public boolean getAutoContentDisplayLinkLoaded() {
        return mSharedPreferences.getBoolean(PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED, PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED_DEFAULT);
    }

    public boolean isIncognitoMode() {
        return mSharedPreferences.getBoolean(PREFERENCE_INCOGNITO_MODE, false);
    }

    public void setWebViewBatterySaveMode(String mode) {
        if (mode.equals("aggressive")) {
            mWebViewBatterySaveMode = WebViewBatterySaveMode.Aggressive;
        } else if (mode.equals("off")) {
            mWebViewBatterySaveMode = WebViewBatterySaveMode.Off;
        } else {
            mWebViewBatterySaveMode = WebViewBatterySaveMode.Default;
        }
    }

    public void setWebViewBatterySaveMode(WebViewBatterySaveMode mode) {
        String value;
        switch (mode) {
            case Off:
                value = "off";
                break;

            case Aggressive:
                value = "aggressive";
                break;

            case Default:
            default:
                value = "default";
                break;
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_WEBVIEW_BATTERY_SAVING_MODE, value);
        editor.apply();

        setWebViewBatterySaveMode(value);
    }

    public WebViewBatterySaveMode getWebViewBatterySaveMode() {
        return mWebViewBatterySaveMode;
    }

    public String getUserAgentString() {
        String defaultUserAgent = mSharedPreferences.getString(PREFERENCE_USER_AGENT, "default");
        if (defaultUserAgent.equals("chrome_phone")) {
            return Constant.USER_AGENT_CHROME_PHONE;
        } else if (defaultUserAgent.equals("chrome_tablet")) {
            return Constant.USER_AGENT_CHROME_TABLET;
        } else if (defaultUserAgent.equals("chrome_desktop")) {
            return Constant.USER_AGENT_CHROME_DESKTOP;
        }

        return null;
    }

    public boolean isEnabled() {
        //return mSharedPreferences.getBoolean(PREFERENCE_ENABLED, false);
        return true;
    }

    public boolean checkForYouTubeEmbeds() {
        return mSharedPreferences.getBoolean(PREFERENCE_CHECK_FOR_YOUTUBE_EMBEDS, false);
    }

    public boolean getSayThanksClicked() {
        return mSharedPreferences.getBoolean(SAY_THANKS_CLICKED, false);
    }

    public void setSayThanksClicked(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SAY_THANKS_CLICKED, value);
        editor.commit();
    }

    void configureFallbackRedirectHosts(Set<String> items) {
        if (items != null && items.size() > 0) {
            // Make a copy to as documentation explicitly states not to trust the result
            // of getStringSet() call. http://stackoverflow.com/a/19949833/328679
            mFallbackRedirectHosts = new HashSet<>(items);
        } else {
            mFallbackRedirectHosts.clear();
        }
    }

    public void addFallbackRedirectHost(String host) {
        mFallbackRedirectHosts.add(host);
        saveFallbackRedirectHosts();
    }

    public void removeFallbackRedirectHost(String host) {
        mFallbackRedirectHosts.remove(host);
        saveFallbackRedirectHosts();
    }

    private void saveFallbackRedirectHosts() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(PREFERENCE_FALLBACK_REDIRECT_HOSTS);      // always remove. See http://stackoverflow.com/a/21401062/328679
        if (mFallbackRedirectHosts.size() > 0) {
            editor.putStringSet(PREFERENCE_FALLBACK_REDIRECT_HOSTS, mFallbackRedirectHosts);
        }
        editor.apply();
    }

    public Set<String> getFallbackRedirectHosts() {
        return mFallbackRedirectHosts;
    }

    public boolean redirectUrlToBrowser(URL url) {
        String host = url.getHost();
        String hostAlt = host.contains("www.") ? host.replace("www.", "") : "www." + host;
        return mFallbackRedirectHosts.contains(host) || mFallbackRedirectHosts.contains(hostAlt);

        /*
         * Temporarily enable DownloadHandlerActivity to see if it might be used to handle this URL. If so, redirect the
         * URL to the default browser rather than allowing an app like ES File Explorer be set as a default app for a host. #338
         */

        /*
        boolean result = false;

        packageManager.setComponentEnabledSetting(mDownloadHandlerComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        mDownloadQueryIntent.setAction(Intent.ACTION_VIEW);
        mDownloadQueryIntent.setData(Uri.parse(url));
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mDownloadQueryIntent, PackageManager.GET_RESOLVED_FILTER);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            String className = DownloadHandlerActivity.class.getName();
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.name.equals(className)) {
                    result = true;
                    break;
                }
            }
        }

        packageManager.setComponentEnabledSetting(mDownloadHandlerComponentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        return result;
                */
    }

    public boolean getAutoArticleMode() {
        //return Constant.ARTICLE_MODE ? false : mSharedPreferences.getBoolean(PREFERENCE_AUTO_ARTICLE_MODE, false);
        return false;
    }

    public int getWebViewTextZoom() {
        return mSharedPreferences.getInt(PREFERENCE_WEBVIEW_TEXT_ZOOM, PREFERENCE_WEBVIEW_TEXT_ZOOM_DEFAULT);
    }

    public void setWebViewTextZoom(int zoom) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_WEBVIEW_TEXT_ZOOM, zoom);
        editor.commit();
    }

    public boolean getShowUndoCloseTab() {
        return mSharedPreferences.getBoolean(PREFERENCE_SHOW_UNDO_CLOSE_TAB, true);
    }

    public List<ResolveInfo> getAppsThatHandleUrl(String urlAsString, PackageManager packageManager) {

        List<Intent> browsers = getBrowsers();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urlAsString));
        List<ResolveInfo> infos = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

        ArrayList<ResolveInfo> results = new ArrayList<ResolveInfo>();

        for (ResolveInfo info : infos) {
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW)) {

                // Check if this item is a browser, and if so, ignore it
                boolean packageOk = true;
                for (Intent browser : browsers) {
                    if (info.activityInfo.packageName.equals(browser.getComponent().getPackageName())) {
                        packageOk = false;
                        break;
                    }
                }

                if (packageOk) {
                    // Ensure TapPath is always ignored
                    if (info.activityInfo.packageName.contains("com.digitalashes.tappath")) {
                        //Log.d("blerg", "ignore " + info.activityInfo.packageName);
                        packageOk = false;
                    } else {
                        // And some special case code for me to ignore alternate builds
                        if (BuildConfig.DEBUG) {
                            if (info.activityInfo.packageName.equals("com.linkbubble.playstore")) {
                                //Log.d("blerg", "ignore " + info.activityInfo.packageName);
                                packageOk = false;
                            }
                        } else {
                            if (info.activityInfo.packageName.equals("com.linkbubble.playstore.dev")) {
                                //Log.d("blerg", "ignore " + info.activityInfo.packageName);
                                packageOk = false;
                            }
                        }
                    }
                }

                if (packageOk) {
                    results.add(info);
                    Log.d("appHandles", info.loadLabel(packageManager) + " for url:" + urlAsString);
                }
            }
        }

        if (results.size() > 0) {
            return results;
        }

        return null;
    }

    public ResolveInfo getDefaultAppForUrl(URL url, List<ResolveInfo> resolveInfos) {
        if (resolveInfos == null || resolveInfos.size() == 0) {
            return null;
        }

        String host = url.getHost();
        if (host.length() > 1) {
            String flattenedComponentName = mDefaultAppsMap.get(host);
            if (flattenedComponentName != null) {
                ComponentName componentName = ComponentName.unflattenFromString(flattenedComponentName);
                if (componentName != null) {
                    for (ResolveInfo resolveInfo : resolveInfos) {
                        if (resolveInfo.activityInfo.packageName.equals(componentName.getPackageName())
                                && resolveInfo.activityInfo.name.equals(componentName.getClassName())) {
                            return resolveInfo;
                        }
                    }

                    if (componentName.getPackageName().equals(mContext.getPackageName())) {
                        return mLinkBubbleEntryActivityResolveInfo;
                    }
                }
            }
        }

        return null;
    }

    public void setDefaultApp(String urlAsString, ResolveInfo resolveInfo) {
        setDefaultApp(urlAsString, resolveInfo, true);
    }

    public void setDefaultApp(String urlAsString, ResolveInfo resolveInfo, boolean save) {
        try {
            URL url = new URL(urlAsString);
            String host = url.getHost();
            if (host.length() > 1) {
                ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                addDefaultApp(host, componentName, save);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void addDefaultApp(String host, ComponentName componentName, boolean save) {

        mDefaultAppsMap.put(host, componentName.flattenToString());

        if (save) {
            saveDefaultApps();
        }
    }

    public void removeDefaultApp(String host) {
        if (mDefaultAppsMap.containsKey(host)) {
            mDefaultAppsMap.remove(host);
            saveDefaultApps();
        }
    }

    public Vector<String> loadCurrentTabs() {
        Vector<String> urls = new Vector<String>();
        String json = mSharedPreferences.getString(PREFERENCE_CURRENT_TABS, "[]");
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i=0 ; i < jsonArray.length() ; ++i) {
                String urlAsString = jsonArray.getString(i);
                if (urlAsString.equals(Constant.WELCOME_MESSAGE_URL) == false) {
                    urls.add(urlAsString);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return urls;
    }

    public void saveCurrentTabs(List<View> bubbles) {
        if (Constant.SAVE_CURRENT_TABS == false) {
            return;
        }

        JSONArray jsonArray = new JSONArray();
        if (bubbles != null) {
            for (View view : bubbles) {
                URL url = ((TabView)view).getUrl();
                if (url.toString().equals(Constant.NEW_TAB_URL) == false) {
                    jsonArray.put(url.toString());
                }
            }
        }

        String currentTabsString = mSharedPreferences.getString(PREFERENCE_CURRENT_TABS, "");
        String newTabsString = jsonArray.toString();

        if (!currentTabsString.equals(newTabsString)) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(PREFERENCE_CURRENT_TABS, newTabsString);
            editor.commit();
        }
    }

    private void saveDefaultApps() {
        JSONArray jsonArray = new JSONArray();
        for (String key : mDefaultAppsMap.keySet()) {
            String component = mDefaultAppsMap.get(key);
            JSONObject object = new JSONObject();
            try {
                object.put(DEFAULT_APPS_MAP_KEY_HOST, key);
                object.put(DEFAULT_APPS_MAP_KEY_COMPONENT, component);
                jsonArray.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_DEFAULT_APPS, jsonArray.toString());
        editor.commit();
    }

    private void configureDefaultApps(String defaultAppsAsString) {
        try {
            if (defaultAppsAsString != null) {
                JSONArray defaultApps = new JSONArray(defaultAppsAsString);
                mDefaultAppsMap.clear();

                for (int i = 0; i < defaultApps.length(); i++) {
                    try {
                        JSONObject object = defaultApps.getJSONObject(i);
                        String host = object.getString(DEFAULT_APPS_MAP_KEY_HOST);
                        String flattenedName = object.getString(DEFAULT_APPS_MAP_KEY_COMPONENT);
                        mDefaultAppsMap.put(host, flattenedName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                mDefaultAppsMap.clear();
            }
        } catch (JSONException e) {
            mDefaultAppsMap.clear();
        }
    }

    public TreeMap<String, ComponentName> getDefaultAppsMap() {
        if (mDefaultAppsMap != null && mDefaultAppsMap.size() > 0) {
            TreeMap<String, ComponentName> result = new TreeMap<String, ComponentName>();
            for (String host : mDefaultAppsMap.keySet()) {
                result.put(host, ComponentName.unflattenFromString(mDefaultAppsMap.get(host)));
            }
            return result;
        }

        return null;
    }

    public Point getBubbleRestingPoint() {
        mTempBubbleRestingPoint.x = mBubbleRestingPoint.x;
        if (mTempBubbleRestingPoint.x > Config.mScreenCenterX) {
            mTempBubbleRestingPoint.x = Config.mBubbleSnapRightX;
        } else {
            mTempBubbleRestingPoint.x = Config.mBubbleSnapLeftX;
        }

        mTempBubbleRestingPoint.y = mBubbleRestingPoint.y;
        int minYPosition = (int) (Config.mScreenHeight * .8f);
        if (mTempBubbleRestingPoint.y > minYPosition) {
            mTempBubbleRestingPoint.y = minYPosition;
        }
        return mTempBubbleRestingPoint;
    }

    public int getBubbleStartingX(Point bubbleRestingPoint) {
        float fromX;
        if (bubbleRestingPoint.x > Config.mScreenCenterX) {
            fromX = Config.mBubbleSnapRightX + Config.mBubbleWidth;
        } else {
            fromX = Config.mBubbleSnapLeftX - Config.mBubbleWidth;
        }
        return (int)fromX;
    }

    public void setBubbleRestingPoint(int x, int y) {
        mBubbleRestingPoint.set(x, y);
    }

    public void saveBubbleRestingPoint() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(BUBBLE_RESTING_X, mBubbleRestingPoint.x);
        editor.putInt(BUBBLE_RESTING_Y, mBubbleRestingPoint.y);
        editor.commit();
    }

    public void setWelcomeMessageDisplayed(boolean displayed) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(WELCOME_MESSAGE_DISPLAYED, displayed);
        editor.commit();
    }

    public boolean getWelcomeMessageDisplayed() {
        return mSharedPreferences.getBoolean(WELCOME_MESSAGE_DISPLAYED, false);
    }

    public void setTermsAccepted(boolean accepted) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(TERMS_ACCEPTED, accepted);
        editor.commit();
    }

    public boolean getTermsAccepted() {
        return mSharedPreferences.getBoolean(TERMS_ACCEPTED, false);
    }

    public boolean debugAutoLoadUrl() {
        return mSharedPreferences.getBoolean("auto_load_url", false);
    }

    public boolean getArticleModeEnabled() {
        return mSharedPreferences.getBoolean(KEY_ARTICLE_MODE_PREFERENCE, true);
    }

    public boolean getArticleModeOnWearEnabled() {
        return mSharedPreferences.getBoolean(KEY_ARTICLE_MODE_ON_WEAR_PREFERENCE, false);
    }

    private static final int APP_CHANGE_ANIM_TIME = 250;

    private static final String TOTAL_TIME_SAVED_KEY = "total_time_saved";
    private static final String TOTAL_LINKS_LOADED_KEY = "total_links_loaded";

    public static final String LOAD_TIME_TAG = "LoadTime";

    public enum LinkLoadType {
        PageLoad,                   // The page was loaded.
        AppRedirectInstant,         // Redirect via MainController (before a ContentView instance is created for this link).
        AppRedirectBrowser,         // Redirect via ContentView.
        ShareToOtherApp,            // Adding to Pocket or sending to Twitter.
        OpenInOtherApp,             // Opening in Chrome.
    }

    long mTotalTimeSaved;
    int mTotalLinksLoaded;

    public static class LinkLoadTimeStatsUpdatedEvent {
    }

    private LinkLoadTimeStatsUpdatedEvent mLinkLoadTimeStatsUpdatedEvent = new LinkLoadTimeStatsUpdatedEvent();

    public void trackLinkLoadTime(long timeSaved, LinkLoadType linkLoadType, String url) {
        switch (linkLoadType) {
            case AppRedirectInstant:
                // Add some time for the time taken to animate to the new app
                timeSaved += APP_CHANGE_ANIM_TIME;
                break;

            case AppRedirectBrowser:
                // Add some time for the time taken to load the browser initially
                timeSaved += APP_CHANGE_ANIM_TIME;
                // Add some time for the time taken to animate to the new app
                timeSaved += APP_CHANGE_ANIM_TIME;
                break;
        }

        mTotalTimeSaved += timeSaved;
        mTotalLinksLoaded++;

        Log.d(LOAD_TIME_TAG, "trackLinkLoadTime() - timeSaved:" + ((float)timeSaved / 1000.f) + " seconds, " + linkLoadType + ", " + url);

        MainApplication.postEvent(mContext, mLinkLoadTimeStatsUpdatedEvent);

        Analytics.trackTimeSaved(timeSaved);
    }

    public long getTotalTimeSaved() {
        return mTotalTimeSaved;
    }

    public long getTimeSavedPerLink() {
        if (mTotalLinksLoaded > 0) {
            return mTotalTimeSaved / mTotalLinksLoaded;
        }
        return -1;
    }

    public void saveLinkLoadStats() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(TOTAL_TIME_SAVED_KEY, mTotalTimeSaved);
        editor.putInt(TOTAL_LINKS_LOADED_KEY, mTotalLinksLoaded);
        editor.commit();
    }

    private void loadLinkLoadStats() {
        mTotalTimeSaved = mSharedPreferences.getLong(TOTAL_TIME_SAVED_KEY, 0);
        mTotalLinksLoaded = mSharedPreferences.getInt(TOTAL_LINKS_LOADED_KEY, 0);
    }


    private static final int RECENT_REDIRECT_TIME_DELTA = (int)(2.5f * 1000);

    private static final String LAST_APP_REDIRECTS = "last_app_redirects";
    private static final String LAST_APP_REDIRECT_KEY_TIME = "time";
    private static final String LAST_APP_REDIRECT_KEY_URL = "url";
    private static final int MAX_LAST_APP_REDIRECT_COUNT = 3;

    private static class LastAppRedirect {
        String mUrl;
        long mTime;
    }

    private List<LastAppRedirect> mLastAppRedirects = new ArrayList<LastAppRedirect>(MAX_LAST_APP_REDIRECT_COUNT);

    // Did we *just* redirect to this URL? We need to store this to fix #276
    public boolean didRecentlyRedirectToApp(String url) {
        long currentTime = System.currentTimeMillis();
        for (LastAppRedirect lastAppRedirect : mLastAppRedirects) {
            long timeDelta = currentTime - lastAppRedirect.mTime;
            if (timeDelta < RECENT_REDIRECT_TIME_DELTA && lastAppRedirect.mUrl.equals(url)) {
                return true;
            }
        }

        return false;
    }

    public void addRedirectToApp(String url) {

        LastAppRedirect record = null;

        // try and find a current record with this url
        for (LastAppRedirect i : mLastAppRedirects) {
            if (i.mUrl.equals(url)) {
                record = i;
                break;
            }
        }

        if (record == null && mLastAppRedirects.size() == MAX_LAST_APP_REDIRECT_COUNT) {
            // Get the oldest record in the list
            for (LastAppRedirect i : mLastAppRedirects) {
                if (record == null) {
                    record = i;
                } else if (i.mTime < record.mTime) {
                    record = i;
                }
            }
        }

        if (record == null) {
            record = new LastAppRedirect();
            mLastAppRedirects.add(record);
        }

        record.mUrl = url;
        record.mTime = System.currentTimeMillis();
    }

    private void saveRecentAppRedirects() {
        JSONArray jsonArray = new JSONArray();
        for (LastAppRedirect lastAppRedirect : mLastAppRedirects) {
            JSONObject object = new JSONObject();
            try {
                object.put(LAST_APP_REDIRECT_KEY_URL, lastAppRedirect.mUrl);
                object.put(LAST_APP_REDIRECT_KEY_TIME, lastAppRedirect.mTime);
                jsonArray.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(LAST_APP_REDIRECTS, jsonArray.toString());
        editor.commit();
    }

    private void loadRecentAppRedirects() {
        mLastAppRedirects.clear();
        String json = mSharedPreferences.getString(LAST_APP_REDIRECTS, "[]");
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i=0 ; i < jsonArray.length() ; ++i) {
                try {
                    JSONObject object = jsonArray.getJSONObject(i);
                    LastAppRedirect lastAppRedirect = new LastAppRedirect();
                    lastAppRedirect.mUrl = object.getString(LAST_APP_REDIRECT_KEY_URL);
                    lastAppRedirect.mTime = object.getLong(LAST_APP_REDIRECT_KEY_TIME);
                    mLastAppRedirects.add(lastAppRedirect);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean canFlushWebViewCache() {
        long lastEmptyTime = mSharedPreferences.getLong(LAST_FLUSH_WEBVIEW_CACHE_TIME, -1);
        if (lastEmptyTime > -1) {
            long delta = System.currentTimeMillis() - lastEmptyTime;
            if (delta > Constant.EMPTY_WEBVIEW_CACHE_INTERVAL) {
                return true;
            }
        }

        return false;
    }

    public void updateLastFlushWebViewCacheTime() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(LAST_FLUSH_WEBVIEW_CACHE_TIME, System.currentTimeMillis());
        editor.commit();
    }

    public boolean getDarkThemeEnabled() {
        return mSharedPreferences.getBoolean(PREFERENCE_THEME_DARK, false);
    }

    public void setDarkThemeEnabled(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREFERENCE_THEME_DARK, value);
        editor.commit();
    }

    public boolean getColoredProgressIndicator() {
        return mSharedPreferences.getBoolean(PREFERENCE_COLORED_PROGRESS_INDICATOR, true);
    }

    public void setColoredProgressIndicator(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREFERENCE_COLORED_PROGRESS_INDICATOR, value);
        editor.commit();
    }

    public int getThemedDefaultProgressColor() {
        if (getDarkThemeEnabled()) {
            return COLOR_PROGRESS_DARK;
        }
        return COLOR_PROGRESS_LIGHT;
    }

    public int getThemedContentViewColor() {
        if (getDarkThemeEnabled()) {
            return COLOR_CONTENT_VIEW_DARK;
        }
        return COLOR_CONTENT_VIEW_LIGHT;
    }

    public int getThemedTextColor() {
        if (getDarkThemeEnabled()) {
            return COLOR_TEXT_DARK;
        }
        return COLOR_TEXT_LIGHT;
    }

    public boolean getThemeToolbar() {
        return mSharedPreferences.getBoolean(PREFERENCE_THEME_TOOLBAR, true);
    }
}
