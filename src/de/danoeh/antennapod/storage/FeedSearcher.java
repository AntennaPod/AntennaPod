package de.danoeh.antennapod.storage;

import android.content.Context;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.SearchResult;
import de.danoeh.antennapod.util.comparator.SearchResultValueComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Performs search on Feeds and FeedItems
 */
public class FeedSearcher {
    private static final String TAG = "FeedSearcher";


    /**
     * Performs a search in all feeds or one specific feed.
     */
    public static List<SearchResult> performSearch(final Context context,
                                                   final String query, final long selectedFeed) {
        final int values[] = {0, 0, 1, 2};
        final String[] subtitles = {context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_chapters_label),
                context.getString(R.string.found_in_title_label)};

        List<SearchResult> result = new ArrayList<SearchResult>();

        FutureTask<List<FeedItem>>[] tasks = new FutureTask[4];
        (tasks[0] = DBTasks.searchFeedItemContentEncoded(context, selectedFeed, query)).run();
        (tasks[1] = DBTasks.searchFeedItemDescription(context, selectedFeed, query)).run();
        (tasks[2] = DBTasks.searchFeedItemChapters(context, selectedFeed, query)).run();
        (tasks[3] = DBTasks.searchFeedItemTitle(context, selectedFeed, query)).run();
        try {
            for (int i = 0; i < tasks.length; i++) {
                FutureTask task = tasks[i];
                List<FeedItem> items = (List<FeedItem>) task.get();
                for (FeedItem item : items) {
                    result.add(new SearchResult(item, values[i], subtitles[i]));
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        Collections.sort(result, new SearchResultValueComparator());
        return result;
    }
  /*
    *//** Performs a search in all feeds or one specific feed. *//*
	public static ArrayList<SearchResult> performSearch(final Context context,
			final String query, final Feed selectedFeed) {
		final String lcQuery = query.toLowerCase();
		final ArrayList<SearchResult> result = new ArrayList<SearchResult>();
		if (selectedFeed == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Performing global search");
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

		Looper.prepare();
		DBTasks.searchFeedItemDescription(context, selectedFeed, lcQuery,
                new DBTasks.QueryTaskCallback() {

                    @Override
                    public void handleResult(Cursor cResult) {
                        searchFeedItemContentEncodedCursor(context, lcQuery, result,
                                selectedFeed, cResult);

                    }

                    @Override
                    public void onCompletion() {
                        DBTasks.searchFeedItemContentEncoded(context,
                                selectedFeed, lcQuery,
                                new DBTasks.QueryTaskCallback() {

                                    @Override
                                    public void handleResult(Cursor cResult) {
                                        searchFeedItemDescriptionCursor(context,
                                                lcQuery, result, selectedFeed,
                                                cResult);
                                    }

                                    @Override
                                    public void onCompletion() {
                                        Looper.myLooper().quit();
                                    }
                                });
                    }
                });

		Looper.loop();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Sorting results");
		Collections.sort(result, new SearchResultValueComparator());

		return result;
	}

	private static void searchFeedtitles(String query,
			ArrayList<SearchResult> destination) {
		FeedManager manager = FeedManager.getInstance();
		for (Feed feed : manager.getFeeds()) {
			SearchResult result = createSearchResult(feed, query, feed
					.getTitle().toLowerCase(), VALUE_FEED_TITLE);
			if (result != null) {
				destination.add(result);
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
			SearchResult result = createSearchResult(item, query, item
					.getTitle().toLowerCase(), VALUE_ITEM_TITLE);
			if (result != null) {
				result.setSubtitle(PodcastApp.getInstance().getString(
						R.string.found_in_title_label));
				destination.add(result);
			}

		}
	}

	private static void searchFeedItemChapters(String query,
			ArrayList<SearchResult> destination, Feed selectedFeed) {
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
			if (item.getChapters() != null) {
				for (Chapter sc : item.getChapters()) {
					SearchResult result = createSearchResult(item, query, sc
							.getTitle().toLowerCase(), VALUE_ITEM_CHAPTER);
					if (result != null) {
						result.setSubtitle(PodcastApp.getInstance().getString(
								R.string.found_in_chapters_label));
						destination.add(result);
					}
				}
			}
		}
	}

	private static void searchFeedItemDescriptionCursor(Context context, String query,
			ArrayList<SearchResult> destination, Feed feed, Cursor cursor) {
		FeedManager manager = FeedManager.getInstance();
        List<FeedItem> items = DBReader.extractItemlistFromCursor(cursor);
        if (cursor.moveToFirst()) {
			do {
				final long itemId = cursor
						.getLong(PodDBAdapter.IDX_FI_EXTRA_ID);
				String content = cursor
						.getString(PodDBAdapter.IDX_FI_EXTRA_DESCRIPTION);
				if (content != null) {
					content = content.toLowerCase();
					final long feedId = cursor
							.getLong(PodDBAdapter.IDX_FI_EXTRA_FEED);
					FeedItem item = null;
					if (feed == null) {
						item = manager.getFeedItem(itemId, feedId);
					} else {
						item = manager.getFeedItem(itemId, feed);
					}
					if (item != null) {
						SearchResult searchResult = createSearchResult(item,
								query, content, VALUE_ITEM_DESCRIPTION);
						if (searchResult != null) {
							searchResult.setSubtitle(PodcastApp.getInstance()
									.getString(
											R.string.found_in_shownotes_label));
							destination.add(searchResult);

						}
					}
				}

			} while (cursor.moveToNext());
		}
	}

	private static void searchFeedItemContentEncodedCursor(Context context, String query,
			ArrayList<SearchResult> destination, Feed feed, Cursor cursor) {
		if (cursor.moveToFirst()) {
			do {
				final long itemId = cursor
						.getLong(PodDBAdapter.IDX_FI_EXTRA_ID);
				String content = cursor
						.getString(PodDBAdapter.IDX_FI_EXTRA_CONTENT_ENCODED);
				if (content != null) {
					content = content.toLowerCase();

					final long feedId = cursor
							.getLong(PodDBAdapter.IDX_FI_EXTRA_FEED);
					FeedItem item = null;
					if (feed == null) {
						item = manager.getFeedItem(itemId, feedId);
					} else {
						item = manager.getFeedItem(itemId, feed);
					}
					if (item != null) {
						SearchResult searchResult = createSearchResult(item,
								query, content, VALUE_ITEM_DESCRIPTION);
						if (searchResult != null) {
							searchResult.setSubtitle(PodcastApp.getInstance()
									.getString(
											R.string.found_in_shownotes_label));
							destination.add(searchResult);
						}
					}
				}
			} while (cursor.moveToNext());
		}
	}

	private static SearchResult createSearchResult(FeedComponent component,
			String query, String text, int baseValue) {
		int bonus = 0;
		boolean found = false;
		// try word search
		Pattern word = Pattern.compile("\b" + query + "\b");
		Matcher matcher = word.matcher(text);
		found = matcher.find();
		if (found) {
			bonus = VALUE_WORD_MATCH;
		} else {
			// search for other occurence
			found = text.contains(query);
		}

		if (found) {
			return new SearchResult(component, baseValue + bonus);
		} else {
			return null;
		}
	}*/

}
