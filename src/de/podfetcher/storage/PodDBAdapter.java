package de.podfetcher.storage;

import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedCategory;
import de.podfetcher.feed.FeedImage;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedMedia;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Implements methods for accessing the database
 * */
public class PodDBAdapter {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "Podfetcher.db";

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
	public static final String KEY_CATEGORY = "category";
	public static final String KEY_FEED = "feed";
	public static final String KEY_MEDIA = "media";
	public static final String KEY_DOWNLOADED = "downloaded";

	// Table names
	public static final String TABLE_NAME_FEEDS = "Feeds";
	public static final String TABLE_NAME_FEED_ITEMS = "FeedItems";
	public static final String TABLE_NAME_FEED_CATEGORIES = "FeedCategories";
	public static final String TABLE_NAME_FEED_IMAGES = "FeedImages";
	public static final String TABLE_NAME_FEED_MEDIA = "FeedMedia";

	// SQL Statements for creating new tables
	private static final String TABLE_PRIMARY_KEY = KEY_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT ,";
	private static final String CREATE_TABLE_FEEDS = "CREATE TABLE "
			+ TABLE_NAME_FEEDS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
			+ " TEXT," + KEY_LINK + " TEXT," + KEY_DESCRIPTION
			+ " TEXT," + KEY_IMAGE + " INTEGER," + KEY_CATEGORY
			+ " INTEGER," + KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL
			+ " TEXT," + KEY_DOWNLOADED + " INTEGER)";

	private static final String CREATE_TABLE_FEED_ITEMS = "CREATE TABLE "
			+ TABLE_NAME_FEED_ITEMS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
			+ " TEXT," + KEY_LINK + " TEXT," + KEY_DESCRIPTION
			+ " TEXT," + KEY_PUBDATE + " INTEGER," + KEY_MEDIA
			+ " INTEGER," + KEY_FEED + " INTEGER," + KEY_READ
			+ " INTEGER)";

	private static final String CREATE_TABLE_FEED_CATEGORIES = "CREATE TABLE "
			+ TABLE_NAME_FEED_CATEGORIES + " (" + TABLE_PRIMARY_KEY + KEY_NAME
			+ " TEXT)";

	private static final String CREATE_TABLE_FEED_IMAGES = "CREATE TABLE "
			+ TABLE_NAME_FEED_IMAGES + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
			+ " TEXT," + KEY_FILE_URL + " TEXT,"
			+ KEY_DOWNLOAD_URL + " TEXT," + KEY_DOWNLOADED + " INTEGER)";

	private static final String CREATE_TABLE_FEED_MEDIA = "CREATE TABLE "
			+ TABLE_NAME_FEED_MEDIA + " (" + TABLE_PRIMARY_KEY + KEY_DURATION
			+ " INTEGER," + KEY_POSITION + " INTEGER,"
			+ KEY_SIZE + " INTEGER," + KEY_MIME_TYPE + " TEXT,"
			+ KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL + " TEXT," + KEY_DOWNLOADED + " INTEGER)";

	private SQLiteDatabase db;
	private final Context context;
	private PodDBHelper helper;

	public PodDBAdapter(Context c) {
		this.context = c;
		helper = new PodDBHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public PodDBAdapter open() {
		if(db == null || !db.isOpen() || db.isReadOnly()) {
		    try {
			    db = helper.getWritableDatabase();
		    } catch (SQLException ex) {
			    db = helper.getReadableDatabase();
		    }
		}
		return this;
	}

	public void close() {
		db.close();
	}
	
	/** Inserts or updates a feed entry 
	 * @return the id of the entry
	 * */
	public long setFeed(Feed feed) {		
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, feed.getTitle());
		values.put(KEY_LINK, feed.getLink());
		values.put(KEY_DESCRIPTION, feed.getDescription());
		if (feed.getImage() != null) {
			if (feed.getImage().getId() == 0) {
				setImage(feed.getImage());
			}
			values.put(KEY_IMAGE, feed.getImage().getId());
		}
		if(feed.getCategory() != null) {
			if(feed.getCategory().getId() == 0) {
				setCategory(feed.getCategory());
			}
			values.put(KEY_CATEGORY, feed.getCategory().getId());
		}
		if(feed.getFile_url() != null) {
			values.put(KEY_FILE_URL, feed.getFile_url());
		}
		values.put(KEY_DOWNLOAD_URL, feed.getDownload_url());
		values.put(KEY_DOWNLOADED, feed.isDownloaded());
		open();
		if(feed.getId() == 0) {
			// Create new entry
			Log.d(this.toString(), "Inserting new Feed into db");
			feed.setId(db.insert(TABLE_NAME_FEEDS, null, values));
		} else {
			Log.d(this.toString(), "Updating existing Feed in db");
			db.update(TABLE_NAME_FEEDS, values, KEY_ID+"=?", new String[]{Long.toString(feed.getId())});
		}
		close();
		return feed.getId();
	}

