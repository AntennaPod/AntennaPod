package de.danoeh.antennapod.storage;

import android.content.Context;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;

public final class DBTasks {
	private static final String TAG = "DBTasks";
	
	private DBTasks() {
	}

	public static void playMedia(final Context context, final long mediaId,
			boolean showPlayer, boolean startWhenPrepared, boolean shouldStream) {

	}

	public static void markItemRead(final Context context, final long itemId,
			final boolean read, boolean resetMediaPosition) {
	}

	public static void markFeedRead(final Context context, final long feedId) {

	}

	public static void markAllItemsRead(final Context context) {
	}

	public static void refreshAllFeeds(final Context context) {
	}

	public void refreshExpiredFeeds(final Context context) {
	}

	public static void notifyInvalidImageFile(final Context context,
			final long imageId) {
	}

	public static void notifyMissingFeedMediaFile(final Context context,
			final long mediaId) {
	}
	
	public static void downloadAllItemsInQueue(final Context context) {}

	public static void refreshFeed(final Context context, final long feedId) {
	}
	
	public static void downloadFeedItem(final Context context, long... itemIds) {}
	
	static void downloadFeedItem(boolean performAutoCleanup,
			final Context context, final long... itemIds)
			throws DownloadRequestException {}

	public static void autodownloadUndownloadedItems(Context context) {
	}
	
	private static int getPerformAutoCleanupArgs(final int episodeNumber) {
		return 0;
	}
	
	public static void performAutoCleanup(final Context context) {}
	
	private static int performAutoCleanup(final Context context, final int episodeNumber) {
		return 0;
	}

	public static void enqueueAllNewItems(final Context context) {}

	public static FeedItem getQueueSuccessorOfItem(final long itemId) {
		return null;
	}
	
	public static Feed updateFeed(final Context context, final long feedId) {
		return null;
	}

}
