package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.DefaultDatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.model.feed.FeedFunding;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.mapper.FeedItemFilterQuery;
import de.danoeh.antennapod.storage.database.mapper.FeedItemSortQuery;

import org.apache.commons.io.FileUtils;

import static de.danoeh.antennapod.model.feed.FeedPreferences.SPEED_USE_GLOBAL;
import static de.danoeh.antennapod.model.feed.SortOrder.toCodeString;

/**
 * Implements methods for accessing the database
 */
public class PodDBAdapter {

    private static final String TAG = "PodDBAdapter";
    public static final String DATABASE_NAME = "Antennapod.db";
    public static final int VERSION = 3080000;

    /**
     * Maximum number of arguments for IN-operator.
     */
    private static final int IN_OPERATOR_MAXIMUM = 800;

    // Key-constants
    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CUSTOM_TITLE = "custom_title";
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
    public static final String KEY_IMAGE_URL = "image_url";
    public static final String KEY_FEED = "feed";
    public static final String KEY_MEDIA = "media";
    public static final String KEY_DOWNLOAD_DATE = "downloaded";
    public static final String KEY_LAST_REFRESH_ATTEMPT = "downloaded";
    public static final String KEY_LASTUPDATE = "last_update";
    public static final String KEY_FEEDFILE = "feedfile";
    public static final String KEY_REASON = "reason";
    public static final String KEY_SUCCESSFUL = "successful";
    public static final String KEY_FEEDFILETYPE = "feedfile_type";
    public static final String KEY_COMPLETION_DATE = "completion_date";
    public static final String KEY_FEEDITEM = "feeditem";
    public static final String KEY_PAYMENT_LINK = "payment_link";
    public static final String KEY_START = "start";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_HAS_CHAPTERS = "has_simple_chapters";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ITEM_IDENTIFIER = "item_identifier";
    public static final String KEY_FEED_IDENTIFIER = "feed_identifier";
    public static final String KEY_REASON_DETAILED = "reason_detailed";
    public static final String KEY_DOWNLOADSTATUS_TITLE = "title";
    public static final String KEY_AUTO_DOWNLOAD_ENABLED = "auto_download"; // Both tables use the same key
    public static final String KEY_KEEP_UPDATED = "keep_updated";
    public static final String KEY_AUTO_DELETE_ACTION = "auto_delete_action";
    public static final String KEY_FEED_VOLUME_ADAPTION = "feed_volume_adaption";
    public static final String KEY_PLAYED_DURATION = "played_duration";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_IS_PAGED = "is_paged";
    public static final String KEY_NEXT_PAGE_LINK = "next_page_link";
    public static final String KEY_HIDE = "hide";
    public static final String KEY_SORT_ORDER = "sort_order";
    public static final String KEY_LAST_UPDATE_FAILED = "last_update_failed";
    public static final String KEY_HAS_EMBEDDED_PICTURE = "has_embedded_picture";
    public static final String KEY_LAST_PLAYED_TIME_HISTORY = "playback_completion_date";
    public static final String KEY_LAST_PLAYED_TIME_STATISTICS = "last_played_time";
    public static final String KEY_INCLUDE_FILTER = "include_filter";
    public static final String KEY_EXCLUDE_FILTER = "exclude_filter";
    public static final String KEY_MINIMAL_DURATION_FILTER = "minimal_duration_filter";
    public static final String KEY_FEED_PLAYBACK_SPEED = "feed_playback_speed";
    public static final String KEY_FEED_SKIP_SILENCE = "feed_skip_silence";
    public static final String KEY_FEED_SKIP_INTRO = "feed_skip_intro";
    public static final String KEY_FEED_SKIP_ENDING = "feed_skip_ending";
    public static final String KEY_FEED_TAGS = "tags";
    public static final String KEY_EPISODE_NOTIFICATION = "episode_notification";
    public static final String KEY_NEW_EPISODES_ACTION = "new_episodes_action";
    public static final String KEY_PODCASTINDEX_CHAPTER_URL = "podcastindex_chapter_url";
    public static final String KEY_SOCIAL_INTERACT_URL = "social_interact_url";
    public static final String KEY_STATE = "state";
    public static final String KEY_PODCASTINDEX_TRANSCRIPT_URL = "podcastindex_transcript_url";
    public static final String KEY_PODCASTINDEX_TRANSCRIPT_TYPE = "podcastindex_transcript_type";

    // Table names
    public static final String TABLE_NAME_FEEDS = "Feeds";
    public static final String TABLE_NAME_FEED_ITEMS = "FeedItems";
    public static final String TABLE_NAME_FEED_IMAGES = "FeedImages";
    public static final String TABLE_NAME_FEED_MEDIA = "FeedMedia";
    public static final String TABLE_NAME_DOWNLOAD_LOG = "DownloadLog";
    public static final String TABLE_NAME_QUEUE = "Queue";
    public static final String TABLE_NAME_SIMPLECHAPTERS = "SimpleChapters";
    public static final String TABLE_NAME_FAVORITES = "Favorites";

    // SQL Statements for creating new tables
    private static final String TABLE_PRIMARY_KEY = KEY_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT ,";

    private static final String CREATE_TABLE_FEEDS = "CREATE TABLE " + TABLE_NAME_FEEDS + " ("
            + TABLE_PRIMARY_KEY + KEY_TITLE + " TEXT,"
            + KEY_CUSTOM_TITLE + " TEXT,"
            + KEY_FILE_URL + " TEXT,"
            + KEY_DOWNLOAD_URL + " TEXT,"
            + KEY_LAST_REFRESH_ATTEMPT + " INTEGER,"
            + KEY_LINK + " TEXT,"
            + KEY_DESCRIPTION + " TEXT,"
            + KEY_PAYMENT_LINK + " TEXT,"
            + KEY_LASTUPDATE + " TEXT,"
            + KEY_LANGUAGE + " TEXT,"
            + KEY_AUTHOR + " TEXT,"
            + KEY_IMAGE_URL + " TEXT,"
            + KEY_TYPE + " TEXT,"
            + KEY_FEED_IDENTIFIER + " TEXT,"
            + KEY_AUTO_DOWNLOAD_ENABLED + " INTEGER DEFAULT 1,"
            + KEY_USERNAME + " TEXT,"
            + KEY_PASSWORD + " TEXT,"
            + KEY_INCLUDE_FILTER + " TEXT DEFAULT '',"
            + KEY_EXCLUDE_FILTER + " TEXT DEFAULT '',"
            + KEY_MINIMAL_DURATION_FILTER + " INTEGER DEFAULT -1,"
            + KEY_KEEP_UPDATED + " INTEGER DEFAULT 1,"
            + KEY_IS_PAGED + " INTEGER DEFAULT 0,"
            + KEY_NEXT_PAGE_LINK + " TEXT,"
            + KEY_HIDE + " TEXT,"
            + KEY_SORT_ORDER + " TEXT,"
            + KEY_LAST_UPDATE_FAILED + " INTEGER DEFAULT 0,"
            + KEY_AUTO_DELETE_ACTION + " INTEGER DEFAULT 0,"
            + KEY_FEED_PLAYBACK_SPEED + " REAL DEFAULT " + SPEED_USE_GLOBAL + ","
            + KEY_FEED_SKIP_SILENCE + " INTEGER DEFAULT " + FeedPreferences.SkipSilence.GLOBAL.code + ","
            + KEY_FEED_VOLUME_ADAPTION + " INTEGER DEFAULT 0,"
            + KEY_FEED_TAGS + " TEXT,"
            + KEY_FEED_SKIP_INTRO + " INTEGER DEFAULT 0,"
            + KEY_FEED_SKIP_ENDING + " INTEGER DEFAULT 0,"
            + KEY_EPISODE_NOTIFICATION + " INTEGER DEFAULT 0,"
            + KEY_STATE + " INTEGER DEFAULT " + Feed.STATE_SUBSCRIBED + ","
            + KEY_NEW_EPISODES_ACTION + " INTEGER DEFAULT 0)";

