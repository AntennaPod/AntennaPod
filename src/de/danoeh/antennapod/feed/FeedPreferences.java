package de.danoeh.antennapod.feed;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences {

    private long feedID;

    public FeedPreferences(long feedID) {
        this.feedID = feedID;
    }

    public long getFeedID() {
        return feedID;
    }

    public void setFeedID(long feedID) {
        this.feedID = feedID;
    }
}
