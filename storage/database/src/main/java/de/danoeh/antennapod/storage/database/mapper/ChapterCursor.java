package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import android.database.CursorWrapper;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link Chapter} object.
 */
public class ChapterCursor extends CursorWrapper {
    private final int indexId;
    private final int indexTitle;
    private final int indexStart;
    private final int indexLink;
    private final int indexImage;

    public ChapterCursor(Cursor cursor) {
        super(cursor);
        indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ID);
        indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        indexStart = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_START);
        indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        indexImage = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);
    }

    /**
     * Create a {@link Chapter} instance from a database row (cursor).
     */
    @NonNull
    public Chapter getChapter() {
        Chapter chapter = new Chapter(
                getLong(indexStart),
                getString(indexTitle),
                getString(indexLink),
                getString(indexImage));
        chapter.setId(getLong(indexId));
        return chapter;
    }
}
