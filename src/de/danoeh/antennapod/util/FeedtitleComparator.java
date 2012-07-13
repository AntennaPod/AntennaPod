package de.danoeh.antennapod.util;

import java.util.Comparator;

import de.danoeh.antennapod.feed.Feed;

/** Compares the title of two feeds for sorting. */
public class FeedtitleComparator implements Comparator<Feed> {

	@Override
	public int compare(Feed lhs, Feed rhs) {
		return lhs.getTitle().compareTo(rhs.getTitle());
	}

}
