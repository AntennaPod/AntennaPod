package de.danoeh.antennapod.util;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.feed.FeedItem;

public class EpisodeFilter {
	private EpisodeFilter() {
		
	}
	
	/** Return a copy of the itemlist without items which have no media. */
	public static ArrayList<FeedItem> getEpisodeList(List<FeedItem> items) {
		ArrayList<FeedItem> episodes = new ArrayList<FeedItem>(items);
		for (FeedItem item : items) {
			if (item.getMedia() == null) {
				episodes.remove(item);
			}
		}
		return episodes;
	}
}
