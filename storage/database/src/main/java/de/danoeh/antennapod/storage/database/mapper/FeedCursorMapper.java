package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link Feed} object.
 */
public abstract class FeedCursorMapper {

    /**
     * Create a {@link Feed} instance from a database row (cursor).
     */
    @NonNull
    public static Feed convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_FEED_ID);
        int indexLastUpdate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LASTUPDATE);
        int indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        int indexCustomTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_CUSTOM_TITLE);
        int indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        int indexDescription = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DESCRIPTION);
        int indexPaymentLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexAuthor = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTHOR);
        int indexLanguage = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LANGUAGE);
        int indexType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TYPE);
        int indexFeedIdentifier = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_IDENTIFIER);
        int indexFileUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOADED);
        int indexIsPaged = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IS_PAGED);
        int indexNextPageLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_NEXT_PAGE_LINK);
        int indexHide = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HIDE);
        int indexSortOrder = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SORT_ORDER);
        int indexLastUpdateFailed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_UPDATE_FAILED);
        int indexImageUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);

        Feed feed = new Feed(
                cursor.getLong(indexId),
                cursor.getString(indexLastUpdate),
                cursor.getString(indexTitle),
                cursor.getString(indexCustomTitle),
                cursor.getString(indexLink),
                cursor.getString(indexDescription),
                cursor.getString(indexPaymentLink),
                cursor.getString(indexAuthor),
                cursor.getString(indexLanguage),
                cursor.getString(indexType),
                cursor.getString(indexFeedIdentifier),
                cursor.getString(indexImageUrl),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                cursor.getInt(indexIsPaged) > 0,
                cursor.getString(indexNextPageLink),
                cursor.getString(indexHide),
                SortOrder.fromCodeString(cursor.getString(indexSortOrder)),
                cursor.getInt(indexLastUpdateFailed) > 0
        );

        FeedPreferences preferences = FeedPreferencesCursorMapper.convert(cursor);
        feed.setPreferences(preferences);
        return feed;
    }
}
