package com.linkbubble;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;

import android.util.Log;
import com.linkbubble.ui.BubbleView;
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

    public static final String PREFERENCE_CURRENT_BUBBLES = "preference_current_bubbles";
    public static final String PREFERENCE_DEFAULT_APPS = "preference_default_apps";
    public static final String PREFERENCE_GOOGLE_ACCOUNTS_REDIRECT = "preference_google_accounts_redirect";

    private static final String DEFAULT_APPS_MAP_KEY_HOST = "host";
    private static final String DEFAULT_APPS_MAP_KEY_COMPONENT = "component";


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
    private ResolveInfo mYouTubeViewResolveInfo;
    public ResolveInfo mLinkBubbleEntryActivityResolveInfo;
    private boolean mCheckedForYouTubeResolveInfo = false;
    private ConsumeBubblesChangedEventHandler mConsumeBubblesChangedEventHandler;

    Settings(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mBrowsers = new Vector<Intent>();
        updateBrowsers();
        setDefaultLeftConsumeBubble();

        configureDefaultApps(mSharedPreferences.getString(PREFERENCE_DEFAULT_APPS, null));
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
        if (fallbackDefaultBrowserPackageName != null
                && (defaultBrowserPackage == null || rightConsumeBubblePackageName == null)) {
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(fallbackDefaultBrowserPackageName, 0);
                String defaultBrowserLabel = packageManager.getApplicationLabel(applicationInfo).toString();

                if (defaultBrowserPackage == null) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString(PREFERENCE_DEFAULT_BROWSER_LABEL, defaultBrowserLabel);
                    editor.putString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, fallbackDefaultBrowserPackageName);
                    editor.commit();
                }
                if (rightConsumeBubblePackageName == null) {
                    setConsumeBubble(Config.BubbleAction.ConsumeRight, Config.ActionType.View,
                            defaultBrowserLabel,
                            fallbackDefaultBrowserPackageName, fallbackDefaultBrowserActivityClassName);
                }

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
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
            setConsumeBubble(Config.BubbleAction.ConsumeLeft, Config.ActionType.Share,
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

    public void setConsumeBubble(Config.BubbleAction action, Config.ActionType type, String label, String packageName, String activityClassName) {
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

    public String getConsumeBubbleLabel(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_LABEL, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_LABEL, null);
        }
        return null;
    }

    String getConsumeBubblePackageName(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_PACKAGE_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_PACKAGE_NAME, null);
        }
        return null;
    }

    String getConsumeBubbleActivityClassName(Config.BubbleAction action) {
        switch (action) {
            case ConsumeLeft:
                return mSharedPreferences.getString(PREFERENCE_LEFT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);

            case ConsumeRight:
                return mSharedPreferences.getString(PREFERENCE_RIGHT_CONSUME_BUBBLE_ACTIVITY_CLASS_NAME, null);
        }
        return null;
    }

    Config.ActionType getConsumeBubbleActionType(Config.BubbleAction action) {
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
            if (actionTypeAsString.equals(Config.ActionType.Share.name())) {
                return Config.ActionType.Share;
            } else if (actionTypeAsString.equals(Config.ActionType.View.name())) {
                return Config.ActionType.View;
            }
        }

        return Config.ActionType.Unknown;
    }

    public Drawable getConsumeBubbleIcon(Config.BubbleAction action) {
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

    public boolean redirectUrlToBrowser(String url) {
        if (url.contains("accounts.google.com") && mSharedPreferences.getBoolean(PREFERENCE_GOOGLE_ACCOUNTS_REDIRECT, true)) {
            return true;
        }
        return false;
    }

    public List<ResolveInfo> getAppsThatHandleUrl(String url) {

        List<Intent> browsers = getBrowsers();

        PackageManager manager = mContext.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

        ArrayList<ResolveInfo> results = null;

        for (ResolveInfo info : infos) {
            IntentFilter filter = info.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {

                // Check if this item is a browser, and if so, ignore it
                boolean packageOk = !info.activityInfo.packageName.equals(mContext.getPackageName());
                for (Intent browser : browsers) {
                    if (info.activityInfo.packageName.equals(browser.getComponent().getPackageName())) {
                        packageOk = false;
                        break;
                    }
                }

                if (packageOk) {
                    if (results == null) {
                        results = new ArrayList<ResolveInfo>();
                    }
                    results.add(info);
                    Log.d("appHandles", info.loadLabel(manager) + " for url:" + url);
                }
            }
        }

        if (results != null && results.size() > 0) {
            ResolveInfo defaultApp = getDefaultApp(url, results);
            if (defaultApp != null) {
                results.clear();
                results.add(defaultApp);
            }
            return results;
        }

        return null;
    }

    /*
    ResolveInfo getAppThatHandlesUrl(String url) {
        List<ResolveInfo> results = getAppsThatHandleUrl(url);
        if (results != null && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }*/

    /*
    private String getDefaultAppKey(String urlHost) {
        return PREFERENCE_DEFAULT_APP_PREFIX + urlHost;
    }*/

    private ResolveInfo getDefaultApp(String urlAsString, List<ResolveInfo> resolveInfos) {
        try {
            URL url = new URL(urlAsString);
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

        } catch (MalformedURLException e) {
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

    public Vector<String> loadCurrentBubbles() {
        Vector<String> urls = new Vector<String>();
        String json = mSharedPreferences.getString(PREFERENCE_CURRENT_BUBBLES, "[]");
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

    public void saveCurrentBubbles(List<BubbleView> bubbles) {
        JSONArray jsonArray = new JSONArray();
        for (BubbleView b : bubbles) {
            URL url = b.getUrl();
            jsonArray.put(url.toString());
        }

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_CURRENT_BUBBLES, jsonArray.toString());
        editor.commit();
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

    public boolean debugAutoLoadUrl() {
        return mSharedPreferences.getBoolean("auto_load_url", false);
    }
}
