package de.danoeh.antennapod.core.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;

// TODO Remove media column from feeditem table

/**
 * Implements methods for accessing the database
 */
public class PodDBAdapter {
    private static final String TAG = "PodDBAdapter";
    public static final String DATABASE_NAME = "Antennapod.db";

    /**
     * Maximum number of arguments for IN-operator.
     */
    public static final int IN_OPERATOR_MAXIMUM = 800;

    /**
     * Maximum number of entries per search request.
     */
    public static final int SEARCH_LIMIT = 30;

    // ----------- Column indices
    // ----------- General indices
    public static final int KEY_ID_INDEX = 0;
    public static final int KEY_TITLE_INDEX = 1;
    public static final int KEY_FILE_URL_INDEX = 2;
    public static final int KEY_DOWNLOAD_URL_INDEX = 3;
    public static final int KEY_DOWNLOADED_INDEX = 4;
    public static final int KEY_LINK_INDEX = 5;
    public static final int KEY_DESCRIPTION_INDEX = 6;
    public static final int KEY_PAYMENT_LINK_INDEX = 7;
    // ----------- Feed indices
    public static final int KEY_LAST_UPDATE_INDEX = 8;
    public static final int KEY_LANGUAGE_INDEX = 9;
    public static final int KEY_AUTHOR_INDEX = 10;
    public static final int KEY_IMAGE_INDEX = 11;
    public static final int KEY_TYPE_INDEX = 12;
    public static final int KEY_FEED_IDENTIFIER_INDEX = 13;
    public static final int KEY_FEED_FLATTR_STATUS_INDEX = 14;
    public static final int KEY_FEED_USERNAME_INDEX = 15;
    public static final int KEY_FEED_PASSWORD_INDEX = 16;
    public static final int KEY_IS_PAGED_INDEX = 17;
    public static final int KEY_LOAD_ALL_PAGES_INDEX = 18;
    public static final int KEY_NEXT_PAGE_LINK_INDEX = 19;
    // ----------- FeedItem indices
    public static final int KEY_CONTENT_ENCODED_INDEX = 2;
    public static final int KEY_PUBDATE_INDEX = 3;
    public static final int KEY_READ_INDEX = 4;
    public static final int KEY_MEDIA_INDEX = 8;
    public static final int KEY_FEED_INDEX = 9;
    public static final int KEY_HAS_SIMPLECHAPTERS_INDEX = 10;
    public static final int KEY_ITEM_IDENTIFIER_INDEX = 11;
    public static final int KEY_ITEM_FLATTR_STATUS_INDEX = 12;
    // ---------- FeedMedia indices
    public static final int KEY_DURATION_INDEX = 1;
    public static final int KEY_POSITION_INDEX = 5;
    public static final int KEY_SIZE_INDEX = 6;
    public static final int KEY_MIME_TYPE_INDEX = 7;
    public static final int KEY_PLAYBACK_COMPLETION_DATE_INDEX = 8;
    public static final int KEY_MEDIA_FEEDITEM_INDEX = 9;
    public static final int KEY_PLAYED_DURATION_INDEX = 10;
    // --------- Download log indices
    public static final int KEY_FEEDFILE_INDEX = 1;
    public static final int KEY_FEEDFILETYPE_INDEX = 2;
    public static final int KEY_REASON_INDEX = 3;
    public static final int KEY_SUCCESSFUL_INDEX = 4;
    public static final int KEY_COMPLETION_DATE_INDEX = 5;
    public static final int KEY_REASON_DETAILED_INDEX = 6;
    public static final int KEY_DOWNLOADSTATUS_TITLE_INDEX = 7;
    // --------- Queue indices
    public static final int KEY_FEEDITEM_INDEX = 1;
    public static final int KEY_QUEUE_FEED_INDEX = 2;
    // --------- Chapters indices
    public static final int KEY_CHAPTER_START_INDEX = 2;
    public static final int KEY_CHAPTER_FEEDITEM_INDEX = 3;
    public static final int KEY_CHAPTER_LINK_INDEX = 4;
    public static final int KEY_CHAPTER_TYPE_INDEX = 5;

    // Key-constants
    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_NAME = "name";
    public static final String KEY_LINK = "link";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_FILE_URL = "file_url";
    public static final String KEY_DOWNLOAD_URL = "download_url";
    public static final String KEY_PUBDATE = "pubDate";
    public static final String KEY_READ = "read";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_POSITION = "position";
    public static final String KEY_SIZE = "filesize";
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FEED = "feed";
    public static final String KEY_MEDIA = "media";
    public static final String KEY_DOWNLOADED = "downloaded";
    public static final String KEY_LASTUPDATE = "last_update";
    public static final String KEY_FEEDFILE = "feedfile";
    public static final String KEY_REASON = "reason";
    public static final String KEY_SUCCESSFUL = "successful";
    public static final String KEY_FEEDFILETYPE = "feedfile_type";
    public static final String KEY_COMPLETION_DATE = "completion_date";
    public static final String KEY_FEEDITEM = "feeditem";
    public static final String KEY_CONTENT_ENCODED = "content_encoded";
    public static final String KEY_PAYMENT_LINK = "payment_link";
    public static final String KEY_START = "start";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_HAS_CHAPTERS = "has_simple_chapters";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ITEM_IDENTIFIER = "item_identifier";
    public static final String KEY_FLATTR_STATUS = "flattr_status";
    public static final String KEY_FEED_IDENTIFIER = "feed_identifier";
    public static final String KEY_REASON_DETAILED = "reason_detailed";
    public static final String KEY_DOWNLOADSTATUS_TITLE = "title";
    public static final String KEY_CHAPTER_TYPE = "type";
    public static final String KEY_PLAYBACK_COMPLETION_DATE = "playback_completion_date";
    public static final String KEY_AUTO_DOWNLOAD = "auto_download";
    public static final String KEY_PLAYED_DURATION = "played_duration";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_IS_PAGED = "is_paged";
    public static final String KEY_NEXT_PAGE_LINK = "next_page_link";

    // Table names
    public static final String TABLE_NAME_FEEDS = "Feeds";
    public static final String TABLE_NAME_FEED_ITEMS = "FeedItems";
    public static final String TABLE_NAME_FEED_IMAGES = "FeedImages";
    public static final String TABLE_NAME_FEED_MEDIA = "FeedMedia";
    public static final String TABLE_NAME_DOWNLOAD_LOG = "DownloadLog";
    public static final String TABLE_NAME_QUEUE = "Queue";
    public static final String TABLE_NAME_SIMPLECHAPTERS = "SimpleChapters";

    // SQL Statements for creating new tables
    private static final String TABLE_PRIMARY_KEY = KEY_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT ,";

