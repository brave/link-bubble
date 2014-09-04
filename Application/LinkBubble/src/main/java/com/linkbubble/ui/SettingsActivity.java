package com.linkbubble.ui;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.linkbubble.BuildConfig;
import com.linkbubble.Config;
import com.linkbubble.Constant;
import com.linkbubble.DRM;
import com.linkbubble.MainApplication;
import com.linkbubble.MainController;
import com.linkbubble.MainService;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.ActionItem;
import com.linkbubble.util.AppPickerList;
import com.linkbubble.util.CrashTracking;
import com.linkbubble.util.FlushCacheService;
import com.linkbubble.util.IconCache;
import com.linkbubble.util.Util;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;


public class SettingsActivity extends PreferenceActivity {

    public static class IncognitoModeChangedEvent {
        public IncognitoModeChangedEvent(boolean value) {
            mIncognito = value;
        }
        public boolean mIncognito;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        CrashTracking.init(this);

        MainApplication mainApplication = (MainApplication) getApplicationContext();
        if (mainApplication.mIconCache == null) {
            mainApplication.mIconCache = new IconCache(mainApplication);
        }

        setContentView(R.layout.activity_settings);
        setTitle(R.string.title_settings);

        // This is a bit messy, but setDisplayHomeAsUpEnabled() for the Home button press event to be received, so
        // enable it but tell it not to draw. Then we can have L's nice looking Up button.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View view = Util.getActionBarUpView(this);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        FlushCacheService.doCheck(this);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        MainApplication.checkRestoreCurrentTabs(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item) == true) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    static public class SettingsFragment extends SettingsBaseFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private Preference mWebViewTextZoomPreference;
        private Preference mThemePreference;
        private Preference mWebViewBatterySavePreference;
        private ListPreference mUserAgentPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            MainApplication app = (MainApplication) getActivity().getApplicationContext();
            Bus bus = app.getBus();
            bus.register(this);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            PreferenceCategory generalCategory = (PreferenceCategory) findPreference("preference_category_general");
            PreferenceCategory configurationCategory = (PreferenceCategory) findPreference("preference_category_configuration");

