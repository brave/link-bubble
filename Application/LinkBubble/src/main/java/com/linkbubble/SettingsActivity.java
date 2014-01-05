package com.linkbubble;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.crashlytics.android.Crashlytics;

/**
 * Created by chrislacy on 5/1/2013.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
    }
}