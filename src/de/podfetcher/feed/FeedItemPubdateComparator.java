package de.podfetcher.feed;

import java.util.Comparator;

/** Compares the pubDate of two FeedItems */
public class FeedItemPubdateComparator implements Comparator<FeedItem> {

	@Override
	public int compare(FeedItem lhs, FeedItem rhs) {
		long diff = lhs.getPubDate().getTime() - rhs.getPubDate().getTime();
		return (int) Math.signum(diff);
	}

}
