package de.danoeh.antennapod.feed;

import java.util.ArrayList;

import android.util.Log;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

/** Performs search on Feeds and FeedItems */
public class FeedSearcher {
	private static final String TAG = "FeedSearcher";

	/** Performs a search in all feeds or one specific feed. */
	public static ArrayList<SearchResult> performSearch(final String query,
			Feed selectedFeed) {
		String lcQuery = query.toLowerCase();
		ArrayList<SearchResult> result = new ArrayList<SearchResult>();
		if (selectedFeed == null) {
			if (AppConfig.DEBUG) Log.d(TAG, "Performing global search");
			if (AppConfig.DEBUG)
				Log.d(TAG, "Searching Feed titles");
			searchFeedtitles(lcQuery, result);
		} else if (AppConfig.DEBUG) {
			Log.d(TAG, "Performing search on specific feed");
		}

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching Feeditem titles");
		searchFeedItemTitles(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item-chaptertitles");
		searchFeedItemChapters(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item descriptions");
		searchFeedItemDescription(lcQuery, result, selectedFeed);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Searching item content encoded data");
		searchFeedItemContentEncoded(lcQuery, result, selectedFeed);

		return result;
	}

	private static void searchFeedtitles(String query,
			ArrayList<SearchResult> destination) {
		FeedManager manager = FeedManager.getInstance();
		for (Feed feed : manager.getFeeds()) {
			if (feed.getTitle().toLowerCase().contains(query)) {
				destination.add(new SearchResult(feed, null));
			}
		}
	}

	private static void searchFeedItemTitles(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemTitlesSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemTitlesSingleFeed(query, destination, selectedFeed);
		}
	}

	private static void searchFeedItemTitlesSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getTitle().toLowerCase().contains(query)) {
				destination.add(new SearchResult(item, PodcastApp.getInstance()
						.getString(R.string.found_in_label)
						+ item.getFeed().getTitle()));
			}
		}
	}

	private static void searchFeedItemChapters(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemChaptersSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemChaptersSingleFeed(query, destination, selectedFeed);
		}
	}

	private static void searchFeedItemChaptersSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getSimpleChapters() != null) {
				for (SimpleChapter sc : item.getSimpleChapters()) {
					if (sc.getTitle().toLowerCase().contains(query)) {
						destination.add(new SearchResult(item, PodcastApp
								.getInstance().getString(
										R.string.found_in_chapters_label)));
					}
				}
			}
		}
	}

	private static void searchFeedItemDescription(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemDescriptionSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemDescriptionSingleFeed(query, destination,
					selectedFeed);
		}
	}

	private static void searchFeedItemDescriptionSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getDescription() != null
					&& item.getDescription().toLowerCase().contains(query)) {
				destination.add(new SearchResult(item, PodcastApp.getInstance()
						.getString(R.string.found_in_shownotes_label)));
			}
		}
	}

	private static void searchFeedItemContentEncoded(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
		FeedManager manager = FeedManager.getInstance();
		if (selectedFeed == null) {
			for (Feed feed : manager.getFeeds()) {
				searchFeedItemContentEncodedSingleFeed(query, destination, feed);
			}
		} else {
			searchFeedItemContentEncodedSingleFeed(query, destination, selectedFeed);
		}
	}

	private static void searchFeedItemContentEncodedSingleFeed(String query,
			ArrayList<SearchResult> destination, Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (!destination.contains(item) && item.getContentEncoded() != null
					&& item.getContentEncoded().toLowerCase().contains(query)) {
				destination.add(new SearchResult(item, PodcastApp.getInstance()
						.getString(R.string.found_in_shownotes_label)));
			}
		}
	}

}
