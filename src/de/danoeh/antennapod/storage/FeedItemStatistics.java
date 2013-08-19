package de.danoeh.antennapod.storage;

import java.util.Date;

/**
 * Contains information about a feed's items.
 */
public class FeedItemStatistics {
    private long feedID;
    private int numberOfItems;
    private int numberOfNewItems;
    private int numberOfInProgressItems;
    private Date lastUpdate;

    public FeedItemStatistics(long feedID, int numberOfItems, int numberOfNewItems, int numberOfInProgressItems, Date lastUpdate) {
        this.feedID = feedID;
        this.numberOfItems = numberOfItems;
        this.numberOfNewItems = numberOfNewItems;
        this.numberOfInProgressItems = numberOfInProgressItems;
        this.lastUpdate = (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
    }

    public long getFeedID() {
        return feedID;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public int getNumberOfNewItems() {
        return numberOfNewItems;
    }

    public int getNumberOfInProgressItems() {
        return numberOfInProgressItems;
    }

    public Date getLastUpdate() {
        return (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
    }
}
