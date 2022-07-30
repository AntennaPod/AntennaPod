package de.danoeh.antennapod.storage.database.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

/**
 * Converts a {@link Cursor} to a {@link Chapter} object.
 */
public abstract class ChapterCursorMapper {
    /**
     * Create a {@link Chapter} instance from a database row (cursor).
     */
    @NonNull
    public static Chapter convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
        int indexStart = cursor.getColumnIndex(PodDBAdapter.KEY_START);
        int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
        int indexImage = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE_URL);

        long id = cursor.getLong(indexId);
        String title = cursor.getString(indexTitle);
        long start = cursor.getLong(indexStart);
        String link = cursor.getString(indexLink);
        String imageUrl = cursor.getString(indexImage);
        Chapter chapter = new Chapter(start, title, link, imageUrl);
        chapter.setId(id);
        return chapter;
    }
}
