package com.chrislacy.linkbubble;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.chrislacy.linkbubble.R;

import java.util.List;
import java.util.Vector;

public class Settings {

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
    public static final String PREFERENCE_DEFAULT_BROWSER_ACTIVITY_CLASS_NAME = "preference_default_browser_activity_class_name";
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
    }

    void updateBrowsers() {
        mBrowsers.clear();
        String packageName = mContext.getPackageName();
        PackageManager packageManager = mContext.getPackageManager();
        Intent queryIntent = new Intent();
        queryIntent.setAction(Intent.ACTION_VIEW);
        queryIntent.setData(Uri.parse("http://www.fdasfjsadfdsfas.com"));        // Something stupid that no non-browser app will handle
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(queryIntent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo resolveInfo : resolveInfos) {
            IntentFilter filter = resolveInfo.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                // Ignore LinkBubble from this list
                if (resolveInfo.activityInfo.packageName.equals(packageName) == false) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    mBrowsers.add(intent);
                }
            }
        }
    }

    List<Intent> getBrowsers() {
        return mBrowsers;
    }

    void setDefaultBrowser(String label, String packageName, String activityClassName) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PREFERENCE_DEFAULT_BROWSER_LABEL, label);
        editor.putString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, packageName);
        editor.putString(PREFERENCE_DEFAULT_BROWSER_ACTIVITY_CLASS_NAME, activityClassName);
        editor.commit();
    }

    String getDefaultBrowserLabel() {
        return mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_LABEL, null);
    }

    String getDefaultBrowserPackageName() {
        return mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_PACKAGE_NAME, null);
    }

    String getDefaultBrowserActivityClassName() {
        return mSharedPreferences.getString(PREFERENCE_DEFAULT_BROWSER_ACTIVITY_CLASS_NAME, null);
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

}
