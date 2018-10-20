package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;

public class SearchResultValueComparator implements Comparator<SearchResult> {

	/**
	 * Compare items based, first, on where they were found (ie. title, chapters, or show notes).
	 * If they were found in the same section, then compare based on the title, in lexicographic
	 * order. This is still not ideal since, for example, "#12 Example A" would be considered
	 * before "#8 Example B" due to the fact that "8" has a larger unicode value than "1"
     */
	@Override
	public int compare(SearchResult lhs, SearchResult rhs) {
		int value = rhs.getValue() - lhs.getValue();
		if (value == 0 && lhs.getComponent() instanceof FeedItem && rhs.getComponent() instanceof  FeedItem) {
			String lhsTitle = ((FeedItem) lhs.getComponent()).getTitle();
			String rhsTitle = ((FeedItem) rhs.getComponent()).getTitle();
			return lhsTitle.compareTo(rhsTitle);
		}
		return value;
	}

}
