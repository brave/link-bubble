package com.chrislacy.linkbubble;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by gw on 11/09/13.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);
    }

}
