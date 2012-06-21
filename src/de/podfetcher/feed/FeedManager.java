package de.podfetcher.feed;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import de.podfetcher.activity.MediaplayerActivity;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.service.PlaybackService;
import de.podfetcher.storage.*;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

/**
 * Singleton class Manages all feeds, categories and feeditems
 * 
 * 
 * */
public class FeedManager {
	private static final String TAG = "FeedManager";

	/** Number of completed Download status entries to store. */
	private static final int DOWNLOAD_LOG_SIZE = 25;

	private static FeedManager singleton;

	private ArrayList<Feed> feeds;
	private ArrayList<FeedCategory> categories;

	/** Contains all items where 'read' is false */
	private ArrayList<FeedItem> unreadItems;

	/** Contains completed Download status entries */
	private ArrayList<DownloadStatus> downloadLog;
	
	/** Contains the queue of items to be played. */
	private ArrayList<FeedItem> queue;

	private DownloadRequester requester;

	private FeedManager() {
		feeds = new ArrayList<Feed>();
		categories = new ArrayList<FeedCategory>();
		unreadItems = new ArrayList<FeedItem>();
		requester = DownloadRequester.getInstance();
		downloadLog = new ArrayList<DownloadStatus>();
		queue = new ArrayList<FeedItem>();
	}

	public static FeedManager getInstance() {
		if (singleton == null) {
			singleton = new FeedManager();
		}
		return singleton;
	}

	/**
	 * Play FeedMedia and start the playback service + launch Mediaplayer
	 * Activity.
	 */
	public void playMedia(Context context, FeedMedia media) {
		// Start playback Service
		Intent launchIntent = new Intent(context, PlaybackService.class);
		launchIntent.putExtra(PlaybackService.EXTRA_MEDIA_ID, media.getId());
		launchIntent.putExtra(PlaybackService.EXTRA_FEED_ID, media.getItem()
				.getFeed().getId());
		context.startService(launchIntent);

		// Launch Mediaplayer
		Intent playerIntent = new Intent(context, MediaplayerActivity.class);
		context.startActivity(playerIntent);
	}

	/** Remove media item that has been downloaded. */
	public boolean deleteFeedMedia(Context context, FeedMedia media) {
		boolean result = false;
		if (media.isDownloaded()) {
			File mediaFile = new File(media.file_url);
			if (mediaFile.exists()) {
				result = mediaFile.delete();
			}
			media.setDownloaded(false);
			media.setFile_url(null);
			setFeedMedia(context, media);
		}
		Log.d(TAG, "Deleting File. Result: " + result);
		return result;
	}
	
	/** Remove a feed with all its items and media files and its image. */
	public boolean deleteFeed(Context context, Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		
		// delete image file
		if (feed.getImage() != null) {
			if (feed.getImage().isDownloaded() && feed.getImage().getFile_url() == null) {
				File imageFile = new File(feed.getImage().getFile_url());
				imageFile.delete();
			}
		}
		// delete stored media files and mark them as read
		for (FeedItem item : feed.getItems()) {
			if (!item.isRead()) {
				unreadItems.remove(item);
			}
			if (item.getMedia() != null && item.getMedia().isDownloaded()) {
				File mediaFile = new File(item.getMedia().getFile_url());
				mediaFile.delete();
			}
		}
		adapter.removeFeed(feed);
		return feeds.remove(feed);
		
	}

	/**
	 * Sets the 'read'-attribute of a FeedItem. Should be used by all Classes
	 * instead of the setters of FeedItem.
	 */
	public void markItemRead(Context context, FeedItem item, boolean read) {
		item.read = read;
		setFeedItem(context, item);
		if (read == true) {
			unreadItems.remove(item);
		} else {
			unreadItems.add(item);
		}
	}

	public void refreshAllFeeds(Context context) {
		Log.d(TAG, "Refreshing all feeds.");
		for (Feed feed : feeds) {
			requester.downloadFeed(context, new Feed(feed.getDownload_url(),
					new Date()));
		}
	}

