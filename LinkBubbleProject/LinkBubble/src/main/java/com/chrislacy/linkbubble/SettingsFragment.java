package com.chrislacy.linkbubble;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.util.List;
import java.util.Vector;

/**
 * Created by gw on 11/09/13.
 */
public class SettingsFragment extends PreferenceFragment {

    public static final int MAX_RECENT_BUBBLES = 10;
    private static SettingsFragment sFragment;

    private static String getKey(int i) {
        return "recent_bubbble_" + i;
    }

    private static Vector<String> readRecentBubbles(Context context) {
        Vector<String> urls = new Vector<String>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i=0 ; i < MAX_RECENT_BUBBLES ; ++i) {
            String url = prefs.getString(getKey(i), null);
            if (url != null) {
                urls.add(url);
            }
        }
        return urls;
    }

    private static void writeRecentBubbles(Context context, Vector<String> bubbles) {
        Util.Assert(bubbles.size() <= MAX_RECENT_BUBBLES);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        for (int i=0 ; i < MAX_RECENT_BUBBLES ; ++i) {
            String key = getKey(i);
            if (i < bubbles.size()) {
                editor.putString(key, bubbles.get(i));
            } else {
                editor.remove(key);
            }
        }

        editor.commit();
    }

    public static void addRecentBubble(Context context, String url) {
        Vector<String> recentBubbles = readRecentBubbles(context);
        if (recentBubbles.size() == MAX_RECENT_BUBBLES) {
            recentBubbles.removeElementAt(MAX_RECENT_BUBBLES-1);
        }
        recentBubbles.insertElementAt(url, 0);
        writeRecentBubbles(context, recentBubbles);

        if (sFragment != null) {
            sFragment.updateRecentBubbles(recentBubbles);
        }
    }

    private void updateRecentBubbles(Vector<String> urls) {
        PreferenceScreen recentPS = (PreferenceScreen) findPreference("recent_bubbles");
        if (recentPS != null) {
            recentPS.removeAll();

            for (int i=0 ; i < urls.size() ; ++i) {
                Preference p = new Preference(getActivity());
                p.setTitle(urls.get(i));
                p.setSummary(urls.get(i));

                p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        MainService.openUrl(preference.getTitle().toString(), false);
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

        Preference clearButton = findPreference("clear_history");
        if (clearButton != null) {
            clearButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Vector<String> dummy = new Vector<String>();
                    writeRecentBubbles(getActivity(), dummy);
                    if (sFragment != null) {
                        sFragment.updateRecentBubbles(dummy);
                    }
                    return false;
                }
            });
        }

        Vector<String> bubbles = readRecentBubbles(getActivity());
        updateRecentBubbles(bubbles);
    }

    @Override
    public void onDestroy() {
        sFragment = null;

        super.onDestroy();
    }
}
