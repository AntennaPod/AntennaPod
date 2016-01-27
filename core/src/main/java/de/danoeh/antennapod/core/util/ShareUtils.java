package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

/** Utility methods for sharing data */
public class ShareUtils {
	private static final String TAG = "ShareUtils";
	
	private ShareUtils() {}
	
	public static void shareLink(Context context, String text) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, text);
		context.startActivity(Intent.createChooser(i, context.getString(R.string.share_url_label)));
	}

	public static void shareFeedlink(Context context, Feed feed) {
		shareLink(context, feed.getTitle() + ": " + feed.getLink());
	}
	
	public static void shareFeedDownloadLink(Context context, Feed feed) {
		shareLink(context, feed.getTitle() + ": " + feed.getDownload_url());
	}

	public static void shareFeedItemLink(Context context, FeedItem item) {
		shareFeedItemLink(context, item, false);
	}

	public static void shareFeedItemDownloadLink(Context context, FeedItem item) {
		shareFeedItemDownloadLink(context, item, false);
	}

	private static String getItemShareText(FeedItem item) {
		return item.getFeed().getTitle() + ": " + item.getTitle();
	}

	public static void shareFeedItemLink(Context context, FeedItem item, boolean withPosition) {
		String text = getItemShareText(item) + " " + item.getLink();
		if(withPosition) {
			int pos = item.getMedia().getPosition();
			text = item.getLink() + " [" + Converter.getDurationStringLong(pos) + "]";
		}
		shareLink(context, text);
	}

	public static void shareFeedItemDownloadLink(Context context, FeedItem item, boolean withPosition) {
		String text = getItemShareText(item) + " " + item.getMedia().getDownload_url();
		if(withPosition) {
			int pos = item.getMedia().getPosition();
			text += " [" + Converter.getDurationStringLong(pos) + "]";
		}
		shareLink(context, text);
	}

}
