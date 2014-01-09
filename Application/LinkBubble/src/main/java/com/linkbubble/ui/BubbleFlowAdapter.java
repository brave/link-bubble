package com.linkbubble.ui;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import at.technikum.mti.fancycoverflow.FancyCoverFlowAdapter;
import com.linkbubble.R;

import java.util.List;

public class BubbleFlowAdapter extends FancyCoverFlowAdapter {


    Context mContext;
    List<BubbleFlowItemView> mBubbles;
    int mItemSize;

    //private final int mSize;
    //private final int mStartIndex;
    private final boolean mLoop;

    public BubbleFlowAdapter(Context context, boolean loop) {
        super();
        mContext = context;

        mItemSize = context.getResources().getDimensionPixelSize(R.dimen.bubble_cover_flow_item_size);

        mLoop = loop;
        /*
        if (mLoop) {
            mSize = Integer.MAX_VALUE;
            int halfMaxValue = mSize/2;
            mStartIndex = halfMaxValue - halfMaxValue % bubbles.size();
        } else {
            mSize = bubbles.size();
            mStartIndex = 0;
        }*/
    }

    void setBubbles(List<BubbleFlowItemView> bubbles) {
        mBubbles = bubbles;
        notifyDataSetChanged();
    }

    // =============================================================================
    // Supertype overrides
    // =============================================================================

    /*
    public int getStartIndex() {
        return mStartIndex;
    }*/

    @Override
    public int getCount() {
        //return mSize;
        return mBubbles == null ? 0 : mBubbles.size();
    }

    @Override
    public BubbleView getItem(int position) {
        return mBubbles.get(position % mBubbles.size());
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getCoverFlowItem(int i, View reuseableView, ViewGroup viewGroup) {
        ViewHolder viewHolder;

        if (reuseableView != null) {
            viewHolder = (ViewHolder) reuseableView.getTag();
        } else {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            reuseableView = inflater.inflate(R.layout.view_bubble_cover_flow_item, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.mFaviconImageView = (ImageView) reuseableView.findViewById(R.id.favicon);
            //viewHolder.mTextView = (TextView) reuseableView.findViewById(R.id.text_view);
            reuseableView.setTag(viewHolder);
            reuseableView.setLayoutParams(new FancyCoverFlow.LayoutParams(mItemSize, mItemSize));
        }

        BubbleView bubble = getItem(i);
        //viewHolder.mTextView.setText(info.getUrl().getHost());
        viewHolder.mFaviconImageView.setImageDrawable(bubble.getFavicon());
        bubble.setAdditionalFaviconView(viewHolder.mFaviconImageView);
        return reuseableView;
    }

    private static class ViewHolder {
        //TextView mTextView;
        ImageView mFaviconImageView;
    }

}
