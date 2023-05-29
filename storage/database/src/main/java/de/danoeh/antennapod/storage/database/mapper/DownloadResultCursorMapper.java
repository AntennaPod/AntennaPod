package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link DownloadResult} object.
 */
public abstract class DownloadResultCursorMapper {
    /**
     * Create a {@link DownloadResult} instance from a database row (cursor).
     */
    @NonNull
    public static DownloadResult convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ID);
        int indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOADSTATUS_TITLE);
        int indexFeedFile = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEEDFILE);
        int indexFileFileType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEEDFILETYPE);
        int indexSuccessful = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SUCCESSFUL);
        int indexReason = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_REASON);
        int indexCompletionDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_COMPLETION_DATE);
        int indexReasonDetailed = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_REASON_DETAILED);

        return new DownloadResult(cursor.getLong(indexId), cursor.getString(indexTitle), cursor.getLong(indexFeedFile),
                cursor.getInt(indexFileFileType), cursor.getInt(indexSuccessful) > 0,
                DownloadError.fromCode(cursor.getInt(indexReason)),
                new Date(cursor.getLong(indexCompletionDate)),
                cursor.getString(indexReasonDetailed));
    }
}
