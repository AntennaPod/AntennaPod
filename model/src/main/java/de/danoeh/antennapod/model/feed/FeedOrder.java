package de.danoeh.antennapod.model.feed;

public enum FeedOrder {
    COUNTER(0),
    ALPHABETICAL(1),
    MOST_PLAYED(3),
    MOST_RECENT_EPISODE(2);

    public final int id;

    FeedOrder(int id) {
        this.id = id;
    }

    public static FeedOrder fromOrdinal(int id) {
        for (FeedOrder counter : values()) {
            if (counter.id == id) {
                return counter;
            }
        }
        return MOST_RECENT_EPISODE;
    }
}
