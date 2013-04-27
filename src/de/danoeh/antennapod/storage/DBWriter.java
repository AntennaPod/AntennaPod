package de.danoeh.antennapod.storage;

import android.content.Context;

public class DBWriter {
	private static final String TAG = "DBWriter";
	
	private DBWriter() {
	}

	public static boolean deleteFeedMedia(final Context context,
			final long mediaId) {
		return false;
	}

	public static void deleteFeed(final Context context, final long feedId) {

	}

	public static void clearPlaybackHistory(final Context context) {

	}

	public static void addItemToPlaybackHistory(final Context context,
			long itemId) {

	}

	private static void removeItemFromPlaybackHistory(final Context context,
			long itemId) {

	}

	public static void addDownloadStatus(final Context context,
			final long statusId) {

	}

	public static void addQueueItemAt(final Context context, final long itemId,
			final int index, final boolean performAutoDownload) {

	}

	public static void addQueueItem(final Context context,
			final long... itemIds) {
	}

	public static void clearQueue(final Context context) {
	}

	public static void removeQueueItem(final Context context,
			final long itemId, final boolean performAutoDownload) {

	}

	public void moveQueueItem(final Context context, int from, int to,
			boolean broadcastUpdate) {
	}

	void addNewFeed(final Context context, final long feedId) {

	}
}
