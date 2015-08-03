package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.AppPickerList;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/*
 * This class exists solely because Android's PreferenceScreen implementation doesn't do anything
 * when the Up button is touched, and we need to go back in that case given our use of the Up button.
 */
public class SettingsMoreActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings_more);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.preference_more_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

    static public class SettingsMoreFragment extends SettingsBaseFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_more);

            final CheckBoxPreference articleModeWearPreference = (CheckBoxPreference) findPreference(Settings.KEY_ARTICLE_MODE_ON_WEAR_PREFERENCE);
            if (DRM.isLicensed()) {
                articleModeWearPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (MainController.get() != null && MainController.get().reloadAllTabs(getActivity())) {
                            Toast.makeText(getActivity(), R.string.article_mode_changed_reloading_current, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
            } else {
                showProBanner(articleModeWearPreference);
                articleModeWearPreference.setChecked(false);
                articleModeWearPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        articleModeWearPreference.setChecked(false);
                        upsellPro(R.string.upgrade_article_mode_wear);
                        return true;
                    }
                });
            }

            final CheckBoxPreference articleModePreference = (CheckBoxPreference) findPreference(Settings.KEY_ARTICLE_MODE_PREFERENCE);
            if (DRM.isLicensed()) {
                articleModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (MainController.get() != null && MainController.get().reloadAllTabs(getActivity())) {
                            Toast.makeText(getActivity(), R.string.article_mode_changed_reloading_current, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
            } else {
                showProBanner(articleModePreference);
                articleModePreference.setChecked(false);
                articleModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        articleModePreference.setChecked(false);
                        upsellPro(R.string.upgrade_article_mode);
                        return true;
                    }
                });
            }

            Preference interceptLinksFromPreference = findPreference(Settings.PREFERENCE_IGNORE_LINKS_FROM);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                interceptLinksFromPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getDontInterceptLinksFromDialog(getActivity()).show();
                        return true;
                    }
                });
            } else {
                interceptLinksFromPreference.setSummary(R.string.preference_intercept_links_from_disabled_for_L);
                interceptLinksFromPreference.setEnabled(false);
            }
        }

        public AlertDialog getDontInterceptLinksFromDialog(final Context context) {
            final List<String> browserPackageNames = Settings.get().getBrowserPackageNames();

            final View layout = AppPickerList.createView(context,
                    ((MainApplication) context.getApplicationContext()).mIconCache,
                    AppPickerList.SelectionType.MultipleSelection, new AppPickerList.Initializer() {
                        @Override
                        public boolean setChecked(String packageName, String activityName) {
                            return Settings.get().ignoreLinkFromPackageName(packageName) ? false : true;
                        }

                        @Override
                        public boolean addToList(String packageName) {
                            if (packageName.equals(BuildConfig.APPLICATION_ID)) {
                                return false;
                            }

                            for (String browserPackageName : browserPackageNames) {
                                if (browserPackageName.equals(packageName)) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    });

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    ArrayList<String> ignorePackageNames = new ArrayList<String>();

                    ArrayList<AppPickerList.AppInfo> results = AppPickerList.getUnselected(layout);
                    if (results != null) {
                        for (AppPickerList.AppInfo result : results) {
                            ignorePackageNames.add(result.mPackageName);
                        }
                    }

                    Settings.get().setIgnoreLinksFromPackageNames(ignorePackageNames);
                }
            });
            builder.setTitle(R.string.preference_intercept_links_from_title);

            return builder.create();
        }
    }


    static class AppInfo {
        String mActivityName;
        String mPackageName;
        String mDisplayName;
        String mSortName;

        AppInfo(String activityName, String packageName, String displayName) {
            mActivityName = activityName;
            mPackageName = packageName;
            mDisplayName = displayName;
            mSortName = displayName.toLowerCase(Locale.getDefault());
        }
    }


    public static class AppInfoComparator implements Comparator<AppInfo> {
        @Override
        public int compare(AppInfo lhs, AppInfo rhs) {
            return lhs.mSortName.compareTo(rhs.mSortName);
        }
    }

}