    public static final String CREATE_TABLE_FEEDS = "CREATE TABLE "
            + TABLE_NAME_FEEDS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
            + " TEXT," + KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL + " TEXT,"
            + KEY_DOWNLOADED + " INTEGER," + KEY_LINK + " TEXT,"
            + KEY_DESCRIPTION + " TEXT," + KEY_PAYMENT_LINK + " TEXT,"
            + KEY_LASTUPDATE + " TEXT," + KEY_LANGUAGE + " TEXT," + KEY_AUTHOR
            + " TEXT," + KEY_IMAGE + " INTEGER," + KEY_TYPE + " TEXT,"
            + KEY_FEED_IDENTIFIER + " TEXT," + KEY_AUTO_DOWNLOAD + " INTEGER DEFAULT 1,"
            + KEY_FLATTR_STATUS + " INTEGER,"
            + KEY_USERNAME + " TEXT,"
            + KEY_PASSWORD + " TEXT,"
            + KEY_IS_PAGED + " INTEGER DEFAULT 0,"
            + KEY_NEXT_PAGE_LINK + " TEXT)";


    public static final String CREATE_TABLE_FEED_ITEMS = "CREATE TABLE "
            + TABLE_NAME_FEED_ITEMS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
            + " TEXT," + KEY_CONTENT_ENCODED + " TEXT," + KEY_PUBDATE
            + " INTEGER," + KEY_READ + " INTEGER," + KEY_LINK + " TEXT,"
            + KEY_DESCRIPTION + " TEXT," + KEY_PAYMENT_LINK + " TEXT,"
            + KEY_MEDIA + " INTEGER," + KEY_FEED + " INTEGER,"
            + KEY_HAS_CHAPTERS + " INTEGER," + KEY_ITEM_IDENTIFIER + " TEXT,"
            + KEY_FLATTR_STATUS + " INTEGER,"
            + KEY_IMAGE + " INTEGER)";

    public static final String CREATE_TABLE_FEED_IMAGES = "CREATE TABLE "
            + TABLE_NAME_FEED_IMAGES + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
            + " TEXT," + KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL + " TEXT,"
            + KEY_DOWNLOADED + " INTEGER)";

    public static final String CREATE_TABLE_FEED_MEDIA = "CREATE TABLE "
            + TABLE_NAME_FEED_MEDIA + " (" + TABLE_PRIMARY_KEY + KEY_DURATION
            + " INTEGER," + KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL
            + " TEXT," + KEY_DOWNLOADED + " INTEGER," + KEY_POSITION
            + " INTEGER," + KEY_SIZE + " INTEGER," + KEY_MIME_TYPE + " TEXT,"
            + KEY_PLAYBACK_COMPLETION_DATE + " INTEGER,"
            + KEY_FEEDITEM + " INTEGER,"
            + KEY_PLAYED_DURATION + " INTEGER)";

    public static final String CREATE_TABLE_DOWNLOAD_LOG = "CREATE TABLE "
            + TABLE_NAME_DOWNLOAD_LOG + " (" + TABLE_PRIMARY_KEY + KEY_FEEDFILE
            + " INTEGER," + KEY_FEEDFILETYPE + " INTEGER," + KEY_REASON
            + " INTEGER," + KEY_SUCCESSFUL + " INTEGER," + KEY_COMPLETION_DATE
            + " INTEGER," + KEY_REASON_DETAILED + " TEXT,"
            + KEY_DOWNLOADSTATUS_TITLE + " TEXT)";

    public static final String CREATE_TABLE_QUEUE = "CREATE TABLE "
            + TABLE_NAME_QUEUE + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_FEEDITEM + " INTEGER," + KEY_FEED + " INTEGER)";

    public static final String CREATE_TABLE_SIMPLECHAPTERS = "CREATE TABLE "
            + TABLE_NAME_SIMPLECHAPTERS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
            + " TEXT," + KEY_START + " INTEGER," + KEY_FEEDITEM + " INTEGER,"
            + KEY_LINK + " TEXT," + KEY_CHAPTER_TYPE + " INTEGER)";

    private SQLiteDatabase db;
    private final Context context;
    private PodDBHelper helper;

    /**
     * Select all columns from the feed-table
     */
    private static final String[] FEED_SEL_STD = {
            TABLE_NAME_FEEDS + "." + KEY_ID,
            TABLE_NAME_FEEDS + "." + KEY_TITLE,
            TABLE_NAME_FEEDS + "." + KEY_FILE_URL,
            TABLE_NAME_FEEDS + "." + KEY_DOWNLOAD_URL,
            TABLE_NAME_FEEDS + "." + KEY_DOWNLOADED,
            TABLE_NAME_FEEDS + "." + KEY_LINK,
            TABLE_NAME_FEEDS + "." + KEY_DESCRIPTION,
            TABLE_NAME_FEEDS + "." + KEY_PAYMENT_LINK,
            TABLE_NAME_FEEDS + "." + KEY_LASTUPDATE,
            TABLE_NAME_FEEDS + "." + KEY_LANGUAGE,
            TABLE_NAME_FEEDS + "." + KEY_AUTHOR,
            TABLE_NAME_FEEDS + "." + KEY_IMAGE,
            TABLE_NAME_FEEDS + "." + KEY_TYPE,
            TABLE_NAME_FEEDS + "." + KEY_FEED_IDENTIFIER,
            TABLE_NAME_FEEDS + "." + KEY_AUTO_DOWNLOAD,
            TABLE_NAME_FEEDS + "." + KEY_FLATTR_STATUS,
            TABLE_NAME_FEEDS + "." + KEY_IS_PAGED,
            TABLE_NAME_FEEDS + "." + KEY_NEXT_PAGE_LINK,
            TABLE_NAME_FEEDS + "." + KEY_USERNAME,
            TABLE_NAME_FEEDS + "." + KEY_PASSWORD,
    };

    // column indices for FEED_SEL_STD
    public static final int IDX_FEED_SEL_STD_ID = 0;
    public static final int IDX_FEED_SEL_STD_TITLE = 1;
    public static final int IDX_FEED_SEL_STD_FILE_URL = 2;
    public static final int IDX_FEED_SEL_STD_DOWNLOAD_URL = 3;
    public static final int IDX_FEED_SEL_STD_DOWNLOADED = 4;
    public static final int IDX_FEED_SEL_STD_LINK = 5;
    public static final int IDX_FEED_SEL_STD_DESCRIPTION = 6;
    public static final int IDX_FEED_SEL_STD_PAYMENT_LINK = 7;
    public static final int IDX_FEED_SEL_STD_LASTUPDATE = 8;
    public static final int IDX_FEED_SEL_STD_LANGUAGE = 9;
    public static final int IDX_FEED_SEL_STD_AUTHOR = 10;
    public static final int IDX_FEED_SEL_STD_IMAGE = 11;
    public static final int IDX_FEED_SEL_STD_TYPE = 12;
    public static final int IDX_FEED_SEL_STD_FEED_IDENTIFIER = 13;
    public static final int IDX_FEED_SEL_PREFERENCES_AUTO_DOWNLOAD = 14;
    public static final int IDX_FEED_SEL_STD_FLATTR_STATUS = 15;
    public static final int IDX_FEED_SEL_STD_IS_PAGED = 16;
    public static final int IDX_FEED_SEL_STD_NEXT_PAGE_LINK = 17;
    public static final int IDX_FEED_SEL_PREFERENCES_USERNAME = 18;
    public static final int IDX_FEED_SEL_PREFERENCES_PASSWORD = 19;


