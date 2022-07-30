package de.danoeh.antennapod.model.feed;

public class Chapter extends FeedComponent {
    /** Defines starting point in milliseconds. */
    private long start;
    private String title;
    private String link;
    private String imageUrl;
    private String chapterId;

    public Chapter() {
    }

    public Chapter(long start, String title, String link, String imageUrl) {
        this.start = start;
        this.title = title;
        this.link = link;
        this.imageUrl = imageUrl;
    }

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

    /**
     * ID from the chapter source, not the database ID.
     */
    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    @Override
    public String getHumanReadableIdentifier() {
        return title;
    }

    @Override
    public String toString() {
        return "ID3Chapter [title=" + getTitle() + ", start=" + getStart() + ", url=" + getLink() + "]";
    }
}
