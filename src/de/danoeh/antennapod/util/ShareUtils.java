package de.danoeh.antennapod.util;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;

/** Utility methods for sharing data */
public class ShareUtils {
	private static final String TAG = "ShareUtils";
	
	private ShareUtils() {}
	
	private static void shareLink(Context context, String link) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL");
		i.putExtra(Intent.EXTRA_TEXT, link);
		context.startActivity(Intent.createChooser(i, "Share URL"));
	}
	
	public static void shareFeedItemLink(Context context, FeedItem item) {
		shareLink(context, item.getLink());
	}
	
	public static void shareFeedDownloadLink(Context context, Feed feed) {
		shareLink(context, feed.getDownload_url());
	}
	
	public static void shareFeedlink(Context context, Feed feed) {
		shareLink(context, feed.getLink());
	}

}
