package de.danoeh.antennapod.storage;

import java.util.List;

import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;

public final class DBReader {
	private static final String TAG = "DBReader";
	
	private DBReader() {
	}

	public static List<Feed> getFeedList() {
		return null;
	}

	public static List<FeedItem> getFeedItemList(long feedId) {
		return null;
	}

	public static List<FeedItem> getQueue() {
		return null;
	}

	public static List<FeedItem> getUnreadItemsList() {
		return null;
	}

	public static List<FeedItem> getPlaybackHistory() {
		return null;
	}

	public static List<DownloadStatus> getDownloadLog() {
		return null;
	}

	public static Feed getFeed(long feedId) {
		return null;
	}

	public FeedItem getFeedItem(long itemId) {
		return null;
	}

	public FeedMedia getFeedMedia(long mediaId) {
		return null;
	}

	public static FeedItem getFirstQueueItem() {
		return null;
	}

}
