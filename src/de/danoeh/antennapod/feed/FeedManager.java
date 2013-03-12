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
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.storage.PodDBAdapter;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.EpisodeFilter;
import de.danoeh.antennapod.util.FeedtitleComparator;
import de.danoeh.antennapod.util.NetworkUtils;
import de.danoeh.antennapod.util.comparator.DownloadStatusComparator;
import de.danoeh.antennapod.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.util.comparator.PlaybackCompletionDateComparator;
import de.danoeh.antennapod.util.exception.MediaFileNotFoundException;

/**
 * Singleton class that - provides access to all Feeds and FeedItems and to
 * several lists of FeedItems. - provides methods for modifying the
 * application's data - takes care of updating the information stored in the
 * database when something is modified
 * 
 * An instance of this class can be retrieved via getInstance().
 * */
public class FeedManager {
	private static final String TAG = "FeedManager";

	/** Number of completed Download status entries to store. */
	private static final int DOWNLOAD_LOG_SIZE = 50;

	private static FeedManager singleton;

	private List<Feed> feeds;

	/** Contains all items where 'read' is false */
	private List<FeedItem> unreadItems;

	/** Contains completed Download status entries */
	private List<DownloadStatus> downloadLog;

	/** Contains the queue of items to be played. */
	private List<FeedItem> queue;

	/** Contains the last played items */
	private List<FeedItem> playbackHistory;

	/** Maximum number of items in the playback history. */
	private static final int PLAYBACK_HISTORY_SIZE = 15;

	private DownloadRequester requester = DownloadRequester.getInstance();
	private EventDistributor eventDist = EventDistributor.getInstance();

	/**
	 * Should be used to change the content of the arrays from another thread to
	 * ensure that arrays are only modified on the main thread.
	 */
	private Handler contentChanger;

	/** Ensures that there are no parallel db operations. */
	private Executor dbExec;

	/** Prevents user from starting several feed updates at the same time. */
	private static boolean isStartingFeedRefresh = false;

