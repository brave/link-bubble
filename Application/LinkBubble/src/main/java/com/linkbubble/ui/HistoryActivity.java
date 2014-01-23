package com.linkbubble.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.linkbubble.db.DatabaseHelper;
import com.linkbubble.util.ActionItem;
import com.linkbubble.db.HistoryRecord;
import com.linkbubble.MainApplication;
import com.linkbubble.R;
import com.linkbubble.Settings;
import com.linkbubble.util.Util;
import com.squareup.otto.Subscribe;
import org.mozilla.gecko.favicons.Favicons;
import org.mozilla.gecko.favicons.LoadFaviconTask;
import org.mozilla.gecko.favicons.OnFaviconLoadedListener;

import java.util.Date;
import java.util.List;


public class HistoryActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private ListView mListView;
    private HistoryAdapter mHistoryAdapter;
    private List<HistoryRecord> mHistoryRecords;

    private static int sInstanceCount = 0;
    private static Favicons sFavicons = null;
    private static final int FAVICON_CACHE_SIZE = 4 * 1024 * 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sInstanceCount++;
        if (sInstanceCount == 1) {
            sFavicons = new Favicons(FAVICON_CACHE_SIZE);
        }

        setContentView(R.layout.activity_history);

        mListView = (ListView) findViewById(R.id.listview);
    }

    @Override
    protected void onDestroy() {
        sInstanceCount--;
        if (sInstanceCount == 0) {
            sFavicons = null;
        }

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        setupListView();

        ((MainApplication)getApplicationContext()).getBus().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        ((MainApplication)getApplicationContext()).getBus().unregister(this);
    }

    private void setupListView() {
        DatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
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
                                DatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;

                                for (int position : reverseSortedPositions) {
                                    Object item = listView.getItemAtPosition(position);
                                    if (item instanceof HistoryRecord) {
                                        databaseHelper.deleteHistoryRecord((HistoryRecord)item);
                                        mHistoryRecords.remove(item);
                                    }
                                }

                                mHistoryAdapter.notifyDataSetChanged();
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
                final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(R.string.erase_all_history_title);
                alertDialog.setMessage(getString(R.string.erase_all_history_message));
                alertDialog.setCancelable(true);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
                        databaseHelper.deleteAllHistoryRecords();
                        mHistoryRecords = null;
                        mHistoryAdapter.notifyDataSetChanged();
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryItem) {
            HistoryItem historyItem = (HistoryItem)view.getTag();
            MainApplication.openLink(this, historyItem.mHistoryRecord.getUrl());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof HistoryItem) {
            final HistoryItem historyItem = (HistoryItem)view.getTag();
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
                            intent.setData(Uri.parse(historyItem.mHistoryRecord.getUrl()));
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
                                    intent.putExtra(Intent.EXTRA_TEXT, historyItem.mHistoryRecord.getUrl());
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

        LayoutInflater mInflater;

        public HistoryAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

            HistoryItem historyItem;
            HistoryRecord historyRecord = mHistoryRecords.get(position);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.history_item, parent, false);
                historyItem = new HistoryItem();
                historyItem.mTitleTextView = (TextView) convertView.findViewById(R.id.page_title);
                historyItem.mUrlTextView = (TextView) convertView.findViewById(R.id.page_url);
                historyItem.mTimeTextView = (TextView) convertView.findViewById(R.id.page_date);
                historyItem.mFaviconImageView = (ImageView) convertView.findViewById(R.id.favicon);
            } else {
                historyItem = (HistoryItem) convertView.getTag();
            }

            historyItem.mHistoryRecord = historyRecord;
            historyItem.mDate.setTime(historyRecord.getTime());
            historyItem.mFaviconSet = false;

            historyItem.mTitleTextView.setText(historyRecord.getTitle());
            historyItem.mUrlTextView.setText(historyRecord.getHost());
            historyItem.mTimeTextView.setText(Util.getPrettyDate(historyItem.mDate));

            int flags = Settings.get().isIncognitoMode() ? 0 : LoadFaviconTask.FLAG_PERSIST;
            String host = historyRecord.getHost();
            String faviconUrl = "http://" + host + "/favicon.ico";

            historyItem.mFaviconUrl = faviconUrl;
            sFavicons.getFaviconForSize(host, faviconUrl, Integer.MAX_VALUE, flags, historyItem.mOnFaviconLoadedListener);
            if (historyItem.mFaviconSet == false) {
                historyItem.mFaviconImageView.setImageResource(R.drawable.fallback_favicon);
            }

            convertView.setTag(historyItem);

            return convertView;
        }
    }

    private static class HistoryItem {
        TextView mTitleTextView;
        TextView mUrlTextView;
        TextView mTimeTextView;
        ImageView mFaviconImageView;
        HistoryRecord mHistoryRecord;
        Date mDate = new Date();
        String mFaviconUrl;
        boolean mFaviconSet;
        OnFaviconLoadedListener mOnFaviconLoadedListener = new OnFaviconLoadedListener() {
            @Override
            public void onFaviconLoaded(String url, String faviconURL, Bitmap favicon) {
                // Ensure the favicon passed in matches the one we want. This can be false as HistoryAdapter recycles
                // Views and favicons are loaded in different orders to that which they are requested.
                if (mFaviconUrl.equals(faviconURL) == false) {
                    return;
                }
                if (favicon != null) {
                    mFaviconSet = true;
                    mFaviconImageView.setImageBitmap(favicon);
                }
            }
        };
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onHistoryRecordChangedEvent(HistoryRecord.ChangedEvent event) {
        HistoryRecord historyRecord = event.mHistoryRecord;
        // find out if the item exists on the list already. This will be true if a HistoryRecord for a URL was updated
        boolean onList = false;
        for (HistoryRecord existing : mHistoryRecords) {
            if (existing.getId() == historyRecord.getId()) {
                mHistoryRecords.remove(existing);
                break;
            }
        }

        // Add it at the top of the list. This assumes the item had it's date updated to 'now',
        // which is the current behaviour.
        mHistoryRecords.add(0, historyRecord);

        mHistoryAdapter.notifyDataSetChanged();
    }
}
