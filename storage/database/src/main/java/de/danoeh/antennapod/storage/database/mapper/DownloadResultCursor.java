package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link DownloadResult} object.
 */
public class DownloadResultCursor extends CursorWrapper {
    private final int indexId;
    private final int indexTitle;
    private final int indexFeedFile;
    private final int indexFileFileType;
    private final int indexSuccessful;
    private final int indexReason;
    private final int indexCompletionDate;
    private final int indexReasonDetailed;

    public DownloadResultCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ID);
        indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE);
        indexFeedFile = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEEDFILE);
        indexFileFileType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEEDFILETYPE);
        indexSuccessful = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SUCCESSFUL);
        indexReason = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_REASON);
        indexCompletionDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_COMPLETION_DATE);
        indexReasonDetailed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_REASON_DETAILED);
    }

    /**
     * Create a {@link DownloadResult} instance from a database row (cursor).
     */
    @NonNull
    public DownloadResult getDownloadResult() {
        return new DownloadResult(
                getLong(indexId),
                getString(indexTitle),
                getLong(indexFeedFile),
                getInt(indexFileFileType),
                getInt(indexSuccessful) > 0,
                DownloadError.fromCode(getInt(indexReason)),
                new Date(getLong(indexCompletionDate)),
                getString(indexReasonDetailed));
    }
}
