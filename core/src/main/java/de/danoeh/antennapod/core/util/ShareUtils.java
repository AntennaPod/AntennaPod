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
	
	public static void shareLink(Context context, String subject, String text) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, subject);
		i.putExtra(Intent.EXTRA_TEXT, text);
		context.startActivity(Intent.createChooser(i, "Share URL"));
	}

	public static void shareFeedlink(Context context, Feed feed) {
		String subject = context.getString(R.string.share_link_label);
		shareLink(context, subject, feed.getLink());
	}
	
	public static void shareFeedDownloadLink(Context context, Feed feed) {
		String subject = context.getString(R.string.share_feed_url_label);
		shareLink(context, subject, feed.getDownload_url());
	}

	public static void shareFeedItemLink(Context context, FeedItem item) {
		shareFeedItemLink(context, item, false);
	}

	public static void shareFeedItemDownloadLink(Context context, FeedItem item) {
		shareFeedItemDownloadLink(context, item, false);
	}

	public static void shareFeedItemLink(Context context, FeedItem item, boolean withPosition) {
		String subject;
		String text;
		if(withPosition) {
			subject = context.getString(R.string.share_link_with_position_label);
			int pos = item.getMedia().getPosition();
			text = item.getLink() + " [" + Converter.getDurationStringLong(pos) + "]";
		} else {
			subject = context.getString(R.string.share_link_label);
			text = item.getLink();
		}
		shareLink(context, subject, text);
	}

	public static void shareFeedItemDownloadLink(Context context, FeedItem item, boolean withPosition) {
		String subject;
		String text;
		if(withPosition) {
			subject = context.getString(R.string.share_item_url_with_position_label);
			int pos = item.getMedia().getPosition();
			text = item.getMedia().getDownload_url() + " [" + Converter.getDurationStringLong(pos) + "]";
		} else {
			subject = context.getString(R.string.share_item_url_label);
			text = item.getMedia().getDownload_url();
		}
		shareLink(context, subject, text);
	}

}
