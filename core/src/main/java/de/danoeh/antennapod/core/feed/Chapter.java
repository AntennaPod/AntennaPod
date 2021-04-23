package de.danoeh.antennapod.core.feed;

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