	private FeedManager() {
		feeds = Collections.synchronizedList(new ArrayList<Feed>());
		unreadItems = Collections.synchronizedList(new ArrayList<FeedItem>());
		downloadLog = new ArrayList<DownloadStatus>();
		queue = Collections.synchronizedList(new ArrayList<FeedItem>());
		playbackHistory = Collections
				.synchronizedList(new ArrayList<FeedItem>());
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

	/** Creates a new instance of this class if necessary and returns it. */
	public static FeedManager getInstance() {
		if (singleton == null) {
			singleton = new FeedManager();
		}
		return singleton;
	}

	/**
	 * Play FeedMedia and start the playback service + launch Mediaplayer
	 * Activity. The FeedItem belonging to the media is moved to the top of the
	 * queue.
	 * 
	 * @param context
	 *            for starting the playbackservice
	 * @param media
	 *            that shall be played
	 * @param showPlayer
	 *            if Mediaplayer activity shall be started
	 * @param startWhenPrepared
	 *            if Mediaplayer shall be started after it has been prepared
	 * @param shouldStream
	 *            if Mediaplayer should stream the file
	 */
	public void playMedia(Context context, FeedMedia media, boolean showPlayer,
			boolean startWhenPrepared, boolean shouldStream) {
		try {
			if (!shouldStream) {
				if (media.fileExists() == false) {
					throw new MediaFileNotFoundException(
							"No episode was found at " + media.getFile_url(),
							media);
				}
			}
			// Start playback Service
			Intent launchIntent = new Intent(context, PlaybackService.class);
			launchIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
			launchIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
					startWhenPrepared);
			launchIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
					shouldStream);
			launchIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY,
					true);
			context.startService(launchIntent);
			if (showPlayer) {
				// Launch Mediaplayer
				context.startActivity(PlaybackService.getPlayerActivityIntent(
						context, media));
			}
			if (queue.contains(media.getItem())) {
				moveQueueItem(context, queue.indexOf(media.getItem()), 0, true);
			} else {
				addQueueItemAt(context, media.getItem(), 0);
			}
		} catch (MediaFileNotFoundException e) {
			e.printStackTrace();
			if (media.isPlaying()) {
				context.sendBroadcast(new Intent(
						PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
			}
			notifyMissingFeedMediaFile(context, media);
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
			if (PlaybackPreferences.getCurrentlyPlayingMedia() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA) {
				if (media.getId() == PlaybackPreferences
						.getCurrentlyPlayingFeedMediaId()) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putBoolean(
							PlaybackPreferences.PREF_CURRENT_EPISODE_IS_STREAM,
							true);
					editor.commit();
				}
				if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == media
						.getId()) {
					context.sendBroadcast(new Intent(
							PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
				}
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
		if (PlaybackPreferences.getCurrentlyPlayingMedia() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA
				&& PlaybackPreferences.getLastPlayedFeedId() == feed.getId()) {
			context.sendBroadcast(new Intent(
					PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
					-1);
			editor.commit();
		}

		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				feeds.remove(feed);
				eventDist.sendFeedUpdateBroadcast();
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
							if (item.getState() == FeedItem.State.NEW) {
								unreadItems.remove(item);
							}
							if (queue.contains(item)) {
								removeQueueItem(item, adapter);
							}
							removeItemFromPlaybackHistory(context, item);
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

	/**
	 * Makes sure that playback history is sorted and is not larger than
	 * PLAYBACK_HISTORY_SIZE.
	 * 
	 * @return an array of all feeditems that were remove from the playback
	 *         history or null if no items were removed.
	 */
	private FeedItem[] cleanupPlaybackHistory() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Cleaning up playback history.");

		Collections.sort(playbackHistory,
				new PlaybackCompletionDateComparator());
		final int initialSize = playbackHistory.size();
		if (initialSize > PLAYBACK_HISTORY_SIZE) {
			FeedItem[] removed = new FeedItem[initialSize
					- PLAYBACK_HISTORY_SIZE];

			for (int i = 0; i < removed.length; i++) {
				removed[i] = playbackHistory.remove(playbackHistory.size() - 1);
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Removed " + removed.length
						+ " items from playback history.");
			return removed;
		}
		return null;
	}

	/**
	 * Executes cleanupPlaybackHistory and deletes the playbackCompletionDate of
	 * all item that were removed from the history.
	 */
	private void cleanupPlaybackHistoryWithDBCleanup(final Context context) {
		final FeedItem[] removedItems = cleanupPlaybackHistory();
		if (removedItems != null) {
			dbExec.execute(new Runnable() {

				@Override
				public void run() {
					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					for (FeedItem item : removedItems) {
						if (item.getMedia() != null) {
							item.getMedia().setPlaybackCompletionDate(null);
							adapter.setMedia(item.getMedia());
						}
					}
					adapter.close();
				}
			});
		}
	}

	/** Removes all items from the playback history. */
	public void clearPlaybackHistory(final Context context) {
		if (!playbackHistory.isEmpty()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Clearing playback history.");
			final FeedItem[] items = playbackHistory
					.toArray(new FeedItem[playbackHistory.size()]);
			playbackHistory.clear();
			eventDist.sendPlaybackHistoryUpdateBroadcast();
			dbExec.execute(new Runnable() {

				@Override
				public void run() {
					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					for (FeedItem item : items) {
						if (item.getMedia() != null
								&& item.getMedia().getPlaybackCompletionDate() != null) {
							item.getMedia().setPlaybackCompletionDate(null);
							adapter.setMedia(item.getMedia());
						}
					}
					adapter.close();
				}
			});
		}
	}

	/** Adds a FeedItem to the playback history. */
	public void addItemToPlaybackHistory(Context context, FeedItem item) {
		if (item.getMedia() != null
				&& item.getMedia().getPlaybackCompletionDate() != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Adding new item to playback history");
			if (!playbackHistory.contains(item)) {
				playbackHistory.add(item);
			}
			cleanupPlaybackHistoryWithDBCleanup(context);
			eventDist.sendPlaybackHistoryUpdateBroadcast();
		}
	}

	private void removeItemFromPlaybackHistory(Context context, FeedItem item) {
		playbackHistory.remove(item);
		eventDist.sendPlaybackHistoryUpdateBroadcast();
	}

	/**
	 * Sets the 'read'-attribute of a FeedItem. Should be used by all Classes
	 * instead of the setters of FeedItem.
	 */
	public void markItemRead(final Context context, final FeedItem item,
			final boolean read, boolean resetMediaPosition) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting item with title " + item.getTitle()
					+ " as read/unread");

		item.setRead(read);
		if (item.hasMedia() && resetMediaPosition) {
			item.getMedia().setPosition(0);
		}
		setFeedItem(context, item);
		if (item.hasMedia() && resetMediaPosition)
			setFeedMedia(context, item.getMedia());

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
				eventDist.sendUnreadItemsUpdateBroadcast();
			}
		});

	}

	/**
	 * Sets the 'read' attribute of all FeedItems of a specific feed to true
	 */
	public void markFeedRead(Context context, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (unreadItems.contains(item)) {
				markItemRead(context, item, true, false);
			}
		}
	}

	/** Marks all items in the unread items list as read */
	public void markAllItemsRead(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "marking all items as read");
		for (FeedItem item : unreadItems) {
			item.setRead(true);
		}
		final ArrayList<FeedItem> unreadItemsCopy = new ArrayList<FeedItem>(
				unreadItems);
		unreadItems.clear();
		eventDist.sendUnreadItemsUpdateBroadcast();
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				for (FeedItem item : unreadItemsCopy) {
					setFeedItem(item, adapter);
					if (item.hasMedia())
						setFeedMedia(context, item.getMedia());
				}
				adapter.close();
			}
		});

	}

	/** Updates all feeds in the feed list. */
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

	/**
	 * Notifies the feed manager that a downloaded episode doesn't exist
	 * anymore. It will update the values of the FeedMedia object accordingly.
	 */
	public void notifyMissingFeedMediaFile(Context context, FeedMedia media) {
		Log.i(TAG,
				"The feedmanager was notified about a missing episode. It will update its database now.");
		media.setDownloaded(false);
		media.setFile_url(null);
		setFeedMedia(context, media);
		eventDist.sendFeedUpdateBroadcast();
	}

	/** Updates a specific feed. */
	public void refreshFeed(Context context, Feed feed)
			throws DownloadRequestException {
		requester.downloadFeed(context, new Feed(feed.getDownload_url(),
				new Date(), feed.getTitle()));
	}

	/** Adds a download status object to the download log. */
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
				eventDist.sendDownloadLogUpdateBroadcast();
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

	/** Downloads all items in the queue that have not been downloaded yet. */
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
		downloadFeedItem(true, context, items);
	}

	/** Downloads FeedItems if they have not been downloaded yet. */
	private void downloadFeedItem(boolean performAutoCleanup,
			final Context context, final FeedItem... items)
			throws DownloadRequestException {
		if (performAutoCleanup) {
			new Thread() {

				@Override
				public void run() {
					performAutoCleanup(context,
							getPerformAutoCleanupArgs(items.length));
				}

			}.start();
		}
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
			}
		}
	}

	/**
	 * This method will try to download undownloaded items in the queue or the
	 * unread items list. If not enough space is available, an episode cleanup
	 * will be performed first.
	 * 
	 * This method assumes that the item that is currently being played is at
	 * index 0 in the queue and therefore will not try to download it.
	 */
	public void autodownloadUndownloadedItems(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Performing auto-dl of undownloaded episodes");
		if (NetworkUtils.autodownloadNetworkAvailable(context)) {
			int undownloadedEpisodes = getNumberOfUndownloadedEpisodes();
			int downloadedEpisodes = getNumberOfDownloadedEpisodes();
			int deletedEpisodes = performAutoCleanup(context,
					getPerformAutoCleanupArgs(undownloadedEpisodes));
			int episodeSpaceLeft = undownloadedEpisodes;
			if (UserPreferences.getEpisodeCacheSize() < downloadedEpisodes
					+ undownloadedEpisodes) {
				episodeSpaceLeft = UserPreferences.getEpisodeCacheSize()
						- (downloadedEpisodes - deletedEpisodes);
			}

			List<FeedItem> itemsToDownload = new ArrayList<FeedItem>();
			if (episodeSpaceLeft > 0 && undownloadedEpisodes > 0) {
				for (int i = 1; i < queue.size(); i++) { // ignore first item in
															// queue
					FeedItem item = queue.get(i);
					if (item.hasMedia() && !item.getMedia().isDownloaded()) {
						itemsToDownload.add(item);
						episodeSpaceLeft--;
						undownloadedEpisodes--;
						if (episodeSpaceLeft == 0 || undownloadedEpisodes == 0) {
							break;
						}
					}
				}
			}
			if (episodeSpaceLeft > 0 && undownloadedEpisodes > 0) {
				for (FeedItem item : unreadItems) {
					if (item.hasMedia() && !item.getMedia().isDownloaded()) {
						itemsToDownload.add(item);
						episodeSpaceLeft--;
						undownloadedEpisodes--;
						if (episodeSpaceLeft == 0 || undownloadedEpisodes == 0) {
							break;
						}
					}
				}
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Enqueueing " + itemsToDownload.size()
						+ " items for download");

			try {
				downloadFeedItem(false, context,
						itemsToDownload.toArray(new FeedItem[itemsToDownload
								.size()]));
			} catch (DownloadRequestException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * This method will determine the number of episodes that have to be deleted
	 * depending on a given number of episodes.
	 * 
	 * @return The argument that has to be passed to performAutoCleanup() so
	 *         that the number of episodes fits into the episode cache.
	 * */
	private int getPerformAutoCleanupArgs(final int episodeNumber) {
		if (episodeNumber >= 0) {
			int downloadedEpisodes = getNumberOfDownloadedEpisodes();
			if (downloadedEpisodes + episodeNumber >= UserPreferences
					.getEpisodeCacheSize()) {

				return downloadedEpisodes + episodeNumber
						- UserPreferences.getEpisodeCacheSize();
			}
		}
		return 0;
	}

	/**
	 * Performs an auto-cleanup so that the number of downloaded episodes is
	 * below or equal to the episode cache size. The method will be executed in
	 * the caller's thread.
	 */
	public void performAutoCleanup(Context context) {
		performAutoCleanup(context, getPerformAutoCleanupArgs(0));
	}

	/**
	 * This method will try to delete a given number of episodes. An episode
	 * will only be deleted if it is not in the queue.
	 * 
	 * @return The number of episodes that were actually deleted
	 * */
	private int performAutoCleanup(Context context, final int episodeNumber) {
		int counter = 0;
		if (episodeNumber > 0) {
			int episodesLeft = episodeNumber;
			feedloop: for (Feed feed : feeds) {
				for (FeedItem item : feed.getItems()) {
					if (item.hasMedia() && item.getMedia().isDownloaded()) {
						if (!isInQueue(item) && item.isRead()) {
							deleteFeedMedia(context, item.getMedia());
							counter++;
							episodesLeft--;
							if (episodesLeft == 0) {
								break feedloop;
							}
						}
					}
				}
			}
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, String.format(
					"Auto-delete deleted %d episodes (%d requested)", counter,
					episodeNumber));

		return counter;
	}

	/**
	 * Counts items in the queue and the unread items list which haven't been
	 * downloaded yet.
	 */
	private int getNumberOfUndownloadedEpisodes() {
		int counter = 0;
		for (FeedItem item : queue) {
			if (item.hasMedia() && !item.getMedia().isDownloaded()) {
				counter++;
			}
		}
		for (FeedItem item : unreadItems) {
			if (item.hasMedia() && !item.getMedia().isDownloaded()) {
				counter++;
			}
		}
		return counter;

	}

	/** Counts all downloaded items. */
	private int getNumberOfDownloadedEpisodes() {
		int counter = 0;
		for (Feed feed : feeds) {
			for (FeedItem item : feed.getItems()) {
				if (item.hasMedia() && item.getMedia().isDownloaded()) {
					counter++;
				}
			}
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Number of downloaded episodes: " + counter);
		return counter;
	}

	/**
	 * Enqueues all items that are currently in the unreadItems list and marks
	 * them as 'read'.
	 */
	public void enqueueAllNewItems(final Context context) {
		if (!unreadItems.isEmpty()) {
			addQueueItem(context,
					unreadItems.toArray(new FeedItem[unreadItems.size()]));
			markAllItemsRead(context);
		}
	}

	/**
	 * Adds a feeditem to the queue at the specified index if it is not in the
	 * queue yet. The item is marked as 'read'.
	 */
	public void addQueueItemAt(final Context context, final FeedItem item,
			final int index) {
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				if (!queue.contains(item)) {
					queue.add(index, item);
					if (!item.isRead()) {
						markItemRead(context, item, true, false);
					}
				}
				eventDist.sendQueueUpdateBroadcast();

				dbExec.execute(new Runnable() {

					@Override
					public void run() {
						PodDBAdapter adapter = new PodDBAdapter(context);
						adapter.open();
						adapter.setQueue(queue);
						adapter.close();
					}
				});
				new Thread() {
					@Override
					public void run() {
						autodownloadUndownloadedItems(context);
					}
				}.start();
			}
		});

	}

	/**
	 * Adds FeedItems to the queue if they are not in the queue yet. The items
	 * are marked as 'read'.
	 */
	public void addQueueItem(final Context context, final FeedItem... items) {
		if (items.length > 0) {
			contentChanger.post(new Runnable() {

				@Override
				public void run() {
					for (FeedItem item : items) {
						if (!queue.contains(item)) {
							queue.add(item);
							if (!item.isRead()) {
								markItemRead(context, item, true, false);
							}
						}
					}
					eventDist.sendQueueUpdateBroadcast();
					dbExec.execute(new Runnable() {

						@Override
						public void run() {
							PodDBAdapter adapter = new PodDBAdapter(context);
							adapter.open();
							adapter.setQueue(queue);
							adapter.close();
						}
					});
					new Thread() {
						@Override
						public void run() {
							autodownloadUndownloadedItems(context);
						}
					}.start();
				}
			});
		}

	}

	/**
	 * Return the item that comes after this item in the queue or null if this
	 * item is not in the queue or if this item has no successor.
	 */
	public FeedItem getQueueSuccessorOfItem(FeedItem item) {
		if (isInQueue(item)) {
			int itemIndex = queue.indexOf(item);
			if (itemIndex != -1 && itemIndex < (queue.size() - 1)) {
				return queue.get(itemIndex + 1);
			}
		}
		return null;
	}

	/** Removes all items in queue */
	public void clearQueue(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Clearing queue");
		queue.clear();
		eventDist.sendQueueUpdateBroadcast();
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

	/** Removes a FeedItem from the queue. Uses external PodDBAdapter. */
	private void removeQueueItem(FeedItem item, PodDBAdapter adapter) {
		boolean removed = queue.remove(item);
		if (removed) {
			adapter.setQueue(queue);
		}
	}

	/** Removes a FeedItem from the queue. */
	public void removeQueueItem(final Context context, FeedItem item) {
		boolean removed = queue.remove(item);
		if (removed) {
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
		new Thread() {
			@Override
			public void run() {
				autodownloadUndownloadedItems(context);
			}
		}.start();
		eventDist.sendQueueUpdateBroadcast();
	}

	/**
	 * Moves the queue item at the specified index to another position. If the
	 * indices are out of range, no operation will be performed.
	 * 
	 * @param from
	 *            index of the item that is going to be moved
	 * @param to
	 *            destination index of item
	 * @param broadcastUpdate
	 *            true if the method should send a queue update broadcast after
	 *            the operation has been performed. This should be set to false
	 *            if the order of the queue is changed through drag & drop
	 *            reordering to avoid visual glitches.
	 */
	public void moveQueueItem(final Context context, int from, int to,
			boolean broadcastUpdate) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Moving queue item from index " + from + " to index "
					+ to);
		if (from >= 0 && from < queue.size() && to >= 0 && to < queue.size()) {
			FeedItem item = queue.remove(from);
			queue.add(to, item);
			dbExec.execute(new Runnable() {
				@Override
				public void run() {
					PodDBAdapter adapter = new PodDBAdapter(context);
					adapter.open();
					adapter.setQueue(queue);
					adapter.close();
				}
			});
			if (broadcastUpdate) {
				eventDist.sendQueueUpdateBroadcast();
			}
		}
	}

	/** Returns true if the specified item is in the queue. */
	public boolean isInQueue(FeedItem item) {
		return queue.contains(item);
	}

	/**
	 * Returns the FeedItem at the beginning of the queue or null if the queue
	 * is empty.
	 */
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
				eventDist.sendFeedUpdateBroadcast();
			}
		});
		setCompleteFeed(context, feed);
	}

	/**
	 * Updates an existing feed or adds it as a new one if it doesn't exist.
	 * 
	 * @return The saved Feed with a database ID
	 */
	public Feed updateFeed(final Context context, final Feed newFeed) {
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
			if (savedFeed.compareWithOther(newFeed)) {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"Feed has updated attribute values. Updating old feed's attributes");
				savedFeed.updateFromOther(newFeed);
			}
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
					markItemRead(context, item, false, false);
				} else {
					oldItem.updateFromOther(item);
				}
			}
			// update attributes
			savedFeed.setLastUpdate(newFeed.getLastUpdate());
			savedFeed.setType(newFeed.getType());
			setCompleteFeed(context, savedFeed);
			new Thread() {
				@Override
				public void run() {
					autodownloadUndownloadedItems(context);
				}
			}.start();
			return savedFeed;
		}

	}

	/** Get a Feed by its identifying value. */
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
	private void setFeed(Feed feed, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setFeed(feed);
			feed.cacheDescriptionsOfItems();
		} else {
			Log.w(TAG, "Adapter in setFeed was null");
		}
	}

	/** Updates Information of an existing Feeditem. Uses external adapter. */
	private void setFeedItem(FeedItem item, PodDBAdapter adapter) {
		if (adapter != null) {
			adapter.setSingleFeedItem(item);
		} else {
			Log.w(TAG, "Adapter in setFeedItem was null");
		}
	}

	/** Updates Information of an existing Feedimage. Uses external adapter. */
	private void setFeedImage(FeedImage image, PodDBAdapter adapter) {
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
	private void setFeedImage(FeedMedia media, PodDBAdapter adapter) {
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
				feed.cacheDescriptionsOfItems();
				adapter.close();
			}
		});

	}

	/**
	 * Updates Information of an existing Feed and its FeedItems. Creates and
	 * opens its own adapter.
	 */
	public void setCompleteFeed(final Context context, final Feed feed) {
		dbExec.execute(new Runnable() {

			@Override
			public void run() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				adapter.setCompleteFeed(feed);
				feed.cacheDescriptionsOfItems();
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

	/** Get a FeedItem by its id and the id of its feed. */
	public FeedItem getFeedItem(long itemId, long feedId) {
		Feed feed = getFeed(feedId);
		if (feed != null && feed.getItems() != null) {
			for (FeedItem item : feed.getItems()) {
				if (item.getId() == itemId) {
					return item;
				}
			}
		}
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

	/** Get a download status object from the download log by its FeedFile. */
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
		feeds.clear();
		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		extractFeedlistFromCursor(context, adapter);
		extractDownloadLogFromCursor(context, adapter);
		extractQueueFromCursor(context, adapter);
		adapter.close();
		Collections.sort(feeds, new FeedtitleComparator());
		Collections.sort(unreadItems, new FeedItemPubdateComparator());
		cleanupPlaybackHistory();
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

				item.id = itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_ID);
				item.setFeed(feed);
				item.setTitle(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_TITLE));
				item.setLink(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_LINK));
				item.setPubDate(new Date(itemlistCursor
						.getLong(PodDBAdapter.IDX_FI_SMALL_PUBDATE)));
				item.setPaymentLink(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_PAYMENT_LINK));
				long mediaId = itemlistCursor
						.getLong(PodDBAdapter.IDX_FI_SMALL_MEDIA);
				if (mediaId != 0) {
					mediaIds.add(String.valueOf(mediaId));
					item.setMedia(new FeedMedia(mediaId, item));
				}
				item.setRead((itemlistCursor
						.getInt(PodDBAdapter.IDX_FI_SMALL_READ) > 0) ? true
						: false);
				item.setItemIdentifier(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_ITEM_IDENTIFIER));
				if (item.getState() == FeedItem.State.NEW) {
					unreadItems.add(item);
				}

				// extract chapters
				boolean hasSimpleChapters = itemlistCursor
						.getInt(PodDBAdapter.IDX_FI_SMALL_HAS_CHAPTERS) > 0;
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
							case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
								chapter = new VorbisCommentChapter(start,
										title, item, link);
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
					if (playbackCompletionDate != null) {
						playbackHistory.add(item);
					}

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

	/**
	 * Loads description and contentEncoded values from the database and caches
	 * it in the feeditem. The task callback will contain a String-array with
	 * the description at index 0 and the value of contentEncoded at index 1.
	 */
	public void loadExtraInformationOfItem(final Context context,
			final FeedItem item, FeedManager.TaskCallback<String[]> callback) {
		if (AppConfig.DEBUG) {
			Log.d(TAG,
					"Loading extra information of item with id " + item.getId());
			if (item.getTitle() != null) {
				Log.d(TAG, "Title: " + item.getTitle());
			}
		}
		dbExec.execute(new FeedManager.Task<String[]>(new Handler(), callback) {

			@Override
			public void execute() {
				PodDBAdapter adapter = new PodDBAdapter(context);
				adapter.open();
				Cursor extraCursor = adapter.getExtraInformationOfItem(item);
				if (extraCursor.moveToFirst()) {
					String description = extraCursor
							.getString(PodDBAdapter.IDX_FI_EXTRA_DESCRIPTION);
					String contentEncoded = extraCursor
							.getString(PodDBAdapter.IDX_FI_EXTRA_CONTENT_ENCODED);
					item.setCachedDescription(description);
					item.setCachedContentEncoded(contentEncoded);
					setResult(new String[] { description, contentEncoded });
				}
				adapter.close();
			}
		});
	}

	/**
	 * Searches the descriptions of FeedItems of a specific feed for a given
	 * string.
	 * 
	 * @param feed
	 *            The feed whose items should be searched.
	 * @param query
	 *            The search string
	 * @param callback
	 *            A callback which will be used to return the search result
	 * */
	public void searchFeedItemDescription(final Context context,
			final Feed feed, final String query,
			FeedManager.QueryTaskCallback callback) {
		dbExec.execute(new FeedManager.QueryTask(context, new Handler(),
				callback) {

			@Override
			public void execute(PodDBAdapter adapter) {
				Cursor searchResult = adapter.searchItemDescriptions(feed,
						query);
				setResult(searchResult);
			}
		});
	}

	/**
	 * Searches the 'contentEncoded' field of FeedItems of a specific feed for a
	 * given string.
	 * 
	 * @param feed
	 *            The feed whose items should be searched.
	 * @param query
	 *            The search string
	 * @param callback
	 *            A callback which will be used to return the search result
	 * */
	public void searchFeedItemContentEncoded(final Context context,
			final Feed feed, final String query,
			FeedManager.QueryTaskCallback callback) {
		dbExec.execute(new FeedManager.QueryTask(context, new Handler(),
				callback) {

			@Override
			public void execute(PodDBAdapter adapter) {
				Cursor searchResult = adapter.searchItemContentEncoded(feed,
						query);
				setResult(searchResult);
			}
		});
	}

	/** Returns the number of feeds that are currently in the feeds list. */
	public int getFeedsSize() {
		return feeds.size();
	}

	/** Returns the feed at the specified index of the feeds list. */
	public Feed getFeedAtIndex(int index) {
		return feeds.get(index);
	}

	/** Returns an array that contains all feeds of the feed manager. */
	public Feed[] getFeedsArray() {
		return feeds.toArray(new Feed[feeds.size()]);
	}

	List<Feed> getFeeds() {
		return feeds;
	}

	/**
	 * Returns the number of items that are currently in the queue.
	 * 
	 * @param enableEpisodeFilter
	 *            true if items without episodes should be ignored by this
	 *            method if the episode filter was enabled by the user.
	 * */
	public int getQueueSize(boolean enableEpisodeFilter) {
		if (UserPreferences.isDisplayOnlyEpisodes() && enableEpisodeFilter) {
			return EpisodeFilter.countItemsWithEpisodes(queue);
		} else {
			return queue.size();
		}
	}

	/**
	 * Returns the FeedItem at the specified index of the queue.
	 * 
	 * @param enableEpisodeFilter
	 *            true if items without episodes should be ignored by this
	 *            method if the episode filter was enabled by the user.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if index is out of range
	 * */
	public FeedItem getQueueItemAtIndex(int index, boolean enableEpisodeFilter) {
		if (UserPreferences.isDisplayOnlyEpisodes() && enableEpisodeFilter) {
			return EpisodeFilter.accessEpisodeByIndex(queue, index);
		} else {
			return queue.get(index);
		}
	}

	/**
	 * Returns true if the first item in the queue is currently being played or
	 * false otherwise. If the queue is empty, this method will also return
	 * false.
	 * */
	public boolean firstQueueItemIsPlaying() {
		FeedManager manager = FeedManager.getInstance();
		int queueSize = manager.getQueueSize(true);
		if (queueSize == 0) {
			return false;
		} else {
			FeedItem item = getQueueItemAtIndex(0, true);
			return item.getState() == FeedItem.State.PLAYING;
		}
	}

	/**
	 * Returns the number of unread items.
	 * 
	 * @param enableEpisodeFilter
	 *            true if items without episodes should be ignored by this
	 *            method if the episode filter was enabled by the user.
	 * */
	public int getUnreadItemsSize(boolean enableEpisodeFilter) {
		if (UserPreferences.isDisplayOnlyEpisodes() && enableEpisodeFilter) {
			return EpisodeFilter.countItemsWithEpisodes(unreadItems);
		} else {
			return unreadItems.size();
		}
	}

	/**
	 * Returns the FeedItem at the specified index of the unread items list.
	 * 
	 * @param enableEpisodeFilter
	 *            true if items without episodes should be ignored by this
	 *            method if the episode filter was enabled by the user.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if index is out of range
	 * */
	public FeedItem getUnreadItemAtIndex(int index, boolean enableEpisodeFilter) {
		if (UserPreferences.isDisplayOnlyEpisodes() && enableEpisodeFilter) {
			return EpisodeFilter.accessEpisodeByIndex(unreadItems, index);
		} else {
			return unreadItems.get(index);
		}
	}

	/**
	 * Returns the number of items in the playback history.
	 * */
	public int getPlaybackHistorySize() {
		return playbackHistory.size();
	}

	/**
	 * Returns the FeedItem at the specified index of the playback history.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if index is out of range
	 * */
	public FeedItem getPlaybackHistoryItemIndex(int index) {
		return playbackHistory.get(index);
	}

	/** Returns the number of items in the download log */
	public int getDownloadLogSize() {
		return downloadLog.size();
	}

	/** Returns the download status at the specified index of the download log. */
	public DownloadStatus getDownloadStatusFromLogAtIndex(int index) {
		return downloadLog.get(index);
	}

	/** Is called by a FeedManagerTask after completion. */
	public interface TaskCallback<V> {
		void onCompletion(V result);
	}

	/** Is called by a FeedManager.QueryTask after completion. */
	public interface QueryTaskCallback {
		void handleResult(Cursor result);

		void onCompletion();
	}

	/** A runnable that can post a callback to a handler after completion. */
	abstract class Task<V> implements Runnable {
		private Handler handler;
		private TaskCallback<V> callback;
		private V result;

		/**
		 * Standard contructor. No callbacks are going to be posted to a
		 * handler.
		 */
		public Task() {
			super();
		}

		/**
		 * The Task will post a Runnable to 'handler' that will execute the
		 * 'callback' after completion.
		 */
		public Task(Handler handler, TaskCallback<V> callback) {
			super();
			this.handler = handler;
			this.callback = callback;
		}

		@Override
		public final void run() {
			execute();
			if (handler != null && callback != null) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						callback.onCompletion(result);
					}
				});
			}
		}

		/** This method will be executed in the same thread as the run() method. */
		public abstract void execute();

		public void setResult(V result) {
			this.result = result;
		}
	}

	/**
	 * A runnable which should be used for database queries. The onCompletion
	 * method is executed on the database executor to handle Cursors correctly.
	 * This class automatically creates a PodDBAdapter object and closes it when
	 * it is no longer in use.
	 */
	abstract class QueryTask implements Runnable {
		private QueryTaskCallback callback;
		private Cursor result;
		private Context context;
		private Handler handler;

		public QueryTask(Context context, Handler handler,
				QueryTaskCallback callback) {
			this.callback = callback;
			this.context = context;
			this.handler = handler;
		}

		@Override
		public final void run() {
			PodDBAdapter adapter = new PodDBAdapter(context);
			adapter.open();
			execute(adapter);
			callback.handleResult(result);
			if (result != null && !result.isClosed()) {
				result.close();
			}
			adapter.close();
			if (handler != null && callback != null) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						callback.onCompletion();
					}

				});
			}
		}

		public abstract void execute(PodDBAdapter adapter);

		protected void setResult(Cursor c) {
			result = c;
		}
	}

}