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

import java.net.URL;
import java.util.List;

class BubbleCoverFlowAdapter extends FancyCoverFlowAdapter {

    Context mContext;
    List<Bubble> mBubbles;
    int mItemSize;

    public final int mSize;
    private final int mMiddleIndex;

    public BubbleCoverFlowAdapter(Context context, List<Bubble> bubbles) {
        super();
        mContext = context;
        mBubbles = bubbles;

        mItemSize = context.getResources().getDimensionPixelSize(R.dimen.bubble_cover_flow_image_size);

        mSize = Integer.MAX_VALUE;
        int halfMaxValue = mSize/2;
        mMiddleIndex = halfMaxValue - halfMaxValue % bubbles.size();
    }

    // =============================================================================
    // Supertype overrides
    // =============================================================================

    public int getMiddleIndex() {
        return mMiddleIndex;
    }

    @Override
    public int getCount() {
        return mSize;
    }

    @Override
    public Bubble getItem(int position) {
        return mBubbles.get(position % mBubbles.size());
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

        Bubble info = getItem(i);

        Drawable icon = mContext.getResources().getDrawable(R.drawable.circle_grey);
        icon.setBounds(0, 0, mItemSize, mItemSize);
        textView.setCompoundDrawables(null, icon, null, null);

        URL url = info.getUrl();
        textView.setText(url.getHost());
        return textView;
    }
}