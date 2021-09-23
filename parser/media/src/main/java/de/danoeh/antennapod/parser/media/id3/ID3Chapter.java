package de.danoeh.antennapod.parser.media.id3;

import de.danoeh.antennapod.model.feed.Chapter;

public class ID3Chapter extends Chapter {
    public static final int CHAPTERTYPE_ID3CHAPTER = 2;

    /**
     * Identifies the chapter in its ID3 tag. This attribute does not have to be
     * store in the DB and is only used for parsing.
     */
    private String id3ID;

    public ID3Chapter(String id3ID, long start) {
        super(start);
        this.id3ID = id3ID;
    }

    public ID3Chapter(long start, String title, String link, String imageUrl) {
        super(start, title, link, imageUrl);
    }

    @Override
    public String toString() {
        return "ID3Chapter [id3ID=" + id3ID + ", title=" + getTitle() + ", start="
                + getStart() + ", url=" + getLink() + "]";
    }

    @Override
    public int getChapterType() {
        return CHAPTERTYPE_ID3CHAPTER;
    }

    public String getId3ID() {
        return id3ID;
    }

}
