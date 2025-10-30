package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link FeedMedia} object.
 */
public class FeedMediaCursor extends CursorWrapper {
    private final int indexId;
    private final int indexLastPlayedTimeHistory;
    private final int indexDuration;
    private final int indexPosition;
    private final int indexSize;
    private final int indexMimeType;
    private final int indexFileUrl;
    private final int indexDownloadUrl;
    private final int indexDownloadDate;
    private final int indexPlayedDuration;
    private final int indexLastPlayedTimeStatistics;
    private final int indexHasEmbeddedPicture;

    public FeedMediaCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_MEDIA_ID);
        indexLastPlayedTimeHistory = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_PLAYED_TIME_HISTORY);
        indexDuration = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DURATION);
        indexPosition = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_POSITION);
        indexSize = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SIZE);
        indexMimeType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_MIME_TYPE);
        indexFileUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FILE_URL);
        indexDownloadUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOAD_URL);
        indexDownloadDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOAD_DATE);
        indexPlayedDuration = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PLAYED_DURATION);
        indexLastPlayedTimeStatistics = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_PLAYED_TIME_STATISTICS);
        indexHasEmbeddedPicture = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HAS_EMBEDDED_PICTURE);
    }

    /**
     * Create a {@link FeedMedia} instance from a database row (cursor).
     */
    @NonNull
    public FeedMedia getFeedMedia() {
        long lastPlayedTimeHistoryTime = getLong(indexLastPlayedTimeHistory);
        Date lastPlayedTimeHistory = lastPlayedTimeHistoryTime > 0 ? new Date(lastPlayedTimeHistoryTime) : null;

        Boolean hasEmbeddedPicture;
        switch (getInt(indexHasEmbeddedPicture)) {
            case 1:
                hasEmbeddedPicture = Boolean.TRUE;
                break;
            case 0:
                hasEmbeddedPicture = Boolean.FALSE;
                break;
            default:
                hasEmbeddedPicture = null;
                break;
        }

        return new FeedMedia(
                getLong(indexId),
                null,
                getInt(indexDuration),
                getInt(indexPosition),
                getLong(indexSize),
                getString(indexMimeType),
                getString(indexFileUrl),
                getString(indexDownloadUrl),
                getLong(indexDownloadDate),
                lastPlayedTimeHistory,
                getInt(indexPlayedDuration),
                hasEmbeddedPicture,
                getLong(indexLastPlayedTimeStatistics)
        );
    }
}
