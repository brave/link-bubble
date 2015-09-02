package com.linkbubble.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.linkbubble.BuildConfig;
import com.linkbubble.Constant;
import com.linkbubble.R;
import com.linkbubble.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ActionItem {

    String mLabel;
    public Constant.ActionType mType;
    String mCategory;
    public String mPackageName;
    public String mActivityClassName;
    private Drawable mIcon;

    public ActionItem(Constant.ActionType type, Resources resources, String label, Drawable icon, String packageName, String activityClassName) {
        mType = type;
        mLabel = label;
        mCategory = resources.getString(type == Constant.ActionType.View ? R.string.consume_category_view : R.string.consume_category_share);
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

    public interface OnActionItemSelectedListener {
        public void onSelected(ActionItem actionItem);
    }

    public interface OnActionItemDefaultSelectedListener {
        public void onSelected(ActionItem actionItem, boolean always);
    }

    private static ArrayList<ActionItem> getActionItems(Context context, boolean viewItems, boolean sendItems, boolean sharePicker) {
        final ArrayList<ActionItem> actionItems = new ArrayList<ActionItem>();

        PackageManager packageManager = context.getPackageManager();
        Resources resources = context.getResources();

        if (viewItems) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://www.fdasfjsadfdsfas.com"));        // Something stupid that no non-browser app will handle
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities (intent, PackageManager.GET_RESOLVED_FILTER);
            for (ResolveInfo resolveInfo : resolveInfos) {
                IntentFilter filter = resolveInfo.filter;
                if (filter != null && filter.hasAction(Intent.ACTION_VIEW) && filter.hasCategory(Intent.CATEGORY_BROWSABLE)) {
                    // Ignore LinkBubble from this list
                    if (Util.isValidBrowserPackageName(resolveInfo.activityInfo.packageName)) {
                        actionItems.add(new ActionItem(Constant.ActionType.View,
                                resources,
                                resolveInfo.loadLabel(packageManager).toString(),
                                resolveInfo.loadIcon(packageManager),
                                resolveInfo.activityInfo.packageName,
                                resolveInfo.activityInfo.name));
                    }
                }
            }
        }

        if (sendItems) {
            // Get list of handler apps that can send
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo.packageName.equals(BuildConfig.APPLICATION_ID) == false) {
                    actionItems.add(new ActionItem(Constant.ActionType.Share,
                            resources,
                            resolveInfo.loadLabel(packageManager).toString(),
                            resolveInfo.loadIcon(packageManager),
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));
                }
            }
        }

        if (sharePicker) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            actionItems.add(new ActionItem(Constant.ActionType.Share,
                    resources,
                    context.getString(R.string.share_picker_label),
                    context.getResources().getDrawable(R.drawable.ic_share_grey600_24dp),
                    BuildConfig.APPLICATION_ID,
                    Constant.SHARE_PICKER_NAME));
        }

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

        return actionItems;
    }

    public static AlertDialog getDefaultBrowserAlert(Context context, final OnActionItemSelectedListener onActionItemSelectedListener) {
        ArrayList<ActionItem> actionItems = getActionItems(context, true, false, false);

        ListView listView = new ListView(context);

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setIcon(Util.getAlertIcon(context));
        alertDialog.setTitle(R.string.preference_default_browser);
        alertDialog.setView(listView);

        ActionItemAdapter adapter = new ActionItemAdapter(context,
                R.layout.action_picker_item,
                actionItems.toArray(new ActionItem[0]));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object tag = view.getTag();
                if (tag instanceof ActionItem) {
                    if (onActionItemSelectedListener != null) {
                        onActionItemSelectedListener.onSelected((ActionItem) tag);
                    }
                    //Settings.get().setConsumeBubble(bubble, actionItem.mType, actionItem.getLabel(),
                    //        actionItem.mPackageName, actionItem.mActivityClassName);
                    //preference.setSummary(Settings.get().getConsumeBubbleLabel(bubble));
                    alertDialog.dismiss();
                }
            }
        });

        return alertDialog;
    }

    public static AlertDialog getActionItemPickerAlert(Context context, final List<ResolveInfo> resolveInfos,
                                                       int titleString, final OnActionItemDefaultSelectedListener onActionItemDefaultSelectedListener) {
        final ArrayList<ActionItem> actionItems = new ArrayList<ActionItem>();
        Resources resources = context.getResources();
        PackageManager packageManager = context.getPackageManager();

        final int backgroundColorResourceId = Settings.get().getDarkThemeEnabled() ? R.color.color_list_background_dark : R.color.color_list_background_light;
        final int selectedBackgroundColorResourceId = Settings.get().getDarkThemeEnabled() ? R.color.color_list_selected_background_dark
                : R.color.color_list_selected_background_light;


        for (ResolveInfo resolveInfo : resolveInfos) {
            actionItems.add(new ActionItem(Constant.ActionType.View,
                    resources,
                    resolveInfo.loadLabel(packageManager).toString(),
                    resolveInfo.loadIcon(packageManager),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name));
        }

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

        class ActionItemListView extends ListView {

            boolean mDefaultSet = false;
            long mLastItemClickTime = -1;

            public ActionItemListView(Context context) {
                super(context);
            }

            @Override
            public void draw(Canvas canvas) {
                if (mDefaultSet == false) {
                    Object tag = getTag();
                    if (tag != null && tag instanceof Integer) {
                        int selectedIndex = (Integer)tag;
                        if (getChildCount() > selectedIndex) {
                            View child = getChildAt(selectedIndex);
                            if (child != null) {
                                child.setBackgroundResource(selectedBackgroundColorResourceId);
                                mDefaultSet = true;
                            }
                        }
                    }
                }
                super.draw(canvas);
            }
        }

        final ActionItemListView listView = new ActionItemListView(context);

        for (int i = 0; i < actionItems.size(); i++) {
            ActionItem actionItem = actionItems.get(i);
            if (actionItem.mPackageName.equals(context.getPackageName())) {
                continue;
            }
            // Set the first non-LinkBubble item as the current selection
            listView.setTag(i);
            break;
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setIcon(Util.getAlertIcon(context));
        alertDialog.setTitle(titleString);
        alertDialog.setView(listView);

        final ActionItemAdapter adapter = new ActionItemAdapter(context,
                R.layout.action_picker_item,
                actionItems.toArray(new ActionItem[0]));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                long currentTime = System.currentTimeMillis();
                long clickDelta = currentTime - listView.mLastItemClickTime;
                // Check for a double-tap to emulate the behavior of the AOSP default app picker
                if (clickDelta < 350) {
                    int selected = (Integer)listView.getTag();
                    if (selected == position) {
                        ActionItem actionItem = actionItems.get(position);
                        if (onActionItemDefaultSelectedListener != null) {
                            onActionItemDefaultSelectedListener.onSelected(actionItem, false);
                        }
                        alertDialog.dismiss();
                        return;
                    }
                }

                listView.mLastItemClickTime = currentTime;

                int viewChildCount = listView.getChildCount();
                for (int i = 0; i < viewChildCount; i++) {
                    View child = listView.getChildAt(i);
                    child.setBackgroundResource(backgroundColorResourceId);
                }
                view.setBackgroundResource(selectedBackgroundColorResourceId);
                listView.setTag(position);

                /*
                Object tag = view.getTag();
                if (tag instanceof ActionItem) {
                    if (onActionItemDefaultSelectedListener != null) {
                        onActionItemDefaultSelectedListener.onSelected((ActionItem) tag);
                    }
                    //Settings.get().setConsumeBubble(bubble, actionItem.mType, actionItem.getLabel(),
                    //        actionItem.mPackageName, actionItem.mActivityClassName);
                    //preference.setSummary(Settings.get().getConsumeBubbleLabel(bubble));
                    alertDialog.dismiss();
                }*/
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.activity_resolver_use_once), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int selectedItem = (Integer) listView.getTag();
                    ActionItem actionItem = actionItems.get(selectedItem);
                    if (onActionItemDefaultSelectedListener != null) {
                        onActionItemDefaultSelectedListener.onSelected(actionItem, false);
                    }
                } catch (NullPointerException npe) {
                    // XXX: ResolveInfos returning null in M preview releases.
                    // See if we can remove this try/catch when M in final, but for now handle the crash.
                    // Implemented in: 89b785a911f734e6ce6b0ecd1b7cb0ff75e88c25
                    CrashTracking.logHandledException(npe);
                }
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.activity_resolver_use_always), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int selectedItem = (Integer) listView.getTag();
                    ActionItem actionItem = actionItems.get(selectedItem);
                    if (onActionItemDefaultSelectedListener != null) {
                        onActionItemDefaultSelectedListener.onSelected(actionItem, true);
                    }
                } catch (NullPointerException npe) {
                    // XXX: ResolveInfos returning null in M preview releases.
                    // See if we can remove this try/catch when M in final, but for now handle the crash.
                    // Implemented in: 89b785a911f734e6ce6b0ecd1b7cb0ff75e88c25
                    CrashTracking.logHandledException(npe);
                }
            }
        });

        return alertDialog;
    }

    public static AlertDialog getConfigureBubbleAlert(Context context, final OnActionItemSelectedListener onActionItemSelectedListener) {

        final ArrayList<ActionItem> actionItems = getActionItems(context, true, true, true);

        StickyListHeadersListView listView = new StickyListHeadersListView(context);

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setIcon(Util.getAlertIcon(context));
        alertDialog.setTitle(R.string.preference_configure_bubble_title);
        alertDialog.setView(listView);

        ActionItemAdapter adapter = new ActionItemAdapter(context,
                R.layout.action_picker_item,
                actionItems.toArray(new ActionItem[0]));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object tag = view.getTag();
                if (tag instanceof ActionItem) {
                    if (onActionItemSelectedListener != null) {
                        onActionItemSelectedListener.onSelected((ActionItem) tag);
                    }
                    alertDialog.dismiss();
                }
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.action_use_default), new DialogInterface.OnClickListener() {

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

    public static AlertDialog getShareAlert(Context context, boolean showSharePicker, final OnActionItemSelectedListener onActionItemSelectedListener) {

        // Build the list of send applications
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(Util.getAlertIcon(context));
        builder.setTitle(R.string.share_via);
        builder.setIcon(R.drawable.ic_launcher);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        ArrayList<ActionItem> actionItems = getActionItems(context, false, true, showSharePicker);
        ActionItemAdapter adapter = new ActionItemAdapter(context,
                R.layout.action_picker_item,
                actionItems.toArray(new ActionItem[0]));

        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object tag = view.getTag();
                if (tag instanceof ActionItem) {
                    if (onActionItemSelectedListener != null) {
                        onActionItemSelectedListener.onSelected((ActionItem) tag);
                    }
                    alertDialog.dismiss();
                }
            }
        });
        alertDialog.setView(listView);
        return alertDialog;
    }

    private static class ActionItemAdapter extends ArrayAdapter<ActionItem> implements StickyListHeadersAdapter {

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
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            convertView = inflater.inflate(R.layout.view_section_header, parent, false);
            ActionItem actionItem = mData[position];
            TextView headerLabel = (TextView)convertView.findViewById(R.id.section_text);
            headerLabel.setText(actionItem.getCategory());
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            ActionItem actionItem = mData[position];
            if (actionItem.mType == Constant.ActionType.View) {
                return 0;
            } else {
                return 1;
            }
        }
    }

}
