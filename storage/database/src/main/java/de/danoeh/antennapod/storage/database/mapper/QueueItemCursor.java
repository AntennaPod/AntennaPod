package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.queue.QueueItem;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link QueueItem} object.
 */
public class QueueItemCursor extends CursorWrapper {
    private final int indexId;
    private final int indexQueueId;
    private final int indexFeedItemId;
    private final int indexFeedId;
    private final int indexPosition;

    public QueueItemCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ID);
        indexQueueId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_QUEUE_ID);
        indexFeedItemId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEEDITEM);
        indexFeedId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED);
        indexPosition = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_POSITION);
    }

    /**
     * Create a {@link QueueItem} instance from the current database row.
     */
    @NonNull
    public QueueItem getQueueItem() {

        return new QueueItem(getLong(indexId), getLong(indexQueueId), getLong(indexFeedItemId), getLong(indexFeedId),
                getInt(indexPosition));
    }
}