    private static final String CREATE_TABLE_FEED_ITEMS = "CREATE TABLE "
            + TABLE_NAME_FEED_ITEMS + " (" + TABLE_PRIMARY_KEY
            + KEY_TITLE + " TEXT," + KEY_PUBDATE + " INTEGER,"
            + KEY_READ + " INTEGER," + KEY_LINK + " TEXT,"
            + KEY_DESCRIPTION + " TEXT," + KEY_PAYMENT_LINK + " TEXT,"
            + KEY_MEDIA + " INTEGER," + KEY_FEED + " INTEGER,"
            + KEY_HAS_CHAPTERS + " INTEGER," + KEY_ITEM_IDENTIFIER + " TEXT,"
            + KEY_IMAGE_URL + " TEXT,"
            + KEY_AUTO_DOWNLOAD_ENABLED + " INTEGER,"
            + KEY_PODCASTINDEX_CHAPTER_URL + " TEXT,"
            + KEY_PODCASTINDEX_TRANSCRIPT_TYPE + " TEXT,"
            + KEY_PODCASTINDEX_TRANSCRIPT_URL + " TEXT,"
            + KEY_SOCIAL_INTERACT_URL + " TEXT)";

    private static final String CREATE_TABLE_FEED_MEDIA = "CREATE TABLE "
            + TABLE_NAME_FEED_MEDIA + " (" + TABLE_PRIMARY_KEY + KEY_DURATION
            + " INTEGER," + KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL
            + " TEXT," + KEY_DOWNLOAD_DATE + " INTEGER," + KEY_POSITION
            + " INTEGER," + KEY_SIZE + " INTEGER," + KEY_MIME_TYPE + " TEXT,"
            + KEY_LAST_PLAYED_TIME_HISTORY + " INTEGER,"
            + KEY_FEEDITEM + " INTEGER,"
            + KEY_PLAYED_DURATION + " INTEGER,"
            + KEY_HAS_EMBEDDED_PICTURE + " INTEGER,"
            + KEY_LAST_PLAYED_TIME_STATISTICS + " INTEGER" + ")";

    private static final String CREATE_TABLE_DOWNLOAD_LOG = "CREATE TABLE "
            + TABLE_NAME_DOWNLOAD_LOG + " (" + TABLE_PRIMARY_KEY + KEY_FEEDFILE
            + " INTEGER," + KEY_FEEDFILETYPE + " INTEGER," + KEY_REASON
            + " INTEGER," + KEY_SUCCESSFUL + " INTEGER," + KEY_COMPLETION_DATE
            + " INTEGER," + KEY_REASON_DETAILED + " TEXT,"
            + KEY_DOWNLOADSTATUS_TITLE + " TEXT)";

    private static final String CREATE_TABLE_QUEUE = "CREATE TABLE "
            + TABLE_NAME_QUEUE + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_FEEDITEM + " INTEGER," + KEY_FEED + " INTEGER)";

    private static final String CREATE_TABLE_SIMPLECHAPTERS = "CREATE TABLE "
            + TABLE_NAME_SIMPLECHAPTERS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
            + " TEXT," + KEY_START + " INTEGER," + KEY_FEEDITEM + " INTEGER,"
            + KEY_LINK + " TEXT," + KEY_IMAGE_URL + " TEXT)";

    // SQL Statements for creating indexes
    static final String CREATE_INDEX_FEEDITEMS_FEED = "CREATE INDEX "
            + TABLE_NAME_FEED_ITEMS + "_" + KEY_FEED + " ON " + TABLE_NAME_FEED_ITEMS + " ("
            + KEY_FEED + ")";

    static final String CREATE_INDEX_FEEDITEMS_PUBDATE = "CREATE INDEX "
            + TABLE_NAME_FEED_ITEMS + "_" + KEY_PUBDATE + " ON " + TABLE_NAME_FEED_ITEMS + " ("
            + KEY_PUBDATE + ")";

    static final String CREATE_INDEX_FEEDITEMS_READ = "CREATE INDEX "
            + TABLE_NAME_FEED_ITEMS + "_" + KEY_READ + " ON " + TABLE_NAME_FEED_ITEMS + " ("
            + KEY_READ + ")";

    static final String CREATE_INDEX_QUEUE_FEEDITEM = "CREATE INDEX "
            + TABLE_NAME_QUEUE + "_" + KEY_FEEDITEM + " ON " + TABLE_NAME_QUEUE + " ("
            + KEY_FEEDITEM + ")";

    static final String CREATE_INDEX_FEEDMEDIA_FEEDITEM = "CREATE INDEX "
            + TABLE_NAME_FEED_MEDIA + "_" + KEY_FEEDITEM + " ON " + TABLE_NAME_FEED_MEDIA + " ("
            + KEY_FEEDITEM + ")";

    static final String CREATE_INDEX_SIMPLECHAPTERS_FEEDITEM = "CREATE INDEX "
            + TABLE_NAME_SIMPLECHAPTERS + "_" + KEY_FEEDITEM + " ON " + TABLE_NAME_SIMPLECHAPTERS + " ("
            + KEY_FEEDITEM + ")";

    static final String CREATE_TABLE_FAVORITES = "CREATE TABLE "
            + TABLE_NAME_FAVORITES + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_FEEDITEM + " INTEGER," + KEY_FEED + " INTEGER)";

    /**
     * All the tables in the database
     */
    private static final String[] ALL_TABLES = {
            TABLE_NAME_FEEDS,
            TABLE_NAME_FEED_ITEMS,
            TABLE_NAME_FEED_MEDIA,
            TABLE_NAME_DOWNLOAD_LOG,
            TABLE_NAME_QUEUE,
            TABLE_NAME_SIMPLECHAPTERS,
            TABLE_NAME_FAVORITES
    };

    public static final String SELECT_KEY_ITEM_ID = "item_id";
    public static final String SELECT_KEY_MEDIA_ID = "media_id";
    public static final String SELECT_KEY_FEED_ID = "feed_id";

    private static final String KEYS_FEED_ITEM_WITHOUT_DESCRIPTION =
            TABLE_NAME_FEED_ITEMS + "." + KEY_ID + " AS " + SELECT_KEY_ITEM_ID + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_TITLE + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_READ + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_LINK + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_PAYMENT_LINK + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_MEDIA + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_HAS_CHAPTERS + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_ITEM_IDENTIFIER + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_IMAGE_URL + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_AUTO_DOWNLOAD_ENABLED + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_PODCASTINDEX_CHAPTER_URL + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_SOCIAL_INTERACT_URL + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_PODCASTINDEX_TRANSCRIPT_TYPE + ", "
            + TABLE_NAME_FEED_ITEMS + "." + KEY_PODCASTINDEX_TRANSCRIPT_URL;

    private static final String KEYS_FEED_MEDIA =
            TABLE_NAME_FEED_MEDIA + "." + KEY_ID + " AS " + SELECT_KEY_MEDIA_ID + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_DURATION + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_FILE_URL + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOAD_URL + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOAD_DATE + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_POSITION + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_SIZE + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_MIME_TYPE + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_HISTORY + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_FEEDITEM + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_PLAYED_DURATION + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_HAS_EMBEDDED_PICTURE + ", "
            + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS;

    private static final String KEYS_FEED =
            TABLE_NAME_FEEDS + "." + KEY_ID + " AS " + SELECT_KEY_FEED_ID + ", "
            + TABLE_NAME_FEEDS + "." + KEY_TITLE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_CUSTOM_TITLE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FILE_URL + ", "
            + TABLE_NAME_FEEDS + "." + KEY_DOWNLOAD_URL + ", "
            + TABLE_NAME_FEEDS + "." + KEY_LAST_REFRESH_ATTEMPT + ", "
            + TABLE_NAME_FEEDS + "." + KEY_LINK + ", "
            + TABLE_NAME_FEEDS + "." + KEY_DESCRIPTION + ", "
            + TABLE_NAME_FEEDS + "." + KEY_PAYMENT_LINK + ", "
            + TABLE_NAME_FEEDS + "." + KEY_LASTUPDATE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_LANGUAGE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_AUTHOR + ", "
            + TABLE_NAME_FEEDS + "." + KEY_IMAGE_URL + ", "
            + TABLE_NAME_FEEDS + "." + KEY_TYPE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_IDENTIFIER + ", "
            + TABLE_NAME_FEEDS + "." + KEY_IS_PAGED + ", "
            + TABLE_NAME_FEEDS + "." + KEY_NEXT_PAGE_LINK + ", "
            + TABLE_NAME_FEEDS + "." + KEY_LAST_UPDATE_FAILED + ", "
            + TABLE_NAME_FEEDS + "." + KEY_AUTO_DOWNLOAD_ENABLED + ", "
            + TABLE_NAME_FEEDS + "." + KEY_KEEP_UPDATED + ", "
            + TABLE_NAME_FEEDS + "." + KEY_USERNAME + ", "
            + TABLE_NAME_FEEDS + "." + KEY_PASSWORD + ", "
            + TABLE_NAME_FEEDS + "." + KEY_HIDE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_SORT_ORDER + ", "
            + TABLE_NAME_FEEDS + "." + KEY_AUTO_DELETE_ACTION + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_VOLUME_ADAPTION + ", "
            + TABLE_NAME_FEEDS + "." + KEY_INCLUDE_FILTER + ", "
            + TABLE_NAME_FEEDS + "." + KEY_EXCLUDE_FILTER + ", "
            + TABLE_NAME_FEEDS + "." + KEY_MINIMAL_DURATION_FILTER + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_PLAYBACK_SPEED + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_SKIP_SILENCE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_TAGS + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_SKIP_INTRO + ", "
            + TABLE_NAME_FEEDS + "." + KEY_FEED_SKIP_ENDING + ", "
            + TABLE_NAME_FEEDS + "." + KEY_EPISODE_NOTIFICATION + ", "
            + TABLE_NAME_FEEDS + "." + KEY_STATE + ", "
            + TABLE_NAME_FEEDS + "." + KEY_NEW_EPISODES_ACTION;

