package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;

import android.database.CursorWrapper;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link Feed} object.
 */
public class FeedCursor extends CursorWrapper {
    private final FeedPreferencesCursor preferencesCursor;
    private final int indexId;
    private final int indexLastUpdate;
    private final int indexTitle;
    private final int indexCustomTitle;
    private final int indexLink;
    private final int indexDescription;
    private final int indexPaymentLink;
    private final int indexAuthor;
    private final int indexLanguage;
    private final int indexType;
    private final int indexFeedIdentifier;
    private final int indexFileUrl;
    private final int indexDownloadUrl;
    private final int indexLastRefreshed;
    private final int indexIsPaged;
    private final int indexNextPageLink;
    private final int indexHide;
    private final int indexSortOrder;
    private final int indexLastUpdateFailed;
    private final int indexImageUrl;
    private final int indexState;

    public FeedCursor(Cursor cursor) {
        super(new FeedPreferencesCursor(cursor));
        preferencesCursor = (FeedPreferencesCursor) getWrappedCursor();
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_FEED_ID);
        indexLastUpdate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LASTUPDATE);
        indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        indexCustomTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_CUSTOM_TITLE);
        indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        indexDescription = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DESCRIPTION);
        indexPaymentLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PAYMENT_LINK);
        indexAuthor = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTHOR);
        indexLanguage = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LANGUAGE);
        indexType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TYPE);
        indexFeedIdentifier = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED_IDENTIFIER);
        indexFileUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FILE_URL);
        indexDownloadUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOAD_URL);
        indexLastRefreshed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_REFRESH_ATTEMPT);
        indexIsPaged = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IS_PAGED);
        indexNextPageLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_NEXT_PAGE_LINK);
        indexHide = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HIDE);
        indexSortOrder = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SORT_ORDER);
        indexLastUpdateFailed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_UPDATE_FAILED);
        indexImageUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);
        indexState = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_STATE);
    }

    /**
     * Create a {@link Feed} instance from the current database row.
     */
    @NonNull
    public Feed getFeed() {
        Feed feed = new Feed(
                getLong(indexId),
                getString(indexLastUpdate),
                getString(indexTitle),
                getString(indexCustomTitle),
                getString(indexLink),
                getString(indexDescription),
                getString(indexPaymentLink),
                getString(indexAuthor),
                getString(indexLanguage),
                getString(indexType),
                getString(indexFeedIdentifier),
                getString(indexImageUrl),
                getString(indexFileUrl),
                getString(indexDownloadUrl),
                getLong(indexLastRefreshed),
                getInt(indexIsPaged) > 0,
                getString(indexNextPageLink),
                getString(indexHide),
                SortOrder.fromCodeString(getString(indexSortOrder)),
                getInt(indexLastUpdateFailed) > 0,
                getInt(indexState));
        feed.setPreferences(preferencesCursor.getFeedPreferences());
        return feed;
    }
}
