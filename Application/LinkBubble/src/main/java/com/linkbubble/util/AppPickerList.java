/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.util;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import com.linkbubble.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AppPickerList {

    public static class AppInfo {
        String mActivityName;
        public String mPackageName;
        String mDisplayName;
        String mSortName;
        Intent mIntent;
        boolean mChecked;

        AppInfo(String activityName, String packageName, String displayName) {
            mActivityName = activityName;
            mPackageName = packageName;
            mDisplayName = displayName;
            mSortName = displayName.toLowerCase(Locale.getDefault());

            mIntent = new Intent(Intent.ACTION_MAIN);
            mIntent.setPackage(mPackageName);
            mIntent.setComponent(new ComponentName(mPackageName, mActivityName));
        }
    };

    public static class AppInfoComparator implements Comparator<AppInfo> {
        @Override
        public int compare(AppInfo lhs, AppInfo rhs) {
            return lhs.mSortName.compareTo(rhs.mSortName);
        }
    }

    static class AppPickerListInfo {
        ArrayList<AppInfo> mAllApps;
        CheckedTextView mSingleCheckedTextView;
    }

    public enum SelectionType {
        SingleSelection,
        MultipleSelection,
    }

    public interface Initializer {
        boolean setChecked(String packageName, String activityName);
        boolean addToList(String packageName);
    }

    public static View createView(final Context context, final IconCache iconCache, final SelectionType selectionType, Initializer initializer) {

        PackageManager pm = context.getPackageManager();
        //final IconCache iconCache = ((LauncherApplication)context.getApplicationContext()).getIconCache();
        final int itemLayout = selectionType == SelectionType.SingleSelection ? R.layout.app_picker_list_item_single : R.layout.app_picker_list_item_multiple;

        ListView listView = (ListView) View.inflate(context, R.layout.app_picker_list, null);

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allResolveInfo = pm.queryIntentActivities(mainIntent, 0);

        final AppPickerListInfo appPickerListInfo = new AppPickerListInfo();
        appPickerListInfo.mAllApps = new ArrayList<AppInfo>();

        for (ResolveInfo info : allResolveInfo) {
            if (info.activityInfo != null && info.activityInfo.packageName != null) {
                if (initializer.addToList(info.activityInfo.packageName)) {
                    // This is the G+ "Photos" Activity. Ignore it.
                    if (info.activityInfo.name.equals("com.google.android.apps.plus.phone.ConversationListActivity") == false) {
                        appPickerListInfo.mAllApps.add(new AppInfo(info.activityInfo.name, info.activityInfo.packageName, info.loadLabel(pm).toString()));
                    }
                }
            }
        }
        Collections.sort(appPickerListInfo.mAllApps, new AppInfoComparator());

        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(context, itemLayout) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView==null){
                    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(itemLayout, parent, false);
                }

                AppInfo appInfo = appPickerListInfo.mAllApps.get(position);
                Bitmap icon = iconCache.getIcon(appInfo.mIntent);
                if (icon != null) {
                    ((ImageView) convertView.findViewById(R.id.image_view)).setImageBitmap(icon);
                }

                CheckedTextView checkedTextView = ((CheckedTextView) convertView.findViewById(R.id.checked_text_view));
                checkedTextView.setText(appInfo.mDisplayName);
                checkedTextView.setChecked(appInfo.mChecked);

                convertView.setTag(appInfo);

                return convertView;
            }
        };

        for (AppInfo appInfo : appPickerListInfo.mAllApps) {
            listAdapter.add(appInfo.mDisplayName);
        }

        listView.setAdapter(listAdapter);
        if (initializer != null) {
            for (AppInfo app : appPickerListInfo.mAllApps) {
                if (initializer.setChecked(app.mPackageName, app.mActivityName)) {
                    app.mChecked = true;
                }
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo appInfo = (AppInfo) view.getTag();
                appInfo.mChecked = !appInfo.mChecked;
                CheckedTextView checkedTextView = ((CheckedTextView) view.findViewById(R.id.checked_text_view));

                if (selectionType == SelectionType.SingleSelection) {
                    for (AppInfo app : appPickerListInfo.mAllApps) {
                        if (app != appInfo && app.mChecked) {
                            app.mChecked = false;
                        }
                    }
                    if (appPickerListInfo.mSingleCheckedTextView != null) {
                        appPickerListInfo.mSingleCheckedTextView.setChecked(false);
                    }
                }

                appPickerListInfo.mSingleCheckedTextView = checkedTextView;
                appPickerListInfo.mSingleCheckedTextView.setChecked(appInfo.mChecked);
            }
        });

        listView.setTag(appPickerListInfo);
        return listView;
    }

    public static ArrayList<AppInfo> getSelected(View view) {
        ArrayList<AppInfo> result = new ArrayList<AppInfo>();

        AppPickerListInfo appPickerListInfo = (AppPickerListInfo) view.getTag();
        for (AppInfo appInfo : appPickerListInfo.mAllApps) {
            if (appInfo.mChecked) {
                result.add(appInfo);
            }
        }

        appPickerListInfo.mSingleCheckedTextView = null;

        return result.size() > 0 ? result : null;
    }

    public static ArrayList<AppInfo> getUnselected(View view) {
        ArrayList<AppInfo> result = new ArrayList<AppInfo>();

        AppPickerListInfo appPickerListInfo = (AppPickerListInfo) view.getTag();
        for (AppInfo appInfo : appPickerListInfo.mAllApps) {
            if (appInfo.mChecked == false) {
                result.add(appInfo);
            }
        }

        appPickerListInfo.mSingleCheckedTextView = null;

        return result.size() > 0 ? result : null;
    }

}
