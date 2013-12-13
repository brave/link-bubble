package com.chrislacy.linkbubble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import java.util.Vector;


/**
 * Created by gw on 11/09/13.
 */
public class SettingsFragment extends PreferenceFragment {

    public static final int MAX_RECENT_BUBBLES = 10;
    private static SettingsFragment sFragment;
    private IncognitoModeChangedEventHandler mIncognitoModeChangedEventHandler;

    public static class RecentBubbleInfo {
        public RecentBubbleInfo(String url, String title, String date) {
            mUrl = url != null ? url : "";
            mTitle = title != null ? title : "";
            mDate = date != null ? date : "";
        }
        public String mUrl;
        public String mTitle;
        public String mDate;
    }

    public interface IncognitoModeChangedEventHandler {
        public void onIncognitoModeChanged(boolean incognito);
    }

    public static void setIncognitoModeChangedEventHandler(IncognitoModeChangedEventHandler eh) {
        Util.Assert(sFragment != null);
        sFragment.mIncognitoModeChangedEventHandler = eh;
    }

    private static String getUrlKey(int i) {
        return "recent_bubbble_url_" + i;
    }
    private static String getTitleKey(int i) {
        return "recent_bubbble_title_" + i;
    }
    private static String getDateKey(int i) {
        return "recent_bubbble_date_" + i;
    }

    private static Vector<RecentBubbleInfo> readRecentBubbles(Context context) {
        Vector<RecentBubbleInfo> items = new Vector<RecentBubbleInfo>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i=0 ; i < MAX_RECENT_BUBBLES ; ++i) {
            String url = prefs.getString(getUrlKey(i), null);
            String title = prefs.getString(getTitleKey(i), null);
            String date = prefs.getString(getDateKey(i), null);
            if (url != null) {
                items.add(new RecentBubbleInfo(url, title, date));
            }
        }
        return items;
    }

    private static void writeRecentBubbles(Context context, Vector<RecentBubbleInfo> bubbles) {
        Util.Assert(bubbles.size() <= MAX_RECENT_BUBBLES);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        for (int i=0 ; i < MAX_RECENT_BUBBLES ; ++i) {
            String urlKey = getUrlKey(i);
            String titleKey = getTitleKey(i);
            String dateKey = getDateKey(i);
            if (i < bubbles.size()) {
                RecentBubbleInfo bi = bubbles.get(i);

                editor.putString(urlKey, bi.mUrl);
                editor.putString(titleKey, bi.mTitle);
                editor.putString(dateKey, bi.mDate);
            } else {
                editor.remove(urlKey);
                editor.remove(titleKey);
                editor.remove(dateKey);
            }
        }

        editor.commit();
    }

    public static void addRecentBubble(Context context, String url, String title, String date) {
        Vector<RecentBubbleInfo> recentBubbles = readRecentBubbles(context);
        if (recentBubbles.size() == MAX_RECENT_BUBBLES) {
            recentBubbles.removeElementAt(MAX_RECENT_BUBBLES-1);
        }
        recentBubbles.insertElementAt(new RecentBubbleInfo(url, title, date), 0);
        writeRecentBubbles(context, recentBubbles);

        if (sFragment != null) {
            sFragment.updateRecentBubbles(recentBubbles);
        }
    }

    private void updateRecentBubbles(Vector<RecentBubbleInfo> urls) {
        PreferenceScreen recentPS = (PreferenceScreen) findPreference("recent_bubbles");
        if (recentPS != null) {
            recentPS.removeAll();

            Preference removeAllPref = new Preference(getActivity());
            removeAllPref.setTitle("Clear All");
            removeAllPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Vector<RecentBubbleInfo> dummy = new Vector<RecentBubbleInfo>();
                    writeRecentBubbles(getActivity(), dummy);
                    if (sFragment != null) {
                        sFragment.updateRecentBubbles(dummy);
                    }
                    return false;
                }
            });
            recentPS.addPreference(removeAllPref);

            for (int i=0 ; i < urls.size() ; ++i) {
                Preference p = new Preference(getActivity());
                RecentBubbleInfo bi = urls.get(i);
                p.setTitle(bi.mTitle);
                String summary = bi.mUrl;
                if (bi.mDate != null && bi.mDate.length() > 0) {
                    summary += "\n" + bi.mDate;
                }
                p.setSummary(summary);

                p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        MainApplication.openLink(getActivity(), preference.getTitle().toString(), false);
                        return true;
                    }
                });

                recentPS.addPreference(p);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.prefs);

        sFragment = this;

        CheckBoxPreference autoLoadContentPreference = (CheckBoxPreference)findPreference(Settings.PREFERENCE_AUTO_LOAD_CONTENT);
        autoLoadContentPreference.setChecked(Settings.get().autoLoadContent());

        Preference incognitoButton = findPreference("preference_incognito");
        if (incognitoButton != null) {
            incognitoButton.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mIncognitoModeChangedEventHandler != null) {
                        mIncognitoModeChangedEventHandler.onIncognitoModeChanged((Boolean)newValue);
                    }
                    return true;
                }
            });
        }

        Preference clearButton = findPreference("clear_history");
        if (clearButton != null) {
            clearButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Vector<RecentBubbleInfo> dummy = new Vector<RecentBubbleInfo>();
                    writeRecentBubbles(getActivity(), dummy);
                    if (sFragment != null) {
                        sFragment.updateRecentBubbles(dummy);
                    }
                    return false;
                }
            });
        }

        Vector<RecentBubbleInfo> bubbles = readRecentBubbles(getActivity());
        updateRecentBubbles(bubbles);

        Preference loadUrlButton = findPreference("load_url");
        if (loadUrlButton != null) {
            loadUrlButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //mainActivity.openLink("http://www.google.com");
                    MainApplication.openLink(getActivity(), "http://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro", true);

                    /*
                    Intent openIntent = new Intent(Intent.ACTION_VIEW);
                    openIntent.setClassName("com.android.vending", "com.google.android.finsky.activities.MainActivity");
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
        defaultBrowserPreference.setSummary(Settings.get().getDefaultBrowserLabel());
        defaultBrowserPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog alertDialog = ActionItem.getDefaultBrowserAlert(getActivity(), new ActionItem.OnActionItemSelectedListener() {
                    @Override
                    public void onSelected(ActionItem actionItem) {
                        Settings.get().setDefaultBrowser(actionItem.getLabel(), actionItem.mPackageName);
                        preference.setSummary(Settings.get().getDefaultBrowserLabel());
                    }
                });
                alertDialog.show();
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        sFragment = null;

        super.onDestroy();
    }

}