    private static final String JOIN_FEED_ITEM_AND_MEDIA = " LEFT JOIN " + TABLE_NAME_FEED_MEDIA
            + " ON " + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + "=" + TABLE_NAME_FEED_MEDIA + "." + KEY_FEEDITEM + " ";

    private static final String SELECT_FEED_ITEMS_AND_MEDIA_WITH_DESCRIPTION =
            "SELECT " + KEYS_FEED_ITEM_WITHOUT_DESCRIPTION + ", " + KEYS_FEED_MEDIA + ", "
                    + TABLE_NAME_FEED_ITEMS + "." + KEY_DESCRIPTION
            + " FROM " + TABLE_NAME_FEED_ITEMS
            + JOIN_FEED_ITEM_AND_MEDIA;
    private static final String SELECT_FEED_ITEMS_AND_MEDIA =
            "SELECT " + KEYS_FEED_ITEM_WITHOUT_DESCRIPTION + ", " + KEYS_FEED_MEDIA
            + " FROM " + TABLE_NAME_FEED_ITEMS
            + JOIN_FEED_ITEM_AND_MEDIA;
    public static final String SELECT_WHERE_FEED_IS_SUBSCRIBED = TABLE_NAME_FEED_ITEMS + "." + KEY_FEED
            + " IN (SELECT " + KEY_ID + " FROM " + TABLE_NAME_FEEDS
            + " WHERE " + KEY_STATE + "=" + Feed.STATE_SUBSCRIBED + ")";

    private static Context context;
    private static PodDBAdapter instance;

    private final SQLiteDatabase db;
    private final PodDBHelper dbHelper;

    public static void init(Context context) {
        PodDBAdapter.context = context.getApplicationContext();
    }

    public static synchronized PodDBAdapter getInstance() {
        if (instance == null) {
            instance = new PodDBAdapter();
        }
        return instance;
    }

    private PodDBAdapter() {
        dbHelper = new PodDBHelper(PodDBAdapter.context, DATABASE_NAME, null);
        db = openDb();
    }

    private SQLiteDatabase openDb() {
        SQLiteDatabase newDb;
        try {
            newDb = dbHelper.getWritableDatabase();
            newDb.disableWriteAheadLogging();
        } catch (SQLException ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            newDb = dbHelper.getReadableDatabase();
        }
        return newDb;
    }

    public synchronized PodDBAdapter open() {
        // do nothing
        return this;
    }

    public synchronized void close() {
        // do nothing
    }

