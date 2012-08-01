package de.danoeh.antennapod.feed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.activity.MediaplayerActivity;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.storage.*;
import de.danoeh.antennapod.util.FeedtitleComparator;
import de.danoeh.antennapod.util.comparator.FeedItemPubdateComparator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Singleton class Manages all feeds, categories and feeditems
 * 
 * 
 * */
public class FeedManager {
	private static final String TAG = "FeedManager";

	public static final String ACITON_FEED_LIST_UPDATE = "de.danoeh.antennapod.action.feed.feedlistUpdate";
	public static final String ACTION_UNREAD_ITEMS_UPDATE = "de.danoeh.antennapod.action.feed.unreadItemsUpdate";
	public static final String ACTION_QUEUE_UPDATE = "de.danoeh.antennapod.action.feed.queueUpdate";
	public static final String EXTRA_FEED_ITEM_ID = "de.danoeh.antennapod.extra.feed.feedItemId";
	public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feed.feedId";

	/** Number of completed Download status entries to store. */
	private static final int DOWNLOAD_LOG_SIZE = 25;

	private static FeedManager singleton;

	private List<Feed> feeds;
	private ArrayList<FeedCategory> categories;

	/** Contains all items where 'read' is false */
	private List<FeedItem> unreadItems;

	/** Contains completed Download status entries */
	private ArrayList<DownloadStatus> downloadLog;

	/** Contains the queue of items to be played. */
	private List<FeedItem> queue;

	private DownloadRequester requester;

	/** Should be used to change the content of the arrays from another thread. */
	private Handler contentChanger;

	/** Prevents user from starting several feed updates at the same time. */
	private static boolean isStartingFeedRefresh = false;

