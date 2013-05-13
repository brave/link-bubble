package com.chrislacy.linkview;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.Menu;
import android.view.MenuInflater;

public class SettingsActivity extends PreferenceActivity {

    public static final String KEY_APP_ENABLED = "preference_app_enabled";
    public static final String KEY_DISPLAY_URLS = "preference_display_urls";

    //private SwitchPreference mAppEnabledPreference;
    //private CheckBoxPreference mChec


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(android.R.style.Theme_Holo);

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        //mAppEnabledPreference = (SwitchPreference) getPreferenceScreen().findPreference(KEY_APP_ENABLED);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.settings, menu);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        //final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        getPreferenceScreen().findPreference("preference_tests").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, TestActivity.class));
                return true;
            }
        });
    }
}
