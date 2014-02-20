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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import com.linkbubble.ui.DownloadHandlerActivity;
import com.linkbubble.ui.TabView;
import com.linkbubble.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

public class Settings {

    public static final String PREFERENCE_ENABLED = "preference_enabled";
    public static final String PREFERENCE_AUTO_CONTENT_DISPLAY_TYPE = "preference_auto_content_display";
    public static final String PREFERENCE_CHECK_FOR_YOUTUBE_EMBEDS = "preference_scan_for_youtube_embeds";
    public static final String PREFERENCE_INTERCEPT_LINKS_FROM = "preference_intercept_links_from";
    public static final String PREFERENCE_INTERCEPT_LINKS_FROM_APP_NAME = "preference_intercept_links_from_app_name";

    public static final String PREFERENCE_AUTO_CONTENT_DISPLAY_APP_REDIRECT = "preference_auto_content_display_app_redirect";
    public static final boolean PREFERENCE_AUTO_CONTENT_DISPLAY_APP_REDIRECT_DEFAULT = true;
    public static final String PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED = "preference_auto_content_display_link_loaded";
    public static final boolean PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED_DEFAULT = true;

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

    public static final String PREFERENCE_DEFAULT_BROWSER = "preference_default_browser";
    public static final String PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME = "preference_default_browser_package_name";
    public static final String PREFERENCE_DEFAULT_BROWSER_LABEL = "preference_default_browser_bubble_label";

    public static final String PREFERENCE_CURRENT_TABS = "preference_current_bubbles";
    public static final String PREFERENCE_DEFAULT_APPS = "preference_default_apps";
    public static final String PREFERENCE_GOOGLE_ACCOUNTS_REDIRECT = "preference_google_accounts_redirect";

    public static final String PREFERENCE_WEBVIEW_TEXT_ZOOM = "preference_webview_text_zoom";
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN = 50;
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_DEFAULT = 120;
    public static final int     PREFERENCE_WEBVIEW_TEXT_ZOOM_MAX = 250;

    private static final String SAY_THANKS_CLICKED = "say_thanks_clicked";

    private static final String DEFAULT_APPS_MAP_KEY_HOST = "host";
    private static final String DEFAULT_APPS_MAP_KEY_COMPONENT = "component";

    private static final String BUBBLE_RESTING_X = "bubble_resting_x";
    private static final String BUBBLE_RESTING_Y = "bubble_resting_y";

    private static final String WELCOME_MESSAGE_DISPLAYED = "welcome_message_displayed";

    public interface ConsumeBubblesChangedEventHandler {
        public void onConsumeBubblesChanged();
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

    public static void setConsumeBubblesChangedEventHandler(ConsumeBubblesChangedEventHandler eh) {
        Util.Assert(mInstance != null);
        mInstance.mConsumeBubblesChangedEventHandler = eh;
    }

    /*
     *
     */
    public static Settings get()	{
        return mInstance;
    }

    private static Settings mInstance = null;

    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private TreeMap<String, String> mDefaultAppsMap = new TreeMap<String, String>();
    private List<Intent> mBrowsers;
    private ComponentName mDownloadHandlerComponentName;
    private Intent mDownloadQueryIntent = new Intent();
    private ResolveInfo mYouTubeViewResolveInfo;
    public ResolveInfo mLinkBubbleEntryActivityResolveInfo;
    private boolean mCheckedForYouTubeResolveInfo = false;
    private ConsumeBubblesChangedEventHandler mConsumeBubblesChangedEventHandler;
    // The point to save
    private Point mBubbleRestingPoint = new Point();
    // The point used as the return value. Required so we don't overwrite the desired point in landscape mode
    private Point mTempBubbleRestingPoint = new Point();

    Settings(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mDownloadHandlerComponentName = new ComponentName(mContext, DownloadHandlerActivity.class);

        mBrowsers = new Vector<Intent>();
        updateBrowsers();
        setDefaultLeftConsumeBubble();

        configureDefaultApps(mSharedPreferences.getString(PREFERENCE_DEFAULT_APPS, null));
        mBubbleRestingPoint.set(-1, -1);

        loadLinkLoadStats();
        loadRecentAppRedirects();
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

    void updateBrowsers() {
        mBrowsers.clear();
        String packageName = mContext.getPackageName();
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
                if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                    mLinkBubbleEntryActivityResolveInfo = resolveInfo;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    mBrowsers.add(intent);
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
                if (rightConsumeBubblePackageName == null || !doesPackageExist(packageManager, rightConsumeBubblePackageName)) {
                    setConsumeBubble(Constant.BubbleAction.ConsumeRight, Constant.ActionType.View,
                            defaultBrowserLabel,
                            fallbackDefaultBrowserPackageName, fallbackDefaultBrowserActivityClassName);
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

    private void setDefaultLeftConsumeBubble() {
        String leftConsumeBubblePackageName = mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        if (leftConsumeBubblePackageName == null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);

            if (setDefaultLeftConsumeBubble(findResolveInfoForPackageName(resolveInfos, "com.ideashower.readitlater.pro"), packageManager) == false) {
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
        return mBrowsers;
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
        }
        editor.commit();

        if (mConsumeBubblesChangedEventHandler != null) {
            mConsumeBubblesChangedEventHandler.onConsumeBubblesChanged();
        }
    }

    public String getConsumeBubbleLabel(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, null);
        }
        return null;
    }

    String getConsumeBubblePackageName(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        }
        return null;
    }

