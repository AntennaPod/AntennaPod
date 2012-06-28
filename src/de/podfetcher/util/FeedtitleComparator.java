package de.podfetcher.util;

import java.util.Comparator;

import de.podfetcher.feed.Feed;

/** Compares the title of two feeds for sorting. */
public class FeedtitleComparator implements Comparator<Feed> {

	@Override
	public int compare(Feed lhs, Feed rhs) {
		return lhs.getTitle().compareTo(rhs.getTitle());
	}

}
