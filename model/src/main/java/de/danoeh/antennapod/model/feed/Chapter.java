package de.danoeh.antennapod.model.feed;

public abstract class Chapter extends FeedComponent {

    /** Defines starting point in milliseconds. */
    private long start;
    private String title;
    private String link;
    private String imageUrl;

    protected Chapter() {
    }

    protected Chapter(long start) {
        super();
        this.start = start;
    }

    protected Chapter(long start, String title, String link, String imageUrl) {
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
