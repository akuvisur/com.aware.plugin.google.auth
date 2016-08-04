package com.aware.plugin.google.auth;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.io.File;
import java.util.HashMap;

/**
 * Created by denzil on 18/11/15.
 */
public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.google.auth.provider.google_login";

    public static final int DATABASE_VERSION = 3;

    public static final String DATABASE_NAME = "plugin_google_login.db";
    public static final String[] DATABASE_TABLES = {"plugin_google_login"};
    public static final String[] TABLES_FIELDS = {
            Google_Account._ID + " integer primary key autoincrement," +
                    Google_Account.TIMESTAMP + " real default 0," +
                    Google_Account.DEVICE_ID + " text default ''," +
                    Google_Account.NAME + " text default ''," +
                    Google_Account.EMAIL + " text default ''," +
                    Google_Account.PHONENUMBER + " text default ''," +
                    Google_Account.PICTURE + " blob default null"
    };

    /**
     * Database table that contains the Google Account information
     */
    public static final class Google_Account implements BaseColumns {
        private Google_Account() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_google_login");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.google.login";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.google.login";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String PHONENUMBER = "phonenumber";
        public static final String PICTURE = "blob_picture";
    }

    private static final int GOOGLE = 1;
    private static final int GOOGLE_ID = 2;
    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (database == null || !database.isOpen()) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null);
    }

    public static void resetDB(Context c) {
        File db = new File(DATABASE_NAME);
        if (db.delete()) {
            Log.d("AWARE", "Resetting " + DATABASE_NAME + "...");
            databaseHelper = new DatabaseHelper(c, DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
            database = databaseHelper.getWritableDatabase();
        }
    }


    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.google_login"; //make AUTHORITY dynamic
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], GOOGLE); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", GOOGLE_ID); //URI for a single record

        tableMap = new HashMap<>();
        tableMap.put(Google_Account._ID, Google_Account._ID);
        tableMap.put(Google_Account.TIMESTAMP, Google_Account.TIMESTAMP);
        tableMap.put(Google_Account.DEVICE_ID, Google_Account.DEVICE_ID);
        tableMap.put(Google_Account.NAME, Google_Account.NAME);
        tableMap.put(Google_Account.EMAIL, Google_Account.EMAIL);
        tableMap.put(Google_Account.PHONENUMBER, Google_Account.PHONENUMBER);
        tableMap.put(Google_Account.PICTURE, Google_Account.PICTURE);

        return true; //let Android know that the database is ready to be used.
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case GOOGLE:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG) Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case GOOGLE:
                return Google_Account.CONTENT_TYPE;
            case GOOGLE_ID:
                return Google_Account.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case GOOGLE:
                long _id = database.insert(DATABASE_TABLES[0], Google_Account.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Google_Account.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case GOOGLE:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case GOOGLE:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
