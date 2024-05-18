package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link FeedItem} object.
 */
public class FeedItemCursor extends CursorWrapper {
    private final FeedMediaCursor feedMediaCursor;
    private final int indexId;
    private final int indexTitle;
    private final int indexLink;
    private final int indexPubDate;
    private final int indexPaymentLink;
    private final int indexFeedId;
    private final int indexHasChapters;
    private final int indexRead;
    private final int indexItemIdentifier;
    private final int indexAutoDownload;
    private final int indexImageUrl;
    private final int indexPodcastIndexChapterUrl;
    private final int indexMediaId;
    private final int indexPodcastIndexTranscriptType;
    private final int indexPodcastIndexTranscriptUrl;

    public FeedItemCursor(Cursor cursor) {
        super(new FeedMediaCursor(cursor));
        feedMediaCursor = (FeedMediaCursor) getWrappedCursor();
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_ITEM_ID);
        indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        indexPubDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PUBDATE);
        indexPaymentLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PAYMENT_LINK);
        indexFeedId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED);
        indexHasChapters = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HAS_CHAPTERS);
        indexRead = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_READ);
        indexItemIdentifier = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ITEM_IDENTIFIER);
        indexAutoDownload = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DOWNLOAD_ENABLED);
        indexImageUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);
        indexPodcastIndexChapterUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PODCASTINDEX_CHAPTER_URL);
        indexMediaId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_MEDIA_ID);
        indexPodcastIndexTranscriptType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PODCASTINDEX_TRANSCRIPT_TYPE);
        indexPodcastIndexTranscriptUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PODCASTINDEX_TRANSCRIPT_URL);
    }

    /**
     * Create a {@link FeedItem} instance from a database row (cursor).
     */
    @NonNull
    public FeedItem getFeedItem() {
        FeedItem item = new FeedItem(
                getInt(indexId),
                getString(indexTitle),
                getString(indexLink),
                new Date(getLong(indexPubDate)),
                getString(indexPaymentLink),
                getLong(indexFeedId),
                getInt(indexHasChapters) > 0,
                getString(indexImageUrl),
                getInt(indexRead),
                getString(indexItemIdentifier),
                getLong(indexAutoDownload) > 0,
                getString(indexPodcastIndexChapterUrl),
                getString(indexPodcastIndexTranscriptType),
                getString(indexPodcastIndexTranscriptUrl));
        if (!isNull(indexMediaId)) {
            item.setMedia(feedMediaCursor.getFeedMedia());
        }
        return item;
    }
}