    /**
     * Select all columns from the feeditems-table except description and
     * content-encoded.
     */
    private static final String[] FEEDITEM_SEL_FI_SMALL = {
            TABLE_NAME_FEED_ITEMS + "." + KEY_ID,
            TABLE_NAME_FEED_ITEMS + "." + KEY_TITLE,
            TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE,
            TABLE_NAME_FEED_ITEMS + "." + KEY_READ,
            TABLE_NAME_FEED_ITEMS + "." + KEY_LINK,
            TABLE_NAME_FEED_ITEMS + "." + KEY_PAYMENT_LINK, KEY_MEDIA,
            TABLE_NAME_FEED_ITEMS + "." + KEY_FEED,
            TABLE_NAME_FEED_ITEMS + "." + KEY_HAS_CHAPTERS,
            TABLE_NAME_FEED_ITEMS + "." + KEY_ITEM_IDENTIFIER,
            TABLE_NAME_FEED_ITEMS + "." + KEY_FLATTR_STATUS,
            TABLE_NAME_FEED_ITEMS + "." + KEY_IMAGE};

    /**
     * Contains FEEDITEM_SEL_FI_SMALL as comma-separated list. Useful for raw queries.
     */
    private static final String SEL_FI_SMALL_STR;

    static {
        String selFiSmall = Arrays.toString(FEEDITEM_SEL_FI_SMALL);
        SEL_FI_SMALL_STR = selFiSmall.substring(1, selFiSmall.length() - 1);
    }

    // column indices for FEEDITEM_SEL_FI_SMALL

    public static final int IDX_FI_SMALL_ID = 0;
    public static final int IDX_FI_SMALL_TITLE = 1;
    public static final int IDX_FI_SMALL_PUBDATE = 2;
    public static final int IDX_FI_SMALL_READ = 3;
    public static final int IDX_FI_SMALL_LINK = 4;
    public static final int IDX_FI_SMALL_PAYMENT_LINK = 5;
    public static final int IDX_FI_SMALL_MEDIA = 6;
    public static final int IDX_FI_SMALL_FEED = 7;
    public static final int IDX_FI_SMALL_HAS_CHAPTERS = 8;
    public static final int IDX_FI_SMALL_ITEM_IDENTIFIER = 9;
    public static final int IDX_FI_SMALL_FLATTR_STATUS = 10;
    public static final int IDX_FI_SMALL_IMAGE = 11;

    /**
     * Select id, description and content-encoded column from feeditems.
     */
    private static final String[] SEL_FI_EXTRA = {KEY_ID, KEY_DESCRIPTION,
            KEY_CONTENT_ENCODED, KEY_FEED};

    // column indices for SEL_FI_EXTRA

    public static final int IDX_FI_EXTRA_ID = 0;
    public static final int IDX_FI_EXTRA_DESCRIPTION = 1;
    public static final int IDX_FI_EXTRA_CONTENT_ENCODED = 2;
    public static final int IDX_FI_EXTRA_FEED = 3;

    static PodDBHelper dbHelperSingleton;

    private static synchronized PodDBHelper getDbHelperSingleton(Context appContext) {
        if (dbHelperSingleton == null) {
            dbHelperSingleton = new PodDBHelper(appContext, DATABASE_NAME, null,
                    ClientConfig.storageCallbacks.getDatabaseVersion());
        }
        return dbHelperSingleton;
    }

    public PodDBAdapter(Context c) {
        this.context = c;
        helper = getDbHelperSingleton(c.getApplicationContext());
    }

    public PodDBAdapter open() {
        if (db == null || !db.isOpen() || db.isReadOnly()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Opening DB");
            try {
                db = helper.getWritableDatabase();
            } catch (SQLException ex) {
                ex.printStackTrace();
                db = helper.getReadableDatabase();
            }
        }
        return this;
    }