            mWebViewBatterySavePreference = findPreference(Settings.PREFERENCE_WEBVIEW_BATTERY_SAVING_MODE);
            mWebViewBatterySavePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Util.showThemedDialog(getWebViewBatterySaveDialog());
                    return true;
                }
            });
            updateWebViewBatterySaveSummary();

            Preference interceptLinksFromPreference = findPreference(Settings.PREFERENCE_IGNORE_LINKS_FROM);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                configurationCategory.removePreference(interceptLinksFromPreference);
            } else {
                interceptLinksFromPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getDontInterceptLinksFromDialog(getActivity()).show();
                        return true;
                    }
                });
            }

            Preference incognitoButton = findPreference(Settings.PREFERENCE_INCOGNITO_MODE);
            if (incognitoButton != null) {
                incognitoButton.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        MainApplication app = (MainApplication) getActivity().getApplication();
                        Bus bus = app.getBus();
                        bus.post(new IncognitoModeChangedEvent((Boolean)newValue));

                        if (MainController.get() != null && MainController.get().reloadAllTabs(getActivity())) {
                            Toast.makeText(getActivity(), R.string.incognito_mode_changed_reloading_current, Toast.LENGTH_SHORT).show();
                        }

                        return true;
                    }
                });
            }

            mThemePreference = findPreference("preference_theme");
            mThemePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Util.showThemedDialog(getThemeDialog());
                    return true;
                }
            });
            updateThemeSummary();
            if (DRM.isLicensed() == false) {
                showProBanner(mThemePreference);
            }

            Preference crashButton = findPreference("debug_crash");
            if (crashButton != null) {
                crashButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        throw new RuntimeException("CRASH BUTTON PRESSED!");                }
                });
            }

            final Preference leftConsumeBubblePreference = findPreference(Settings.PREFERENCE_LEFT_CONSUME_BUBBLE);
            leftConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog alertDialog = ActionItem.getConfigureBubbleAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                        @Override
                        public void onSelected(ActionItem actionItem) {
                            Settings.get().setConsumeBubble(Constant.BubbleAction.ConsumeLeft, actionItem.mType, actionItem.getLabel(),
                                    actionItem.mPackageName, actionItem.mActivityClassName);
                            updateConsumeBubblePreference(leftConsumeBubblePreference, Constant.BubbleAction.ConsumeLeft);
                        }
                    });
                    Util.showThemedDialog(alertDialog);
                    return true;
                }
            });
            updateConsumeBubblePreference(leftConsumeBubblePreference, Constant.BubbleAction.ConsumeLeft);

            final Preference rightConsumeBubblePreference = findPreference(Settings.PREFERENCE_RIGHT_CONSUME_BUBBLE);
            rightConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog alertDialog = ActionItem.getConfigureBubbleAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                        @Override
                        public void onSelected(ActionItem actionItem) {
                            Settings.get().setConsumeBubble(Constant.BubbleAction.ConsumeRight, actionItem.mType, actionItem.getLabel(),
                                    actionItem.mPackageName, actionItem.mActivityClassName);
                            updateConsumeBubblePreference(rightConsumeBubblePreference, Constant.BubbleAction.ConsumeRight);
                        }
                    });
                    Util.showThemedDialog(alertDialog);
                    return true;
                }
            });
            updateConsumeBubblePreference(rightConsumeBubblePreference, Constant.BubbleAction.ConsumeRight);

        /*
        final Preference linkDoubleTapPreference = findPreference(Settings.PREFERENCE_LINK_DOUBLE_TAP);
        linkDoubleTapPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = ActionItem.getConfigureBubbleAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Settings.get().setConsumeBubble(Constant.BubbleAction.LinkDoubleTap, actionItem.mType, actionItem.getLabel(),
                                actionItem.mPackageName, actionItem.mActivityClassName);
                        linkDoubleTapPreference.setSummary(Settings.get().getConsumeBubbleLabel(Constant.BubbleAction.LinkDoubleTap));
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        linkDoubleTapPreference.setSummary(Settings.get().getConsumeBubbleLabel(Constant.BubbleAction.LinkDoubleTap));
        */

            findPreference("preference_clear_browser_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    return onClearBrowserCachePreferenceClick();
                }
            });

            mWebViewTextZoomPreference = findPreference(Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM);
            mWebViewTextZoomPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Util.showThemedDialog(getTextZoomDialog());
                    return true;
                }
            });
            mWebViewTextZoomPreference.setSummary(Settings.get().getWebViewTextZoom() + "%");

            mUserAgentPreference = (ListPreference) findPreference(Settings.PREFERENCE_USER_AGENT);

            Preference sayThanksPreference = findPreference("preference_say_thanks");
            if (sayThanksPreference != null) {
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
            }

            Preference getProPreference = findPreference("preference_get_pro");
            if (getProPreference != null) {
                getProPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = MainApplication.getStoreIntent(getActivity(), BuildConfig.STORE_PRO_URL);
                        if (intent != null) {
                            startActivity(intent);
                            return true;
                        }
                        return false;
                    }
                });
            }

            Preference otherAppsPreference = findPreference("preference_my_other_apps");
            otherAppsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = MainApplication.getStoreIntent(getActivity(), BuildConfig.STORE_MY_OTHER_APPS_URL);
                    if (intent != null) {
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });

            if (DRM.isLicensed()) {
                generalCategory.removePreference(getProPreference);
                if (Settings.get().getSayThanksClicked()) {
                    generalCategory.removePreference(sayThanksPreference);
                }
            } else {
                generalCategory.removePreference(sayThanksPreference);
            }


            findPreference("preference_faq").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FAQDialog dialog = new FAQDialog(getActivity());
                    dialog.show();
                    return true;
                }
            });

            findPreference("preference_default_apps").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SettingsDefaultAppsActivity.class));
                    return true;
                }
            });

            findPreference("preference_more").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SettingsMoreActivity.class));
                    return true;
                }
            });

            findPreference("preference_help").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SettingsHelpActivity.class));
                    return true;
                }
            });

            Preference versionPreference = findPreference("preference_version");
            try {
                PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                versionPreference.setTitle(getString(R.string.preference_version_title) + " " + packageInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
            }
            versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ChangeLogDialog changelogDialog = new ChangeLogDialog(getActivity());
                    changelogDialog.show();
                    //FAQDialog faqDialog = new FAQDialog(SettingsActivity.this);
                    //faqDialog.show();
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            if (mUserAgentPreference.getEntry() == null) {
                mUserAgentPreference.setValueIndex(0);
            }
            mUserAgentPreference.setSummary(mUserAgentPreference.getEntry());

            Preference defaultAppsPreference = findPreference(Settings.PREFERENCE_DEFAULT_APPS);
            setPreferenceIcon(defaultAppsPreference, Settings.get().getDefaultBrowserIcon(getActivity()));

            checkDefaultBrowser();
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            MainApplication app = (MainApplication) getActivity().getApplicationContext();
            Bus bus = app.getBus();
            bus.unregister(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);

            if (preference instanceof ListPreference) {
                ListPreference listPref = (ListPreference) preference;
                preference.setSummary(listPref.getEntry());
                if (preference == mUserAgentPreference) {
                    if (MainController.get() != null && MainController.get().reloadAllTabs(getActivity())) {
                        Toast.makeText(getActivity(), R.string.user_agent_changed_reloading_current, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        void checkDefaultBrowser() {

            PackageManager packageManager = getActivity().getPackageManager();

            Preference setDefaultBrowserPreference = findPreference("preference_set_default_browser");
            // Will be null if onResume() is called after the preference has already been removed.
            if (setDefaultBrowserPreference != null) {
                //PreferenceCategory category = (PreferenceCategory) findPreference("preference_category_configuration");
                //category.removePreference(setDefaultBrowserPreference);
                setDefaultBrowserPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Via http://stackoverflow.com/a/13239706/328679
                        PackageManager packageManager = getActivity().getPackageManager();

                        ComponentName dummyComponentName = new ComponentName(getActivity().getApplication(),
                                DefaultBrowserResetActivity.class);
                        packageManager.setComponentEnabledSetting(dummyComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Config.SET_DEFAULT_BROWSER_URL));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        getActivity().startActivity(intent);

                        packageManager.setComponentEnabledSetting(dummyComponentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
                        return true;
                    }
                });

                ResolveInfo defaultBrowserResolveInfo = Util.getDefaultBrowser(packageManager);
                if (defaultBrowserResolveInfo != null) {
                    String defaultBrowserPackageName = defaultBrowserResolveInfo.activityInfo != null ? defaultBrowserResolveInfo.activityInfo.packageName : null;
                    if (defaultBrowserPackageName != null
                            && (defaultBrowserPackageName.equals(BuildConfig.PACKAGE_NAME)
                            || defaultBrowserPackageName.equals(BuildConfig.TAP_PATH_PACKAGE_NAME))) {
                        PreferenceCategory category = (PreferenceCategory) findPreference("preference_category_configuration");
                        category.removePreference(setDefaultBrowserPreference);
                    }
                }
            }
        }

        private static final int THEME_LIGHT_COLOR = 0;
        private static final int THEME_LIGHT_NO_COLOR = 1;
        private static final int THEME_DARK_COLOR = 2;
        private static final int THEME_DARK_NO_COLOR = 3;

        void updateThemeSummary() {
            boolean darkTheme = Settings.get().getDarkThemeEnabled();
            boolean color = Settings.get().getColoredProgressIndicator();
            if (darkTheme) {
                if (color) {
                    mThemePreference.setSummary(R.string.preference_theme_dark_color);
                    setPreferenceIcon(mThemePreference, R.drawable.preference_theme_dark_color);
                } else {
                    mThemePreference.setSummary(R.string.preference_theme_dark_no_color);
                    setPreferenceIcon(mThemePreference, R.drawable.preference_theme_dark_no_color);
                }
            } else {
                if (color) {
                    mThemePreference.setSummary(R.string.preference_theme_light_color);
                    setPreferenceIcon(mThemePreference, R.drawable.preference_theme_light_color);
                } else {
                    mThemePreference.setSummary(R.string.preference_theme_light_no_color);
                    setPreferenceIcon(mThemePreference, R.drawable.preference_theme_light_no_color);
                }
            }
        }

        AlertDialog getThemeDialog() {
            final String lightColor = getString(R.string.preference_theme_light_color);
            final String lightNoColor = getString(R.string.preference_theme_light_no_color);
            final String darkColor = getString(R.string.preference_theme_dark_color);
            final String darkNoColor = getString(R.string.preference_theme_dark_no_color);

            final ArrayList<String> items = new ArrayList<String>();
            items.add(lightColor);
            items.add(lightNoColor);
            items.add(darkColor);
            items.add(darkNoColor);

            boolean darkTheme = Settings.get().getDarkThemeEnabled();
            boolean color = Settings.get().getColoredProgressIndicator();
            final int startSelectedIndex = darkTheme ? (color ? THEME_DARK_COLOR : 3) : (color ? THEME_LIGHT_COLOR : THEME_LIGHT_NO_COLOR);

            final PreferenceThemeAdapter adapter = new PreferenceThemeAdapter(getActivity(),
                    R.layout.view_preference_theme_item,
                    startSelectedIndex,
                    items.toArray(new String[0]));

            final ListView listView = new ListView(getActivity());
            listView.setAdapter(adapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(listView);
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (adapter.mSelectedIndex != startSelectedIndex) {
                        if (DRM.isLicensed() == false) {
                            upsellPro(R.string.upgrade_theme);
                            Settings.get().setDarkThemeEnabled(false);
                            Settings.get().setColoredProgressIndicator(true);
                            updateThemeSummary();
                            return;
                        }

                        switch (adapter.mSelectedIndex) {
                            case THEME_LIGHT_COLOR:
                                Settings.get().setDarkThemeEnabled(false);
                                Settings.get().setColoredProgressIndicator(true);
                                break;
                            case THEME_LIGHT_NO_COLOR:
                                Settings.get().setDarkThemeEnabled(false);
                                Settings.get().setColoredProgressIndicator(false);
                                break;
                            case THEME_DARK_COLOR:
                                Settings.get().setDarkThemeEnabled(true);
                                Settings.get().setColoredProgressIndicator(true);
                                break;
                            case THEME_DARK_NO_COLOR:
                                Settings.get().setDarkThemeEnabled(true);
                                Settings.get().setColoredProgressIndicator(false);
                                break;
                        }

                        updateThemeSummary();

                        if (MainController.get() != null) {
                            MainApplication.postEvent(getActivity(), new MainService.ReloadMainServiceEvent(getActivity()));
                        }
                    }
                }
            });
            builder.setTitle(R.string.preference_theme_title);

            return builder.create();
        }

        private static class PreferenceThemeAdapter extends ArrayAdapter<String> {

            Context mContext;
            int mLayoutResourceId;
            int mSelectedIndex;

            public PreferenceThemeAdapter(Context context, int layoutResourceId, int initialSelectedIndex, String[] data) {
                super(context, layoutResourceId, data);
                mLayoutResourceId = layoutResourceId;
                mContext = context;
                mSelectedIndex = initialSelectedIndex;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                if (convertView==null) {
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(mLayoutResourceId, parent, false);
                }

                TextView label = (TextView) convertView.findViewById(R.id.label);
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio_button);

                switch (position) {
                    case THEME_LIGHT_COLOR:
                        label.setText(mContext.getString(R.string.preference_theme_light_color));
                        icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.preference_theme_light_color));
                        break;
                    case THEME_LIGHT_NO_COLOR:
                        label.setText(mContext.getString(R.string.preference_theme_light_no_color));
                        icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.preference_theme_light_no_color));
                        break;
                    case THEME_DARK_COLOR:
                        label.setText(mContext.getString(R.string.preference_theme_dark_color));
                        icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.preference_theme_dark_color));
                        break;
                    case THEME_DARK_NO_COLOR:
                        label.setText(mContext.getString(R.string.preference_theme_dark_no_color));
                        icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.preference_theme_dark_no_color));
                        break;
                    default:
                        break;
                }
                convertView.setTag(position);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        radioButton.setChecked(true);
                        mSelectedIndex = position;
                        PreferenceThemeAdapter.this.notifyDataSetChanged();
                    }
                });
                convertView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            // Pass event along to radio button so UI visually updates
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP: {
                                radioButton.onTouchEvent(event);
                                return true;
                            }
                        }
                        return false;
                    }
                });
                radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mSelectedIndex = position;
                            PreferenceThemeAdapter.this.notifyDataSetChanged();
                        }
                    }
                });

                radioButton.setChecked(position == mSelectedIndex);
                return convertView;
            }
        }

        void updateWebViewBatterySaveSummary() {
            int position = Settings.get().getWebViewBatterySaveMode().ordinal();
            if (position == Settings.WebViewBatterySaveMode.Aggressive.ordinal()) {
                mWebViewBatterySavePreference.setSummary(getString(R.string.preference_webview_battery_save_aggressive_title));
            } else if (position == Settings.WebViewBatterySaveMode.Default.ordinal()) {
                mWebViewBatterySavePreference.setSummary(getString(R.string.preference_webview_battery_save_default_title));
            } else if (position == Settings.WebViewBatterySaveMode.Off.ordinal()) {
                mWebViewBatterySavePreference.setSummary(getString(R.string.preference_webview_battery_save_off_title));
            }
        }

        AlertDialog getWebViewBatterySaveDialog() {
            final ArrayList<String> items = new ArrayList<String>();
            items.add(getString(R.string.preference_webview_battery_save_aggressive_title));
            items.add(getString(R.string.preference_webview_battery_save_default_title));
            items.add(getString(R.string.preference_webview_battery_save_off_title));

            final PreferenceBatterySaveAdapter adapter = new PreferenceBatterySaveAdapter(getActivity(),
                    R.layout.view_preference_webview_battery_save_item,
                    Settings.get().getWebViewBatterySaveMode().ordinal(),
                    items.toArray(new String[0]));

            final ListView listView = new ListView(getActivity());
            listView.setAdapter(adapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(listView);
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Settings.WebViewBatterySaveMode mode = Settings.WebViewBatterySaveMode.values()[adapter.mSelectedIndex];
                    Settings.get().setWebViewBatterySaveMode(mode);
                    updateWebViewBatterySaveSummary();
                }
            });
            builder.setTitle(R.string.preference_webview_battery_save_title);

            return builder.create();
        }

        private static class PreferenceBatterySaveAdapter extends ArrayAdapter<String> {

            Context mContext;
            int mLayoutResourceId;
            int mSelectedIndex;

            public PreferenceBatterySaveAdapter(Context context, int layoutResourceId, int initialSelectedIndex, String[] data) {
                super(context, layoutResourceId, data);
                mLayoutResourceId = layoutResourceId;
                mContext = context;
                mSelectedIndex = initialSelectedIndex;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {

                if (convertView==null) {
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(mLayoutResourceId, parent, false);
                }

                TextView label = (TextView) convertView.findViewById(R.id.title);
                TextView summary = (TextView) convertView.findViewById(R.id.summary);
                final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio_button);

                if (position == Settings.WebViewBatterySaveMode.Aggressive.ordinal()) {
                    label.setText(mContext.getString(R.string.preference_webview_battery_save_aggressive_title));
                    summary.setText(mContext.getString(R.string.preference_webview_battery_save_aggressive_summary));
                } else if (position == Settings.WebViewBatterySaveMode.Default.ordinal()) {
                    label.setText(mContext.getString(R.string.preference_webview_battery_save_default_title));
                    summary.setText(mContext.getString(R.string.preference_webview_battery_save_default_summary));
                } else if (position == Settings.WebViewBatterySaveMode.Off.ordinal()) {
                    label.setText(mContext.getString(R.string.preference_webview_battery_save_off_title));
                    summary.setText(mContext.getString(R.string.preference_webview_battery_save_off_summary));
                }
                convertView.setTag(position);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        radioButton.setChecked(true);
                        mSelectedIndex = position;
                        PreferenceBatterySaveAdapter.this.notifyDataSetChanged();
                    }
                });
                convertView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            // Pass event along to radio button so UI visually updates
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP: {
                                radioButton.onTouchEvent(event);
                                return true;
                            }
                        }
                        return false;
                    }
                });
                radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mSelectedIndex = position;
                            PreferenceBatterySaveAdapter.this.notifyDataSetChanged();
                        }
                    }
                });

                radioButton.setChecked(position == mSelectedIndex);
                return convertView;
            }
        }

        AlertDialog getTextZoomDialog() {
            final View layout = View.inflate(getActivity(), R.layout.view_preference_text_zoom, null);

            final int initialZoom = Settings.get().getWebViewTextZoom();
            final TextView textView = (TextView) layout.findViewById(R.id.seekbar_title);
            final SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekbar_text_zoom);
            textView.setText((initialZoom + Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN) + "%");
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (progress < 0) {
                        progress = 0;
                    } else {
                        final int stepSize = 5;
                        progress = (Math.round(progress/stepSize))*stepSize;
                    }
                    seekBar.setProgress(progress);

                    textView.setText((progress + Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN) + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            seekBar.setMax(Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MAX - Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN);
            seekBar.setProgress(initialZoom - Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setView(layout);
            builder.setTitle(R.string.preference_webview_text_zoom_title);

            AlertDialog alertDialog = builder.create();

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.get().setWebViewTextZoom(seekBar.getProgress() + Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_MIN);
                    int currentZoom = Settings.get().getWebViewTextZoom();
                    mWebViewTextZoomPreference.setSummary(currentZoom + "%");
                    if (currentZoom != initialZoom && MainController.get() != null) {
                        if (MainController.get().reloadAllTabs(getActivity())) {
                            Toast.makeText(getActivity(), R.string.preference_webview_text_zoom_reloading_current, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.action_use_default), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Settings.get().setWebViewTextZoom(Settings.PREFERENCE_WEBVIEW_TEXT_ZOOM_DEFAULT);
                    int currentZoom = Settings.get().getWebViewTextZoom();
                    mWebViewTextZoomPreference.setSummary(currentZoom + "%");
                    if (currentZoom != initialZoom && MainController.get() != null) {
                        if (MainController.get().reloadAllTabs(getActivity())) {
                            Toast.makeText(getActivity(), R.string.preference_webview_text_zoom_reloading_current, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            return alertDialog;
        }


        private boolean onClearBrowserCachePreferenceClick() {

            final String clearCache = getString(R.string.preference_clear_cache);
            final String clearCookies = getString(R.string.preference_clear_cookies);
            final String clearFavicons = getString(R.string.preference_clear_favicons);
            final String clearFormData = getString(R.string.preference_clear_form_data);
            final String clearHistory = getString(R.string.preference_clear_history);
            final String clearPasswords = getString(R.string.preference_clear_passwords);

            final ArrayList<String> items = new ArrayList<String>();
            items.add(clearCache);
            items.add(clearCookies);
            items.add(clearFavicons);
            items.add(clearFormData);
            items.add(clearHistory);
            items.add(clearPasswords);

            ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, items);

            final ListView listView = new ListView(getActivity());
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(listAdapter);
            for (int i = 0; i < items.size(); i++) {
                listView.setItemChecked(i, items.get(i).equals(clearFavicons) ? false : true);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(listView);
            builder.setIcon(Util.getAlertIcon(getActivity()));
            builder.setPositiveButton(R.string.action_clear_data, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    WebView webView = new WebView(getActivity());
                    WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getActivity().getApplicationContext());
                    boolean dataCleared = false;
                    int count = listView.getCount();
                    for (int i = 0; i < count; i++) {
                        if (listView.isItemChecked(i)) {
                            String item = items.get(i);
                            if (item.equals(clearCache)) {
                                webView.clearCache(true);
                                dataCleared = true;
                            } else if (item.equals(clearCookies)) {
                                CookieManager cookieManager = CookieManager.getInstance();
                                if (cookieManager != null && cookieManager.hasCookies()) {
                                    cookieManager.removeAllCookie();
                                }
                                dataCleared = true;
                            } else if (item.equals(clearFavicons)) {
                                MainApplication.sDatabaseHelper.deleteAllFavicons();
                                MainApplication.recreateFaviconCache();
                                dataCleared = true;
                            } else if (item.equals(clearFormData)) {
                                if (webViewDatabase != null) {
                                    webViewDatabase.clearFormData();
                                    dataCleared = true;
                                }
                            } else if (item.equals(clearHistory)) {
                                webView.clearHistory();
                                MainApplication.sDatabaseHelper.deleteAllHistoryRecords();
                                Settings.get().saveCurrentTabs(null);
                                dataCleared = true;
                            } else if (item.equals(clearPasswords)) {
                                if (webViewDatabase != null) {
                                    webViewDatabase.clearHttpAuthUsernamePassword();
                                    webViewDatabase.clearUsernamePassword();
                                    dataCleared = true;
                                }
                            }
                        }
                    }

                    if (dataCleared) {
                        boolean reloaded = false;

                        if (MainController.get() != null) {
                            reloaded = MainController.get().reloadAllTabs(getActivity());
                        }

                        Toast.makeText(getActivity(), reloaded ? R.string.private_data_cleared_reloading_current : R.string.private_data_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setTitle(R.string.preference_clear_browser_cache_title);

            AlertDialog alertDialog = builder.create();
            Util.showThemedDialog(alertDialog);

            return true;
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
                            if (packageName.equals(BuildConfig.PACKAGE_NAME)) {
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

        void updateConsumeBubblePreference(Preference preference, Constant.BubbleAction action) {
            preference.setSummary(Settings.get().getConsumeBubbleLabel(action));
            setPreferenceIcon(preference, Settings.get().getConsumeBubbleIcon(action, false));
        }


    }

}