    String getConsumeBubbleActivityClassName(Constant.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);
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
        PackageManager packageManager = mContext.getPackageManager();
        try {
            String packageName = getConsumeBubblePackageName(action);
            if (packageName != null) {
                return packageManager.getApplicationIcon(packageName);
            }
        } catch (OutOfMemoryError ex) {
        } catch (PackageManager.NameNotFoundException e) {
        }
        return mContext.getResources().getDrawable(R.drawable.ic_launcher);
    }

    public String getInterceptLinksFromPackageName() {
        return mSharedPreferences.getString(PREFERENCE_INTERCEPT_LINKS_FROM, null);
    }

    public String getInterceptLinksFromAppName() {
        String name = mSharedPreferences.getString(PREFERENCE_INTERCEPT_LINKS_FROM_APP_NAME, null);
        if (name == null) {
            name = mContext.getString(R.string.intercept_links_from_default_label);
        }
        return name;
    }

    public void setInterceptLinksFrom(String packageName, String appName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_INTERCEPT_LINKS_FROM, packageName);
        editor.putString(PREFERENCE_INTERCEPT_LINKS_FROM_APP_NAME, appName);
        editor.commit();
    }

    public boolean getAutoContentDisplayAppRedirect() {
        return mSharedPreferences.getBoolean(PREFERENCE_AUTO_CONTENT_DISPLAY_APP_REDIRECT, PREFERENCE_AUTO_CONTENT_DISPLAY_APP_REDIRECT_DEFAULT);
    }

    public void setAutoContentDisplayAppRedirect(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREFERENCE_AUTO_CONTENT_DISPLAY_APP_REDIRECT, value);
        editor.commit();
    }

    public boolean getAutoContentDisplayLinkLoaded() {
        return mSharedPreferences.getBoolean(PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED, PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED_DEFAULT);
    }

    public void setAutoContentDisplayLinkLoaded(boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(PREFERENCE_AUTO_CONTENT_DISPLAY_LINK_LOADED, value);
        editor.commit();
    }

    public boolean isIncognitoMode() {
        return mSharedPreferences.getBoolean("preference_incognito", false);
    }

    public boolean isEnabled() {
        return mSharedPreferences.getBoolean(PREFERENCE_ENABLED, false);
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

    public boolean redirectUrlToBrowser(String url, PackageManager packageManager) {
        if (url.contains("accounts.google.com") && mSharedPreferences.getBoolean(PREFERENCE_GOOGLE_ACCOUNTS_REDIRECT, true)) {
            return true;
        }

        boolean result = false;

        /*
         * Temporarily enable DownloadHandlerActivity to see if it might be used to handle this URL. If so, redirect the
         * URL to the default browser rather than allowing an app like ES File Explorer be set as a default app for a host. #338
         */

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
    }

    public int getWebViewTextZoom() {
        return mSharedPreferences.getInt(PREFERENCE_WEBVIEW_TEXT_ZOOM, PREFERENCE_WEBVIEW_TEXT_ZOOM_DEFAULT);
    }

    public void setWebViewTextZoom(int zoom) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREFERENCE_WEBVIEW_TEXT_ZOOM, zoom);
        editor.commit();
    }

    public List<ResolveInfo> getAppsThatHandleUrl(URL url, PackageManager packageManager) {

        List<Intent> browsers = getBrowsers();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
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
                    results.add(info);
                    Log.d("appHandles", info.loadLabel(packageManager) + " for url:" + url);
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
        try {
            URL url = new URL(urlAsString);
            String host = url.getHost();
            if (host.length() > 1) {
                ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                addDefaultApp(host, componentName);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void addDefaultApp(String host, ComponentName componentName) {

        mDefaultAppsMap.put(host, componentName.flattenToString());

        saveDefaultApps();
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
                urls.add(jsonArray.getString(i));
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

    public boolean debugAutoLoadUrl() {
        return mSharedPreferences.getBoolean("auto_load_url", false);
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
}
