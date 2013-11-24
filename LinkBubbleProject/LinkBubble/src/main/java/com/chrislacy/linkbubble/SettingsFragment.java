package com.chrislacy.linkbubble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import android.widget.TextView;

import com.chrislacy.linkbubble.R;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

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

                p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        MainActivity.openLink(getActivity(), preference.getTitle().toString(), false);
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

        Preference loadUrlButton = findPreference("load_url");
        if (loadUrlButton != null) {
            loadUrlButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    //mainActivity.openLink("http://www.google.com");
                    mainActivity.openLink(getActivity(), "http://play.google.com/store/apps/details?id=com.chrislacy.actionlauncher.pro", true);
                    return true;
                }
            });
        }

        final Preference leftConsumeBubblePreference = findPreference(Settings.PREFERENCE_LEFT_CONSUME_BUBBLE);
        if (leftConsumeBubblePreference != null) {
            leftConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog alertDialog = getConfigureBubbleAlert(Config.BubbleAction.ConsumeLeft, leftConsumeBubblePreference);
                    alertDialog.show();
                    return true;
                }
            });
            leftConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeLeft));
        }

        final Preference rightConsumeBubblePreference = findPreference(Settings.PREFERENCE_RIGHT_CONSUME_BUBBLE);
        if (rightConsumeBubblePreference != null) {
            rightConsumeBubblePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog alertDialog = getConfigureBubbleAlert(Config.BubbleAction.ConsumeRight, rightConsumeBubblePreference);
                    alertDialog.show();
                    return true;
                }
            });
            rightConsumeBubblePreference.setSummary(Settings.get().getConsumeBubbleLabel(Config.BubbleAction.ConsumeRight));
        }
    }

    @Override
    public void onDestroy() {
        sFragment = null;

        super.onDestroy();
    }

    public AlertDialog getConfigureBubbleAlert(final Config.BubbleAction bubble, final Preference preference) {

        final ArrayList<ActionItem> actionItems = new ArrayList<ActionItem>();

        String packageName = getActivity().getPackageName();
        PackageManager packageManager = getActivity().getPackageManager();
        Resources resources = getActivity().getResources();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.fdasfjsadfdsfas.com"));        // Something stupid that no non-browser app will handle
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo resolveInfo : resolveInfos) {
            IntentFilter filter = resolveInfo.filter;
            if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                // Ignore LinkBubble from this list
                if (resolveInfo.activityInfo.packageName.equals(packageName) == false) {
                    actionItems.add(new ActionItem(Config.ActionType.View,
                                            resources,
                                            resolveInfo.loadLabel(packageManager).toString(),
                                            resolveInfo.loadIcon(packageManager),
                                            resolveInfo.activityInfo.packageName,
                                            resolveInfo.activityInfo.name));
                }
            }
        }

        // Get list of handler apps that can send
        intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        resolveInfos = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            actionItems.add(new ActionItem(Config.ActionType.Share,
                                            resources,
                                            resolveInfo.loadLabel(packageManager).toString(),
                                            resolveInfo.loadIcon(packageManager),
                                            resolveInfo.activityInfo.packageName,
                                            resolveInfo.activityInfo.name));
        }

        StickyListHeadersListView listView = new StickyListHeadersListView(getActivity());

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(R.string.preference_configure_bubble_title);
        alertDialog.setView(listView);

        Collections.sort(actionItems, new Comparator<ActionItem>() {

            @Override
            public int compare(ActionItem lhs, ActionItem rhs) {
                int categoryComparison = lhs.getCategory().compareTo(rhs.getCategory());
                if (categoryComparison == 0) {
                    return lhs.getLabel().compareTo(rhs.getLabel());
                }
                return categoryComparison;
            }
        });

        ActionItemAdapter adapter = new ActionItemAdapter(getActivity(),
                                                                    R.layout.action_picker_item,
                                                                    actionItems.toArray(new ActionItem[0]));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object tag = view.getTag();
                if (tag instanceof ActionItem) {
                    ActionItem actionItem = (ActionItem) tag;
                    Settings.get().setConsumeBubble(bubble, actionItem.mType, actionItem.getLabel(),
                                                    actionItem.mPackageName, actionItem.mActivityClassName);
                    preference.setSummary(Settings.get().getConsumeBubbleLabel(bubble));
                    alertDialog.dismiss();
                }
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.action_use_default), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }

        });

        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });

        return alertDialog;
    }

    private class ActionItemAdapter extends ArrayAdapter<ActionItem> implements StickyListHeadersAdapter {

        Context mContext;
        int mLayoutResourceId;
        ActionItem mData[] = null;

        public ActionItemAdapter(Context context, int layoutResourceId, ActionItem[] data) {
            super(context, layoutResourceId, data);
            mLayoutResourceId = layoutResourceId;
            mContext = context;
            mData = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView==null){
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(mLayoutResourceId, parent, false);
            }

            ActionItem actionItem = mData[position];

            TextView label = (TextView) convertView.findViewById(R.id.label);
            label.setText(actionItem.getLabel());

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageDrawable(actionItem.mIcon);

            convertView.setTag(actionItem);

            return convertView;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(R.layout.section_header, parent, false);
            ActionItem actionItem = mData[position];
            TextView headerLabel = (TextView)convertView.findViewById(R.id.section_text);
            headerLabel.setText(actionItem.getCategory());
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            ActionItem actionItem = mData[position];
            if (actionItem.mType == Config.ActionType.View) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    private static class ActionItem {

        private String mLabel;
        private Config.ActionType mType;
        private String mCategory;
        private String mPackageName;
        private String mActivityClassName;
        private Drawable mIcon;

        public ActionItem(Config.ActionType type, Resources resources, String label, Drawable icon, String packageName, String activityClassName) {
            mType = type;
            mLabel = label;
            mCategory = resources.getString(type == Config.ActionType.View ? R.string.consume_category_view : R.string.consume_category_share);
            mIcon = icon;
            mPackageName = packageName;
            mActivityClassName = activityClassName;
        }

        public String getLabel() {
            return mLabel;
        }

        public String getCategory() {
            return mCategory;
        }
    }
}
