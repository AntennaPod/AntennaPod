package de.danoeh.antennapod.core.util;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.Feed;

/** Compares the title of two feeds for sorting. */
class FeedtitleComparator implements Comparator<Feed> {

	@Override
	public int compare(Feed lhs, Feed rhs) {
		return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
	}

}
