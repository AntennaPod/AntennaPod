package de.podfetcher.util;

import java.util.Comparator;

import de.podfetcher.feed.FeedItem;

/** Compares the pubDate of two FeedItems for sorting*/
public class FeedItemPubdateComparator implements Comparator<FeedItem> {

	@Override
	public int compare(FeedItem lhs, FeedItem rhs) {
		return -lhs.getPubDate().compareTo(rhs.getPubDate());
	}

}
