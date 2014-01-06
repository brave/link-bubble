package com.linkbubble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
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
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by chrislacy on 5/1/2013.
 */
public class ActionItem {

    String mLabel;
    public Config.ActionType mType;
    String mCategory;
    public String mPackageName;
    public String mActivityClassName;
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

    public interface OnActionItemSelectedListener {
        public void onSelected(ActionItem actionItem);
    }

    private static ArrayList<ActionItem> getActionItems(Context context, boolean viewItems, boolean sendItems) {
        final ArrayList<ActionItem> actionItems = new ArrayList<ActionItem>();

        String packageName = context.getPackageName();
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
        }

        if (sendItems) {
            // Get list of handler apps that can send
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                actionItems.add(new ActionItem(Config.ActionType.Share,
                        resources,
                        resolveInfo.loadLabel(packageManager).toString(),
                        resolveInfo.loadIcon(packageManager),
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name));
            }
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
        ArrayList<ActionItem> actionItems = getActionItems(context, true, false);
        return getActionItemPickerAlert(context, actionItems, R.string.preference_default_browser, onActionItemSelectedListener);
    }

    public static AlertDialog getActionItemPickerAlert(Context context, final List<ResolveInfo> resolveInfos,
                                                       int titleString, final OnActionItemSelectedListener onActionItemSelectedListener) {
        ArrayList<ActionItem> actionItems = new ArrayList<ActionItem>();
        Resources resources = context.getResources();
        PackageManager packageManager = context.getPackageManager();

        if (Settings.get().mLinkBubbleEntryActivityResolveInfo != null) {
            resolveInfos.add(Settings.get().mLinkBubbleEntryActivityResolveInfo);
        }

        for (ResolveInfo resolveInfo : resolveInfos) {
            actionItems.add(new ActionItem(Config.ActionType.View,
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

        return getActionItemPickerAlert(context, actionItems, titleString, onActionItemSelectedListener);
    }

    public static AlertDialog getActionItemPickerAlert(Context context, final ArrayList<ActionItem> actionItems,
                                                       int titleString, final OnActionItemSelectedListener onActionItemSelectedListener) {
        ListView listView = new ListView(context);

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(titleString);
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

        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });

        return alertDialog;
    }

    public static AlertDialog getConfigureBubbleAlert(Context context, final OnActionItemSelectedListener onActionItemSelectedListener) {

        final ArrayList<ActionItem> actionItems = getActionItems(context, true, true);

        StickyListHeadersListView listView = new StickyListHeadersListView(context);

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
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

    public static AlertDialog getShareAlert(Context context, final OnActionItemSelectedListener onActionItemSelectedListener) {

        // Build the list of send applications
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.share_via);
        builder.setIcon(android.R.drawable.sym_def_app_icon);

        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        ArrayList<ActionItem> actionItems = getActionItems(context, false, true);
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

}
