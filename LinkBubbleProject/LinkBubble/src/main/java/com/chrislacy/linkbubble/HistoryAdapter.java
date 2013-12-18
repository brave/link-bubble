package com.chrislacy.linkbubble;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class HistoryAdapter extends ArrayAdapter<LinkHistoryRecord> {

    Context mContext;
    int mLayoutResourceId;
    LinkHistoryRecord mData[] = null;

    public HistoryAdapter(Context context, int layoutResourceId, LinkHistoryRecord[] data) {
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

        LinkHistoryRecord linkHistoryRecord = mData[position];

        TextView title = (TextView) convertView.findViewById(R.id.page_title);
        title.setText(linkHistoryRecord.getTitle());

        TextView url = (TextView) convertView.findViewById(R.id.page_url);
        url.setText(linkHistoryRecord.getUrl());

        convertView.setTag(linkHistoryRecord);

        return convertView;
    }

}
