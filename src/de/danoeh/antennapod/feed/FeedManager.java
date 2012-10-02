package de.danoeh.antennapod.feed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.storage.PodDBAdapter;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.FeedtitleComparator;
import de.danoeh.antennapod.util.comparator.DownloadStatusComparator;
import de.danoeh.antennapod.util.comparator.FeedItemPubdateComparator;

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
	public static final String ACTION_DOWNLOADLOG_UPDATE = "de.danoeh.antennapod.action.feed.downloadLogUpdate";
	public static final String EXTRA_FEED_ITEM_ID = "de.danoeh.antennapod.extra.feed.feedItemId";
	public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feed.feedId";

	/** Number of completed Download status entries to store. */
	private static final int DOWNLOAD_LOG_SIZE = 50;

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
	/** Ensures that there are no parallel db operations. */
	private Executor dbExec;

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
		dbExec = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setPriority(Thread.MIN_PRIORITY);
				return t;
			}
		});
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
			context.startActivity(PlaybackService.getPlayerActivityIntent(
					context, media));
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

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			final long lastPlayedId = prefs.getLong(
					PlaybackService.PREF_LAST_PLAYED_ID, -1);
			if (media.getId() == lastPlayedId) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putBoolean(PlaybackService.PREF_LAST_IS_STREAM, true);
				editor.commit();
			}
			if (lastPlayedId == media.getId()) {
				context.sendBroadcast(new Intent(
						PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
			}
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Deleting File. Result: " + result);
		return result;
	}

	/** Remove a feed with all its items and media files and its image. */
	public void deleteFeed(final Context context, final Feed feed) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		long lastPlayedFeed = prefs.getLong(
				PlaybackService.PREF_LAST_PLAYED_FEED_ID, -1);
		if (lastPlayedFeed == feed.getId()) {
			context.sendBroadcast(new Intent(
					PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(PlaybackService.PREF_LAST_PLAYED_ID, -1);
			editor.putLong(PlaybackService.PREF_LAST_PLAYED_FEED_ID, -1);
			editor.commit();
		}

		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				feeds.remove(feed);
				sendFeedUpdateBroadcast(context);
				dbExec.execute(new Runnable() {

					@Override
					public void run() {
						PodDBAdapter adapter = new PodDBAdapter(context);
						DownloadRequester requester = DownloadRequester
								.getInstance();
						adapter.open();
						// delete image file
						if (feed.getImage() != null) {
							if (feed.getImage().isDownloaded()
									&& feed.getImage().getFile_url() != null) {
								File imageFile = new File(feed.getImage()
										.getFile_url());
								imageFile.delete();
							} else if (requester.isDownloadingFile(feed
									.getImage())) {
								requester.cancelDownload(context,
										feed.getImage());
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
							if (item.getMedia() != null
									&& item.getMedia().isDownloaded()) {
								File mediaFile = new File(item.getMedia()
										.getFile_url());
								mediaFile.delete();
							} else if (item.getMedia() != null
									&& requester.isDownloadingFile(item
											.getMedia())) {
								requester.cancelDownload(context,
										item.getMedia());
							}
						}

						adapter.removeFeed(feed);
						adapter.close();
					}
				});
			}
		});

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
	public void markItemRead(final Context context, final FeedItem item,
			final boolean read) {
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
					Collections.sort(unreadItems,
							new FeedItemPubdateComparator());
				}
				sendUnreadItemsUpdateBroadcast(context, item);
			}
		});

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
	public void markAllItemsRead(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "marking all items as read");
		for (FeedItem item : unreadItems) {
			item.read = true;
		}
		final ArrayList<FeedItem> unreadItemsCopy = new ArrayList<FeedItem>(
				unreadItems);
		unreadItems.clear();
		sendUnreadItemsUpdateBroadcast(context, null);
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				for (FeedItem item : unreadItemsCopy) {
					setFeedItem(item, adapter);
				}
				adapter.close();
			}
		});

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
						try {
							refreshFeed(context, feed);
						} catch (DownloadRequestException e) {
							e.printStackTrace();
							addDownloadStatus(
									context,
									new DownloadStatus(feed, feed
											.getHumanReadableIdentifier(),
											DownloadError.ERROR_REQUEST_ERROR,
											false, e.getMessage()));
						}
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
		try {
			requester.downloadImage(context, image);
		} catch (DownloadRequestException e) {
			e.printStackTrace();
			Log.w(TAG, "Failed to download invalid feed image");
		}
	}

	public void refreshFeed(Context context, Feed feed)
			throws DownloadRequestException {
		requester.downloadFeed(context, new Feed(feed.getDownload_url(),
				new Date(), feed.getTitle()));
	}

	public void addDownloadStatus(final Context context,
			final DownloadStatus status) {
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				downloadLog.add(status);
				Collections.sort(downloadLog, new DownloadStatusComparator());
				final DownloadStatus removedStatus;
				if (downloadLog.size() > DOWNLOAD_LOG_SIZE) {
					removedStatus = downloadLog.remove(downloadLog.size() - 1);
				} else {
					removedStatus = null;
				}
				context.sendBroadcast(new Intent(ACTION_DOWNLOADLOG_UPDATE));
				dbExec.execute(new Runnable() {

					@Override
					public void run() {
						PodDBAdapter adapter = new PodDBAdapter(context);
						adapter.open();
						if (removedStatus != null) {
							adapter.removeDownloadStatus(removedStatus);
						}
						adapter.setDownloadStatus(status);
						adapter.close();
					}
				});
			}
		});

	}

	public void downloadAllItemsInQueue(final Context context) {
		if (!queue.isEmpty()) {
			try {
				downloadFeedItem(context,
						queue.toArray(new FeedItem[queue.size()]));
			} catch (DownloadRequestException e) {
				e.printStackTrace();
			}
		}
	}

	public void downloadFeedItem(final Context context, FeedItem... items)
			throws DownloadRequestException {
		boolean autoQueue = PreferenceManager.getDefaultSharedPreferences(
				context.getApplicationContext()).getBoolean(
				PodcastApp.PREF_AUTO_QUEUE, true);
		List<FeedItem> addToQueue = new ArrayList<FeedItem>();

		for (FeedItem item : items) {
			if (item.getMedia() != null
					&& !requester.isDownloadingFile(item.getMedia())
					&& !item.getMedia().isDownloaded()) {
				if (items.length > 1) {
					try {
						requester.downloadMedia(context, item.getMedia());
					} catch (DownloadRequestException e) {
						e.printStackTrace();
						addDownloadStatus(context,
								new DownloadStatus(item.getMedia(), item
										.getMedia()
										.getHumanReadableIdentifier(),
										DownloadError.ERROR_REQUEST_ERROR,
										false, e.getMessage()));
					}
				} else {
					requester.downloadMedia(context, item.getMedia());
				}
				addToQueue.add(item);
			}
		}
		if (autoQueue) {
			addQueueItem(context,
					addToQueue.toArray(new FeedItem[addToQueue.size()]));
		}
	}

	public void enqueueAllNewItems(final Context context) {
		if (!unreadItems.isEmpty()) {
			addQueueItem(context,
					unreadItems.toArray(new FeedItem[unreadItems.size()]));
			markAllItemsRead(context);
		}
	}

	public void addQueueItem(final Context context, final FeedItem... items) {
		if (items.length > 0) {
			contentChanger.post(new Runnable() {

				@Override
				public void run() {
					for (FeedItem item : items) {
						if (!queue.contains(item)) {
							queue.add(item);
						}
					}
					sendQueueUpdateBroadcast(context, items[0]);
					dbExec.execute(new Runnable() {

						@Override
						public void run() {
							PodDBAdapter adapter = new PodDBAdapter(context);
							adapter.open();
							adapter.setQueue(queue);
							adapter.close();
						}
					});
				}
			});
		}

	}

	/** Removes all items in queue */
	public void clearQueue(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Clearing queue");
		queue.clear();
		sendQueueUpdateBroadcast(context, null);
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setQueue(queue);
				adapter.close();
			}
		});

	}

	/** Uses external adapter. */
	public void removeQueueItem(FeedItem item, PodDBAdapter adapter) {
		boolean removed = queue.remove(item);
		if (removed) {
			adapter.setQueue(queue);
		}

	}

	/** Uses its own adapter. */
	public void removeQueueItem(final Context context, FeedItem item) {
		boolean removed = queue.remove(item);
		if (removed) {
			autoDeleteIfPossible(context, item.getMedia());
			dbExec.execute(new Runnable() {

				@Override
				public void run() {
					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					adapter.setQueue(queue);
					adapter.close();
				}
			});

		}
		sendQueueUpdateBroadcast(context, item);
	}

	/**
	 * Delete the episode of this FeedMedia object if auto-delete is enabled and
	 * it is not the last played media or it is the last played media and
	 * playback has been completed.
	 */
	public void autoDeleteIfPossible(Context context, FeedMedia media) {
		if (media != null) {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context
							.getApplicationContext());
			boolean autoDelete = prefs.getBoolean(PodcastApp.PREF_AUTO_DELETE,
					false);
			if (autoDelete) {
				long lastPlayedId = prefs.getLong(
						PlaybackService.PREF_LAST_PLAYED_ID, -1);
				long autoDeleteId = prefs.getLong(
						PlaybackService.PREF_AUTODELETE_MEDIA_ID, -1);
				boolean playbackCompleted = prefs
						.getBoolean(
								PlaybackService.PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED,
								false);
				if ((media.getId() != lastPlayedId)
						&& ((media.getId() != autoDeleteId) || (media.getId() == autoDeleteId && playbackCompleted))) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Performing auto-cleanup");
					deleteFeedMedia(context, media);

					SharedPreferences.Editor editor = prefs.edit();
					editor.putLong(PlaybackService.PREF_AUTODELETE_MEDIA_ID, -1);
					editor.commit();
				} else {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Didn't do auto-cleanup");
				}
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Auto-delete preference is disabled");
			}
		} else {
			Log.e(TAG, "Could not do auto-cleanup: media was null");
		}
	}

	public void moveQueueItem(final Context context, FeedItem item, int delta) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Moving queue item");
		int itemIndex = queue.indexOf(item);
		int newIndex = itemIndex + delta;
		if (newIndex >= 0 && newIndex < queue.size()) {
			FeedItem oldItem = queue.set(newIndex, item);
			queue.set(itemIndex, oldItem);
			dbExec.execute(new Runnable() {

				@Override
				public void run() {
					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					adapter.setQueue(queue);
					adapter.close();
				}
			});

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
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setCompleteFeed(feed);
				adapter.close();
			}
		});

	}

	/**
	 * Updates an existing feed or adds it as a new one if it doesn't exist.
	 * 
	 * @return The saved Feed with a database ID
	 */
	public Feed updateFeed(Context context, final Feed newFeed) {
		// Look up feed in the feedslist
		final Feed savedFeed = searchFeedByIdentifyingValue(newFeed
				.getIdentifyingValue());
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
	private Feed searchFeedByIdentifyingValue(String identifier) {
		for (Feed feed : feeds) {
			if (feed.getIdentifyingValue().equals(identifier)) {
				return feed;
			}
		}
		return null;
	}

	/**
	 * Returns true if a feed with the given download link is already in the
	 * feedlist.
	 */
	public boolean feedExists(String downloadUrl) {
		for (Feed feed : feeds) {
			if (feed.getDownload_url().equals(downloadUrl)) {
				return true;
			}
		}
		return false;
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
	public void setFeed(Feed feed, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setFeed(feed);
		} else {
			Log.w(TAG, "Adapter in setFeed was null");
		}
	}

	/** Updates Information of an existing Feeditem. Uses external adapter. */
	public void setFeedItem(FeedItem item, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setSingleFeedItem(item);
		} else {
			Log.w(TAG, "Adapter in setFeedItem was null");
		}
	}

	/** Updates Information of an existing Feedimage. Uses external adapter. */
	public void setFeedImage(FeedImage image, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setImage(image);
		} else {
			Log.w(TAG, "Adapter in setFeedImage was null");
		}
	}

	/**
	 * Updates Information of an existing Feedmedia object. Uses external
	 * adapter.
	 */
	public void setFeedImage(FeedMedia media, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setMedia(media);
		} else {
			Log.w(TAG, "Adapter in setFeedMedia was null");
		}
	}

	/**
	 * Updates Information of an existing Feed. Creates and opens its own
	 * adapter.
	 */
	public void setFeed(final Context context, final Feed feed) {
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setFeed(feed);
				adapter.close();
			}
		});

	}

	/**
	 * Updates information of an existing FeedItem. Creates and opens its own
	 * adapter.
	 */
	public void setFeedItem(final Context context, final FeedItem item) {
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setSingleFeedItem(item);
				adapter.close();
			}
		});

	}

	/**
	 * Updates information of an existing FeedImage. Creates and opens its own
	 * adapter.
	 */
	public void setFeedImage(final Context context, final FeedImage image) {
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setImage(image);
				adapter.close();
			}
		});

	}

	/**
	 * Updates information of an existing FeedMedia object. Creates and opens
	 * its own adapter.
	 */
	public void setFeedMedia(final Context context, final FeedMedia media) {
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setMedia(media);
				adapter.close();
			}
		});

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
				if (item.getMedia() != null && item.getMedia().getId() == id) {
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

	public DownloadStatus getDownloadStatus(FeedFile feedFile) {
		for (DownloadStatus status : downloadLog) {
			if (status.getFeedFile() == feedFile) {
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
				feed.setFeedIdentifier(feedlistCursor
						.getString(PodDBAdapter.KEY_FEED_IDENTIFIER_INDEX));
				long imageIndex = feedlistCursor
						.getLong(PodDBAdapter.KEY_IMAGE_INDEX);
				if (imageIndex != 0) {
					feed.setImage(adapter.getFeedImage(imageIndex));
					feed.getImage().setFeed(feed);
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
						item.setChapters(new ArrayList<Chapter>());
						do {
							int chapterType = chapterCursor
									.getInt(PodDBAdapter.KEY_CHAPTER_TYPE_INDEX);
							Chapter chapter = null;
							long start = chapterCursor
									.getLong(PodDBAdapter.KEY_CHAPTER_START_INDEX);
							String title = chapterCursor
									.getString(PodDBAdapter.KEY_TITLE_INDEX);
							String link = chapterCursor
									.getString(PodDBAdapter.KEY_CHAPTER_LINK_INDEX);

							switch (chapterType) {
							case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
								chapter = new SimpleChapter(start, title, item,
										link);
								break;
							case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
								chapter = new ID3Chapter(start, title, item,
										link);
								break;
							}
							chapter.setId(chapterCursor
									.getLong(PodDBAdapter.KEY_ID_INDEX));
							item.getChapters().add(chapter);
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
					Date playbackCompletionDate = null;
					long playbackCompletionTime = cursor
							.getLong(PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE_INDEX);
					if (playbackCompletionTime > 0) {
						playbackCompletionDate = new Date(
								playbackCompletionTime);
					}

					item.setMedia(new FeedMedia(
							mediaId,
							item,
							cursor.getInt(PodDBAdapter.KEY_DURATION_INDEX),
							cursor.getInt(PodDBAdapter.KEY_POSITION_INDEX),
							cursor.getLong(PodDBAdapter.KEY_SIZE_INDEX),
							cursor.getString(PodDBAdapter.KEY_MIME_TYPE_INDEX),
							cursor.getString(PodDBAdapter.KEY_FILE_URL_INDEX),
							cursor.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX),
							cursor.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0,
							playbackCompletionDate));

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
				FeedFile feedfile = null;

				long feedfileId = logCursor
						.getLong(PodDBAdapter.KEY_FEEDFILE_INDEX);
				int feedfileType = logCursor
						.getInt(PodDBAdapter.KEY_FEEDFILETYPE_INDEX);
				if (feedfileId != 0) {
					switch (feedfileType) {
					case Feed.FEEDFILETYPE_FEED:
						feedfile = getFeed(feedfileId);
						break;
					case FeedImage.FEEDFILETYPE_FEEDIMAGE:
						feedfile = getFeedImage(feedfileId);
						break;
					case FeedMedia.FEEDFILETYPE_FEEDMEDIA:
						feedfile = getFeedMedia(feedfileId);
					}
				}
				boolean successful = logCursor
						.getInt(PodDBAdapter.KEY_SUCCESSFUL_INDEX) > 0;
				int reason = logCursor.getInt(PodDBAdapter.KEY_REASON_INDEX);
				String reasonDetailed = logCursor
						.getString(PodDBAdapter.KEY_REASON_DETAILED_INDEX);
				String title = logCursor
						.getString(PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE_INDEX);
				Date completionDate = new Date(
						logCursor
								.getLong(PodDBAdapter.KEY_COMPLETION_DATE_INDEX));
				downloadLog.add(new DownloadStatus(id, title, feedfile,
						feedfileType, successful, reason, completionDate,
						reasonDetailed));

			} while (logCursor.moveToNext());
		}
		logCursor.close();
		Collections.sort(downloadLog, new DownloadStatusComparator());
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
