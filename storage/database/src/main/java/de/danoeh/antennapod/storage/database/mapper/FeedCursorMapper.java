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
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexLastUpdate = cursor.getColumnIndex(PodDBAdapter.KEY_LASTUPDATE);
        int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
        int indexCustomTitle = cursor.getColumnIndex(PodDBAdapter.KEY_CUSTOM_TITLE);
        int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
        int indexDescription = cursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION);
        int indexPaymentLink = cursor.getColumnIndex(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexAuthor = cursor.getColumnIndex(PodDBAdapter.KEY_AUTHOR);
        int indexLanguage = cursor.getColumnIndex(PodDBAdapter.KEY_LANGUAGE);
        int indexType = cursor.getColumnIndex(PodDBAdapter.KEY_TYPE);
        int indexFeedIdentifier = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_IDENTIFIER);
        int indexFileUrl = cursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED);
        int indexIsPaged = cursor.getColumnIndex(PodDBAdapter.KEY_IS_PAGED);
        int indexNextPageLink = cursor.getColumnIndex(PodDBAdapter.KEY_NEXT_PAGE_LINK);
        int indexHide = cursor.getColumnIndex(PodDBAdapter.KEY_HIDE);
        int indexSortOrder = cursor.getColumnIndex(PodDBAdapter.KEY_SORT_ORDER);
        int indexLastUpdateFailed = cursor.getColumnIndex(PodDBAdapter.KEY_LAST_UPDATE_FAILED);
        int indexImageUrl = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE_URL);

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
