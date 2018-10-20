package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;

/** Compares the pubDate of two FeedItems for sorting*/
public class FeedItemPubdateComparator implements Comparator<FeedItem> {

	/** Returns a new instance of this comparator in reverse order. 
	public static FeedItemPubdateComparator newInstance() {
		FeedItemPubdateComparator
	}*/
	@Override
	public int compare(FeedItem lhs, FeedItem rhs) {
		return rhs.getPubDate().compareTo(lhs.getPubDate());
	}

}
