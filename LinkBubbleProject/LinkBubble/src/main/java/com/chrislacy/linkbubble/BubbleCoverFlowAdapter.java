package com.chrislacy.linkbubble;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;

import java.util.List;

class BubbleCoverFlowAdapter extends FancyCoverFlowAdapter {

    List<ActivityManager.RunningTaskInfo> mRunningTaskInfos;
    PackageManager mPackageManager;
    int mItemSize;

    public static final int HALF_MAX_VALUE = Integer.MAX_VALUE/2;
    public final int MIDDLE;

    public BubbleCoverFlowAdapter(Context context, List<ActivityManager.RunningTaskInfo> runningTaskInfos,
                              PackageManager packageManager) {
        super();
        mRunningTaskInfos = runningTaskInfos;
        mPackageManager = packageManager;

        mItemSize = context.getResources().getDimensionPixelSize(R.dimen.bubble_cover_flow_image_size);
        MIDDLE = HALF_MAX_VALUE - HALF_MAX_VALUE % runningTaskInfos.size();
    }

    // =============================================================================
    // Supertype overrides
    // =============================================================================

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ActivityManager.RunningTaskInfo  getItem(int position) {
        return mRunningTaskInfos.get(position % mRunningTaskInfos.size());
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getCoverFlowItem(int i, View reuseableView, ViewGroup viewGroup) {
        TextView textView;

        if (reuseableView != null) {
            textView = (TextView) reuseableView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            textView = (TextView) inflater.inflate(R.layout.view_bubble_cover_flow_item, viewGroup, false);
        }

        ActivityManager.RunningTaskInfo info = getItem(i);
        final ComponentName componentName = info.topActivity;

        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(componentName.getPackageName(), 0);
            Drawable icon = applicationInfo.loadIcon(mPackageManager);
            icon.setBounds(0, 0, mItemSize, mItemSize);
            textView.setCompoundDrawables(null, icon, null, null);
            textView.setText(applicationInfo.loadLabel(mPackageManager));
        } catch (PackageManager.NameNotFoundException e) {
        }
        return textView;
    }
}