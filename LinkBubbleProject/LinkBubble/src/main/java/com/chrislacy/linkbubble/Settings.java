package com.chrislacy.linkbubble;


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
import com.chrislacy.linkbubble.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Settings {

    public static final String PREFERENCE_ENABLED = "preference_enabled";
    public static final String PREFERENCE_AUTO_LOAD_CONTENT = "preference_auto_load_content";

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
    private List<Intent> mBrowsers;
    private ConsumeBubblesChangedEventHandler mConsumeBubblesChangedEventHandler;

    Settings(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        mBrowsers = new Vector<Intent>();
        updateBrowsers();
        setDefaultLeftConsumeBubble();
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
                if (resolveInfo.activityInfo.packageName.equals(packageName) == false) {
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
                    setConsumeBubble(Config.BubbleAction.ConsumeRight, Config.ActionType.Share,
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

    List<Intent> getBrowsers() {
        return mBrowsers;
    }

    void setDefaultBrowser(String label, String packageName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_DEFAULT_BROWSER_LABEL, label);
        editor.putString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, packageName);
        editor.commit();
    }

    String getDefaultBrowserLabel() {
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

    void setConsumeBubble(Config.BubbleAction action, Config.ActionType type, String label, String packageName, String activityClassName) {
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

    String getConsumeBubbleLabel(Config.BubbleAction action) {
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

    Drawable getConsumeBubbleIcon(Config.BubbleAction action) {
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

    boolean isIncognitoMode() {
        return mSharedPreferences.getBoolean("preference_incognito", false);
    }

    boolean autoLoadContent() {
        return mSharedPreferences.getBoolean(PREFERENCE_AUTO_LOAD_CONTENT, false);
    }

    boolean isEnabled() {
        return mSharedPreferences.getBoolean(PREFERENCE_ENABLED, false);
    }

    List<ResolveInfo> getAppsThatHandleUrl(String url) {

        List<Intent> browsers = Settings.get().getBrowsers();

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
            return results;
        }

        return null;
    }

    ResolveInfo getAppThatHandlesUrl(String url) {
        List<ResolveInfo> results = getAppsThatHandleUrl(url);
        if (results != null && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
}
