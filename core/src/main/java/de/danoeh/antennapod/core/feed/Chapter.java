package de.danoeh.antennapod.core.feed;

import android.database.Cursor;

import de.danoeh.antennapod.core.storage.PodDBAdapter;

public abstract class Chapter extends FeedComponent {

    /** Defines starting point in milliseconds. */
    long start;
    String title;
    String link;
    String imageUrl;

    Chapter() {
    }

    Chapter(long start) {
        super();
        this.start = start;
    }

    Chapter(long start, String title, String link, String imageUrl) {
        super();
        this.start = start;
        this.title = title;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    public static Chapter fromCursor(Cursor cursor) {
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

        Chapter chapter = null;
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
        }
        chapter.setId(id);
        return chapter;
    }

    public abstract int getChapterType();

    public long getStart() {
        return start;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }
}
