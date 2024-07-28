package de.danoeh.antennapod.model.feed;

import java.util.List;
import java.util.Objects;

public class Chapter {
    private long id;
    /** The start time of the chapter in milliseconds */
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
    public String toString() {
        return "Chapter [title=" + getTitle() + ", start=" + getStart() + ", url=" + getLink() + "]";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static int getAfterPosition(List<Chapter> chapters, int playbackPosition) {
        if (chapters == null || chapters.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getStart() > playbackPosition) {
                return i - 1;
            }
        }
        return chapters.size() - 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Chapter chapter = (Chapter) o;
        return id == chapter.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
