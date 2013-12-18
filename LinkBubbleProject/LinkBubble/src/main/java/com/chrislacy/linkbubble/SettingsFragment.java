package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.TreeMap;
import java.util.Vector;


/**
 * Created by gw on 11/09/13.
 */
public class SettingsFragment extends PreferenceFragment {



    public static class IncognitoModeChangedEvent {
        public IncognitoModeChangedEvent(boolean value) {
            mIncognito = value;
        }
        boolean mIncognito;
    }

    public interface IncognitoModeChangedEventHandler {
        public void onIncognitoModeChanged(boolean incognito);
    }

    private void configureDefaultAppsList() {
        PreferenceCategory preferenceCategory = (PreferenceCategory)findPreference("preference_category_other_apps");
        preferenceCategory.removeAll();

        Preference noticePreference = new Preference(getActivity());

        PackageManager packageManager = getActivity().getPackageManager();
        TreeMap<String, ComponentName> defaultAppsMap = Settings.get().getDefaultAppsMap();
        if (defaultAppsMap != null && defaultAppsMap.size() > 0) {
            noticePreference.setSummary(R.string.preference_default_apps_notice_summary);
            preferenceCategory.addPreference(noticePreference);

            for (String key : defaultAppsMap.keySet()) {
                ComponentName componentName = defaultAppsMap.get(key);
                try {
                    ActivityInfo info = packageManager.getActivityInfo(componentName, 0);
                    final CharSequence label = info.loadLabel(packageManager);
                    final String host = key;
                    Preference preference = new Preference(getActivity());
                    preference.setTitle(label);
                    preference.setIcon(info.loadIcon(packageManager));
                    preference.setSummary(key);
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Resources resources = getActivity().getResources();
                            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                            alertDialog.setTitle(R.string.remove_default_title);
                            alertDialog.setMessage(String.format(resources.getString(R.string.remove_default_message), label, host, host));
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.action_remove),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Settings.get().removeDefaultApp(host);
                                            configureDefaultAppsList();
                                        }
                                    });
                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.action_cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                            alertDialog.show();
                            return true;
                        }
                    });
                    preferenceCategory.addPreference(preference);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else {
            noticePreference.setSummary(R.string.preference_default_apps_notice_no_defaults_summary);
            preferenceCategory.addPreference(noticePreference);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainApplication app = (MainApplication) getActivity().getApplicationContext();
        Bus bus = app.getBus();
        bus.register(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);

        CheckBoxPreference autoLoadContentPreference = (CheckBoxPreference)findPreference(Settings.PREFERENCE_AUTO_LOAD_CONTENT);
        autoLoadContentPreference.setChecked(Settings.get().autoLoadContent());

        Preference incognitoButton = findPreference("preference_incognito");
        if (incognitoButton != null) {
            incognitoButton.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    MainApplication app = (MainApplication) getActivity().getApplication();
                    Bus bus = app.getBus();
                    bus.post(new IncognitoModeChangedEvent((Boolean)newValue));

                    return true;
                }
            });
        }

        Preference loadUrlButton = findPreference("load_url");
        if (loadUrlButton != null) {
            loadUrlButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //mainActivity.openLink("http://www.google.com");
                    MainApplication.openLink(getActivity(), "http://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro", true);

                    /*
                    Intent openIntent = new Intent(Intent.ACTION_VIEW);
                    openIntent.setClassName("com.android.vending", "com.google.android.finsky.activities.EntryActivity");
                    openIntent.setData(Uri.parse("http://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro"));
                    //openIntent.setClassName("com.google.android.youtube", "com.google.android.apps.youtube.app.honeycomb.Shell$WatchActivity");
                    //openIntent.setData(Uri.parse("http://www.youtube.com/watch?v=CevxZvSJLk8"));
                    openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    getActivity().startActivity(openIntent);
                    */

                    return true;
                }
            });
        }

        final Preference leftConsumeBubblePreference = findPreference(Settings.PREFERENCE_LEFT_CONSUME_BUBBLE);
        leftConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = ActionItem.getConfigureBubbleAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Settings.get().setConsumeBubble(Config.BubbleAction.ConsumeLeft, actionItem.mType, actionItem.getLabel(),
                                actionItem.mPackageName, actionItem.mActivityClassName);
                        leftConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeLeft));
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        leftConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeLeft));

        final Preference rightConsumeBubblePreference = findPreference(Settings.PREFERENCE_RIGHT_CONSUME_BUBBLE);
        rightConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = ActionItem.getConfigureBubbleAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Settings.get().setConsumeBubble(Config.BubbleAction.ConsumeRight, actionItem.mType, actionItem.getLabel(),
                                actionItem.mPackageName, actionItem.mActivityClassName);
                        rightConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeRight));
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        rightConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeRight));

        Preference defaultBrowserPreference = findPreference(Settings.PREFERENCE_DEFAULT_BROWSER);
        defaultBrowserPreference.setTitle(Settings.get().getDefaultBrowserLabel());
        Drawable defaultBrowserIcon = Settings.get().getDefaultBrowserIcon(getActivity());
        if (defaultBrowserIcon != null) {
            defaultBrowserPreference.setIcon(defaultBrowserIcon);
        }
        defaultBrowserPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog alertDialog = ActionItem.getDefaultBrowserAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Settings.get().setDefaultBrowser(actionItem.getLabel(), actionItem.mPackageName);
                        preference.setTitle(Settings.get().getDefaultBrowserLabel());
                        Drawable defaultBrowserIcon = Settings.get().getDefaultBrowserIcon(getActivity());
                        if (defaultBrowserIcon != null) {
                            preference.setIcon(defaultBrowserIcon);
                        }
                    }
                });
                alertDialog.show();
                return true;
            }
        });

        configureDefaultAppsList();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (prefs.getBoolean("auto_load_url", true)) {
                MainApplication.openLink(getActivity(), "http://abc.net.au", false);
            //MainApplication.openLink(getActivity(), "https://twitter.com/lokibartleby/status/412160702707539968", false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkDefaultBrowser();
        configureDefaultAppsList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        MainApplication app = (MainApplication) getActivity().getApplicationContext();
        Bus bus = app.getBus();
        bus.unregister(this);
    }

    void checkDefaultBrowser() {

        Preference setDefaultPreference = getPreferenceScreen().findPreference("preference_set_default_browser");
        // Will be null if onResume() is called after the preference has already been removed.
        if (setDefaultPreference != null) {
            if (Util.isDefaultBrowser(getActivity().getPackageName(), getActivity().getPackageManager())) {
                PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().findPreference("preference_category_general");
                category.removePreference(setDefaultPreference);
            } else {
                setDefaultPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Via http://stackoverflow.com/a/13239706/328679
                        PackageManager packageManager = getActivity().getPackageManager();

                        ComponentName dummyComponentName = new ComponentName(getActivity().getApplication(),
                                                                DefaultBrowserResetActivity.class);
                        packageManager.setComponentEnabledSetting(dummyComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Config.SET_DEFAULT_BROSWER_URL));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        getActivity().startActivity(intent);

                        packageManager.setComponentEnabledSetting(dummyComponentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
                        return true;
                    }
                });
            }
        }
    }
}