    /**
     * <p>Resets all database connections to ensure new database connections for
     * the next test case. Call method only for unit tests.</p>
     *
     * <p>That's a workaround for a Robolectric issue in ShadowSQLiteConnection
     * that leads to an error <tt>IllegalStateException: Illegal connection
     * pointer</tt> if several threads try to use the same database connection.
     * For more information see
     * <a href="https://github.com/robolectric/robolectric/issues/1890">robolectric/robolectric#1890</a>.</p>
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void tearDownTests() {
        getInstance().dbHelper.close();
        instance = null;
    }

    public static boolean deleteDatabase() {
        PodDBAdapter adapter = getInstance();
        adapter.open();
        try {
            for (String tableName : ALL_TABLES) {
                adapter.db.delete(tableName, "1", null);
            }
            return true;
        } finally {
            adapter.close();
        }
    }

    /**
     * Inserts or updates a feed entry
     *
     * @return the id of the entry
     */
    private long setFeed(Feed feed) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, feed.getFeedTitle());
        values.put(KEY_LINK, feed.getLink());
        values.put(KEY_DESCRIPTION, feed.getDescription());
        values.put(KEY_PAYMENT_LINK, FeedFunding.getPaymentLinksAsString(feed.getPaymentLinks()));
        values.put(KEY_AUTHOR, feed.getAuthor());
        values.put(KEY_LANGUAGE, feed.getLanguage());
        values.put(KEY_IMAGE_URL, feed.getImageUrl());

        values.put(KEY_FILE_URL, feed.getLocalFileUrl());
        values.put(KEY_DOWNLOAD_URL, feed.getDownloadUrl());
        values.put(KEY_LAST_REFRESH_ATTEMPT, feed.getLastRefreshAttempt());
        values.put(KEY_LASTUPDATE, feed.getLastModified());
        values.put(KEY_TYPE, feed.getType());
        values.put(KEY_FEED_IDENTIFIER, feed.getFeedIdentifier());
        values.put(KEY_STATE, feed.getState());

        values.put(KEY_IS_PAGED, feed.isPaged());
        values.put(KEY_NEXT_PAGE_LINK, feed.getNextPageLink());
        if (feed.getItemFilter() != null && feed.getItemFilter().getValues().length > 0) {
            values.put(KEY_HIDE, TextUtils.join(",", feed.getItemFilter().getValues()));
        } else {
            values.put(KEY_HIDE, "");
        }
        values.put(KEY_SORT_ORDER, toCodeString(feed.getSortOrder()));
        values.put(KEY_LAST_UPDATE_FAILED, feed.hasLastUpdateFailed());
        if (feed.getId() == 0) {
            // Create new entry
            Log.d(this.toString(), "Inserting new Feed into db");
            feed.setId(db.insert(TABLE_NAME_FEEDS, null, values));
        } else {
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
        values.put(KEY_AUTO_DOWNLOAD_ENABLED, prefs.getAutoDownload().code);
        values.put(KEY_KEEP_UPDATED, prefs.getKeepUpdated());
        values.put(KEY_AUTO_DELETE_ACTION, prefs.getAutoDeleteAction().code);
        values.put(KEY_FEED_VOLUME_ADAPTION, prefs.getVolumeAdaptionSetting().toInteger());
        values.put(KEY_USERNAME, prefs.getUsername());
        values.put(KEY_PASSWORD, prefs.getPassword());
        values.put(KEY_INCLUDE_FILTER, prefs.getFilter().getIncludeFilterRaw());
        values.put(KEY_EXCLUDE_FILTER, prefs.getFilter().getExcludeFilterRaw());
        values.put(KEY_MINIMAL_DURATION_FILTER, prefs.getFilter().getMinimalDurationFilter());
        values.put(KEY_FEED_PLAYBACK_SPEED, prefs.getFeedPlaybackSpeed());
        values.put(KEY_FEED_SKIP_SILENCE, prefs.getFeedSkipSilence().code);
        values.put(KEY_FEED_TAGS, prefs.getTagsAsString());
        values.put(KEY_FEED_SKIP_INTRO, prefs.getFeedSkipIntro());
        values.put(KEY_FEED_SKIP_ENDING, prefs.getFeedSkipEnding());
        values.put(KEY_EPISODE_NOTIFICATION, prefs.getShowEpisodeNotification());
        values.put(KEY_NEW_EPISODES_ACTION, prefs.getNewEpisodesAction().code);
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(prefs.getFeedID())});
    }

    public void setFeedItemFilter(long feedId, Set<String> filterValues) {
        String valuesList = TextUtils.join(",", filterValues);
        Log.d(TAG, String.format(Locale.US,
                "setFeedItemFilter() called with: feedId = [%d], filterValues = [%s]", feedId, valuesList));
        ContentValues values = new ContentValues();
        values.put(KEY_HIDE, valuesList);
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(feedId)});
    }

    public void setFeedItemSortOrder(long feedId, @Nullable SortOrder sortOrder) {
        ContentValues values = new ContentValues();
        values.put(KEY_SORT_ORDER, toCodeString(sortOrder));
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(feedId)});
    }

    /**
     * Inserts or updates a media entry
     *
     * @return the id of the entry
     */
    public long setMedia(FeedMedia media) {
        ContentValues values = new ContentValues();
        values.put(KEY_DURATION, media.getDuration());
        values.put(KEY_POSITION, media.getPosition());
        values.put(KEY_SIZE, media.getSize());
        values.put(KEY_MIME_TYPE, media.getMimeType());
        values.put(KEY_DOWNLOAD_URL, media.getDownloadUrl());
        values.put(KEY_DOWNLOAD_DATE, media.getDownloadDate());
        values.put(KEY_FILE_URL, media.getLocalFileUrl());
        values.put(KEY_HAS_EMBEDDED_PICTURE, media.hasEmbeddedPicture());
        values.put(KEY_LAST_PLAYED_TIME_STATISTICS, media.getLastPlayedTimeStatistics());

        if (media.getLastPlayedTimeHistory() != null) {
            values.put(KEY_LAST_PLAYED_TIME_HISTORY, media.getLastPlayedTimeHistory().getTime());
        } else {
            values.put(KEY_LAST_PLAYED_TIME_HISTORY, 0);
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
            values.put(KEY_LAST_PLAYED_TIME_STATISTICS, media.getLastPlayedTimeStatistics());
            values.put(KEY_LAST_PLAYED_TIME_HISTORY, media.getLastPlayedTimeHistory().getTime());
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                    new String[]{String.valueOf(media.getId())});
        } else {
            Log.e(TAG, "setFeedMediaPlaybackInformation: ID of media was 0");
        }
    }

    public void setFeedMediaLastPlayedTimeHistory(FeedMedia media) {
        if (media.getId() != 0) {
            ContentValues values = new ContentValues();
            values.put(KEY_LAST_PLAYED_TIME_HISTORY, media.getLastPlayedTimeHistory().getTime());
            values.put(KEY_PLAYED_DURATION, media.getPlayedDuration());
            db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                    new String[]{String.valueOf(media.getId())});
        } else {
            Log.e(TAG, "setFeedMediaLastPlayedTimeHistory: ID of media was 0");
        }
    }

    public void resetAllMediaPlayedDuration() {
        try {
            db.beginTransactionNonExclusive();
            ContentValues values = new ContentValues();
            values.put(KEY_PLAYED_DURATION, 0);
            db.update(TABLE_NAME_FEED_MEDIA, values, null, new String[0]);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Insert all FeedItems of a feed and the feed object itself in a single
     * transaction
     */
    public void setCompleteFeed(Feed... feeds) {
        try {
            db.beginTransactionNonExclusive();
            for (Feed feed : feeds) {
                setFeed(feed);
                if (feed.getItems() != null) {
                    for (FeedItem item : feed.getItems()) {
                        updateOrInsertFeedItem(item, false);
                    }
                }
                if (feed.getPreferences() != null) {
                    setFeedPreferences(feed.getPreferences());
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Updates the download URL of a Feed.
     */
    public void setFeedDownloadUrl(String original, String updated) {
        ContentValues values = new ContentValues();
        values.put(KEY_DOWNLOAD_URL, updated);
        db.update(TABLE_NAME_FEEDS, values, KEY_DOWNLOAD_URL + "=?", new String[]{original});
    }

    public void storeFeedItemlist(List<FeedItem> items) {
        try {
            db.beginTransactionNonExclusive();
            for (FeedItem item : items) {
                updateOrInsertFeedItem(item, true);
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    public long setSingleFeedItem(FeedItem item) {
        long result = 0;
        try {
            db.beginTransactionNonExclusive();
            result = updateOrInsertFeedItem(item, true);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
        return result;
    }

    /**
     * Inserts or updates a feeditem entry
     *
     * @param item     The FeedItem
     * @param saveFeed true if the Feed of the item should also be saved. This should be set to
     *                 false if the method is executed on a list of FeedItems of the same Feed.
     * @return the id of the entry
     */
    private long updateOrInsertFeedItem(FeedItem item, boolean saveFeed) {
        if (item.getId() == 0 && item.getPubDate() == null) {
            Log.e(TAG, "Newly saved item has no pubDate. Using current date as pubDate");
            item.setPubDate(new Date());
        }

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, item.getTitle());
        values.put(KEY_LINK, item.getLink());
        if (item.getDescription() != null) {
            values.put(KEY_DESCRIPTION, item.getDescription());
        }
        values.put(KEY_PUBDATE, item.getPubDate().getTime());
        values.put(KEY_PAYMENT_LINK, item.getPaymentLink());
        if (saveFeed && item.getFeed() != null) {
            setFeed(item.getFeed());
        }
        values.put(KEY_FEED, item.getFeed().getId());
        if (item.isNew()) {
            values.put(KEY_READ, FeedItem.NEW);
        } else if (item.isPlayed()) {
            values.put(KEY_READ, FeedItem.PLAYED);
        } else {
            values.put(KEY_READ, FeedItem.UNPLAYED);
        }
        values.put(KEY_HAS_CHAPTERS, item.getChapters() != null || item.hasChapters());
        values.put(KEY_ITEM_IDENTIFIER, item.getItemIdentifier());
        values.put(KEY_AUTO_DOWNLOAD_ENABLED, item.isAutoDownloadEnabled());
        values.put(KEY_IMAGE_URL, item.getImageUrl());
        values.put(KEY_PODCASTINDEX_CHAPTER_URL, item.getPodcastIndexChapterUrl());
        values.put(KEY_SOCIAL_INTERACT_URL, item.getSocialInteractUrl());

        // We only store one transcript url, we prefer JSON if it exists
        String type = item.getTranscriptType();
        String url = item.getTranscriptUrl();
        if (url != null) {
            values.put(KEY_PODCASTINDEX_TRANSCRIPT_TYPE, type);
            values.put(KEY_PODCASTINDEX_TRANSCRIPT_URL, url);
        }

        if (item.getId() == 0) {
            item.setId(db.insert(TABLE_NAME_FEED_ITEMS, null, values));
        } else {
            db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?",
                    new String[]{String.valueOf(item.getId())});
        }
        if (item.getMedia() != null) {
            setMedia(item.getMedia());
            item.getMedia().setItemId(item.getId());
        }
        if (item.getChapters() != null) {
            setChapters(item);
        }
        return item.getId();
    }

    public void setFeedItemRead(FeedItem item, int played, boolean resetMediaPosition) {
        try {
            db.beginTransactionNonExclusive();
            ContentValues values = new ContentValues();

            values.put(KEY_READ, played);
            db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?", new String[]{String.valueOf(item.getId())});
            item.setPlayed(played == FeedItem.PLAYED);

            if (resetMediaPosition && item.hasMedia()) {
                values.clear();
                values.put(KEY_POSITION, 0);
                db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?",
                        new String[]{String.valueOf(item.getMedia().getId())});
                item.getMedia().setPosition(0);
            }

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sets the 'read' attribute of the item.
     *
     * @param read    must be one of FeedItem.PLAYED, FeedItem.NEW, FeedItem.UNPLAYED
     * @param itemIds items to change the value of
     */
    public void setFeedItemRead(int read, long... itemIds) {
        try {
            db.beginTransactionNonExclusive();
            ContentValues values = new ContentValues();
            for (long id : itemIds) {
                values.clear();
                values.put(KEY_READ, read);
                db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    private void setChapters(FeedItem item) {
        ContentValues values = new ContentValues();
        for (Chapter chapter : item.getChapters()) {
            values.put(KEY_TITLE, chapter.getTitle());
            values.put(KEY_START, chapter.getStart());
            values.put(KEY_FEEDITEM, item.getId());
            values.put(KEY_LINK, chapter.getLink());
            values.put(KEY_IMAGE_URL, chapter.getImageUrl());
            if (chapter.getId() == 0) {
                chapter.setId(db.insert(TABLE_NAME_SIMPLECHAPTERS, null, values));
            } else {
                db.update(TABLE_NAME_SIMPLECHAPTERS, values, KEY_ID + "=?",
                        new String[]{String.valueOf(chapter.getId())});
            }
        }
    }

    public void resetPagedFeedPage(Feed feed) {
        final String sql = "UPDATE " + TABLE_NAME_FEEDS
                + " SET " + KEY_NEXT_PAGE_LINK + "=" + KEY_DOWNLOAD_URL
                + " WHERE " + KEY_ID + "=" + feed.getId();
        db.execSQL(sql);
    }

    public void setFeedLastUpdateFailed(long feedId, boolean failed) {
        final String sql = "UPDATE " + TABLE_NAME_FEEDS
                + " SET " + KEY_LAST_UPDATE_FAILED + "=" + (failed ? "1" : "0")
                + "," + KEY_LAST_REFRESH_ATTEMPT + "=" + System.currentTimeMillis()
                + " WHERE " + KEY_ID + "=" + feedId;
        db.execSQL(sql);
    }

    public void setFeedCustomTitle(long feedId, String customTitle) {
        ContentValues values = new ContentValues();
        values.put(KEY_CUSTOM_TITLE, customTitle);
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(feedId)});
    }

    public void setFeedState(long feedId, int state) {
        ContentValues values = new ContentValues();
        values.put(KEY_STATE, state);
        db.update(TABLE_NAME_FEEDS, values, KEY_ID + "=?", new String[]{String.valueOf(feedId)});
    }

    /**
     * Inserts or updates a download status.
     */
    public long setDownloadStatus(DownloadResult status) {
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

    public void setFavorites(List<FeedItem> favorites) {
        ContentValues values = new ContentValues();
        try {
            db.beginTransactionNonExclusive();
            db.delete(TABLE_NAME_FAVORITES, null, null);
            for (int i = 0; i < favorites.size(); i++) {
                FeedItem item = favorites.get(i);
                values.put(KEY_ID, i);
                values.put(KEY_FEEDITEM, item.getId());
                values.put(KEY_FEED, item.getFeed().getId());
                db.insertWithOnConflict(TABLE_NAME_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Adds the item to favorites
     */
    public void addFavoriteItem(FeedItem item) {
        // don't add an item that's already there...
        if (isItemInFavorites(item)) {
            Log.d(TAG, "item already in favorites");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FEEDITEM, item.getId());
        values.put(KEY_FEED, item.getFeedId());
        db.insert(TABLE_NAME_FAVORITES, null, values);
    }

    public void removeFavoriteItem(FeedItem item) {
        db.execSQL("DELETE FROM " + TABLE_NAME_FAVORITES + " WHERE " + KEY_FEEDITEM + "=" + item.getId());
    }

    private boolean isItemInFavorites(FeedItem item) {
        String query = String.format(Locale.US, "SELECT %s from %s WHERE %s=%d",
                KEY_ID, TABLE_NAME_FAVORITES, KEY_FEEDITEM, item.getId());
        Cursor c = db.rawQuery(query, null);
        int count = c.getCount();
        c.close();
        return count > 0;
    }

    public void setQueue(List<FeedItem> queue) {
        ContentValues values = new ContentValues();
        try {
            db.beginTransactionNonExclusive();
            db.delete(TABLE_NAME_QUEUE, null, null);
            for (int i = 0; i < queue.size(); i++) {
                FeedItem item = queue.get(i);
                values.put(KEY_ID, i);
                values.put(KEY_FEEDITEM, item.getId());
                values.put(KEY_FEED, item.getFeed().getId());
                db.insertWithOnConflict(TABLE_NAME_QUEUE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    public void clearQueue() {
        db.delete(TABLE_NAME_QUEUE, null, null);
    }

    /**
     * Remove the listed items and their FeedMedia entries.
     */
    public void removeFeedItems(@NonNull List<FeedItem> items) {
        try {
            StringBuilder mediaIds = new StringBuilder();
            StringBuilder itemIds = new StringBuilder();
            for (FeedItem item : items) {
                if (item.getMedia() != null) {
                    if (mediaIds.length() != 0) {
                        mediaIds.append(",");
                    }
                    mediaIds.append(item.getMedia().getId());
                }
                if (itemIds.length() != 0) {
                    itemIds.append(",");
                }
                itemIds.append(item.getId());
            }

            db.beginTransactionNonExclusive();
            db.delete(TABLE_NAME_SIMPLECHAPTERS, KEY_FEEDITEM + " IN (" + itemIds + ")", null);
            db.delete(TABLE_NAME_DOWNLOAD_LOG, KEY_FEEDFILETYPE + "=" + FeedMedia.FEEDFILETYPE_FEEDMEDIA
                            + " AND " + KEY_FEEDFILE + " IN (" + mediaIds + ")", null);
            db.delete(TABLE_NAME_FEED_MEDIA, KEY_ID + " IN (" + mediaIds + ")", null);
            db.delete(TABLE_NAME_FEED_ITEMS, KEY_ID + " IN (" + itemIds + ")", null);
            db.delete(TABLE_NAME_FAVORITES, KEY_FEEDITEM + " IN (" + itemIds + ")", null);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Remove a feed with all its FeedItems and Media entries.
     */
    public void removeFeed(Feed feed) {
        try {
            db.beginTransactionNonExclusive();
            if (feed.getItems() != null) {
                removeFeedItems(feed.getItems());
            }
            // delete download log entries for feed
            db.delete(TABLE_NAME_DOWNLOAD_LOG, KEY_FEEDFILE + "=? AND " + KEY_FEEDFILETYPE + "=?",
                    new String[]{String.valueOf(feed.getId()), String.valueOf(Feed.FEEDFILETYPE_FEED)});

            db.delete(TABLE_NAME_FEEDS, KEY_ID + "=?",
                    new String[]{String.valueOf(feed.getId())});
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            db.endTransaction();
        }
    }

    public void clearPlaybackHistory() {
        ContentValues values = new ContentValues();
        values.put(KEY_LAST_PLAYED_TIME_HISTORY, 0);
        db.update(TABLE_NAME_FEED_MEDIA, values, null, null);
    }

    public void clearDownloadLog() {
        db.delete(TABLE_NAME_DOWNLOAD_LOG, null, null);
    }

    /**
     * Get all Feeds from the Feed Table.
     *
     * @return The cursor of the query
     */
    public final Cursor getAllFeedsCursor() {
        final String query = "SELECT " + KEYS_FEED
                + " FROM " + TABLE_NAME_FEEDS
                + " ORDER BY " + TABLE_NAME_FEEDS + "." + KEY_TITLE + " COLLATE NOCASE ASC";
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedCursorDownloadUrls() {
        return db.query(TABLE_NAME_FEEDS, new String[]{KEY_ID, KEY_DOWNLOAD_URL}, null, null, null, null, null);
    }

    /**
     * Returns a cursor with all FeedItems of a Feed. Uses FEEDITEM_SEL_FI_SMALL
     *
     * @param feed The feed you want to get the FeedItems from.
     * @return The cursor of the query
     */
    public final Cursor getItemsOfFeedCursor(final Feed feed, FeedItemFilter filter, SortOrder sortOrder,
                                             int offset, int limit) {
        String orderByQuery = FeedItemSortQuery.generateFrom(sortOrder);
        String filterQuery = FeedItemFilterQuery.generateFrom(filter);
        String whereClauseAnd = "".equals(filterQuery) ? "" : " AND " + filterQuery;
        final String query = SELECT_FEED_ITEMS_AND_MEDIA
                + " WHERE " + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + "=" + feed.getId()
                + whereClauseAnd
                + " ORDER BY " + orderByQuery
                + " LIMIT " + offset + ", " + limit;
        return db.rawQuery(query, null);
    }

    /**
     * Return the description and content_encoded of item
     */
    public final Cursor getDescriptionOfItem(final FeedItem item) {
        final String query = "SELECT " + KEY_DESCRIPTION
                + " FROM " + TABLE_NAME_FEED_ITEMS
                + " WHERE " + KEY_ID + "=" + item.getId();
        return db.rawQuery(query, null);
    }

    public final Cursor getSimpleChaptersOfFeedItemCursor(final FeedItem item) {
        return db.query(TABLE_NAME_SIMPLECHAPTERS, null, KEY_FEEDITEM
                        + "=?", new String[]{String.valueOf(item.getId())}, null,
                null, null
        );
    }

    public final Cursor getDownloadLog(final int feedFileType, final long feedFileId) {
        final String query = "SELECT * FROM " + TABLE_NAME_DOWNLOAD_LOG +
                " WHERE " + KEY_FEEDFILE + "=" + feedFileId + " AND " + KEY_FEEDFILETYPE + "=" + feedFileType
                + " ORDER BY " + KEY_COMPLETION_DATE + " DESC";
        return db.rawQuery(query, null);
    }

    public final Cursor getDownloadLogCursor(final int limit) {
        return db.query(TABLE_NAME_DOWNLOAD_LOG, null, null, null, null,
                null, KEY_COMPLETION_DATE + " DESC LIMIT " + limit);
    }

    /**
     * Returns a cursor which contains all feed items in the queue. The returned
     * cursor uses the FEEDITEM_SEL_FI_SMALL selection.
     * cursor uses the FEEDITEM_SEL_FI_SMALL selection.
     */
    public final Cursor getQueueCursor() {
        final String query = "SELECT " + KEYS_FEED_ITEM_WITHOUT_DESCRIPTION + ", " + KEYS_FEED_MEDIA
                + " FROM " + TABLE_NAME_QUEUE
                + " INNER JOIN " + TABLE_NAME_FEED_ITEMS
                + " ON " + SELECT_KEY_ITEM_ID + " = " + TABLE_NAME_QUEUE + "." + KEY_FEEDITEM
                +  JOIN_FEED_ITEM_AND_MEDIA
                + " ORDER BY " + TABLE_NAME_QUEUE + "." + KEY_ID;
        return db.rawQuery(query, null);
    }

    public Cursor getQueueIDCursor() {
        return db.query(TABLE_NAME_QUEUE, new String[]{KEY_FEEDITEM}, null, null, null, null, KEY_ID + " ASC", null);
    }

    public Cursor getNextInQueue(final FeedItem item) {
        final String query = "SELECT " + KEYS_FEED_ITEM_WITHOUT_DESCRIPTION + ", " + KEYS_FEED_MEDIA
                + " FROM " + TABLE_NAME_QUEUE
                + " INNER JOIN " + TABLE_NAME_FEED_ITEMS
                + " ON " + SELECT_KEY_ITEM_ID + " = " + TABLE_NAME_QUEUE + "." + KEY_FEEDITEM
                +  JOIN_FEED_ITEM_AND_MEDIA
                + " WHERE Queue.ID > (SELECT Queue.ID FROM Queue WHERE Queue.FeedItem = "
                +  item.getId()
                + ")"
                + " ORDER BY Queue.ID"
                + " LIMIT 1";
        return db.rawQuery(query, null);
    }

    public final Cursor getPausedQueueCursor(int limit) {
        final String hasPositionOrRecentlyPlayed = TABLE_NAME_FEED_MEDIA + "."  + KEY_POSITION + " >= 1000"
                + " OR " + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS
                + " >= " + (System.currentTimeMillis() - 30000);
        final String query = "SELECT " + KEYS_FEED_ITEM_WITHOUT_DESCRIPTION + ", " + KEYS_FEED_MEDIA
                + " FROM " + TABLE_NAME_QUEUE
                + " INNER JOIN " + TABLE_NAME_FEED_ITEMS
                + " ON " + SELECT_KEY_ITEM_ID + " = " + TABLE_NAME_QUEUE + "." + KEY_FEEDITEM
                +  JOIN_FEED_ITEM_AND_MEDIA
                + " ORDER BY (CASE WHEN " + hasPositionOrRecentlyPlayed + " THEN "
                    + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS + " ELSE 0 END) DESC , "
                + TABLE_NAME_QUEUE + "." + KEY_ID
                + " LIMIT " + limit;
        return db.rawQuery(query, null);
    }

    public final Cursor getFavoritesIdsCursor() {
        final String query = "SELECT " + TABLE_NAME_FAVORITES + "." + KEY_FEEDITEM
                + " FROM " + TABLE_NAME_FAVORITES;
        return db.rawQuery(query, null);
    }

    public void setFeedItems(int oldState, int newState) {
        setFeedItems(oldState, newState, 0);
    }

    public void setFeedItems(int oldState, int newState, long feedId) {
        String sql = "UPDATE " + TABLE_NAME_FEED_ITEMS + " SET " + KEY_READ + "=" + newState;
        if (feedId > 0) {
            sql += " WHERE " + KEY_FEED + "=" + feedId;
        }
        if (FeedItem.NEW <= oldState && oldState <= FeedItem.PLAYED) {
            sql += feedId > 0 ? " AND " : " WHERE ";
            sql += KEY_READ + "=" + oldState;
        }
        db.execSQL(sql);
    }

    public final Cursor getEpisodesCursor(int offset, int limit, FeedItemFilter filter, SortOrder sortOrder) {
        String orderByQuery = FeedItemSortQuery.generateFrom(sortOrder);
        String filterQuery = FeedItemFilterQuery.generateFrom(filter);
        String whereClause = "".equals(filterQuery) ? "" : " WHERE " + filterQuery;
        final String query = SELECT_FEED_ITEMS_AND_MEDIA + whereClause
                + "ORDER BY " +  orderByQuery + " LIMIT " + offset + ", " + limit;
        return db.rawQuery(query, null);
    }

    public final Cursor getEpisodeCountCursor(FeedItemFilter filter) {
        String filterQuery = FeedItemFilterQuery.generateFrom(filter);
        String whereClause = "".equals(filterQuery) ? "" : " WHERE " + filterQuery;
        final String query = "SELECT count(" + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + ") FROM " + TABLE_NAME_FEED_ITEMS
                + JOIN_FEED_ITEM_AND_MEDIA + whereClause;
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedEpisodeCountCursor(long feedId, FeedItemFilter filter) {
        String filterQuery = FeedItemFilterQuery.generateFrom(filter);
        String whereAndClause = "".equals(filterQuery) ? "" : " AND " + filterQuery;
        final String query = "SELECT count(" + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + ") FROM " + TABLE_NAME_FEED_ITEMS
                + JOIN_FEED_ITEM_AND_MEDIA
                + " WHERE " + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + "=" + feedId + whereAndClause;
        return db.rawQuery(query, null);
    }

    public Cursor getRandomEpisodesCursor(int limit, int seed) {
        long oneHourAgo = System.currentTimeMillis() - 1000L * 3600L;
        final String allItems = SELECT_FEED_ITEMS_AND_MEDIA
                + " WHERE (" + KEY_READ + " = " + FeedItem.NEW + " OR " + KEY_READ + " = " + FeedItem.UNPLAYED + ") "
                    // Only from the last two years. Older episodes often contain broken covers and stuff like that
                    + " AND " + KEY_PUBDATE + " > " + (System.currentTimeMillis() - 1000L * 3600L * 24L * 356L * 2)
                    // Hide episodes that have been played but not completed
                    + " AND (" + KEY_LAST_PLAYED_TIME_STATISTICS + " == 0"
                        + " OR " + KEY_LAST_PLAYED_TIME_STATISTICS + " > " + oneHourAgo + ")"
                    + " AND " + SELECT_WHERE_FEED_IS_SUBSCRIBED;
        final String query = "SELECT MAX(" + randomEpisodeNumber(seed) + "), * FROM (" + allItems + ")"
                + " GROUP BY " + KEY_FEED
                + " ORDER BY " + randomEpisodeNumber(seed * 3) + " DESC LIMIT " + limit;
        return db.rawQuery(query, null);
    }

    /**
     * SQLite does not support random seeds. Create our own "random" number based on that seed and the item ID
     */
    private String randomEpisodeNumber(int seed) {
        return "((" + SELECT_KEY_ITEM_ID + " * " + seed + ") % 46471)";
    }

    public final Cursor getFeedItemFromMediaIdCursor(long mediaId) {
        final String query = SELECT_FEED_ITEMS_AND_MEDIA
                + " WHERE " + SELECT_KEY_MEDIA_ID + " = " + mediaId;
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedCursor(final long id) {
        final String query = "SELECT " + KEYS_FEED
                + " FROM " + TABLE_NAME_FEEDS
                + " WHERE " + SELECT_KEY_FEED_ID + " = " + id;
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedItemCursor(final String id) {
        return getFeedItemCursor(new String[]{id});
    }

    public final Cursor getFeedItemCursor(final String[] ids) {
        if (ids.length > IN_OPERATOR_MAXIMUM) {
            throw new IllegalArgumentException("number of IDs must not be larger than " + IN_OPERATOR_MAXIMUM);
        }
        final String query = SELECT_FEED_ITEMS_AND_MEDIA
                + " WHERE " + SELECT_KEY_ITEM_ID + " IN (" + TextUtils.join(",", ids) + ")";
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedItemCursorByUrl(List<String> urls) {
        if (urls.size() > IN_OPERATOR_MAXIMUM) {
            throw new IllegalArgumentException("number of IDs must not be larger than " + IN_OPERATOR_MAXIMUM);
        }
        StringBuilder urlsString = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            if (i != 0) {
                urlsString.append(",");
            }
            urlsString.append(DatabaseUtils.sqlEscapeString(urls.get(i)));
        }
        final String query = SELECT_FEED_ITEMS_AND_MEDIA
                + " WHERE " + KEY_DOWNLOAD_URL + " IN (" + urlsString + ")"
                + " ORDER BY " + KEY_LAST_PLAYED_TIME_HISTORY + " DESC";
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedItemCursor(final String guid, final String episodeUrl) {
        String escapedEpisodeUrl = DatabaseUtils.sqlEscapeString(episodeUrl);
        String whereClauseCondition = TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOAD_URL + "=" + escapedEpisodeUrl;

        if (guid != null) {
            String escapedGuid = DatabaseUtils.sqlEscapeString(guid);
            whereClauseCondition = TABLE_NAME_FEED_ITEMS + "." + KEY_ITEM_IDENTIFIER + "=" + escapedGuid;
        }

        final String query = SELECT_FEED_ITEMS_AND_MEDIA
                + " INNER JOIN " + TABLE_NAME_FEEDS
                + " ON " + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + "=" + TABLE_NAME_FEEDS + "." + KEY_ID
                + " WHERE " + whereClauseCondition;
        return db.rawQuery(query, null);
    }

    public final Cursor getMonthlyStatisticsCursor() {
        final String query = "SELECT SUM(" + KEY_PLAYED_DURATION + ") AS total_duration"
                + ", strftime('%m', datetime(" + KEY_LAST_PLAYED_TIME_STATISTICS + "/1000, 'unixepoch')) AS month"
                + ", strftime('%Y', datetime(" + KEY_LAST_PLAYED_TIME_STATISTICS + "/1000, 'unixepoch')) AS year"
                + " FROM " + TABLE_NAME_FEED_MEDIA
                + " WHERE " + KEY_LAST_PLAYED_TIME_STATISTICS + " > 0 AND " + KEY_PLAYED_DURATION + " > 0"
                + " GROUP BY year, month"
                + " ORDER BY year, month";
        return db.rawQuery(query, null);
    }

    public final Cursor getFeedStatisticsCursor(boolean includeMarkedAsPlayed, long timeFilterFrom,
                                                long timeFilterTo, long sixMonthsAgo) {
        final String lastPlayedTimeStatistics = TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS;
        String wasStarted = TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_HISTORY + " > 0"
                + " AND " + TABLE_NAME_FEED_MEDIA + "." + KEY_PLAYED_DURATION + " > 0";
        if (includeMarkedAsPlayed) {
            wasStarted = "(" + wasStarted + ") OR "
                    + TABLE_NAME_FEED_ITEMS + "." + KEY_READ + "=" + FeedItem.PLAYED + " OR "
                    + TABLE_NAME_FEED_MEDIA + "." + KEY_POSITION + "> 0";
        }
        final String timeFilter = lastPlayedTimeStatistics + ">=" + timeFilterFrom
                + " AND " + lastPlayedTimeStatistics + "<" + timeFilterTo;
        String playedTime = TABLE_NAME_FEED_MEDIA + "." + KEY_PLAYED_DURATION;
        if (includeMarkedAsPlayed) {
            playedTime = "(CASE WHEN " + playedTime + " != 0"
                    + " THEN " + playedTime + " ELSE ("
                            + "CASE WHEN " + TABLE_NAME_FEED_ITEMS + "." + KEY_READ + "=" + FeedItem.PLAYED
                                + " THEN " + TABLE_NAME_FEED_MEDIA + "." + KEY_DURATION + " ELSE 0 END"
                    + ") END)";
        }

        final String query = "SELECT " + KEYS_FEED + ", "
                        + "COUNT(*) AS num_episodes, "
                        + "MIN(CASE WHEN " + lastPlayedTimeStatistics + " > 0"
                                + " THEN " + lastPlayedTimeStatistics
                                + " ELSE " + Long.MAX_VALUE + " END) AS oldest_date, "
                        + "SUM(CASE WHEN (" + wasStarted + ") THEN 1 ELSE 0 END) AS episodes_started, "
                        + "IFNULL(SUM(CASE WHEN (" + timeFilter + ")"
                                + " THEN (" + playedTime + ") ELSE 0 END), 0) AS played_time, "
                        + "IFNULL(SUM(" + TABLE_NAME_FEED_MEDIA + "." + KEY_DURATION + "), 0) AS total_time, "
                        + "SUM(CASE WHEN " + TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOAD_DATE + " > 0"
                                + " THEN 1 ELSE 0 END) AS num_downloaded, "
                        + "SUM(CASE WHEN " + TABLE_NAME_FEED_MEDIA + "." + KEY_DOWNLOAD_DATE + " > 0"
                                + " THEN " + TABLE_NAME_FEED_MEDIA + "." + KEY_SIZE + " ELSE 0 END) AS download_size, "
                        + "SUM(CASE WHEN " + TABLE_NAME_FEED_ITEMS + "." + KEY_READ + " != " + FeedItem.PLAYED
                                + " AND " + TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE + " >= " + sixMonthsAgo
                                + " THEN 1 ELSE 0 END) AS num_recent_unplayed "
                + " FROM " + TABLE_NAME_FEED_ITEMS
                + JOIN_FEED_ITEM_AND_MEDIA
                + " INNER JOIN " + TABLE_NAME_FEEDS
                + " ON " + TABLE_NAME_FEED_ITEMS + "." + KEY_FEED + "=" + TABLE_NAME_FEEDS + "." + KEY_ID
                + " WHERE " + TABLE_NAME_FEEDS + "." + KEY_STATE + "=" + Feed.STATE_SUBSCRIBED
                + " GROUP BY " + TABLE_NAME_FEEDS + "." + KEY_ID;
        return db.rawQuery(query, null);
    }

    public final Cursor getTimeBetweenReleaseAndPlayback(long timeFilterFrom, long timeFilterTo) {
        final String from = " FROM " + TABLE_NAME_FEED_ITEMS
                + JOIN_FEED_ITEM_AND_MEDIA
                + " WHERE " + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS + ">=" + timeFilterFrom
                        + " AND " + TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE + ">=" + timeFilterFrom
                        + " AND " + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS + "<" + timeFilterTo;
        final String query = "SELECT " + TABLE_NAME_FEED_MEDIA + "." + KEY_LAST_PLAYED_TIME_STATISTICS
                + " - " + TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE + " AS diff"
                + from
                + " ORDER BY diff ASC"
                + " LIMIT 1"
                + " OFFSET (SELECT count(*)/2 " + from + ")";
        return db.rawQuery(query, null);
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

    public final Map<Long, Integer> getFeedCounters(FeedCounter setting, long... feedIds) {
        String whereRead;
        switch (setting) {
            case SHOW_NEW:
                whereRead = KEY_READ + "=" + FeedItem.NEW;
                break;
            case SHOW_UNPLAYED:
                whereRead = "(" + KEY_READ + "=" + FeedItem.NEW
                        + " OR " + KEY_READ + "=" + FeedItem.UNPLAYED + ")";
                break;
            case SHOW_DOWNLOADED:
                whereRead = KEY_DOWNLOAD_DATE + ">0";
                break;
            case SHOW_DOWNLOADED_UNPLAYED:
                whereRead = "(" + KEY_READ + "=" + FeedItem.NEW
                        + " OR " + KEY_READ + "=" + FeedItem.UNPLAYED + ")"
                        + " AND " + KEY_DOWNLOAD_DATE + ">0";
                break;
            case SHOW_NONE:
                // deliberate fall-through
            default: // NONE
                return new HashMap<>();
        }
        return conditionalFeedCounterRead(whereRead, feedIds);
    }

    private Map<Long, Integer> conditionalFeedCounterRead(String whereRead, long... feedIds) {
        String limitFeeds;
        if (feedIds.length > 0) {
            // work around TextUtils.join wanting only boxed items
            // and StringUtils.join() causing NoSuchMethodErrors on MIUI
            StringBuilder builder = new StringBuilder();
            for (long id : feedIds) {
                builder.append(id);
                builder.append(',');
            }
            // there's an extra ',', get rid of it
            builder.deleteCharAt(builder.length() - 1);
            limitFeeds = KEY_FEED + " IN (" + builder.toString() + ") AND ";
        } else {
            limitFeeds = SELECT_WHERE_FEED_IS_SUBSCRIBED + " AND ";
        }

        final String query = "SELECT " + KEY_FEED + ", COUNT(" + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + ") AS count "
                + " FROM " + TABLE_NAME_FEED_ITEMS
                + " LEFT JOIN " + TABLE_NAME_FEED_MEDIA + " ON "
                + TABLE_NAME_FEED_ITEMS + "." + KEY_ID + "=" + TABLE_NAME_FEED_MEDIA + "." + KEY_FEEDITEM
                + " WHERE " + limitFeeds + " "
                + whereRead + " GROUP BY " + KEY_FEED;

        Cursor c = db.rawQuery(query, null);
        Map<Long, Integer> result = new HashMap<>();
        if (c.moveToFirst()) {
            do {
                long feedId = c.getLong(0);
                int count = c.getInt(1);
                result.put(feedId, count);
            } while (c.moveToNext());
        }
        c.close();
        return result;
    }

    public final Map<Long, Integer> getPlayedEpisodesCounters(long... feedIds) {
        String whereRead = KEY_READ + "=" + FeedItem.PLAYED;
        return conditionalFeedCounterRead(whereRead, feedIds);
    }

    public final Map<Long, Long> getMostRecentItemDates() {
        final String query = "SELECT " + KEY_FEED + ","
                + " MAX(" + TABLE_NAME_FEED_ITEMS + "." + KEY_PUBDATE + ") AS most_recent_pubdate"
                + " FROM " + TABLE_NAME_FEED_ITEMS
                + " GROUP BY " + KEY_FEED;

        Cursor c = db.rawQuery(query, null);
        Map<Long, Long> result = new HashMap<>();
        if (c.moveToFirst()) {
            do {
                long feedId = c.getLong(0);
                long date = c.getLong(1);
                result.put(feedId, date);
            } while (c.moveToNext());
        }
        c.close();
        return result;
    }

    /**
     * Uses DatabaseUtils to escape a search query and removes ' at the
     * beginning and the end of the string returned by the escape method.
     */
    private String[] prepareSearchQuery(String query) {
        String[] queryWords = query.split("\\s+");
        for (int i = 0; i < queryWords.length; ++i) {
            StringBuilder builder = new StringBuilder();
            DatabaseUtils.appendEscapedSQLString(builder, queryWords[i]);
            builder.deleteCharAt(0);
            builder.deleteCharAt(builder.length() - 1);
            queryWords[i] = builder.toString();
        }

        return queryWords;
    }

    /**
     * Searches for the given query in various values of all items or the items
     * of a specified feed.
     *
     * @return A cursor with all search results in SEL_FI_EXTRA selection.
     */
    public Cursor searchItems(long feedID, String searchQuery) {
        String[] queryWords = prepareSearchQuery(searchQuery);

        String queryFeedId;
        if (feedID != 0) {
            // search items in specific feed
            queryFeedId = KEY_FEED + " = " + feedID;
        } else {
            // search through all items
            queryFeedId = "1 = 1";
        }

        String queryStart = SELECT_FEED_ITEMS_AND_MEDIA_WITH_DESCRIPTION
                + " WHERE " + queryFeedId + " AND " + SELECT_WHERE_FEED_IS_SUBSCRIBED + " AND (";
        StringBuilder sb = new StringBuilder(queryStart);

        for (int i = 0; i < queryWords.length; i++) {
            sb
                    .append("(")
                    .append(KEY_DESCRIPTION + " LIKE '%").append(queryWords[i])
                    .append("%' OR ")
                    .append(KEY_TITLE).append(" LIKE '%").append(queryWords[i])
                    .append("%') ");

            if (i != queryWords.length - 1) {
                sb.append("AND ");
            }
        }

        sb.append(") ORDER BY " + KEY_PUBDATE + " DESC LIMIT 300");

        return db.rawQuery(sb.toString(), null);
    }

    /**
     * Searches for the given query in various values of all feeds.
     *
     * @return A cursor with all search results in SEL_FI_EXTRA selection.
     */
    public Cursor searchFeeds(String searchQuery) {
        String[] queryWords = prepareSearchQuery(searchQuery);

        String queryStart = "SELECT " + KEYS_FEED + " FROM " + TABLE_NAME_FEEDS
                + " WHERE " + KEY_STATE + " = " + Feed.STATE_SUBSCRIBED;
        StringBuilder sb = new StringBuilder(queryStart);

        for (int i = 0; i < queryWords.length; i++) {
            sb
                    .append(" AND (")
                    .append(KEY_TITLE).append(" LIKE '%").append(queryWords[i])
                    .append("%' OR ")
                    .append(KEY_CUSTOM_TITLE).append(" LIKE '%").append(queryWords[i])
                    .append("%' OR ")
                    .append(KEY_AUTHOR).append(" LIKE '%").append(queryWords[i])
                    .append("%' OR ")
                    .append(KEY_DESCRIPTION).append(" LIKE '%").append(queryWords[i])
                    .append("%') ");
        }

        sb.append(" ORDER BY " + KEY_TITLE + " ASC LIMIT 300");

        return db.rawQuery(sb.toString(), null);
    }

    /**
     * Insert raw data to the database.
     * Call method only for unit tests.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void insertTestData(@NonNull String table, @NonNull ContentValues values) {
        db.insert(table, null, values);
    }

    /**
     * Called when a database corruption happens.
     */
    public static class PodDbErrorHandler implements DatabaseErrorHandler {
        @Override
        public void onCorruption(SQLiteDatabase db) {
            Log.e(TAG, "Database corrupted: " + db.getPath());

            File dbPath = new File(db.getPath());
            File backupFolder = PodDBAdapter.context.getExternalFilesDir(null);
            File backupFile = new File(backupFolder, "CorruptedDatabaseBackup.db");
            try {
                FileUtils.copyFile(dbPath, backupFile);
                Log.d(TAG, "Dumped database to " + backupFile.getPath());
            } catch (IOException e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }

            new DefaultDatabaseErrorHandler().onCorruption(db); // This deletes the database
        }
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
         */
        public PodDBHelper(final Context context, final String name, final CursorFactory factory) {
            super(context, name, factory, VERSION, new PodDbErrorHandler());
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_FEEDS);
            db.execSQL(CREATE_TABLE_FEED_ITEMS);
            db.execSQL(CREATE_TABLE_FEED_MEDIA);
            db.execSQL(CREATE_TABLE_DOWNLOAD_LOG);
            db.execSQL(CREATE_TABLE_QUEUE);
            db.execSQL(CREATE_TABLE_SIMPLECHAPTERS);
            db.execSQL(CREATE_TABLE_FAVORITES);

            db.execSQL(CREATE_INDEX_FEEDITEMS_FEED);
            db.execSQL(CREATE_INDEX_FEEDITEMS_PUBDATE);
            db.execSQL(CREATE_INDEX_FEEDITEMS_READ);
            db.execSQL(CREATE_INDEX_FEEDMEDIA_FEEDITEM);
            db.execSQL(CREATE_INDEX_QUEUE_FEEDITEM);
            db.execSQL(CREATE_INDEX_SIMPLECHAPTERS_FEEDITEM);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.w("DBAdapter", "Upgrading from version " + oldVersion + " to " + newVersion + ".");
            DBUpgrader.upgrade(db, oldVersion, newVersion);

            db.execSQL("DELETE FROM " + PodDBAdapter.TABLE_NAME_DOWNLOAD_LOG + " WHERE "
                    + PodDBAdapter.KEY_COMPLETION_DATE + "<" + (System.currentTimeMillis() - 7L * 24L * 3600L * 1000L));
        }
    }
}
