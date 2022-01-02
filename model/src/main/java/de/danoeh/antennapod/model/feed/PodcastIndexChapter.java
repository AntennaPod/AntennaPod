package de.danoeh.antennapod.model.feed;

public class PodcastIndexChapter extends Chapter {
    public static final int PODCASTINDEX_CHAPTER = 4;

    public PodcastIndexChapter(int startTime,
                               String title,
                               String link,
                               String imageUrl) {
        super(startTime * 1000, title, link, imageUrl);
    }

    @Override
    public int getChapterType() {
        return PODCASTINDEX_CHAPTER;
    }

    @Override
    public String toString() {
        return "PodcastIndexChapter "
                + getTitle()
                + " "
                + getStart()
                + " "
                + getLink()
                + " "
                + getImageUrl();
    }
}