	private FeedManager() {
		feeds = Collections.synchronizedList(new ArrayList<Feed>());
		categories = new ArrayList<FeedCategory>();
		unreadItems = Collections.synchronizedList(new ArrayList<FeedItem>());
		requester = DownloadRequester.getInstance();
		downloadLog = new ArrayList<DownloadStatus>();
		queue = Collections.synchronizedList(new ArrayList<FeedItem>());
		contentChanger = new Handler();
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
	 * 
	 * @param context
	 *            for starting the playbackservice
	 * @param media
	 *            that shall be played
	 * @param showPlayer
	 *            if Mediaplayer activity shall be started
	 * @param startWhenPrepared
	 *            if Mediaplayer shall be started after it has been prepared
	 */
	public void playMedia(Context context, FeedMedia media, boolean showPlayer,
			boolean startWhenPrepared, boolean shouldStream) {
		// Start playback Service
		Intent launchIntent = new Intent(context, PlaybackService.class);
		launchIntent.putExtra(PlaybackService.EXTRA_MEDIA_ID, media.getId());
		launchIntent.putExtra(PlaybackService.EXTRA_FEED_ID, media.getItem()
				.getFeed().getId());
		launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
				startWhenPrepared);
		launchIntent
				.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, shouldStream);
		context.startService(launchIntent);
		if (showPlayer) {
			// Launch Mediaplayer
			Intent playerIntent = new Intent(context, MediaplayerActivity.class);
			context.startActivity(playerIntent);
		}
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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Deleting File. Result: " + result);
		return result;
	}

	/** Remove a feed with all its items and media files and its image. */
	public void deleteFeed(final Context context, final Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		// delete image file
		if (feed.getImage() != null) {
			if (feed.getImage().isDownloaded()
					&& feed.getImage().getFile_url() == null) {
				File imageFile = new File(feed.getImage().getFile_url());
				imageFile.delete();
			}
		}
		// delete stored media files and mark them as read
		for (FeedItem item : feed.getItems()) {
			if (!item.isRead()) {
				unreadItems.remove(item);
			}
			if (queue.contains(item)) {
				removeQueueItem(item, adapter);
			}
			if (item.getMedia() != null && item.getMedia().isDownloaded()) {
				File mediaFile = new File(item.getMedia().getFile_url());
				mediaFile.delete();
			}
		}

		adapter.removeFeed(feed);
		adapter.close();
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				feeds.remove(feed);
				sendFeedUpdateBroadcast(context);
			}});

	}

	private void sendUnreadItemsUpdateBroadcast(Context context, FeedItem item) {
		Intent update = new Intent(ACTION_UNREAD_ITEMS_UPDATE);
		if (item != null) {
			update.putExtra(EXTRA_FEED_ID, item.getFeed().getId());
			update.putExtra(EXTRA_FEED_ITEM_ID, item.getId());
		}
		context.sendBroadcast(update);
	}

	private void sendQueueUpdateBroadcast(Context context, FeedItem item) {
		Intent update = new Intent(ACTION_QUEUE_UPDATE);
		if (item != null) {
			update.putExtra(EXTRA_FEED_ID, item.getFeed().getId());
			update.putExtra(EXTRA_FEED_ITEM_ID, item.getId());
		}
		context.sendBroadcast(update);
	}
	
	private void sendFeedUpdateBroadcast(Context context) {
		context.sendBroadcast(new Intent(ACITON_FEED_LIST_UPDATE));
	}

	/**
	 * Sets the 'read'-attribute of a FeedItem. Should be used by all Classes
	 * instead of the setters of FeedItem.
	 */
	public void markItemRead(final Context context, final FeedItem item, final boolean read) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting item with title " + item.getTitle()
					+ " as read/unread");
		item.read = read;
		setFeedItem(context, item);
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				if (read == true) {
					unreadItems.remove(item);
				} else {
					unreadItems.add(item);
					Collections.sort(unreadItems, new FeedItemPubdateComparator());
				}
				sendUnreadItemsUpdateBroadcast(context, item);
			}});
		
	}

	/**
	 * Sets the 'read' attribute of all FeedItems of a specific feed to true
	 * 
	 * @param context
	 */
	public void markFeedRead(Context context, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (unreadItems.contains(item)) {
				markItemRead(context, item, true);
			}
		}
	}

	/** Marks all items in the unread items list as read */
	public void markAllItemsRead(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "marking all items as read");
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		for (FeedItem item : unreadItems) {
			item.read = true;
			setFeedItem(item, adapter);
		}
		adapter.close();
		unreadItems.clear();
		sendUnreadItemsUpdateBroadcast(context, null);
	}

	@SuppressLint("NewApi")
	public void refreshAllFeeds(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Refreshing all feeds.");
		if (!isStartingFeedRefresh) {
			isStartingFeedRefresh = true;
			AsyncTask<Void, Void, Void> updateWorker = new AsyncTask<Void, Void, Void>() {

				@Override
				protected void onPostExecute(Void result) {
					if (AppConfig.DEBUG)
						Log.d(TAG,
								"All feeds have been sent to the downloadmanager");
					isStartingFeedRefresh = false;
				}

				@Override
				protected Void doInBackground(Void... params) {
					for (Feed feed : feeds) {
						refreshFeed(context, feed);
					}
					return null;
				}

			};
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				updateWorker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				updateWorker.execute();
			}
		}

	}

	/**
	 * Notifies the feed manager that the an image file is invalid. It will try
	 * to redownload it
	 */
	public void notifyInvalidImageFile(Context context, FeedImage image) {
		Log.i(TAG,
				"The feedmanager was notified about an invalid image download. It will now try to redownload the image file");
		image.setDownloaded(false);
		image.setFile_url(null);
		requester.downloadImage(context, image);
	}

	public void refreshFeed(Context context, Feed feed) {
		requester.downloadFeed(context, new Feed(feed.getDownload_url(),
				new Date()));
	}

	public long addDownloadStatus(Context context, DownloadStatus status) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		downloadLog.add(status);
		adapter.open();
		if (downloadLog.size() > DOWNLOAD_LOG_SIZE) {
			adapter.removeDownloadStatus(downloadLog.remove(0));
		}
		long result = adapter.setDownloadStatus(status);
		adapter.close();
		return result;
	}

	public void addQueueItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		queue.add(item);
		adapter.open();
		adapter.setQueue(queue);
		adapter.close();
		sendQueueUpdateBroadcast(context, item);
	}

	/** Removes all items in queue */
	public void clearQueue(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Clearing queue");
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		queue.clear();
		adapter.setQueue(queue);
		adapter.close();
		sendQueueUpdateBroadcast(context, null);
	}

	/** Uses external adapter. */
	public void removeQueueItem(FeedItem item, PodDBAdapter adapter) {
		boolean removed = queue.remove(item);
		if (removed) {
			adapter.setQueue(queue);
		}

	}

	/** Uses its own adapter. */
	public void removeQueueItem(Context context, FeedItem item) {
		boolean removed = queue.remove(item);
		if (removed) {
			PodDBAdapter adapter = new PodDBAdapter(context);
			adapter.open();
			adapter.setQueue(queue);
			adapter.close();
		}
		sendQueueUpdateBroadcast(context, item);
	}

	public void moveQueueItem(Context context, FeedItem item, int delta) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Moving queue item");
		int itemIndex = queue.indexOf(item);
		int newIndex = itemIndex + delta;
		if (newIndex >= 0 && newIndex < queue.size()) {
			FeedItem oldItem = queue.set(newIndex, item);
			queue.set(itemIndex, oldItem);
			PodDBAdapter adapter = new PodDBAdapter(context);
			adapter.open();
			adapter.setQueue(queue);
			adapter.close();
		}
		sendQueueUpdateBroadcast(context, item);
	}

	public boolean isInQueue(FeedItem item) {
		return queue.contains(item);
	}

	public FeedItem getFirstQueueItem() {
		if (queue.isEmpty()) {
			return null;
		} else {
			return queue.get(0);
		}
	}

	private void addNewFeed(final Context context, final Feed feed) {
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				feeds.add(feed);
				Collections.sort(feeds, new FeedtitleComparator());
				sendFeedUpdateBroadcast(context);
			}
		});

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		adapter.setCompleteFeed(feed);
		adapter.close();
	}

	/**
	 * Updates an existing feed or adds it as a new one if it doesn't exist.
	 * 
	 * @return The saved Feed with a database ID
	 */
	public Feed updateFeed(Context context, final Feed newFeed) {
		// Look up feed in the feedslist
		final Feed savedFeed = searchFeedByLink(newFeed.getLink());
		if (savedFeed == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"Found no existing Feed with title "
								+ newFeed.getTitle() + ". Adding as new one.");
			// Add a new Feed
			addNewFeed(context, newFeed);
			return newFeed;
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Feed with title " + newFeed.getTitle()
						+ " already exists. Syncing new with existing one.");
			// Look for new or updated Items
			for (int idx = 0; idx < newFeed.getItems().size(); idx++) {
				final FeedItem item = newFeed.getItems().get(idx);
				FeedItem oldItem = searchFeedItemByIdentifyingValue(savedFeed,
						item.getIdentifyingValue());
				if (oldItem == null) {
					// item is new
					final int i = idx;
					item.setFeed(savedFeed);
					contentChanger.post(new Runnable() {
						@Override
						public void run() {
							savedFeed.getItems().add(i, item);

						}
					});
					markItemRead(context, item, false);
				}
			}
			// update attributes
			savedFeed.setLastUpdate(newFeed.getLastUpdate());
			savedFeed.setType(newFeed.getType());
			setFeed(context, savedFeed);
			return savedFeed;
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

	/** Get a FeedItem by its identifying value. */
	private FeedItem searchFeedItemByIdentifyingValue(Feed feed,
			String identifier) {
		for (FeedItem item : feed.getItems()) {
			if (item.getIdentifyingValue().equals(identifier)) {
				return item;
			}
		}
		return null;
	}

	/** Updates Information of an existing Feed. Uses external adapter. */
	public long setFeed(Feed feed, PodDBAdapter adapter) {
		if (adapter != null) {
			return adapter.setFeed(feed);
		} else {
			Log.w(TAG, "Adapter in setFeed was null");
			return 0;
		}
	}

	/** Updates Information of an existing Feeditem. Uses external adapter. */
	public long setFeedItem(FeedItem item, PodDBAdapter adapter) {
		if (adapter != null) {
			return adapter.setSingleFeedItem(item);
		} else {
			Log.w(TAG, "Adapter in setFeedItem was null");
			return 0;
		}
	}

	/** Updates Information of an existing Feedimage. Uses external adapter. */
	public long setFeedImage(FeedImage image, PodDBAdapter adapter) {
		if (adapter != null) {
			return adapter.setImage(image);
		} else {
			Log.w(TAG, "Adapter in setFeedImage was null");
			return 0;
		}
	}

	/**
	 * Updates Information of an existing Feedmedia object. Uses external
	 * adapter.
	 */
	public long setFeedImage(FeedMedia media, PodDBAdapter adapter) {
		if (adapter != null) {
			return adapter.setMedia(media);
		} else {
			Log.w(TAG, "Adapter in setFeedMedia was null");
			return 0;
		}
	}

	/**
	 * Updates Information of an existing Feed. Creates and opens its own
	 * adapter.
	 */
	public long setFeed(Context context, Feed feed) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		long result = adapter.setFeed(feed);
		adapter.close();
		return result;
	}

	/**
	 * Updates information of an existing FeedItem. Creates and opens its own
	 * adapter.
	 */
	public long setFeedItem(Context context, FeedItem item) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		long result = adapter.setSingleFeedItem(item);
		adapter.close();
		return result;
	}

	/**
	 * Updates information of an existing FeedImage. Creates and opens its own
	 * adapter.
	 */
	public long setFeedImage(Context context, FeedImage image) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		long result = adapter.setImage(image);
		adapter.close();
		return result;
	}

	/**
	 * Updates information of an existing FeedMedia object. Creates and opens
	 * its own adapter.
	 */
	public long setFeedMedia(Context context, FeedMedia media) {
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		long result = adapter.setMedia(media);
		adapter.close();
		return result;
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
		if (feed != null) {
			for (FeedItem item : feed.getItems()) {
				if (item.getId() == id) {
					return item;
				}
			}
		}
		Log.e(TAG, "Couldn't find FeedItem with id " + id);
		return null;
	}

	/** Get a FeedMedia object by the id of the Media object and the feed object */
	public FeedMedia getFeedMedia(long id, Feed feed) {
		if (feed != null) {
			for (FeedItem item : feed.getItems()) {
				if (item.getMedia().getId() == id) {
					return item.getMedia();
				}
			}
		}
		Log.e(TAG, "Couldn't find FeedMedia with id " + id);
		if (feed == null)
			Log.e(TAG, "Feed was null");
		return null;
	}

	/** Get a FeedMedia object by the id of the Media object. */
	public FeedMedia getFeedMedia(long id) {
		for (Feed feed : feeds) {
			for (FeedItem item : feed.getItems()) {
				if (item.getMedia() != null && item.getMedia().getId() == id) {
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
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		extractFeedlistFromCursor(context, adapter);
		extractDownloadLogFromCursor(context, adapter);
		extractQueueFromCursor(context, adapter);
		adapter.close();
		Collections.sort(feeds, new FeedtitleComparator());
		Collections.sort(unreadItems, new FeedItemPubdateComparator());
	}

	private void extractFeedlistFromCursor(Context context, PodDBAdapter adapter) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting Feedlist");
		Cursor feedlistCursor = adapter.getAllFeedsCursor();
		if (feedlistCursor.moveToFirst()) {
			do {
				Date lastUpdate = new Date(
						feedlistCursor
								.getLong(PodDBAdapter.KEY_LAST_UPDATE_INDEX));
				Feed feed = new Feed(lastUpdate);

				feed.id = feedlistCursor.getLong(PodDBAdapter.KEY_ID_INDEX);
				feed.setTitle(feedlistCursor
						.getString(PodDBAdapter.KEY_TITLE_INDEX));
				feed.setLink(feedlistCursor
						.getString(PodDBAdapter.KEY_LINK_INDEX));
				feed.setDescription(feedlistCursor
						.getString(PodDBAdapter.KEY_DESCRIPTION_INDEX));
				feed.setPaymentLink(feedlistCursor
						.getString(PodDBAdapter.KEY_PAYMENT_LINK_INDEX));
				feed.setAuthor(feedlistCursor
						.getString(PodDBAdapter.KEY_AUTHOR_INDEX));
				feed.setLanguage(feedlistCursor
						.getString(PodDBAdapter.KEY_LANGUAGE_INDEX));
				feed.setType(feedlistCursor
						.getString(PodDBAdapter.KEY_TYPE_INDEX));
				long imageIndex = feedlistCursor
						.getLong(PodDBAdapter.KEY_IMAGE_INDEX);
				if (imageIndex != 0) {
					feed.setImage(adapter.getFeedImage(imageIndex));
				}
				feed.file_url = feedlistCursor
						.getString(PodDBAdapter.KEY_FILE_URL_INDEX);
				feed.download_url = feedlistCursor
						.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX);
				feed.setDownloaded(feedlistCursor
						.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0);
				// Get FeedItem-Object
				Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
				feed.setItems(extractFeedItemsFromCursor(context, feed,
						itemlistCursor, adapter));
				itemlistCursor.close();

				feeds.add(feed);
			} while (feedlistCursor.moveToNext());
		}
		feedlistCursor.close();

	}

	private ArrayList<FeedItem> extractFeedItemsFromCursor(Context context,
			Feed feed, Cursor itemlistCursor, PodDBAdapter adapter) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting Feeditems of feed " + feed.getTitle());
		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		ArrayList<String> mediaIds = new ArrayList<String>();

		if (itemlistCursor.moveToFirst()) {
			do {
				FeedItem item = new FeedItem();

				item.id = itemlistCursor.getLong(PodDBAdapter.KEY_ID_INDEX);
				item.setFeed(feed);
				item.setTitle(itemlistCursor
						.getString(PodDBAdapter.KEY_TITLE_INDEX));
				item.setLink(itemlistCursor
						.getString(PodDBAdapter.KEY_LINK_INDEX));
				item.setDescription(itemlistCursor
						.getString(PodDBAdapter.KEY_DESCRIPTION_INDEX));
				item.setContentEncoded(itemlistCursor
						.getString(PodDBAdapter.KEY_CONTENT_ENCODED_INDEX));
				item.setPubDate(new Date(itemlistCursor
						.getLong(PodDBAdapter.KEY_PUBDATE_INDEX)));
				item.setPaymentLink(itemlistCursor
						.getString(PodDBAdapter.KEY_PAYMENT_LINK_INDEX));
				long mediaId = itemlistCursor
						.getLong(PodDBAdapter.KEY_MEDIA_INDEX);
				if (mediaId != 0) {
					mediaIds.add(String.valueOf(mediaId));
					item.setMedia(new FeedMedia(mediaId, item));
				}
				item.read = (itemlistCursor.getInt(PodDBAdapter.KEY_READ_INDEX) > 0) ? true
						: false;
				item.setItemIdentifier(itemlistCursor
						.getString(PodDBAdapter.KEY_ITEM_IDENTIFIER_INDEX));
				if (!item.read) {
					unreadItems.add(item);
				}

				// extract chapters
				boolean hasSimpleChapters = itemlistCursor
						.getInt(PodDBAdapter.KEY_HAS_SIMPLECHAPTERS_INDEX) > 0;
				if (hasSimpleChapters) {
					Cursor chapterCursor = adapter
							.getSimpleChaptersOfFeedItemCursor(item);
					if (chapterCursor.moveToFirst()) {
						item.setSimpleChapters(new ArrayList<SimpleChapter>());
						do {
							SimpleChapter chapter = new SimpleChapter(
									item,
									chapterCursor
											.getLong(PodDBAdapter.KEY_SC_START_INDEX),
									chapterCursor
											.getString(PodDBAdapter.KEY_TITLE_INDEX),
									chapterCursor
											.getString(PodDBAdapter.KEY_SC_LINK_INDEX));
							chapter.setId(chapterCursor
									.getLong(PodDBAdapter.KEY_ID_INDEX));
							item.getSimpleChapters().add(chapter);
						} while (chapterCursor.moveToNext());
					}
					chapterCursor.close();
				}
				items.add(item);
			} while (itemlistCursor.moveToNext());
		}
		extractMediafromFeedItemlist(adapter, items, mediaIds);
		Collections.sort(items, new FeedItemPubdateComparator());
		return items;
	}

	private void extractMediafromFeedItemlist(PodDBAdapter adapter,
			ArrayList<FeedItem> items, ArrayList<String> mediaIds) {
		ArrayList<FeedItem> itemsCopy = new ArrayList<FeedItem>(items);
		Cursor cursor = adapter.getFeedMediaCursor(mediaIds
				.toArray(new String[mediaIds.size()]));
		if (cursor.moveToFirst()) {
			do {
				long mediaId = cursor.getLong(PodDBAdapter.KEY_ID_INDEX);
				// find matching feed item
				FeedItem item = getMatchingItemForMedia(mediaId, itemsCopy);
				itemsCopy.remove(item);
				if (item != null) {
					item.setMedia(new FeedMedia(
							mediaId,
							item,
							cursor.getInt(PodDBAdapter.KEY_DURATION_INDEX),
							cursor.getInt(PodDBAdapter.KEY_POSITION_INDEX),
							cursor.getLong(PodDBAdapter.KEY_SIZE_INDEX),
							cursor.getString(PodDBAdapter.KEY_MIME_TYPE_INDEX),
							cursor.getString(PodDBAdapter.KEY_FILE_URL_INDEX),
							cursor.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX),
							cursor.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0));

				}
			} while (cursor.moveToNext());
			cursor.close();
		}
	}

	private FeedItem getMatchingItemForMedia(long mediaId,
			ArrayList<FeedItem> items) {
		for (FeedItem item : items) {
			if (item.getMedia() != null && item.getMedia().getId() == mediaId) {
				return item;
			}
		}
		return null;
	}

	private void extractDownloadLogFromCursor(Context context,
			PodDBAdapter adapter) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting DownloadLog");
		Cursor logCursor = adapter.getDownloadLogCursor();
		if (logCursor.moveToFirst()) {
			do {
				long id = logCursor.getLong(PodDBAdapter.KEY_ID_INDEX);
				long feedfileId = logCursor
						.getLong(PodDBAdapter.KEY_FEEDFILE_INDEX);
				int feedfileType = logCursor
						.getInt(PodDBAdapter.KEY_FEEDFILETYPE_INDEX);
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
					boolean successful = logCursor
							.getInt(PodDBAdapter.KEY_SUCCESSFUL_INDEX) > 0;
					int reason = logCursor
							.getInt(PodDBAdapter.KEY_REASON_INDEX);
					Date completionDate = new Date(
							logCursor
									.getLong(PodDBAdapter.KEY_COMPLETION_DATE_INDEX));
					downloadLog.add(new DownloadStatus(id, feedfile,
							successful, reason, completionDate));
				}

			} while (logCursor.moveToNext());
		}
		logCursor.close();
	}

	private void extractQueueFromCursor(Context context, PodDBAdapter adapter) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting Queue");
		Cursor cursor = adapter.getQueueCursor();
		if (cursor.moveToFirst()) {
			do {
				int index = cursor.getInt(PodDBAdapter.KEY_ID_INDEX);
				Feed feed = getFeed(cursor
						.getLong(PodDBAdapter.KEY_QUEUE_FEED_INDEX));
				if (feed != null) {
					FeedItem item = getFeedItem(
							cursor.getLong(PodDBAdapter.KEY_FEEDITEM_INDEX),
							feed);
					if (item != null) {
						queue.add(index, item);
					}
				}

			} while (cursor.moveToNext());
		}
		cursor.close();
	}

	public List<Feed> getFeeds() {
		return feeds;
	}

	public List<FeedItem> getUnreadItems() {
		return unreadItems;
	}

	public ArrayList<DownloadStatus> getDownloadLog() {
		return downloadLog;
	}

	public List<FeedItem> getQueue() {
		return queue;
	}

}
