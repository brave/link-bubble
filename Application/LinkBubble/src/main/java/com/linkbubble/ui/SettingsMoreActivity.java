package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Analytics;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.Util;

/*
 * This class exists solely because Android's PreferenceScreen implementation doesn't do anything
 * when the Up button is touched, and we need to go back in that case given our use of the Up button.
 */
public class SettingsMoreActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        setContentView(R.layout.activity_settings_more);
        setTitle(R.string.preference_more_title);

        // This is a bit messy, but setDisplayHomeAsUpEnabled() for the Home button press event to be received, so
        // enable it but tell it not to draw. Then we can have L's nice looking Up button.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View view = Util.getActionBarUpView(this);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
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

            PreferenceScreen rootPreferenceScreen = (PreferenceScreen) findPreference("preference_more_root");

            final CheckBoxPreference okGooglePreference = (CheckBoxPreference) findPreference(Settings.KEY_OK_GOOGLE_PREFERENCE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (DRM.isLicensed()) {
                    okGooglePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            boolean checked = (Boolean) newValue;
                            MainApplication.postEvent(getActivity(), new ExpandedActivity.EnableHotwordServiceEvent(checked));
                            return true;
                        }
                    });
                } else {
                    showProBanner(okGooglePreference);
                    okGooglePreference.setChecked(false);
                    okGooglePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            okGooglePreference.setChecked(false);
                            upsellPro(R.string.upgrade_ok_google);
                            return true;
                        }
                    });
                }
            } else {
                okGooglePreference.setSummary(R.string.preference_ok_google_summary_jelly_bean);
                okGooglePreference.setEnabled(false);
            }

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
                rootPreferenceScreen.removePreference(interceptLinksFromPreference);
            }
        }
    }

}