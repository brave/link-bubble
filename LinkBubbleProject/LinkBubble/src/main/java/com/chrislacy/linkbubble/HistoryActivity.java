package com.chrislacy.linkbubble;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.apps.dashclock.ui.SwipeDismissListViewTouchListener;
import com.squareup.otto.Subscribe;

import java.util.Date;
import java.util.List;


public class HistoryActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private ListView mListView;
    private LinkHistoryAdapter mHistoryAdapter;
    private List<LinkHistoryRecord> mLinkHistoryRecords;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mListView = (ListView) findViewById(R.id.listview);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateListViewData();

        ((MainApplication)getApplicationContext()).getBus().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        ((MainApplication)getApplicationContext()).getBus().unregister(this);
    }

    private void setupListView() {
        MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
        mLinkHistoryRecords = databaseHelper.getAllLinkHistoryRecords();
        if (mLinkHistoryRecords == null || mLinkHistoryRecords.size() == 0) {
            return;
        }

        mHistoryAdapter = new LinkHistoryAdapter(this);

        mListView.setAdapter(mHistoryAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
    }

    void updateListViewData() {
        if (mLinkHistoryRecords != null) {
            synchronized (mLinkHistoryRecords) {
                setupListView();
            }
        } else {
            setupListView();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_history: {
                MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
                databaseHelper.deleteAllLinkHistoryRecords();
                mLinkHistoryRecords = null;
                mHistoryAdapter.notifyDataSetChanged();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof LinkHistoryRecord) {
            LinkHistoryRecord linkHistoryRecord = (LinkHistoryRecord)view.getTag();
            MainApplication.openLink(this, linkHistoryRecord.getUrl());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof LinkHistoryRecord) {
            final LinkHistoryRecord linkHistoryRecord = (LinkHistoryRecord)view.getTag();
            Resources resources = getResources();

            PopupMenu popupMenu;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                popupMenu = new PopupMenu(this, view, Gravity.RIGHT);
            } else {
                popupMenu = new PopupMenu(this, view);
            }

            String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
            if (defaultBrowserLabel != null) {
                popupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                        String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel));
            }

            popupMenu.getMenu().add(Menu.NONE, R.id.item_share, Menu.NONE,
                    resources.getString(R.string.action_share));

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_open_in_browser: {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(linkHistoryRecord.getUrl()));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            MainApplication.loadInBrowser(HistoryActivity.this, intent, true);
                            return true;
                        }

                        case R.id.item_share: {
                            AlertDialog alertDialog = ActionItem.getShareAlert(HistoryActivity.this, new ActionItem.OnActionItemSelectedListener() {
                                @Override
                                public void onSelected(ActionItem actionItem) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(Intent.EXTRA_TEXT, linkHistoryRecord.getUrl());
                                    startActivity(intent);
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    return false;
                }
            });
            popupMenu.show();
            return true;
        }

        return false;
    }

    private class LinkHistoryAdapter extends BaseAdapter {

        Context mContext;
        Date mDate;

        public LinkHistoryAdapter(Context context) {
            mContext = context;
            mDate = new Date();
        }

        @Override
        public int getCount() {
            return mLinkHistoryRecords != null ? mLinkHistoryRecords.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mLinkHistoryRecords != null ? mLinkHistoryRecords.get(position) : position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_item, parent, false);
            }

            LinkHistoryRecord linkHistoryRecord = mLinkHistoryRecords.get(position);

            TextView title = (TextView) convertView.findViewById(R.id.page_title);
            title.setText(linkHistoryRecord.getTitle());

            TextView url = (TextView) convertView.findViewById(R.id.page_url);
            url.setText(linkHistoryRecord.getUrl());

            TextView time = (TextView) convertView.findViewById(R.id.page_date);
            mDate.setTime(linkHistoryRecord.getTime());
            time.setText(Util.getPrettyDate(mDate));

            convertView.setTag(linkHistoryRecord);

            return convertView;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onLinkHistoryRecordChangedEvent(LinkHistoryRecord.ChangedEvent event) {
        updateListViewData();
    }
}
