package com.linkbubble.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
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
public class SettingsHelpActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings_help);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.preference_help_title);
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

    static public class SettingsHelpFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_help);

            PreferenceScreen rootPreferenceScreen = (PreferenceScreen) findPreference("preference_help_root");

            findPreference("preference_credits").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showCreditsDialog();
                    return true;
                }
            });

            findPreference("preference_osl").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showOpenSourceLicensesDialog();
                    return true;
                }
            });

            findPreference("preference_show_welcome_message").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MainApplication.openLink(getActivity(), Constant.WELCOME_MESSAGE_URL, Analytics.OPENED_URL_FROM_SETTINGS);
                    return true;
                }
            });

            findPreference("preference_privacy_policy").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MainApplication.openLink(getActivity(), Constant.PRIVACY_POLICY_URL, Analytics.OPENED_URL_FROM_SETTINGS);
                    return true;
                }
            });

            findPreference("preference_terms_of_service").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MainApplication.openLink(getActivity(), Constant.TERMS_OF_SERVICE_URL, Analytics.OPENED_URL_FROM_SETTINGS);
                    return true;
                }
            });

            /*
            Preference sayThanksPreference = findPreference("preference_say_thanks");
            if (Settings.get().getSayThanksClicked()) {
                sayThanksPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = MainApplication.getStoreIntent(getActivity(), BuildConfig.STORE_FREE_URL);
                        if (intent != null) {
                            startActivity(intent);
                            Settings.get().setSayThanksClicked(true);
                            return true;
                        }
                        return false;
                    }
                });
            } else {
                rootPreferenceScreen.removePreference(sayThanksPreference);
            }*/

        }

        private static int TAPS_TO_FORCE_A_CRASH = 7;
        private int mForceCrashCountdown = TAPS_TO_FORCE_A_CRASH;
        Toast mForceCrashToast;

        void showCreditsDialog() {
            final View layout = View.inflate(getActivity(), R.layout.view_credits, null);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setNegativeButton(android.R.string.ok, null);
            builder.setView(layout);
            builder.setTitle(R.string.credits_title);

            AlertDialog alertDialog = builder.create();
            alertDialog.setIcon(Util.getAlertIcon(getActivity()));
            Util.showThemedDialog(alertDialog);
        }

        private void showOpenSourceLicensesDialog() {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/open_source_licenses.html");
            webView.setWebViewClient(new WebViewClient() {
                public boolean shouldOverrideUrlLoading(WebView view, String url){
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return true;
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setNegativeButton(R.string.action_ok, null);
            builder.setView(webView);
            builder.setTitle(R.string.preference_osl_title);

            AlertDialog alertDialog = builder.create();
            Util.showThemedDialog(alertDialog);
        }
    }

}