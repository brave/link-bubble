package com.linkbubble.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.linkbubble.Settings;
import com.linkbubble.util.CrashTracking;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
    private static final String KEY_IMAGE_SIZE = "imageSize";

    private static final String[] LINK_HISTORY_COLUMNS = {KEY_ID, KEY_TIME};
    private static final String[] FAVICON_COLUMNS = {KEY_ID, KEY_URL, KEY_PAGE_URL, KEY_DATA, KEY_TIME};
    private static final String[] FAVICON_EXISTS_COLUMNS = {KEY_ID, KEY_IMAGE_SIZE, KEY_TIME};
    private static final String[] FAVICON_FETCH_COLUMNS = {KEY_ID, KEY_TIME, KEY_DATA};

    private static final int FAVICON_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

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
                KEY_IMAGE_SIZE + " INTEGER, " +
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
        values.put(KEY_TIME, historyRecord.getTime());
        return values;
    }

    public void addHistoryRecord(HistoryRecord historyRecord){
        if (Settings.get().isIncognitoMode() == true) {
            return;
        }

        Log.d(TAG, "addHistoryRecord() - " + historyRecord.toString());

        int existingId = getRecentHistoryRecordId(historyRecord.getUrl());
        // If there is a history record from the last 12 hours, just update that item
        if (existingId > -1) {
            historyRecord.setId(existingId);
            updateHistoryRecord(historyRecord);
            return;
        }

        SQLiteDatabase db = getWritableDatabase();
        try {
            db.insert(TABLE_LINK_HISTORY, null, getContentValues(historyRecord));
            CrashTracking.log("DatabaseHelper.addHistoryRecord() success");
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.addHistoryRecord() IllegalStateException");
        }
        db.close();
    }

    public void updateHistoryRecord(HistoryRecord historyRecord) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = getContentValues(historyRecord);
            String id = String.valueOf(historyRecord.getId());
            db.update(TABLE_LINK_HISTORY, values,
                                KEY_ID + " = ?", new String[] { id });
            CrashTracking.log("DatabaseHelper.updateHistoryRecord() success, id:" + id);
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.addHistoryRecord() IllegalStateException");
        }
        db.close();
    }

    public boolean deleteHistoryRecord(HistoryRecord historyRecord) {

        boolean result = false;

        SQLiteDatabase db = getWritableDatabase();
        try {
            String id = String.valueOf(historyRecord.getId());
            db.delete(TABLE_LINK_HISTORY, KEY_ID + " = ?", new String[]{id});
            result = true;
            Log.d(TAG, "deleted historyRecord:" + historyRecord.toString());
            CrashTracking.log("DatabaseHelper.deleteHistoryRecord() success, id:" + id);
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.deleteHistoryRecord() IllegalStateException");
        }
        db.close();

        return result;
    }

    public void deleteAllHistoryRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LINK_HISTORY, null, null);
        db.close();
    }

    public int getRecentHistoryRecordId(String url){
        int result = -1;
        SQLiteDatabase db = getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_LINK_HISTORY, // a. table
                                        LINK_HISTORY_COLUMNS, // b. column names
                                        " " + KEY_URL + " = ?", // c. selections
                                        new String[] { String.valueOf(url) }, // d. selections args
                                        null, // e. group by
                                        null, // f. having
                                        " " + KEY_TIME + " DESC", // g. order by
                                        null); // h. limit
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                // If there is a history entry for this URL from the last 12 hours...
                int id = Integer.parseInt(cursor.getString(0));
                long time = cursor.getLong(1);
                long timeDelta = System.currentTimeMillis() - time;
                if (timeDelta < 12 * 60 * 60 * 1000) {
                    result = id;
                }
            }
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.getRecentHistoryRecordId() IllegalStateException");
        }

        db.close();
        return result;
    }

    public List<HistoryRecord> getAllHistoryRecords() {
        List<HistoryRecord> records = new ArrayList<HistoryRecord>();

        String query = "SELECT * FROM " + TABLE_LINK_HISTORY + " ORDER BY " + KEY_TIME + " DESC;";

        SQLiteDatabase db = getWritableDatabase();
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

        db.close();
        return records;
    }

    public void deleteAllFavicons() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FAVICON_CACHE, null, null);
        db.close();
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
        Bitmap result = null;
        SQLiteDatabase db = getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_FAVICON_CACHE, // a. table
                    FAVICON_FETCH_COLUMNS, // b. column names
                    " " + KEY_URL + " = ?", // c. selections
                    new String[]{faviconUrl}, // d. selections args
                    null, // e. group by
                    null, // f. having
                    null, // g. order by
                    null); // h. limit

            if (cursor != null) {
                long idToDelete = -1;
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    long id = cursor.getLong(0);
                    long createTime = cursor.getLong(1);
                    long timeDelta = System.currentTimeMillis() - createTime;
                    if (timeDelta < FAVICON_EXPIRE_TIME) {
                        byte[] byteArray = cursor.getBlob(2);
                        result = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                        Log.d(TAG, "getFavicon() - fetched favicon for " + faviconUrl);
                        CrashTracking.log("DatabaseHelper.getFavicon() success, id:" + id);
                    } else {
                        idToDelete = id;
                    }
                }
                cursor.close();

                if (idToDelete > -1) {
                    deleteFavicon(idToDelete);
                }
            }
        } catch (IllegalStateException ex) {    // #302
            CrashTracking.log("DatabaseHelper.getFavicon() IllegalStateException");
        }

        db.close();
        return result;
    }

    public boolean faviconExists(String faviconUrl, Bitmap favicon) {
        boolean result = false;
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_FAVICON_CACHE, // a. table
                FAVICON_EXISTS_COLUMNS, // b. column names
                " " + KEY_URL + " = ?", // c. selections
                new String[] { faviconUrl }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null) {
            long idToDelete = -1;
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    long id = cursor.getLong(0);
                    long imageSize = cursor.getInt(1);
                    long createTime = cursor.getLong(2);
                    long timeDelta = System.currentTimeMillis() - createTime;
                    if (favicon != null && favicon.getHeight() > imageSize) {
                        idToDelete = id;
                    } else if (timeDelta >= FAVICON_EXPIRE_TIME) {
                        idToDelete = id;
                    } else {
                        result = true;
                    }
                }
                cursor.close();

                if (idToDelete > -1) {
                    deleteFavicon(idToDelete);
                }
            } catch (IllegalStateException ex) {
            }
        }

        db.close();
        return result;
    }

    private void deleteFavicon(long id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String idAsString = String.valueOf(id);
            db.delete(TABLE_FAVICON_CACHE, KEY_ID + " = ?", new String[]{idAsString});
            CrashTracking.log("DatabaseHelper.deleteFavicon() success, id:" + idAsString);
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.deleteFavicon(): IllegalStateException");
        }

        db.close();

        Log.d(TAG, "deleteFavicon() - id:" + id);
    }

    public void addFaviconForUrl(String faviconUrl,
                                    Bitmap favicon, String pageUri) {
        if (Settings.get().isIncognitoMode() == true || faviconUrl == null || favicon == null) {
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
        values.put(KEY_IMAGE_SIZE, favicon.getHeight());  // assume square
        values.put(KEY_TIME, System.currentTimeMillis());

        SQLiteDatabase db = getWritableDatabase();
        try {
            db.insert(TABLE_FAVICON_CACHE, null, values);
            CrashTracking.log("DatabaseHelper.addFaviconForUrl() success");
        } catch (IllegalStateException ex) {
            CrashTracking.log("DatabaseHelper.addFaviconForUrl(): IllegalStateException");
        }
        db.close();

        Log.d(TAG, "addFaviconForUrl() - " + faviconUrl);
    }
}