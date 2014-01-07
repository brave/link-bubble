package com.linkbubble.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.linkbubble.Constant;
import com.linkbubble.Settings;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LinkBubbleDB";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "LinkBubbleDB";

    private static final String TABLE_LINK_HISTORY = "linkHistory";
    private static final String TABLE_FAVICON_CACHE = "favicons";

    // Link History Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_HOST = "host";
    private static final String KEY_TIME = "time";
    private static final String KEY_PAGE_URL = "pageUrl";
    private static final String KEY_DATA = "data";

    private static final String[] LINK_HISTORY_COLUMNS = {KEY_ID, KEY_TITLE, KEY_URL, KEY_HOST, KEY_TIME};
    private static final String[] FAVICON_COLUMNS = {KEY_ID, KEY_URL, KEY_PAGE_URL, KEY_DATA};
    private static final String[] FAVICON_DATA = {KEY_DATA};

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LINK_HISTORY_TABLE = "CREATE TABLE " + TABLE_LINK_HISTORY + " ( " +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_TITLE + " TEXT, " +
                KEY_URL + " TEXT, " +
                KEY_HOST + " TEXT, " +
                KEY_TIME + " INTEGER" + " )";

        db.execSQL(CREATE_LINK_HISTORY_TABLE);

        String CREATE_FAVICON_CACHE_TABLE = "CREATE TABLE " + TABLE_FAVICON_CACHE + " ( " +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_URL + " TEXT, " +
                KEY_PAGE_URL + " TEXT, " +
                KEY_DATA + " BLOB, " +
                KEY_TIME + " INTEGER" + " )";

        db.execSQL(CREATE_FAVICON_CACHE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINK_HISTORY);

        // create fresh table
        onCreate(db);
    }

    private ContentValues getContentValues(HistoryRecord historyRecord) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, historyRecord.getTitle());
        values.put(KEY_URL, historyRecord.getUrl());
        values.put(KEY_HOST, historyRecord.getHost());
        //values.put(KEY_TIME, historyRecord.getTime());
        return values;
    }

    public void addHistoryRecord(HistoryRecord historyRecord){
        if (Settings.get().isIncognitoMode() == true) {
            return;
        }

        Log.d(TAG, "addHistoryRecord() - " + historyRecord.toString());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LINK_HISTORY, null, getContentValues(historyRecord));
        db.close();
    }

    public int updateHistoryRecord(HistoryRecord historyRecord) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getContentValues(historyRecord);
        int i = db.update(TABLE_LINK_HISTORY, values,
                            KEY_ID + " = ?", new String[] { String.valueOf(historyRecord.getId()) });
        db.close();
        return i;
    }

    public void deleteHistoryRecord(HistoryRecord historyRecord) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LINK_HISTORY, KEY_ID + " = ?", new String[] { String.valueOf(historyRecord.getId()) });
        db.close();

        Log.d(TAG, "deleted historyRecord:" + historyRecord.toString());
    }

    public void deleteAllHistoryRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LINK_HISTORY, null, null);
        db.close();
    }

    public HistoryRecord getHistoryRecord(int id){

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_LINK_HISTORY, // a. table
                                    LINK_HISTORY_COLUMNS, // b. column names
                                    " id = ?", // c. selections
                                    new String[] { String.valueOf(id) }, // d. selections args
                                    null, // e. group by
                                    null, // f. having
                                    null, // g. order by
                                    null); // h. limit

        if (cursor != null) {
            cursor.moveToFirst();
        }

        HistoryRecord historyRecord = new HistoryRecord();
        historyRecord.setId(Integer.parseInt(cursor.getString(0)));
        historyRecord.setTitle(cursor.getString(1));
        historyRecord.setUrl(cursor.getString(2));
        historyRecord.setHost(cursor.getString(3));
        historyRecord.setTime(cursor.getLong(4));

        return historyRecord;
    }

    public List<HistoryRecord> getAllHistoryRecords() {
        List<HistoryRecord> records = new LinkedList<HistoryRecord>();

        String query = "SELECT * FROM " + TABLE_LINK_HISTORY + " ORDER BY " + KEY_TIME + " DESC;";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                HistoryRecord historyRecord = new HistoryRecord();
                historyRecord.setId(Integer.parseInt(cursor.getString(0)));
                historyRecord.setTitle(cursor.getString(1));
                historyRecord.setUrl(cursor.getString(2));
                historyRecord.setHost(cursor.getString(3));
                historyRecord.setTime(cursor.getLong(4));

                records.add(historyRecord);
            } while (cursor.moveToNext());
        }

        return records;
    }

    /**
     * Get the favicon from the database, if any, associated with the given favicon URL. (That is,
     * the URL of the actual favicon image, not the URL of the page with which the favicon is associated.)
     * @param faviconUrl The URL of the favicon to fetch from the database.
     * @return The decoded Bitmap from the database, if any. null if none is stored.
     */
    /*
    public FaviconRecord getFaviconRecord(String faviconUrl) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_FAVICON_CACHE, // a. table
                FAVICON_COLUMNS, // b. column names
                " " + KEY_URL + " = ?", // c. selections
                new String[] { faviconUrl }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null) {
            cursor.moveToFirst();
        }

        FaviconRecord faviconRecord = new FaviconRecord();
        faviconRecord.setId(Integer.parseInt(cursor.getString(0)));
        faviconRecord.setUrl(cursor.getString(1));
        faviconRecord.setPageUrl(cursor.getString(2));

        byte[] byteArray = cursor.getBlob(3);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        faviconRecord.setFavicon(bitmap);
        cursor.closeZZZ();  // TODO, also put this inside the if check above

        return faviconRecord;
    }*/
    public Bitmap getFavicon(String faviconUrl) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_FAVICON_CACHE, // a. table
                FAVICON_DATA, // b. column names
                " " + KEY_URL + " = ?", // c. selections
                new String[] { faviconUrl }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null) {
            Bitmap result = null;
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();

                byte[] byteArray = cursor.getBlob(0);
                result = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            }
            cursor.close();
            return result;
        }

        return null;
    }

    public void addFaviconForUrl(String faviconUrl,
                                    Bitmap favicon, String pageUri) {
        if (Settings.get().isIncognitoMode() == true) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(KEY_URL, faviconUrl);
        values.put(KEY_PAGE_URL, pageUri);

        byte[] data = null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (favicon.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            data = stream.toByteArray();
        } else {
            Log.w(TAG, "Favicon compression failed.");
        }
        values.put(KEY_DATA, data);

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_FAVICON_CACHE, null, values);
        db.close();
    }
}