package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.queue.Queue;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link Queue} object.
 */
public class QueueCursor extends CursorWrapper {
    private final int indexId;
    private final int indexName;

    public QueueCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ID);
        indexName = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_QUEUE_NAME);
    }

    /**
     * Create a {@link Queue} instance from the current database row.
     */
    @NonNull
    public Queue getQueue() {
        return new Queue(getLong(indexId), getString(indexName));
    }
}
