package de.danoeh.antennapod.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.feed.SearchResult;

public class SearchResultValueComparator implements Comparator<SearchResult> {

	@Override
	public int compare(SearchResult lhs, SearchResult rhs) {
		return rhs.getValue() - lhs.getValue();
	}

}
