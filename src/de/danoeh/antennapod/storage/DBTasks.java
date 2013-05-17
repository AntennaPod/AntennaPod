package de.danoeh.antennapod.storage;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.util.exception.MediaFileNotFoundException;

public final class DBTasks {
	private static final String TAG = "DBTasks";

	private DBTasks() {
	}

	public static void playMedia(final Context context, final FeedMedia media,
			boolean showPlayer, boolean startWhenPrepared, boolean shouldStream) {
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
			DBWriter.addQueueItemAt(context, media.getItem().getId(), 0, false);
		} catch (MediaFileNotFoundException e) {
			e.printStackTrace();
			if (media.isPlaying()) {
				context.sendBroadcast(new Intent(
						PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
			}
			notifyMissingFeedMediaFile(context, media);
		}
	}

	public static void refreshAllFeeds(final Context context) {
	}

	public void refreshExpiredFeeds(final Context context) {
	}

	public static void notifyInvalidImageFile(final Context context,
			final long imageId) {
	}

	public static void notifyMissingFeedMediaFile(final Context context,
			final FeedMedia media) {
	}

	public static void downloadAllItemsInQueue(final Context context) {
	}

	public static void refreshFeed(final Context context, final long feedId) {
	}

	public static void downloadFeedItem(final Context context, long... itemIds) {
	}

	static void downloadFeedItem(boolean performAutoCleanup,
			final Context context, final long... itemIds)
			throws DownloadRequestException {
	}

	public static void autodownloadUndownloadedItems(Context context) {
	}

	private static int getPerformAutoCleanupArgs(final int episodeNumber) {
		return 0;
	}

	public static void performAutoCleanup(final Context context) {
	}

	private static int performAutoCleanup(final Context context,
			final int episodeNumber) {
		return 0;
	}

	public static void enqueueAllNewItems(final Context context) {
		long[] unreadItems = DBReader.getUnreadItemIds(context);
		DBWriter.addQueueItem(context, unreadItems);
	}

	public static FeedItem getQueueSuccessorOfItem(Context context,
			final long itemId) {
		FeedItem result = null;
		List<FeedItem> queue = DBReader.getQueue(context);
		if (queue != null) {
			Iterator<FeedItem> iterator = queue.iterator();
			while (iterator.hasNext()) {
				FeedItem item = iterator.next();
				if (item.getId() == itemId) {
					if (iterator.hasNext()) {
						result = iterator.next();
					}
					break;
				}
			}
		}
		return result;
	}

	private static Feed searchFeedByIdentifyingValue(Context context,
			String identifier) {
		List<Feed> feeds = DBReader.getFeedList(context);
		for (Feed feed : feeds) {
			if (feed.getIdentifyingValue().equals(identifier)) {
				return feed;
			}
		}
		return null;
	}
	
	/** Get a FeedItem by its identifying value. */
	private static FeedItem searchFeedItemByIdentifyingValue(Feed feed,
			String identifier) {
		for (FeedItem item : feed.getItems()) {
			if (item.getIdentifyingValue().equals(identifier)) {
				return item;
			}
		}
		return null;
	}


	public static synchronized Feed updateFeed(final Context context, final Feed newFeed) {
		// Look up feed in the feedslist
		final Feed savedFeed = searchFeedByIdentifyingValue(context,
				newFeed.getIdentifyingValue());
		if (savedFeed == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"Found no existing Feed with title "
								+ newFeed.getTitle() + ". Adding as new one.");
			// Add a new Feed
			DBWriter.addNewFeed(context, newFeed);
			return newFeed;
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Feed with title " + newFeed.getTitle()
						+ " already exists. Syncing new with existing one.");
			
			savedFeed.setItems(DBReader.getFeedItemList(context, savedFeed));
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
					savedFeed.getItems().add(i, item);
					DBWriter.markItemRead(context, item.getId(), false);
				} else {
					oldItem.updateFromOther(item);
				}
			}
			// update attributes
			savedFeed.setLastUpdate(newFeed.getLastUpdate());
			savedFeed.setType(newFeed.getType());
			DBWriter.setCompleteFeed(context, savedFeed);
			new Thread() {
				@Override
				public void run() {
					autodownloadUndownloadedItems(context);
				}
			}.start();
			return savedFeed;
		}

	}

}