	public long addDownloadStatus(Context context, DownloadStatus status) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		downloadLog.add(status);
		if (downloadLog.size() > DOWNLOAD_LOG_SIZE) {
			adapter.removeDownloadStatus(downloadLog.remove(0));
		}
		return adapter.setDownloadStatus(status);
	}
	
	public void addQueueItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		queue.add(item);
		adapter.setQueue(queue);
	}
	
	public void removeQueueItem(Context context, FeedItem item) {
		boolean removed = queue.remove(item);
		if (removed) {
			PodDBAdapter adapter = new PodDBAdapter(context);
			adapter.setQueue(queue);
		}
	}
	
	public boolean isInQueue(FeedItem item) {
		return queue.contains(item);
	}

	private void addNewFeed(Context context, Feed feed) {
		feeds.add(feed);
		feed.setId(setFeed(context, feed));
		for (FeedItem item : feed.getItems()) {
			setFeedItem(context, item);
		}
	}

	/*
	 * TODO Decide if still useful
	 * 
	 * public void addFeedItem(Context context, FeedItem item) { PodDBAdapter
	 * adapter = new PodDBAdapter(context); // Search list for feeditem Feed
	 * feed = item.getFeed(); FeedItem foundItem = searchFeedItemByLink(feed,
	 * item.getLink()); if (foundItem != null) { // Update Information item.id =
	 * foundItem.id; foundItem = item; item.setRead(foundItem.isRead());
	 * adapter.setFeedItem(item); } else { feed.getItems().add(item); item.id =
	 * adapter.setFeedItem(item); } }
	 */
	public void updateFeed(Context context, Feed newFeed) {
		// Look up feed in the feedslist
		Feed savedFeed = searchFeedByLink(newFeed.getLink());
		if (savedFeed == null) {
			Log.d(TAG,
					"Found no existing Feed with title " + newFeed.getTitle()
							+ ". Adding as new one.");
			// Add a new Feed
			markItemRead(context, newFeed.getItems().get(0), false);
			addNewFeed(context, newFeed);
		} else {
			Log.d(TAG, "Feed with title " + newFeed.getTitle()
					+ " already exists. Syncing new with existing one.");
			// Look for new or updated Items
			for (FeedItem item : newFeed.getItems()) {
				FeedItem oldItem = searchFeedItemByLink(savedFeed,
						item.getLink());
				if (oldItem == null) {
					// item is new
					savedFeed.getItems().add(item);
					markItemRead(context, item, false);
				}
			}
			savedFeed.setLastUpdate(newFeed.getLastUpdate());
			setFeed(context, savedFeed);
		}

	}

	/** Get a Feed by its link */
	private Feed searchFeedByLink(String link) {
		for (Feed feed : feeds) {
			if (feed.getLink().equals(link)) {
				return feed;
			}
		}
		return null;
	}

	/** Get a FeedItem by its link */
	private FeedItem searchFeedItemByLink(Feed feed, String link) {
		for (FeedItem item : feed.getItems()) {
			if (item.getLink().equals(link)) {
				return item;
			}
		}
		return null;
	}

	/** Updates Information of an existing Feed */
	public long setFeed(Context context, Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setFeed(feed);
	}

	public long setFeedItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setFeedItem(item);
	}

	/** Updates information of an existing FeedImage */
	public long setFeedImage(Context context, FeedImage image) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setImage(image);
	}

	/** Updates information of an existing FeedMedia object. */
	public long setFeedMedia(Context context, FeedMedia media) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		return adapter.setMedia(media);
	}

	/** Get a Feed by its id */
	public Feed getFeed(long id) {
		for (Feed f : feeds) {
			if (f.id == id) {
				return f;
			}
		}
		Log.e(TAG, "Couldn't find Feed with id " + id);
		return null;
	}

	/** Get a Feed Image by its id */
	public FeedImage getFeedImage(long id) {
		for (Feed f : feeds) {
			FeedImage image = f.getImage();
			if (image != null && image.getId() == id) {
				return image;
			}
		}
		return null;
	}

	/** Get a Feed Item by its id and its feed */
	public FeedItem getFeedItem(long id, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getId() == id) {
				return item;
			}
		}
		Log.e(TAG, "Couldn't find FeedItem with id " + id);
		return null;
	}

	/** Get a FeedMedia object by the id of the Media object and the feed object */
	public FeedMedia getFeedMedia(long id, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getMedia().getId() == id) {
				return item.getMedia();
			}
		}
		Log.e(TAG, "Couldn't find FeedMedia with id " + id);
		return null;
	}

	/** Get a FeedMedia object by the id of the Media object. */
	public FeedMedia getFeedMedia(long id) {
		for (Feed feed : feeds) {
			for (FeedItem item : feed.getItems()) {
				if (item.getMedia().getId() == id) {
					return item.getMedia();
				}
			}
		}
		Log.w(TAG, "Couldn't find FeedMedia with id " + id);
		return null;
	}

	public DownloadStatus getDownloadStatus(long statusId) {
		for (DownloadStatus status : downloadLog) {
			if (status.getId() == statusId) {
				return status;
			}
		}
		return null;
	}

	/** Reads the database */
	public void loadDBData(Context context) {
		updateArrays(context);
	}

	public void updateArrays(Context context) {
		feeds.clear();
		categories.clear();
		extractFeedlistFromCursor(context);
		extractDownloadLogFromCursor(context);
		extractQueueFromCursor(context);
	}

	private void extractFeedlistFromCursor(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor feedlistCursor = adapter.getAllFeedsCursor();
		if (feedlistCursor.moveToFirst()) {
			do {
				Date lastUpdate = new Date(
						feedlistCursor.getLong(feedlistCursor
								.getColumnIndex(PodDBAdapter.KEY_LASTUPDATE)));
				Feed feed = new Feed(lastUpdate);

				feed.id = feedlistCursor.getLong(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_ID));
				feed.setTitle(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				feed.setLink(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_LINK)));
				feed.setDescription(feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				feed.setImage(adapter.getFeedImage(feedlistCursor
						.getLong(feedlistCursor
								.getColumnIndex(PodDBAdapter.KEY_IMAGE))));
				feed.file_url = feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_FILE_URL));
				feed.download_url = feedlistCursor.getString(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL));
				feed.setDownloaded(feedlistCursor.getInt(feedlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED)) > 0);
				// Get FeedItem-Object
				Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
				feed.setItems(extractFeedItemsFromCursor(context, feed,
						itemlistCursor));

				feeds.add(feed);
			} while (feedlistCursor.moveToNext());
		}
		adapter.close();
	}

	private ArrayList<FeedItem> extractFeedItemsFromCursor(Context context,
			Feed feed, Cursor itemlistCursor) {
		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		if (itemlistCursor.moveToFirst()) {
			do {
				FeedItem item = new FeedItem();

				item.id = itemlistCursor.getLong(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_ID));
				item.setFeed(feed);
				item.setTitle(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_TITLE)));
				item.setLink(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_LINK)));
				item.setDescription(itemlistCursor.getString(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION)));
				item.setPubDate(new Date(itemlistCursor.getLong(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_PUBDATE))));
				item.setMedia(adapter.getFeedMedia(itemlistCursor
						.getLong(itemlistCursor
								.getColumnIndex(PodDBAdapter.KEY_MEDIA)), item));
				item.read = (itemlistCursor.getInt(itemlistCursor
						.getColumnIndex(PodDBAdapter.KEY_READ)) > 0) ? true
						: false;
				if (!item.read) {
					unreadItems.add(item);
				}
				items.add(item);
			} while (itemlistCursor.moveToNext());
		}
		adapter.close();
		return items;
	}

	private void extractDownloadLogFromCursor(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor logCursor = adapter.getDownloadLogCursor();
		if (logCursor.moveToFirst()) {
			do {
				long id = logCursor.getLong(logCursor
						.getColumnIndex(PodDBAdapter.KEY_ID));
				long feedfileId = logCursor.getLong(logCursor
						.getColumnIndex(PodDBAdapter.KEY_FEEDFILE));
				int feedfileType = logCursor.getInt(logCursor
						.getColumnIndex(PodDBAdapter.KEY_FEEDFILETYPE));
				FeedFile feedfile = null;
				switch (feedfileType) {
				case PodDBAdapter.FEEDFILETYPE_FEED:
					feedfile = getFeed(feedfileId);
					break;
				case PodDBAdapter.FEEDFILETYPE_FEEDIMAGE:
					feedfile = getFeedImage(feedfileId);
					break;
				case PodDBAdapter.FEEDFILETYPE_FEEDMEDIA:
					feedfile = getFeedMedia(feedfileId);
				}
				if (feedfile != null) { // otherwise ignore status
					boolean successful = logCursor.getInt(logCursor
							.getColumnIndex(PodDBAdapter.KEY_SUCCESSFUL)) > 0;
					int reason = logCursor.getInt(logCursor
							.getColumnIndex(PodDBAdapter.KEY_REASON));
					Date completionDate = new Date(logCursor.getLong(logCursor
							.getColumnIndex(PodDBAdapter.KEY_COMPLETION_DATE)));
					downloadLog.add(new DownloadStatus(id, feedfile,
							successful, reason, completionDate));
				}

			} while (logCursor.moveToNext());
		}
		adapter.close();
	}
	
	private void extractQueueFromCursor(Context context) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor cursor = adapter.getQueueCursor();
		if (cursor.moveToFirst()) {
			do {
				int index = cursor.getInt(cursor.getColumnIndex(PodDBAdapter.KEY_ID));
				Feed feed = getFeed(cursor.getLong(cursor.getColumnIndex(PodDBAdapter.KEY_FEED)));
				FeedItem item = getFeedItem(cursor.getColumnIndex(PodDBAdapter.KEY_FEEDITEM), feed);
				queue.add(index, item);
			} while (cursor.moveToNext());
		}
		adapter.close();
	}

	public ArrayList<Feed> getFeeds() {
		return feeds;
	}

	public ArrayList<FeedItem> getUnreadItems() {
		return unreadItems;
	}

	public ArrayList<DownloadStatus> getDownloadLog() {
		return downloadLog;
	}

}
