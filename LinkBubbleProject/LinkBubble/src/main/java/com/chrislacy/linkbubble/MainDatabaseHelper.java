package com.chrislacy.linkbubble;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.util.LinkedList;
import java.util.List;

public class MainDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LinkBubbleDB";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "LinkBubbleDB";

    private static final String TABLE_LINK_HISTORY = "linkHistory";

    // Link History Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "author";
    private static final String KEY_TIME = "time";

    private static final String[] LINK_HISTORY_COLUMNS = {KEY_ID, KEY_TITLE, KEY_URL, KEY_TIME};

    public MainDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LINK_HISTORY_TABLE = "CREATE TABLE " + TABLE_LINK_HISTORY + " ( " +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_TITLE + " TEXT, " +
                KEY_URL + " TEXT, " +
                KEY_TIME + " INTEGER" + " )";

        db.execSQL(CREATE_LINK_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINK_HISTORY);

        // create fresh table
        onCreate(db);
    }

    private ContentValues getContentValues(LinkHistoryRecord linkHistoryRecord) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, linkHistoryRecord.getTitle());
        values.put(KEY_URL, linkHistoryRecord.getUrl());
        values.put(KEY_TIME, linkHistoryRecord.getTime());
        return values;
    }

    public void addLinkHistoryRecord(LinkHistoryRecord linkHistoryRecord){
        Log.d(TAG, "addLinkHistoryRecord() - " + linkHistoryRecord.toString());

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_LINK_HISTORY, null, getContentValues(linkHistoryRecord));
        db.close();
    }

    public int updateLinkHistoryRecord(LinkHistoryRecord linkHistoryRecord) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = getContentValues(linkHistoryRecord);
        int i = db.update(TABLE_LINK_HISTORY, values,
                            KEY_ID + " = ?", new String[] { String.valueOf(linkHistoryRecord.getId()) });
        db.close();
        return i;
    }

    public void deleteLinkHistoryRecord(LinkHistoryRecord linkHistoryRecord) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_LINK_HISTORY, KEY_ID + " = ?", new String[] { String.valueOf(linkHistoryRecord.getId()) });
        db.close();

        Log.d(TAG, "deleted linkHistoryRecord:" + linkHistoryRecord.toString());
    }

    public LinkHistoryRecord getLinkHistoryRecord(int id){

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

        LinkHistoryRecord linkHistoryRecord = new LinkHistoryRecord();
        linkHistoryRecord.setId(Integer.parseInt(cursor.getString(0)));
        linkHistoryRecord.setTitle(cursor.getString(1));
        linkHistoryRecord.setUrl(cursor.getString(2));
        linkHistoryRecord.setTime(cursor.getInt(3));

        return linkHistoryRecord;
    }

    public List<LinkHistoryRecord> getAllLinkHistoryRecords() {
        List<LinkHistoryRecord> records = new LinkedList<LinkHistoryRecord>();

        String query = "SELECT * FROM " + TABLE_LINK_HISTORY + " ORDER BY " + KEY_TIME + " DESC;";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                LinkHistoryRecord linkHistoryRecord = new LinkHistoryRecord();
                linkHistoryRecord.setId(Integer.parseInt(cursor.getString(0)));
                linkHistoryRecord.setTitle(cursor.getString(1));
                linkHistoryRecord.setUrl(cursor.getString(2));
                linkHistoryRecord.setTime(cursor.getInt(3));

                records.add(linkHistoryRecord);
            } while (cursor.moveToNext());
        }

        return records;
    }

}