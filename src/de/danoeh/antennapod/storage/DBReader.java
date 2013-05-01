package de.danoeh.antennapod.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.ID3Chapter;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.feed.VorbisCommentChapter;
import de.danoeh.antennapod.util.comparator.DownloadStatusComparator;
import de.danoeh.antennapod.util.comparator.FeedItemPubdateComparator;

public final class DBReader {
	private static final String TAG = "DBReader";

	private DBReader() {
	}

	public static List<Feed> getFeedList(final Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting Feedlist");

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();

		Cursor feedlistCursor = adapter.getAllFeedsCursor();
		List<Feed> feeds = new ArrayList<Feed>(feedlistCursor.getCount());

		if (feedlistCursor.moveToFirst()) {
			do {
				Feed feed = extractFeedFromCursorRow(adapter, feedlistCursor);
				feeds.add(feed);
			} while (feedlistCursor.moveToNext());
		}
		feedlistCursor.close();
		return feeds;
	}

	public static void loadFeedDataOfFeedItemlist(Context context,
			List<FeedItem> items) {
		List<Feed> feeds = getFeedList(context);
		for (FeedItem item : items) {
			for (Feed feed : feeds) {
				if (feed.getId() == item.getFeedId()) {
					item.setFeed(feed);
					break;
				}
			}
		}
	}

	public static List<FeedItem> getFeedItemList(Context context,
			final Feed feed) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting Feeditems of feed " + feed.getTitle());

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();

		Cursor itemlistCursor = adapter.getAllItemsOfFeedCursor(feed);
		List<FeedItem> items = extractItemlistFromCursor(adapter,
				itemlistCursor);
		itemlistCursor.close();

		Collections.sort(items, new FeedItemPubdateComparator());

		adapter.close();

		for (FeedItem item : items) {
			item.setFeed(feed);
		}

