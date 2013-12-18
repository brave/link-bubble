package com.chrislacy.linkbubble;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class HistoryAdapter extends ArrayAdapter<Settings.RecentBubbleInfo> {

    Context mContext;
    int mLayoutResourceId;
    Settings.RecentBubbleInfo mData[] = null;

    public HistoryAdapter(Context context, int layoutResourceId, Settings.RecentBubbleInfo[] data) {
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

        Settings.RecentBubbleInfo recentBubbleInfo = mData[position];

        TextView title = (TextView) convertView.findViewById(R.id.page_title);
        title.setText(recentBubbleInfo.mTitle);

        TextView url = (TextView) convertView.findViewById(R.id.page_url);
        url.setText(recentBubbleInfo.mUrl);

        convertView.setTag(recentBubbleInfo);

        return convertView;
    }

}