	/** Inserts or updates a category entry 
	 * @return the id of the entry
	 * */
	public long setCategory(FeedCategory category) {
	    open();
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, category.getName());
		if(category.getId() == 0) {
			category.setId(db.insert(TABLE_NAME_FEED_CATEGORIES, null, values));
		} else {
			db.update(TABLE_NAME_FEED_CATEGORIES, values, KEY_ID+"=?", new String[]{String.valueOf(category.getId())});
			
		}
		close();
		return category.getId();
	}

	/** 
	 * Inserts or updates an image entry 
	 * @return the id of the entry
	 * */
	public long setImage(FeedImage image) {
	    open();
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, image.getTitle());
		values.put(KEY_DOWNLOAD_URL, image.getDownload_url());
		values.put(KEY_DOWNLOADED, image.isDownloaded());
		if(image.getFile_url() != null) {
			values.put(KEY_FILE_URL, image.getFile_url());
		}
		if(image.getId() == 0) {
			image.setId(db.insert(TABLE_NAME_FEED_IMAGES, null, values));
		} else {
			db.update(TABLE_NAME_FEED_IMAGES, values, KEY_ID + "=?", new String[]{String.valueOf(image.getId())});
		}
		close();
		return image.getId();
	}
	
	/**
	 * Inserts or updates an image entry 
	 * @return the id of the entry
	 */
	public long setMedia(FeedMedia media) {
	    open();
		ContentValues values = new ContentValues();
		values.put(KEY_DURATION, media.getDuration());
		values.put(KEY_POSITION, media.getPosition());
		values.put(KEY_SIZE, media.getSize());
		values.put(KEY_MIME_TYPE, media.getMime_type());
		values.put(KEY_DOWNLOAD_URL, media.getDownload_url());
		values.put(KEY_DOWNLOADED, media.isDownloaded());
		if(media.getFile_url() != null) {
			values.put(KEY_FILE_URL, media.getFile_url());
		}
		if(media.getId() == 0) {
			media.setId(db.insert(TABLE_NAME_FEED_MEDIA, null, values));
		} else {
			db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID + "=?", new String[]{String.valueOf(media.getId())});
		}
		close();
		return media.getId();
	}
	
	/**
	 * Inserts or updates a feeditem entry 
	 * @return the id of the entry
	 */
	public long setFeedItem(FeedItem item) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, item.getTitle());
		values.put(KEY_LINK, item.getLink());
		values.put(KEY_DESCRIPTION, item.getDescription());
		values.put(KEY_PUBDATE, item.getPubDate().getTime());
		if (item.getMedia() != null) {
			if(item.getMedia().getId() == 0) {
				setMedia(item.getMedia());
			}
			values.put(KEY_MEDIA, item.getMedia().getId());
		}
		if (item.getFeed().getId() == 0) {
			setFeed(item.getFeed());
		}
		values.put(KEY_FEED, item.getFeed().getId());
		values.put(KEY_READ, (item.isRead()) ? 1 : 0);

		open();
		if (item.getId() == 0) {
		    item.setId(db.insert(TABLE_NAME_FEED_ITEMS, null, values));
		} else {
		    db.update(TABLE_NAME_FEED_ITEMS, values, KEY_ID + "=?",
                    new String[]{String.valueOf(item.getId())});
		}
		close();
		return item.getId();
	}

    /** Get all Categories from the Categories Table.
     *  @return The cursor of the query
     * */
	public final Cursor getAllCategoriesCursor() {
	    open();
	    Cursor c = db.query(TABLE_NAME_FEED_CATEGORIES, null, null, null, null, null, null);
		return c;
	}

    /** Get all Feeds from the Feed Table.
     *  @return The cursor of the query
     * */
	public final Cursor getAllFeedsCursor() {
	    open();
	    Cursor c = db.query(TABLE_NAME_FEEDS, null, null, null, null, null, null);
		return c;
	}

    /** Returns a cursor with all FeedItems of a Feed.
     *  @param feed The feed you want to get the FeedItems from.
     *  @return The cursor of the query
     * */
	public final Cursor getAllItemsOfFeedCursor(final Feed feed) {
	    open();
	    Cursor c = db.query(TABLE_NAME_FEED_ITEMS, null, KEY_FEED + "=?",
				new String[]{String.valueOf(feed.getId())}, null, null, null);
		return c;
	}

    /** Returns a cursor for a DB query in the FeedMedia table for a given ID.
     *  @param item The item you want to get the FeedMedia from
     *  @return The cursor of the query
     * */
	public final Cursor getFeedMediaOfItemCursor(final FeedItem item) {
	    open();
	    Cursor c = db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID + "=?",
				new String[]{String.valueOf(item.getMedia().getId())},
                null, null, null);
		return c;
	}

    /** Returns a cursor for a DB query in the FeedImages table for a given ID.
     *  @param id ID of the FeedImage
     *  @return The cursor of the query
     * */
	public final Cursor getImageOfFeedCursor(final long id) {
	    open();
	    Cursor c = db.query(TABLE_NAME_FEED_IMAGES, null, KEY_ID + "=?",
				new String[]{String.valueOf(id)}, null, null, null);
		return c;
	}

    /** Get a FeedMedia object from the Database.
     *  @param rowIndex DB Index of Media object
     *  @param owner FeedItem the Media object belongs to
     *  @return A newly created FeedMedia object
     * */
	public final FeedMedia getFeedMedia(final long rowIndex, final FeedItem owner)
        throws SQLException {
	    open();
		Cursor cursor = db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID + "=?",
                new String[]{String.valueOf(rowIndex)}, null, null, null);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No FeedMedia found at index: " + rowIndex);
		}
		FeedMedia media = new FeedMedia(rowIndex,
				owner,
				cursor.getInt(cursor.getColumnIndex(KEY_DURATION)),
				cursor.getInt(cursor.getColumnIndex(KEY_POSITION)),
				cursor.getLong(cursor.getColumnIndex(KEY_SIZE)),
				cursor.getString(cursor.getColumnIndex(KEY_MIME_TYPE)),
				cursor.getString(cursor.getColumnIndex(KEY_FILE_URL)),
				cursor.getString(cursor.getColumnIndex(KEY_DOWNLOAD_URL)),
				cursor.getInt(cursor.getColumnIndex(KEY_DOWNLOADED)) > 0);
		close();
		return media;
	}

	/** Searches the DB for a FeedImage of the given id.
	 *	@param id The id of the object
	 *	@return The found object
	 * */
	public final FeedImage getFeedImage(final long id) throws SQLException {
	    open();
		Cursor cursor = this.getImageOfFeedCursor(id);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No FeedImage found at index: " + id);
		}
		FeedImage image = new FeedImage(id,
				cursor.getString(
				cursor.getColumnIndex(KEY_TITLE)),
				cursor.getString(
				cursor.getColumnIndex(KEY_FILE_URL)),
				cursor.getString(
				cursor.getColumnIndex(KEY_DOWNLOAD_URL)),
				cursor.getInt(cursor.getColumnIndex(KEY_DOWNLOADED)) > 0);
		close();
		return image;
	}

	/** Helper class for opening the Podfetcher database. */
	private static class PodDBHelper extends SQLiteOpenHelper {

		/** Constructor.
		 * 	@param context Context to use
		 *	@param name Name of the database
		 *	@param factory to use for creating cursor objects
		 *	@param version number of the database
		 * */
		public PodDBHelper(final Context context,
				final String name, final CursorFactory factory,
				final int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_FEEDS);
			db.execSQL(CREATE_TABLE_FEED_ITEMS);
			db.execSQL(CREATE_TABLE_FEED_CATEGORIES);
			db.execSQL(CREATE_TABLE_FEED_IMAGES);
			db.execSQL(CREATE_TABLE_FEED_MEDIA);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w("DBAdapter", "Upgrading from version "
					+ oldVersion + " to "
					+ newVersion + ".");
			// TODO delete Database
		}
	}
}
