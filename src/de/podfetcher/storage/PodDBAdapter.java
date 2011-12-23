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
	public static final String KEY_LENGTH = "length";
	public static final String KEY_POSITION = "position";
	public static final String KEY_SIZE = "filesize";
	public static final String KEY_MIME_TYPE = "mime_type";
	public static final String KEY_IMAGE = "image";
	public static final String KEY_CATEGORY = "category";
	public static final String KEY_FEED = "feed";
	public static final String KEY_MEDIA = "media";

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
			+ " TEXT)";

	private static final String CREATE_TABLE_FEED_ITEMS = "CREATE TABLE "
			+ TABLE_NAME_FEED_ITEMS + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
			+ " TEXT," + KEY_LINK + " TEXT," + KEY_DESCRIPTION
			+ " TEXT," + KEY_PUBDATE + " TEXT," + KEY_MEDIA
			+ " INTEGER," + KEY_FEED + " INTEGER," + KEY_READ
			+ " INTEGER)";

	private static final String CREATE_TABLE_FEED_CATEGORIES = "CREATE TABLE "
			+ TABLE_NAME_FEED_CATEGORIES + " (" + TABLE_PRIMARY_KEY + KEY_NAME
			+ " TEXT)";

	private static final String CREATE_TABLE_FEED_IMAGES = "CREATE TABLE "
			+ TABLE_NAME_FEED_IMAGES + " (" + TABLE_PRIMARY_KEY + KEY_TITLE
			+ " TEXT," + KEY_FILE_URL + " TEXT,"
			+ KEY_DOWNLOAD_URL + " TEXT)";

	private static final String CREATE_TABLE_FEED_MEDIA = "CREATE TABLE "
			+ TABLE_NAME_FEED_MEDIA + " (" + TABLE_PRIMARY_KEY + KEY_LENGTH
			+ " INTEGER," + KEY_POSITION + " INTEGER,"
			+ KEY_SIZE + " INTEGER," + KEY_MIME_TYPE + " TEXT,"
			+ KEY_FILE_URL + " TEXT," + KEY_DOWNLOAD_URL + " TEXT)";

	private SQLiteDatabase db;
	private final Context context;
	private PodDBHelper helper;

	public PodDBAdapter(Context c) {
		this.context = c;
		helper = new PodDBHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public PodDBAdapter open() {
		try {
			db = helper.getWritableDatabase();
		} catch (SQLException ex) {
			db = helper.getReadableDatabase();
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
	        open();
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, feed.title);
		values.put(KEY_LINK, feed.link);
		values.put(KEY_DESCRIPTION, feed.description);
		if (feed.image != null) {
			if (feed.image.id == 0) {
				setImage(feed.image);
			}
			values.put(KEY_IMAGE, feed.image.id);
		}
		if(feed.category != null) {
			if(feed.category.id == 0) {
				setCategory(feed.category);
			}
			values.put(KEY_CATEGORY, feed.category.id);
		}
		if(feed.file_url != null) {
			values.put(KEY_FILE_URL, feed.file_url);
		}
		values.put(KEY_DOWNLOAD_URL, feed.download_url);
		
		if(feed.id == 0) {
			// Create new entry
			feed.id = db.insert(TABLE_NAME_FEEDS, null, values);
		} else {
			db.update(TABLE_NAME_FEEDS, values, KEY_ID+"=?", new String[]{String.valueOf(feed.id)});
		}
		close();
		return feed.id;
	}

	/** Inserts or updates a category entry 
	 * @return the id of the entry
	 * */
	public long setCategory(FeedCategory category) {
		ContentValues values = new ContentValues();
		values.put(KEY_NAME, category.name);
		if(category.id == 0) {
			category.id = db.insert(TABLE_NAME_FEED_CATEGORIES, null, values);
		} else {
			db.update(TABLE_NAME_FEED_CATEGORIES, values, KEY_ID+"=?", new String[]{String.valueOf(category.id)});
			
		}
		return category.id;
	}

	/** 
	 * Inserts or updates an image entry 
	 * @return the id of the entry
	 * */
	public long setImage(FeedImage image) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, image.title);
		values.put(KEY_DOWNLOAD_URL, image.download_url);
		if(image.file_url != null) {
			values.put(KEY_FILE_URL, image.file_url);
		}
		if(image.id == 0) {
			image.id = db.insert(TABLE_NAME_FEED_IMAGES, null, values);
		} else {
			db.update(TABLE_NAME_FEED_IMAGES, values, KEY_ID+"=?", new String[]{String.valueOf(image.id)});
		}
		return image.id;
	}
	
	/**
	 * Inserts or updates an image entry 
	 * @return the id of the entry
	 */
	public long setMedia(FeedMedia media) {
		ContentValues values = new ContentValues();
		values.put(KEY_LENGTH, media.length);
		values.put(KEY_POSITION, media.position);
		values.put(KEY_SIZE, media.size);
		values.put(KEY_MIME_TYPE, media.mime_type);
		values.put(KEY_DOWNLOAD_URL, media.download_url);
		if(media.file_url != null) {
			values.put(KEY_FILE_URL, media.file_url);
		}
		if(media.id == 0) {
			media.id = db.insert(TABLE_NAME_FEED_MEDIA, null, values);
		} else {
			db.update(TABLE_NAME_FEED_MEDIA, values, KEY_ID+"=?", new String[]{String.valueOf(media.id)});
		}
		return media.id;
	}
	
	/**
	 * Inserts or updates a feeditem entry 
	 * @return the id of the entry
	 */
	public long setFeedItem(FeedItem item) {
		ContentValues values = new ContentValues();
		values.put(KEY_TITLE, item.title);
		values.put(KEY_LINK, item.link);
		values.put(KEY_DESCRIPTION, item.description);
		values.put(KEY_PUBDATE, item.pubDate);
		if(item.media != null) {
			if(item.media.id == 0) {
				setMedia(item.media);
			}
			values.put(KEY_MEDIA, item.media.id);
		}
		if(item.feed.id == 0) {
			setFeed(item.feed);
		}
		values.put(KEY_FEED, item.feed.id);
		values.put(KEY_READ, (item.read) ? 1 : 0);
		return item.id;
	}
	
	public Cursor getAllCategoriesCursor() {
		return db.query(TABLE_NAME_FEED_CATEGORIES, null, null, null, null, null, null);
	}
	
	public Cursor getAllFeedsCursor() {
		return db.query(TABLE_NAME_FEEDS, null, null, null, null, null, null);
	}
	
	public Cursor getAllItemsOfFeedCursor(Feed feed) {
		return db.query(TABLE_NAME_FEED_ITEMS, null, KEY_FEED+"=?", new String[]{String.valueOf(feed.id)}, null, null, null);
	}
	
	public Cursor getFeedMediaOfItemCursor(FeedItem item) {
		return db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID+"=?", new String[]{String.valueOf(item.media.id)}, null, null, null);
	}
	
	public Cursor getImageOfFeedCursor(Feed feed) {
		return db.query(TABLE_NAME_FEED_IMAGES, null, KEY_ID+"=?", new String[]{String.valueOf(feed.image.id)}, null, null, null);
	}
	
	public FeedMedia getFeedMedia(long row_index) throws SQLException{
		Cursor cursor = db.query(TABLE_NAME_FEED_MEDIA, null, KEY_ID+"=?", new String[]{String.valueOf(row_index)}, null, null, null);
		
		if((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No FeedMedia found at index: "+ row_index);
		}
		
		return new FeedMedia(row_index, 
				cursor.getLong(cursor.getColumnIndex(KEY_LENGTH)), 
				cursor.getLong(cursor.getColumnIndex(KEY_POSITION)), 
				cursor.getLong(cursor.getColumnIndex(KEY_SIZE)), 
				cursor.getString(cursor.getColumnIndex(KEY_MIME_TYPE)), 
				cursor.getString(cursor.getColumnIndex(KEY_FILE_URL)), 
				cursor.getString(cursor.getColumnIndex(KEY_DOWNLOAD_URL)));
		
	}
	
	public FeedImage getFeedImage(Feed feed) throws SQLException {
		Cursor cursor = this.getImageOfFeedCursor(feed);
		if((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No FeedImage found at index: "+ feed.image.id);
		}
		
		return new FeedImage(feed.image.id, cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
				cursor.getString(cursor.getColumnIndex(KEY_FILE_URL)),
				cursor.getString(cursor.getColumnIndex(KEY_DOWNLOAD_URL)));
	}

	private static class PodDBHelper extends SQLiteOpenHelper {

		public PodDBHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_FEEDS);
			db.execSQL(CREATE_TABLE_FEED_ITEMS);
			db.execSQL(CREATE_TABLE_FEED_CATEGORIES);
			db.execSQL(CREATE_TABLE_FEED_IMAGES);
			db.execSQL(CREATE_TABLE_FEED_MEDIA);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("DBAdapter", "Upgrading from version " + oldVersion + " to "
					+ newVersion + ".");
			// TODO delete Database

		}

	}
}
