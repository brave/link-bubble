package com.linkbubble.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.crashlytics.android.Crashlytics;
import com.linkbubble.R;


public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }
}