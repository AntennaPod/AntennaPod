package de.danoeh.antennapod.core.storage.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.feed.VorbisCommentChapter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

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
        int indexChapterType = cursor.getColumnIndex(PodDBAdapter.KEY_CHAPTER_TYPE);

        long id = cursor.getLong(indexId);
        String title = cursor.getString(indexTitle);
        long start = cursor.getLong(indexStart);
        String link = cursor.getString(indexLink);
        String imageUrl = cursor.getString(indexImage);
        int chapterType = cursor.getInt(indexChapterType);

        Chapter chapter;
        switch (chapterType) {
            case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
                chapter = new SimpleChapter(start, title, link, imageUrl);
                break;
            case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
                chapter = new ID3Chapter(start, title, link, imageUrl);
                break;
            case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
                chapter = new VorbisCommentChapter(start, title, link, imageUrl);
                break;
            default:
                throw new IllegalArgumentException("Unknown chapter type");
        }
        chapter.setId(id);
        return chapter;
    }
}
