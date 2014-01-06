package com.linkbubble.ui;

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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.linkbubble.ActionItem;
import com.linkbubble.HistoryRecord;
import com.linkbubble.MainApplication;
import com.linkbubble.MainDatabaseHelper;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.Util;
import com.squareup.otto.Subscribe;

import java.util.Date;
import java.util.List;


public class HistoryActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private ListView mListView;
    private HistoryAdapter mHistoryAdapter;
    private List<HistoryRecord> mHistoryRecords;


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
        mHistoryRecords = databaseHelper.getAllHistoryRecords();
        if (mHistoryRecords == null || mHistoryRecords.size() == 0) {
            return;
        }

        mHistoryAdapter = new HistoryAdapter(this);

        mListView.setAdapter(mHistoryAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        final SwipeDismissListViewTouchListener swipeDismissTouchListener =
                new SwipeDismissListViewTouchListener(
                        mListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;

                                for (int position : reverseSortedPositions) {
                                    Object item = listView.getItemAtPosition(position);
                                    if (item instanceof HistoryRecord) {
                                        databaseHelper.deleteHistoryRecord((HistoryRecord)item);
                                    }
                                }

                                updateListViewData();
                            }
                        });
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(swipeDismissTouchListener.makeScrollListener());
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return swipeDismissTouchListener.onTouch(view, motionEvent);
            }
        });

        mListView.setItemsCanFocus(true);
    }

    void updateListViewData() {
        if (mHistoryRecords != null) {
            synchronized (mHistoryRecords) {
                setupListView();
            }
        } else {
            setupListView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_history: {
                MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
                databaseHelper.deleteAllHistoryRecords();
                mHistoryRecords = null;
                mHistoryAdapter.notifyDataSetChanged();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryRecord) {
            HistoryRecord historyRecord = (HistoryRecord)view.getTag();
            MainApplication.openLink(this, historyRecord.getUrl());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryRecord) {
            final HistoryRecord historyRecord = (HistoryRecord)view.getTag();
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
                            intent.setData(Uri.parse(historyRecord.getUrl()));
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
                                    intent.putExtra(Intent.EXTRA_TEXT, historyRecord.getUrl());
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

    private class HistoryAdapter extends BaseAdapter {

        Context mContext;
        Date mDate;

        public HistoryAdapter(Context context) {
            mContext = context;
            mDate = new Date();
        }

        @Override
        public int getCount() {
            return mHistoryRecords != null ? mHistoryRecords.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mHistoryRecords != null ? mHistoryRecords.get(position) : position;
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

            HistoryRecord historyRecord = mHistoryRecords.get(position);

            TextView title = (TextView) convertView.findViewById(R.id.page_title);
            title.setText(historyRecord.getTitle());

            TextView url = (TextView) convertView.findViewById(R.id.page_url);
            url.setText(historyRecord.getHost());

            TextView time = (TextView) convertView.findViewById(R.id.page_date);
            mDate.setTime(historyRecord.getTime());
            time.setText(Util.getPrettyDate(mDate));

            convertView.setTag(historyRecord);

            return convertView;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHistoryRecordChangedEvent(HistoryRecord.ChangedEvent event) {
        updateListViewData();
    }
}
