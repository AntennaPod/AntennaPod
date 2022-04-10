package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link FeedItem} object.
 */
public abstract class FeedItemCursorMapper {
    /**
     * Create a {@link FeedItem} instance from a database row (cursor).
     */
    @NonNull
    public static FeedItem convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_ITEM_ID);
        int indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        int indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        int indexPubDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PUBDATE);
        int indexPaymentLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexFeedId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED);
        int indexHasChapters = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HAS_CHAPTERS);
        int indexRead = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_READ);
        int indexItemIdentifier = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ITEM_IDENTIFIER);
        int indexAutoDownload = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DOWNLOAD_ATTEMPTS);
        int indexImageUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);
        int indexPodcastIndexChapterUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PODCASTINDEX_CHAPTER_URL);

        long id = cursor.getInt(indexId);
        String title = cursor.getString(indexTitle);
        String link = cursor.getString(indexLink);
        Date pubDate = new Date(cursor.getLong(indexPubDate));
        String paymentLink = cursor.getString(indexPaymentLink);
        long feedId = cursor.getLong(indexFeedId);
        boolean hasChapters = cursor.getInt(indexHasChapters) > 0;
        int state = cursor.getInt(indexRead);
        String itemIdentifier = cursor.getString(indexItemIdentifier);
        long autoDownload = cursor.getLong(indexAutoDownload);
        String imageUrl = cursor.getString(indexImageUrl);
        String podcastIndexChapterUrl = cursor.getString(indexPodcastIndexChapterUrl);

        return new FeedItem(id, title, link, pubDate, paymentLink, feedId,
                hasChapters, imageUrl, state, itemIdentifier, autoDownload, podcastIndexChapterUrl);
    }
}
