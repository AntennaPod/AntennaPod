package de.danoeh.antennapod.config;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import de.danoeh.antennapod.core.StorageCallbacks;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

public class StorageCallbacksImpl implements StorageCallbacks {

    @Override
    public int getDatabaseVersion() {
        return 12;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DBAdapter", "Upgrading from version " + oldVersion + " to "
                + newVersion + ".");
        if (oldVersion <= 1) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS + " ADD COLUMN "
                    + PodDBAdapter.KEY_TYPE + " TEXT");
        }
        if (oldVersion <= 2) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_SIMPLECHAPTERS
                    + " ADD COLUMN " + PodDBAdapter.KEY_LINK + " TEXT");
        }
        if (oldVersion <= 3) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_ITEMS
                    + " ADD COLUMN " + PodDBAdapter.KEY_ITEM_IDENTIFIER + " TEXT");
        }
        if (oldVersion <= 4) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS + " ADD COLUMN "
                    + PodDBAdapter.KEY_FEED_IDENTIFIER + " TEXT");
        }
        if (oldVersion <= 5) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_DOWNLOAD_LOG
                    + " ADD COLUMN " + PodDBAdapter.KEY_REASON_DETAILED + " TEXT");
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_DOWNLOAD_LOG
                    + " ADD COLUMN " + PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE + " TEXT");
        }
        if (oldVersion <= 6) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_SIMPLECHAPTERS
                    + " ADD COLUMN " + PodDBAdapter.KEY_CHAPTER_TYPE + " INTEGER");
        }
        if (oldVersion <= 7) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_MEDIA
                    + " ADD COLUMN " + PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE
                    + " INTEGER");
        }
        if (oldVersion <= 8) {
            final int KEY_ID_POSITION = 0;
            final int KEY_MEDIA_POSITION = 1;

            // Add feeditem column to feedmedia table
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_MEDIA
                    + " ADD COLUMN " + PodDBAdapter.KEY_FEEDITEM
                    + " INTEGER");
            Cursor feeditemCursor = db.query(PodDBAdapter.TABLE_NAME_FEED_ITEMS,
                    new String[]{PodDBAdapter.KEY_ID, PodDBAdapter.KEY_MEDIA}, "? > 0",
                    new String[]{PodDBAdapter.KEY_MEDIA}, null, null, null);
            if (feeditemCursor.moveToFirst()) {
                db.beginTransaction();
                ContentValues contentValues = new ContentValues();
                do {
                    long mediaId = feeditemCursor.getLong(KEY_MEDIA_POSITION);
                    contentValues.put(PodDBAdapter.KEY_FEEDITEM, feeditemCursor.getLong(KEY_ID_POSITION));
                    db.update(PodDBAdapter.TABLE_NAME_FEED_MEDIA, contentValues, PodDBAdapter.KEY_ID + "=?", new String[]{String.valueOf(mediaId)});
                    contentValues.clear();
                } while (feeditemCursor.moveToNext());
                db.setTransactionSuccessful();
                db.endTransaction();
            }
            feeditemCursor.close();
        }
        if (oldVersion <= 9) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS
                    + " ADD COLUMN " + PodDBAdapter.KEY_AUTO_DOWNLOAD
                    + " INTEGER DEFAULT 1");
        }
        if (oldVersion <= 10) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS
                    + " ADD COLUMN " + PodDBAdapter.KEY_FLATTR_STATUS
                    + " INTEGER");
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_ITEMS
                    + " ADD COLUMN " + PodDBAdapter.KEY_FLATTR_STATUS
                    + " INTEGER");
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_MEDIA
                    + " ADD COLUMN " + PodDBAdapter.KEY_PLAYED_DURATION
                    + " INTEGER");
        }
        if (oldVersion <= 11) {
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS
                    + " ADD COLUMN " + PodDBAdapter.KEY_USERNAME
                    + " TEXT");
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEEDS
                    + " ADD COLUMN " + PodDBAdapter.KEY_PASSWORD
                    + " TEXT");
            db.execSQL("ALTER TABLE " + PodDBAdapter.TABLE_NAME_FEED_ITEMS
                    + " ADD COLUMN " + PodDBAdapter.KEY_IMAGE
                    + " INTEGER");
        }
    }
}
