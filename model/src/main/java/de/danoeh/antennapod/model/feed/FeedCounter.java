package de.danoeh.antennapod.model.feed;

public enum FeedCounter {
    SHOW_NEW(1),
    SHOW_UNPLAYED(2),
    SHOW_NONE(3),
    SHOW_DOWNLOADED(4),
    SHOW_DOWNLOADED_UNPLAYED(5);

    public final int id;

    FeedCounter(int id) {
        this.id = id;
    }

    public static FeedCounter fromOrdinal(int id) {
        for (FeedCounter counter : values()) {
            if (counter.id == id) {
                return counter;
            }
        }
        return SHOW_NONE;
    }
}