		return items;
	}

	private static List<FeedItem> extractItemlistFromCursor(
			PodDBAdapter adapter, Cursor itemlistCursor) {
		ArrayList<String> mediaIds = new ArrayList<String>();
		List<FeedItem> items = new ArrayList<FeedItem>(
				itemlistCursor.getCount());

		if (itemlistCursor.moveToFirst()) {
			do {
				FeedItem item = new FeedItem();

				item.setId(itemlistCursor.getLong(PodDBAdapter.IDX_FI_SMALL_ID));
				item.setTitle(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_TITLE));
				item.setLink(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_LINK));
				item.setPubDate(new Date(itemlistCursor
						.getLong(PodDBAdapter.IDX_FI_SMALL_PUBDATE)));
				item.setPaymentLink(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_PAYMENT_LINK));
				item.setFeedId(itemlistCursor
						.getLong(PodDBAdapter.IDX_FI_SMALL_FEED));
				long mediaId = itemlistCursor
						.getLong(PodDBAdapter.IDX_FI_SMALL_MEDIA);
				if (mediaId != 0) {
					mediaIds.add(String.valueOf(mediaId));
					item.setMedia(new FeedMedia(mediaId, item));
				}
				item.setRead((itemlistCursor
						.getInt(PodDBAdapter.IDX_FI_SMALL_READ) > 0) ? true
						: false);
				item.setItemIdentifier(itemlistCursor
						.getString(PodDBAdapter.IDX_FI_SMALL_ITEM_IDENTIFIER));

				// extract chapters
				boolean hasSimpleChapters = itemlistCursor
						.getInt(PodDBAdapter.IDX_FI_SMALL_HAS_CHAPTERS) > 0;
				if (hasSimpleChapters) {
					Cursor chapterCursor = adapter
							.getSimpleChaptersOfFeedItemCursor(item);
					if (chapterCursor.moveToFirst()) {
						item.setChapters(new ArrayList<Chapter>());
						do {
							int chapterType = chapterCursor
									.getInt(PodDBAdapter.KEY_CHAPTER_TYPE_INDEX);
							Chapter chapter = null;
							long start = chapterCursor
									.getLong(PodDBAdapter.KEY_CHAPTER_START_INDEX);
							String title = chapterCursor
									.getString(PodDBAdapter.KEY_TITLE_INDEX);
							String link = chapterCursor
									.getString(PodDBAdapter.KEY_CHAPTER_LINK_INDEX);

							switch (chapterType) {
							case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
								chapter = new SimpleChapter(start, title, item,
										link);
								break;
							case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
								chapter = new ID3Chapter(start, title, item,
										link);
								break;
							case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
								chapter = new VorbisCommentChapter(start,
										title, item, link);
								break;
							}
							chapter.setId(chapterCursor
									.getLong(PodDBAdapter.KEY_ID_INDEX));
							item.getChapters().add(chapter);
						} while (chapterCursor.moveToNext());
					}
					chapterCursor.close();
				}
				items.add(item);
			} while (itemlistCursor.moveToNext());
		}

		extractMediafromItemlist(adapter, items, mediaIds);
		Collections.sort(items, new FeedItemPubdateComparator());
		return items;
	}

	private static void extractMediafromItemlist(PodDBAdapter adapter,
			List<FeedItem> items, ArrayList<String> mediaIds) {

		List<FeedItem> itemsCopy = new ArrayList<FeedItem>(items);
		Cursor cursor = adapter.getFeedMediaCursor(mediaIds
				.toArray(new String[mediaIds.size()]));
		if (cursor.moveToFirst()) {
			do {
				long mediaId = cursor.getLong(PodDBAdapter.KEY_ID_INDEX);
				// find matching feed item
				FeedItem item = getMatchingItemForMedia(mediaId, itemsCopy);
				itemsCopy.remove(item);
				if (item != null) {
					Date playbackCompletionDate = null;
					long playbackCompletionTime = cursor
							.getLong(PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE_INDEX);
					if (playbackCompletionTime > 0) {
						playbackCompletionDate = new Date(
								playbackCompletionTime);
					}

					item.setMedia(new FeedMedia(
							mediaId,
							item,
							cursor.getInt(PodDBAdapter.KEY_DURATION_INDEX),
							cursor.getInt(PodDBAdapter.KEY_POSITION_INDEX),
							cursor.getLong(PodDBAdapter.KEY_SIZE_INDEX),
							cursor.getString(PodDBAdapter.KEY_MIME_TYPE_INDEX),
							cursor.getString(PodDBAdapter.KEY_FILE_URL_INDEX),
							cursor.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX),
							cursor.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0,
							playbackCompletionDate));

				}
			} while (cursor.moveToNext());
			cursor.close();
		}
	}

	private static Feed extractFeedFromCursorRow(PodDBAdapter adapter,
			Cursor cursor) {
		Date lastUpdate = new Date(
				cursor.getLong(PodDBAdapter.KEY_LAST_UPDATE_INDEX));
		Feed feed = new Feed(lastUpdate);

		feed.setId(cursor.getLong(PodDBAdapter.KEY_ID_INDEX));
		feed.setTitle(cursor.getString(PodDBAdapter.KEY_TITLE_INDEX));
		feed.setLink(cursor.getString(PodDBAdapter.KEY_LINK_INDEX));
		feed.setDescription(cursor
				.getString(PodDBAdapter.KEY_DESCRIPTION_INDEX));
		feed.setPaymentLink(cursor
				.getString(PodDBAdapter.KEY_PAYMENT_LINK_INDEX));
		feed.setAuthor(cursor.getString(PodDBAdapter.KEY_AUTHOR_INDEX));
		feed.setLanguage(cursor.getString(PodDBAdapter.KEY_LANGUAGE_INDEX));
		feed.setType(cursor.getString(PodDBAdapter.KEY_TYPE_INDEX));
		feed.setFeedIdentifier(cursor
				.getString(PodDBAdapter.KEY_FEED_IDENTIFIER_INDEX));
		long imageIndex = cursor.getLong(PodDBAdapter.KEY_IMAGE_INDEX);
		if (imageIndex != 0) {
			feed.setImage(getFeedImage(adapter, imageIndex));
			feed.getImage().setFeed(feed);
		}
		feed.setFile_url(cursor.getString(PodDBAdapter.KEY_FILE_URL_INDEX));
		feed.setDownload_url(cursor
				.getString(PodDBAdapter.KEY_DOWNLOAD_URL_INDEX));
		feed.setDownloaded(cursor.getInt(PodDBAdapter.KEY_DOWNLOADED_INDEX) > 0);

		return feed;
	}

	private static FeedItem getMatchingItemForMedia(long mediaId,
			List<FeedItem> items) {
		for (FeedItem item : items) {
			if (item.getMedia() != null && item.getMedia().getId() == mediaId) {
				return item;
			}
		}
		return null;
	}

	public static List<FeedItem> getQueue(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting queue");

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();

		Cursor itemlistCursor = adapter.getQueueCursor();
		List<FeedItem> items = extractItemlistFromCursor(adapter,
				itemlistCursor);
		itemlistCursor.close();

		loadFeedDataOfFeedItemlist(context, items);

		adapter.close();

		Collections.sort(items, new FeedItemPubdateComparator());

		return items;
	}

	public static List<FeedItem> getUnreadItemsList(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting unread items list");

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();

		Cursor itemlistCursor = adapter.getUnreadItemsCursor();
		List<FeedItem> items = extractItemlistFromCursor(adapter,
				itemlistCursor);
		itemlistCursor.close();

		loadFeedDataOfFeedItemlist(context, items);

		adapter.close();

		return items;
	}

	public static List<FeedItem> getPlaybackHistory() {
		return null;
	}

	public static List<DownloadStatus> getDownloadLog(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Extracting DownloadLog");

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor logCursor = adapter.getDownloadLogCursor();
		List<DownloadStatus> downloadLog = new ArrayList<DownloadStatus>(
				logCursor.getCount());

		if (logCursor.moveToFirst()) {
			do {
				long id = logCursor.getLong(PodDBAdapter.KEY_ID_INDEX);

				long feedfileId = logCursor
						.getLong(PodDBAdapter.KEY_FEEDFILE_INDEX);
				int feedfileType = logCursor
						.getInt(PodDBAdapter.KEY_FEEDFILETYPE_INDEX);
				boolean successful = logCursor
						.getInt(PodDBAdapter.KEY_SUCCESSFUL_INDEX) > 0;
				int reason = logCursor.getInt(PodDBAdapter.KEY_REASON_INDEX);
				String reasonDetailed = logCursor
						.getString(PodDBAdapter.KEY_REASON_DETAILED_INDEX);
				String title = logCursor
						.getString(PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE_INDEX);
				Date completionDate = new Date(
						logCursor
								.getLong(PodDBAdapter.KEY_COMPLETION_DATE_INDEX));
				downloadLog.add(new DownloadStatus(id, title, feedfileId,
						feedfileType, successful, reason, completionDate,
						reasonDetailed));

			} while (logCursor.moveToNext());
		}
		logCursor.close();
		Collections.sort(downloadLog, new DownloadStatusComparator());
		return downloadLog;
	}

	public static Feed getFeed(final Context context, final long feedId) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Loading feed with id " + feedId);
		Feed feed = null;

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor feedCursor = adapter.getFeedCursor(feedId);
		if (feedCursor.moveToFirst()) {
			feed = extractFeedFromCursorRow(adapter, feedCursor);
			feed.setItems(getFeedItemList(context, feed));
		}
		adapter.close();
		return feed;
	}

	public FeedItem getFeedItem(final Context context, final long itemId) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Loading feeditem with id " + itemId);
		FeedItem item = null;

		PodDBAdapter adapter = new PodDBAdapter(context);
		adapter.open();
		Cursor itemCursor = adapter.getFeedItemCursor(itemId);
		if (itemCursor.moveToFirst()) {
			List<FeedItem> list = extractItemlistFromCursor(adapter, itemCursor);
			if (list.size() > 0) {
				item = list.get(0);
			}
		}
		adapter.close();
		return item;

	}

	/**
	 * Searches the DB for a FeedImage of the given id.
	 * 
	 * @param id
	 *            The id of the object
	 * @return The found object
	 * */
	private static FeedImage getFeedImage(PodDBAdapter adapter, final long id) {
		Cursor cursor = adapter.getImageOfFeedCursor(id);
		if ((cursor.getCount() == 0) || !cursor.moveToFirst()) {
			throw new SQLException("No FeedImage found at index: " + id);
		}
		FeedImage image = new FeedImage(id, cursor.getString(cursor
				.getColumnIndex(PodDBAdapter.KEY_TITLE)),
				cursor.getString(cursor
						.getColumnIndex(PodDBAdapter.KEY_FILE_URL)),
				cursor.getString(cursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL)),
				cursor.getInt(cursor
						.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED)) > 0);
		cursor.close();
		return image;
	}
}