    public void close() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Closing DB");
        //db.close();
    }

    public static boolean deleteDatabase(Context context) {
        Log.w(TAG, "Deleting database");
        dbHelperSingleton.close();
        dbHelperSingleton = null;
        return context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Inserts or updates a feed entry
     *
     * @return the id of the entry
     */
    public long setFeed(Feed feed) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, feed.getTitle());
        values.put(KEY_LINK, feed.getLink());
        values.put(KEY_DESCRIPTION, feed.getDescription());
        values.put(KEY_PAYMENT_LINK, feed.getPaymentLink());
        values.put(KEY_AUTHOR, feed.getAuthor());
        values.put(KEY_LANGUAGE, feed.getLanguage());
        if (feed.getImage() != null) {
            if (feed.getImage().getId() == 0) {
                setImage(feed.getImage());
            }
            values.put(KEY_IMAGE, feed.getImage().getId());
        }

        values.put(KEY_FILE_URL, feed.getFile_url());
        values.put(KEY_DOWNLOAD_URL, feed.getDownload_url());
        values.put(KEY_DOWNLOADED, feed.isDownloaded());
        values.put(KEY_LASTUPDATE, feed.getLastUpdate().getTime());
        values.put(KEY_TYPE, feed.getType());
        values.put(KEY_FEED_IDENTIFIER, feed.getFeedIdentifier());

        Log.d(TAG, "Setting feed with flattr status " + feed.getTitle() + ": " + feed.getFlattrStatus().toLong());

        values.put(KEY_FLATTR_STATUS, feed.getFlattrStatus().toLong());
        values.put(KEY_IS_PAGED, feed.isPaged());
        values.put(KEY_NEXT_PAGE_LINK, feed.getNextPageLink());
        if (feed.getId() == 0) {
            // Create new entry
            if (BuildConfig.DEBUG)
                Log.d(this.toString(), "Inserting new Feed into db");
            feed.setId(db.insert(TABLE_NAME_FEEDS, null, values));
        } else {
            if (BuildConfig.DEBUG)
                Log.d(this.toString(), "Updating existing Feed in db");
            db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?",
                    new String[]{String.valueOf(feed.getId())});

        }
        return feed.getId();
    }

    public void setFeedPreferences(FeedPreferences prefs) {
        if (prefs.getFeedID() == 0) {
            throw new IllegalArgumentException("Feed ID of preference must not be null");
        }
        ContentValues values = new ContentValues();
        values.put(KEY_AUTO_DOWNLOAD, prefs.getAutoDownload());
        values.put(KEY_USERNAME, prefs.getUsername());
        values.put(KEY_PASSWORD, prefs.getPassword());
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(prefs.getFeedID())});
    }

    /**
     * Inserts or updates an image entry
     *
     * @return the id of the entry
     */
    public long setImage(FeedImage image) {
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, image.getTitle());
        values.put(KEY_DOWNLOAD_URL, image.getDownload_url());
        values.put(KEY_DOWNLOADED, image.isDownloaded());
        values.put(KEY_FILE_URL, image.getFile_url());
        if (image.getId() == 0) {
            image.setId(db.insert(TABLE_NAME_FEED_IMAGES, null, values));
        } else {
            db.update(TABLE_NAME_FEED_IMAGES, values, KEY_ID + "=?",
                    new String[]{String.valueOf(image.getId())});
        }

        final FeedComponent owner = image.getOwner();
        if (owner != null && owner.getId() != 0) {
            values.clear();
            values.put(KEY_IMAGE, image.getId());
            if (owner instanceof Feed) {
                db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(image.getOwner().getId())});
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        return image.getId();
    }

    /**
     * Inserts or updates an image entry
     *
     * @return the id of the entry
     */
    public long setMedia(FeedMedia media) {
        ContentValues values = new ContentValues();
        values.put(KEY_DURATION, media.getDuration());
        values.put(KEY_POSITION, media.getPosition());
        values.put(KEY_SIZE, media.getSize());
        values.put(KEY_MIME_TYPE, media.getMime_type());
        values.put(KEY_DOWNLOAD_URL, media.getDownload_url());
        values.put(KEY_DOWNLOADED, media.isDownloaded());
        values.put(KEY_FILE_URL, media.getFile_url());

        if (media.getPlaybackCompletionDate() != null) {
            values.put(KEY_PLAYBACK_COMPLETION_DATE, media
                    .getPlaybackCompletionDate().getTime());
        } else {
            values.put(KEY_PLAYBACK_COMPLETION_DATE, 0);
        }
        if (media.getItem() != null) {
            values.put(KEY_FEEDITEM, media.getItem().getId());
        }
        if (media.getId() == 0) {
            media.setId(db.insert(TABLE_NAME_FEED_MEDIA, null, values));
        } else {
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                    new String[]{String.valueOf(media.getId())});
        }
        return media.getId();
    }

    public void setFeedMediaPlaybackInformation(FeedMedia media) {
        if (media.getId() != 0) {
            ContentValues values = new ContentValues();
            values.put(KEY_POSITION, media.getPosition());
            values.put(KEY_DURATION, media.getDuration());
            values.put(KEY_PLAYED_DURATION, media.getPlayedDuration());
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                    new String[]{String.valueOf(media.getId())});
        } else {
            Log.e(TAG, "setFeedMediaPlaybackInformation: ID of media was 0");
        }
    }

    public void setFeedMediaPlaybackCompletionDate(FeedMedia media) {
        if (media.getId() != 0) {
            ContentValues values = new ContentValues();
            values.put(KEY_PLAYBACK_COMPLETION_DATE, media.getPlaybackCompletionDate().getTime());
            values.put(KEY_PLAYED_DURATION, media.getPlayedDuration());
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                    new String[]{String.valueOf(media.getId())});
        } else {
            Log.e(TAG, "setFeedMediaPlaybackCompletionDate: ID of media was 0");
        }
    }

    /**
     * Insert all FeedItems of a feed and the feed object itself in a single
     * transaction
     */
    public void setCompleteFeed(Feed... feeds) {
        db.beginTransaction();
        for (Feed feed : feeds) {
            setFeed(feed);
            if (feed.getItems() != null) {
                for (FeedItem item : feed.getItems()) {
                    setFeedItem(item, false);
                }
            }
            if (feed.getPreferences() != null) {
                setFeedPreferences(feed.getPreferences());
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * Update the flattr status of a feed
     */
    public void setFeedFlattrStatus(Feed feed) {
        ContentValues values = new ContentValues();
        values.put(KEY_FLATTR_STATUS, feed.getFlattrStatus().toLong());
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(feed.getId())});
    }

    /**
     * Get all feeds in the flattr queue.
     */
    public Cursor getFeedsInFlattrQueueCursor() {
        return db.query(TABLE_NAME_FEEDS, FEED_SEL_STD, KEY_FLATTR_STATUS + "=?",
                new String[]{String.valueOf(FlattrStatus.STATUS_QUEUE)}, null, null, null);
    }

    /**
     * Get all feed items in the flattr queue.
     */
    public Cursor getFeedItemsInFlattrQueueCursor() {
        return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_FLATTR_STATUS + "=?",
                new String[]{String.valueOf(FlattrStatus.STATUS_QUEUE)}, null, null, null);
    }

    /**
     * Counts feeds and feed items in the flattr queue
     */
    public int getFlattrQueueSize() {
        int res = 0;
        Cursor c = db.rawQuery(String.format("SELECT count(*) FROM %s WHERE %s=%s",
                TABLE_NAME_FEEDS, KEY_FLATTR_STATUS, String.valueOf(FlattrStatus.STATUS_QUEUE)), null);
        if (c.moveToFirst()) {
            res = c.getInt(0);
            c.close();
        } else {
            Log.e(TAG, "Unable to determine size of flattr queue: Could not count number of feeds");
        }
        c = db.rawQuery(String.format("SELECT count(*) FROM %s WHERE %s=%s",
                TABLE_NAME_FEED_ITEMS, KEY_FLATTR_STATUS, String.valueOf(FlattrStatus.STATUS_QUEUE)), null);
        if (c.moveToFirst()) {
            res += c.getInt(0);
            c.close();
        } else {
            Log.e(TAG, "Unable to determine size of flattr queue: Could not count number of feed items");
        }

        return res;
    }

    /**
     * Updates the download URL of a Feed.
     */
    public void setFeedDownloadUrl(String original, String updated) {
        ContentValues values = new ContentValues();
        values.put(KEY_DOWNLOAD_URL, updated);
        db.update(TABLE_NAME_FEEDS, values, KEY_DOWNLOAD_URL + "=?", new String[]{original});
    }

    public void setFeedItemlist(List<FeedItem> items) {
        db.beginTransaction();
        for (FeedItem item : items) {
            setFeedItem(item, true);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public long setSingleFeedItem(FeedItem item) {
        db.beginTransaction();
        long result = setFeedItem(item, true);
        db.setTransactionSuccessful();
        db.endTransaction();
        return result;
    }

    /**
     * Update the flattr status of a FeedItem
     */
    public void setFeedItemFlattrStatus(FeedItem feedItem) {
        ContentValues values = new ContentValues();
        values.put(KEY_FLATTR_STATUS, feedItem.getFlattrStatus().toLong());
        db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?", new String[]{String.valueOf(feedItem.getId())});
    }

    /**
     * Update the flattr status of a feed or feed item specified by its payment link
     * and the new flattr status to use
     */
    public void setItemFlattrStatus(String url, FlattrStatus status) {
        //Log.d(TAG, "setItemFlattrStatus(" + url + ") = " + status.toString());
        ContentValues values = new ContentValues();
        values.put(KEY_FLATTR_STATUS, status.toLong());

        // regexps in sqlite would be neat!
        String[] query_urls = new String[]{
                "*" + url + "&*",
                "*" + url + "%2F&*",
                "*" + url + "",
                "*" + url + "%2F"
        };

        if (db.update(TABLE_NAME_FEEDS, values,
                KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?", query_urls
        ) > 0) {
            Log.i(TAG, "setItemFlattrStatus found match for " + url + " = " + status.toLong() + " in Feeds table");
            return;
        }
        if (db.update(TABLE_NAME_FEED_ITEMS, values,
                KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?"
                        + " OR " + KEY_PAYMENT_LINK + " GLOB ?", query_urls
        ) > 0) {
            Log.i(TAG, "setItemFlattrStatus found match for " + url + " = " + status.toLong() + " in FeedsItems table");
        }
    }

    /**
     * Reset flattr status to unflattrd for all items
     */
    public void clearAllFlattrStatus() {
        ContentValues values = new ContentValues();
        values.put(KEY_FLATTR_STATUS, 0);
        db.update(TABLE_NAME_FEEDS, values, null, null);
        db.update(TABLE_NAME_FEED_ITEMS, values, null, null);
    }

    /**
     * Inserts or updates a feeditem entry
     *
     * @param item     The FeedItem
     * @param saveFeed true if the Feed of the item should also be saved. This should be set to
     *                 false if the method is executed on a list of FeedItems of the same Feed.
     * @return the id of the entry
     */
    private long setFeedItem(FeedItem item, boolean saveFeed) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_LINK, item.getLink());
        if (item.getDescription() != null) {
            values.put(KEY_DESCRIPTION, item.getDescription());
        }
        if (item.getContentEncoded() != null) {
            values.put(KEY_CONTENT_ENCODED, item.getContentEncoded());
        }
        values.put(KEY_PUBDATE, item.getPubDate().getTime());
        values.put(KEY_PAYMENT_LINK, item.getPaymentLink());
        if (saveFeed && item.getFeed() != null) {
            setFeed(item.getFeed());
        }
        values.put(KEY_FEED, item.getFeed().getId());
        values.put(KEY_READ, item.isRead());
        values.put(KEY_HAS_CHAPTERS, item.getChapters() != null || item.hasChapters());
        values.put(KEY_ITEM_IDENTIFIER, item.getItemIdentifier());
        values.put(KEY_FLATTR_STATUS, item.getFlattrStatus().toLong());
        if (item.hasItemImage()) {
            if (item.getImage().getId() == 0) {
                setImage(item.getImage());
            }
            values.put(KEY_IMAGE, item.getImage().getId());
        }

        if (item.getId() == 0) {
            item.setId(db.insert(TABLE_NAME_FEED_ITEMS, null, values));
        } else {
            db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?",
                    new String[]{String.valueOf(item.getId())});
        }
        if (item.getMedia() != null) {
            setMedia(item.getMedia());
        }
        if (item.getChapters() != null) {
            setChapters(item);
        }
        return item.getId();
    }

    public void setFeedItemRead(boolean read, long itemId, long mediaId,
                                boolean resetMediaPosition) {
        db.beginTransaction();
        ContentValues values = new ContentValues();

        values.put(KEY_READ, read);
        db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?", new String[]{String.valueOf(itemId)});

        if (resetMediaPosition) {
            values.clear();
            values.put(KEY_POSITION, 0);
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?", new String[]{String.valueOf(mediaId)});
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void setFeedItemRead(boolean read, long... itemIds) {
        db.beginTransaction();
        ContentValues values = new ContentValues();
        for (long id : itemIds) {
            values.clear();
            values.put(KEY_READ, read);
            db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?", new String[]{String.valueOf(id)});
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void setChapters(FeedItem item) {
        ContentValues values = new ContentValues();
        for (Chapter chapter : item.getChapters()) {
            values.put(KEY_TITLE, chapter.getTitle());
            values.put(KEY_START, chapter.getStart());
            values.put(KEY_FEEDITEM, item.getId());
            values.put(KEY_LINK, chapter.getLink());
            values.put(KEY_CHAPTER_TYPE, chapter.getChapterType());
            if (chapter.getId() == 0) {
                chapter.setId(db
                        .insert(TABLE_NAME_SIMPLECHAPTERS, null, values));
            } else {
                db.update(TABLE_NAME_SIMPLECHAPTERS, values, KEY_ID + "=?",
                        new String[]{String.valueOf(chapter.getId())});
            }
        }
    }

    /**
     * Inserts or updates a download status.
     */
    public long setDownloadStatus(DownloadStatus status) {
        ContentValues values = new ContentValues();
        values.put(KEY_FEEDFILE, status.getFeedfileId());
        values.put(KEY_FEEDFILETYPE, status.getFeedfileType());
        values.put(KEY_REASON, status.getReason().getCode());
        values.put(KEY_SUCCESSFUL, status.isSuccessful());
        values.put(KEY_COMPLETION_DATE, status.getCompletionDate().getTime());
        values.put(KEY_REASON_DETAILED, status.getReasonDetailed());
        values.put(KEY_DOWNLOADSTATUS_TITLE, status.getTitle());
        if (status.getId() == 0) {
            status.setId(db.insert(TABLE_NAME_DOWNLOAD_LOG, null, values));
        } else {
            db.update(TABLE_NAME_DOWNLOAD_LOG, values, KEY_ID + "=?",
                    new String[]{String.valueOf(status.getId())});
        }
        return status.getId();
    }

    public long getDownloadLogSize() {
        final String query = String.format("SELECT COUNT(%s) FROM %s", KEY_ID, TABLE_NAME_DOWNLOAD_LOG);
        Cursor result = db.rawQuery(query, null);
        long count = 0;
        if (result.moveToFirst()) {
            count = result.getLong(0);
        }
        result.close();
        return count;
    }

    public void removeDownloadLogItems(long count) {
        if (count > 0) {
            final String sql = String.format("DELETE FROM %s WHERE %s in (SELECT %s from %s ORDER BY %s ASC LIMIT %d)",
                    TABLE_NAME_DOWNLOAD_LOG, KEY_ID, KEY_ID, TABLE_NAME_DOWNLOAD_LOG, KEY_COMPLETION_DATE, count);
            db.execSQL(sql, null);
        }
    }

    public void setQueue(List<FeedItem> queue) {
        ContentValues values = new ContentValues();
        db.beginTransaction();
        db.delete(TABLE_NAME_QUEUE, null, null);
        for (int i = 0; i < queue.size(); i++) {
            FeedItem item = queue.get(i);
            values.put(KEY_ID, i);
            values.put(KEY_FEEDITEM, item.getId());
            values.put(KEY_FEED, item.getFeed().getId());
            db.insertWithOnConflict(TABLE_NAME_QUEUE, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void clearQueue() {
        db.delete(TABLE_NAME_QUEUE, null, null);
    }

    public void removeFeedMedia(FeedMedia media) {
        db.delete(TABLE_NAME_FEED_MEDIA, KEY_ID + "=?",
                new String[]{String.valueOf(media.getId())});
    }

    public void removeChaptersOfItem(FeedItem item) {
        db.delete(TABLE_NAME_SIMPLECHAPTERS, KEY_FEEDITEM + "=?",
                new String[]{String.valueOf(item.getId())});
    }

    public void removeFeedImage(FeedImage image) {
        db.delete(TABLE_NAME_FEED_IMAGES, KEY_ID + "=?",
                new String[]{String.valueOf(image.getId())});
    }

    /**
     * Remove a FeedItem and its FeedMedia entry.
     */
    public void removeFeedItem(FeedItem item) {
        if (item.getMedia() != null) {
            removeFeedMedia(item.getMedia());
        }
        if (item.hasChapters() || item.getChapters() != null) {
            removeChaptersOfItem(item);
        }
        if (item.hasItemImage()) {
            removeFeedImage(item.getImage());
        }
        db.delete(TABLE_NAME_FEED_ITEMS, KEY_ID + "=?",
                new String[]{String.valueOf(item.getId())});
    }

    /**
     * Remove a feed with all its FeedItems and Media entries.
     */
    public void removeFeed(Feed feed) {
        db.beginTransaction();
        if (feed.getImage() != null) {
            removeFeedImage(feed.getImage());
        }
        if (feed.getItems() != null) {
            for (FeedItem item : feed.getItems()) {
                removeFeedItem(item);
            }
        }

        db.delete(TABLE_NAME_FEEDS, KEY_ID + "=?",
                new String[]{String.valueOf(feed.getId())});
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void removeDownloadStatus(DownloadStatus remove) {
        db.delete(TABLE_NAME_DOWNLOAD_LOG, KEY_ID + "=?",
                new String[]{String.valueOf(remove.getId())});
    }

    public void clearPlaybackHistory() {
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYBACK_COMPLETION_DATE, 0);
        db.update(TABLE_NAME_FEED_MEDIA, values, null, null);
    }

    /**
     * Get all Feeds from the Feed Table.
     *
     * @return The cursor of the query
     */
    public final Cursor getAllFeedsCursor() {
        Cursor c = db.query(TABLE_NAME_FEEDS, FEED_SEL_STD, null, null, null, null,
                KEY_TITLE + " COLLATE NOCASE ASC");
        return c;
    }

    public final Cursor getFeedCursorDownloadUrls() {
        return db.query(TABLE_NAME_FEEDS, new String[]{KEY_ID, KEY_DOWNLOAD_URL}, null, null, null, null, null);
    }

    public final Cursor getExpiredFeedsCursor(long expirationTime) {
        Cursor c = db.query(TABLE_NAME_FEEDS, FEED_SEL_STD, KEY_LASTUPDATE + " < " + String.valueOf(System.currentTimeMillis() - expirationTime),
                null, null, null,
                null);
        return c;
    }

    /**
     * Returns a cursor with all FeedItems of a Feed. Uses FEEDITEM_SEL_FI_SMALL
     *
     * @param feed The feed you want to get the FeedItems from.
     * @return The cursor of the query
     */
    public final Cursor getAllItemsOfFeedCursor(final Feed feed) {
        return getAllItemsOfFeedCursor(feed.getId());
    }

    public final Cursor getAllItemsOfFeedCursor(final long feedId) {
        Cursor c = db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_FEED
                        + "=?", new String[]{String.valueOf(feedId)}, null, null,
                null
        );
        return c;
    }

    /**
     * Return a cursor with the SEL_FI_EXTRA selection of a single feeditem.
     */
    public final Cursor getExtraInformationOfItem(final FeedItem item) {
        Cursor c = db
                .query(TABLE_NAME_FEED_ITEMS, SEL_FI_EXTRA, KEY_ID + "=?",
                        new String[]{String.valueOf(item.getId())}, null,
                        null, null);
        return c;
    }

    /**
     * Returns a cursor for a DB query in the FeedMedia table for a given ID.
     *
     * @param item The item you want to get the FeedMedia from
     * @return The cursor of the query
     */
    public final Cursor getFeedMediaOfItemCursor(final FeedItem item) {
        Cursor c = db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID + "=?",
                new String[]{String.valueOf(item.getMedia().getId())}, null,
                null, null);
        return c;
    }

    /**
     * Returns a cursor for a DB query in the FeedImages table for a given ID.
     *
     * @param id ID of the FeedImage
     * @return The cursor of the query
     */
    public final Cursor getImageCursor(final long id) {
        Cursor c = db.query(TABLE_NAME_FEED_IMAGES, null, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        return c;
    }

    public final Cursor getSimpleChaptersOfFeedItemCursor(final FeedItem item) {
        Cursor c = db.query(TABLE_NAME_SIMPLECHAPTERS, null, KEY_FEEDITEM
                        + "=?", new String[]{String.valueOf(item.getId())}, null,
                null, null
        );
        return c;
    }

    public final Cursor getDownloadLogCursor(final int limit) {
        Cursor c = db.query(TABLE_NAME_DOWNLOAD_LOG, null, null, null, null,
                null, KEY_COMPLETION_DATE + " DESC LIMIT " + limit);
        return c;
    }

    /**
     * Returns a cursor which contains all feed items in the queue. The returned
     * cursor uses the FEEDITEM_SEL_FI_SMALL selection.
     */
    public final Cursor getQueueCursor() {
        Object[] args = (Object[]) new String[]{
                SEL_FI_SMALL_STR + "," + TABLE_NAME_QUEUE + "." + KEY_ID,
                TABLE_NAME_FEED_ITEMS, TABLE_NAME_QUEUE,
                TABLE_NAME_FEED_ITEMS + "." + KEY_ID,
                TABLE_NAME_QUEUE + "." + KEY_FEEDITEM,
                TABLE_NAME_QUEUE + "." + KEY_ID};
        String query = String.format(
                "SELECT %s FROM %s INNER JOIN %s ON %s=%s ORDER BY %s", args);
        Cursor c = db.rawQuery(query, null);
        /*
         * Cursor c = db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL,
		 * "INNER JOIN ? ON ?=?", new String[] { TABLE_NAME_QUEUE,
		 * TABLE_NAME_FEED_ITEMS + "." + KEY_ID, TABLE_NAME_QUEUE + "." +
		 * KEY_FEEDITEM }, null, null, TABLE_NAME_QUEUE + "." + KEY_FEEDITEM);
		 */
        return c;
    }

    public Cursor getQueueIDCursor() {
        Cursor c = db.query(TABLE_NAME_QUEUE, new String[]{KEY_FEEDITEM}, null, null, null, null, KEY_ID + " ASC", null);
        return c;
    }

    /**
     * Returns a cursor which contains all feed items in the unread items list.
     * The returned cursor uses the FEEDITEM_SEL_FI_SMALL selection.
     */
    public final Cursor getUnreadItemsCursor() {
        Cursor c = db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_READ
                + "=0", null, null, null, KEY_PUBDATE + " DESC");
        return c;
    }

    public final Cursor getUnreadItemIdsCursor() {
        Cursor c = db.query(TABLE_NAME_FEED_ITEMS, new String[]{KEY_ID},
                KEY_READ + "=0", null, null, null, KEY_PUBDATE + " DESC");
        return c;

    }

    public final Cursor getRecentlyPublishedItemsCursor(int limit) {
        Cursor c = db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, null, null, null, null, KEY_PUBDATE + " DESC LIMIT " + limit);
        return c;
    }

    public Cursor getDownloadedItemsCursor() {
        final String query = "SELECT " + SEL_FI_SMALL_STR + " FROM " + TABLE_NAME_FEED_ITEMS
                + " INNER JOIN " + TABLE_NAME_FEED_MEDIA + " ON "
                + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + "="
                + TABLE_NAME_FEED_MEDIA + "." + KEY_FEEDITEM + " WHERE "
                + TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOADED + ">0";
        Cursor c = db.rawQuery(query, null);
        return c;
    }

    /**
     * Returns a cursor which contains feed media objects with a playback
     * completion date in ascending order.
     *
     * @param limit The maximum row count of the returned cursor. Must be an
     *              integer >= 0.
     * @throws IllegalArgumentException if limit < 0
     */
    public final Cursor getCompletedMediaCursor(int limit) {
        Validate.isTrue(limit >= 0, "Limit must be >= 0");

        Cursor c = db.query(TABLE_NAME_FEED_MEDIA, null,
                KEY_PLAYBACK_COMPLETION_DATE + " > 0", null, null,
                null, String.format("%s DESC LIMIT %d", KEY_PLAYBACK_COMPLETION_DATE, limit));
        return c;
    }

    public final Cursor getSingleFeedMediaCursor(long id) {
        return db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
    }

    public final Cursor getFeedMediaCursorByItemID(String... mediaIds) {
        int length = mediaIds.length;
        if (length > IN_OPERATOR_MAXIMUM) {
            Log.w(TAG, "Length of id array is larger than "
                    + IN_OPERATOR_MAXIMUM + ". Creating multiple cursors");
            int numCursors = (int) (((double) length) / (IN_OPERATOR_MAXIMUM)) + 1;
            Cursor[] cursors = new Cursor[numCursors];
            for (int i = 0; i < numCursors; i++) {
                int neededLength = 0;
                String[] parts = null;
                final int elementsLeft = length - i * IN_OPERATOR_MAXIMUM;

                if (elementsLeft >= IN_OPERATOR_MAXIMUM) {
                    neededLength = IN_OPERATOR_MAXIMUM;
                    parts = Arrays.copyOfRange(mediaIds, i
                            * IN_OPERATOR_MAXIMUM, (i + 1)
                            * IN_OPERATOR_MAXIMUM);
                } else {
                    neededLength = elementsLeft;
                    parts = Arrays.copyOfRange(mediaIds, i
                            * IN_OPERATOR_MAXIMUM, (i * IN_OPERATOR_MAXIMUM)
                            + neededLength);
                }

                cursors[i] = db.rawQuery("SELECT * FROM "
                        + TABLE_NAME_FEED_MEDIA + " WHERE " + KEY_FEEDITEM + " IN "
                        + buildInOperator(neededLength), parts);
            }
            return new MergeCursor(cursors);
        } else {
            return db.query(TABLE_NAME_FEED_MEDIA, null, KEY_FEEDITEM + " IN "
                    + buildInOperator(length), mediaIds, null, null, null);
        }
    }

    /**
     * Builds an IN-operator argument depending on the number of items.
     */
    private String buildInOperator(int size) {
        if (size == 1) {
            return "(?)";
        }
        StringBuffer buffer = new StringBuffer("(");
        for (int i = 0; i < size - 1; i++) {
            buffer.append("?,");
        }
        buffer.append("?)");
        return buffer.toString();
    }

    public final Cursor getFeedCursor(final long id) {
        Cursor c = db.query(TABLE_NAME_FEEDS, FEED_SEL_STD, KEY_ID + "=" + id, null,
                null, null, null);
        return c;
    }

    public final Cursor getFeedItemCursor(final String... ids) {
        if (ids.length > IN_OPERATOR_MAXIMUM) {
            throw new IllegalArgumentException(
                    "number of IDs must not be larger than "
                            + IN_OPERATOR_MAXIMUM
            );
        }

        return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_ID + " IN "
                + buildInOperator(ids.length), ids, null, null, null);

    }

    public int getQueueSize() {
        final String query = String.format("SELECT COUNT(%s) FROM %s", KEY_ID, TABLE_NAME_QUEUE);
        Cursor c = db.rawQuery(query, null);
        int result = 0;
        if (c.moveToFirst()) {
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    public final int getNumberOfUnreadItems() {
        final String query = "SELECT COUNT(DISTINCT " + KEY_ID + ") AS count FROM " + TABLE_NAME_FEED_ITEMS +
                " WHERE " + KEY_READ + " = 0";
        Cursor c = db.rawQuery(query, null);
        int result = 0;
        if (c.moveToFirst()) {
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    public final int getNumberOfDownloadedEpisodes() {
        final String query = "SELECT COUNT(DISTINCT " + KEY_ID + ") AS count FROM " + TABLE_NAME_FEED_MEDIA +
                " WHERE " + KEY_DOWNLOADED + " > 0";

        Cursor c = db.rawQuery(query, null);
        int result = 0;
        if (c.moveToFirst()) {
            result = c.getInt(0);
        }
        c.close();
        return result;
    }

    /**
     * Uses DatabaseUtils to escape a search query and removes ' at the
     * beginning and the end of the string returned by the escape method.
     */
    private String prepareSearchQuery(String query) {
        StringBuilder builder = new StringBuilder();
        DatabaseUtils.appendEscapedSQLString(builder, query);
        builder.deleteCharAt(0);
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Searches for the given query in the description of all items or the items
     * of a specified feed.
     *
     * @return A cursor with all search results in SEL_FI_EXTRA selection.
     */
    public Cursor searchItemDescriptions(long feedID, String query) {
        if (feedID != 0) {
            // search items in specific feed
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_FEED
                            + "=? AND " + KEY_DESCRIPTION + " LIKE '%"
                            + prepareSearchQuery(query) + "%'",
                    new String[]{String.valueOf(feedID)}, null, null,
                    null
            );
        } else {
            // search through all items
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL,
                    KEY_DESCRIPTION + " LIKE '%" + prepareSearchQuery(query)
                            + "%'", null, null, null, null
            );
        }
    }

    /**
     * Searches for the given query in the content-encoded field of all items or
     * the items of a specified feed.
     *
     * @return A cursor with all search results in SEL_FI_EXTRA selection.
     */
    public Cursor searchItemContentEncoded(long feedID, String query) {
        if (feedID != 0) {
            // search items in specific feed
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_FEED
                            + "=? AND " + KEY_CONTENT_ENCODED + " LIKE '%"
                            + prepareSearchQuery(query) + "%'",
                    new String[]{String.valueOf(feedID)}, null, null,
                    null
            );
        } else {
            // search through all items
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL,
                    KEY_CONTENT_ENCODED + " LIKE '%"
                            + prepareSearchQuery(query) + "%'", null, null,
                    null, null
            );
        }
    }

    public Cursor searchItemTitles(long feedID, String query) {
        if (feedID != 0) {
            // search items in specific feed
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL, KEY_FEED
                            + "=? AND " + KEY_TITLE + " LIKE '%"
                            + prepareSearchQuery(query) + "%'",
                    new String[]{String.valueOf(feedID)}, null, null,
                    null
            );
        } else {
            // search through all items
            return db.query(TABLE_NAME_FEED_ITEMS, FEEDITEM_SEL_FI_SMALL,
                    KEY_TITLE + " LIKE '%"
                            + prepareSearchQuery(query) + "%'", null, null,
                    null, null
            );
        }
    }

    public Cursor searchItemChapters(long feedID, String searchQuery) {
        final String query;
        if (feedID != 0) {
            query = "SELECT " + SEL_FI_SMALL_STR + " FROM " + TABLE_NAME_FEED_ITEMS + " INNER JOIN " +
                    TABLE_NAME_SIMPLECHAPTERS + " ON " + TABLE_NAME_SIMPLECHAPTERS + "." + KEY_FEEDITEM + "=" +
                    TABLE_NAME_FEED_ITEMS + "." + KEY_ID + " WHERE " + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + "=" +
                    feedID + " AND " + TABLE_NAME_SIMPLECHAPTERS + "." + KEY_TITLE + " LIKE '%"
                    + prepareSearchQuery(searchQuery) + "%'";
        } else {
            query = "SELECT " + SEL_FI_SMALL_STR + " FROM " + TABLE_NAME_FEED_ITEMS + " INNER JOIN " +
                    TABLE_NAME_SIMPLECHAPTERS + " ON " + TABLE_NAME_SIMPLECHAPTERS + "." + KEY_FEEDITEM + "=" +
                    TABLE_NAME_FEED_ITEMS + "." + KEY_ID + " WHERE " + TABLE_NAME_SIMPLECHAPTERS + "." + KEY_TITLE + " LIKE '%"
                    + prepareSearchQuery(searchQuery) + "%'";
        }
        return db.rawQuery(query, null);
    }


    public static final int IDX_FEEDSTATISTICS_FEED = 0;
    public static final int IDX_FEEDSTATISTICS_NUM_ITEMS = 1;
    public static final int IDX_FEEDSTATISTICS_NEW_ITEMS = 2;
    public static final int IDX_FEEDSTATISTICS_LATEST_EPISODE = 3;
    public static final int IDX_FEEDSTATISTICS_IN_PROGRESS_EPISODES = 4;

    /**
     * Select number of items, new items, the date of the latest episode and the number of episodes in progress. The result
     * is sorted by the title of the feed.
     */
    private static final String FEED_STATISTICS_QUERY = "SELECT Feeds.id, num_items, new_items, latest_episode, in_progress FROM " +
            " Feeds LEFT JOIN " +
            "(SELECT feed,count(*) AS num_items," +
            " COUNT(CASE WHEN read=0 THEN 1 END) AS new_items," +
            " MAX(pubDate) AS latest_episode," +
            " COUNT(CASE WHEN position>0 THEN 1 END) AS in_progress," +
            " COUNT(CASE WHEN downloaded=1 THEN 1 END) AS episodes_downloaded " +
            " FROM FeedItems LEFT JOIN FeedMedia ON FeedItems.id=FeedMedia.feeditem GROUP BY FeedItems.feed)" +
            " ON Feeds.id = feed ORDER BY Feeds.title COLLATE NOCASE ASC;";

    public Cursor getFeedStatisticsCursor() {
        return db.rawQuery(FEED_STATISTICS_QUERY, null);
    }

    /**
     * Helper class for opening the Antennapod database.
     */
    private static class PodDBHelper extends SQLiteOpenHelper {
        /**
         * Constructor.
         *
         * @param context Context to use
         * @param name    Name of the database
         * @param factory to use for creating cursor objects
         * @param version number of the database
         */
        public PodDBHelper(final Context context, final String name,
                           final CursorFactory factory, final int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_FEEDS);
            db.execSQL(CREATE_TABLE_FEED_ITEMS);
            db.execSQL(CREATE_TABLE_FEED_IMAGES);
            db.execSQL(CREATE_TABLE_FEED_MEDIA);
            db.execSQL(CREATE_TABLE_DOWNLOAD_LOG);
            db.execSQL(CREATE_TABLE_QUEUE);
            db.execSQL(CREATE_TABLE_SIMPLECHAPTERS);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                              final int newVersion) {
            ClientConfig.storageCallbacks.onUpgrade(db, oldVersion, newVersion);
        }
    }
}
