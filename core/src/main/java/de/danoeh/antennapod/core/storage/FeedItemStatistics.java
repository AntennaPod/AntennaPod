package de.danoeh.antennapod.core.storage;

import android.database.Cursor;

import java.util.Date;

/**
 * Contains information about a feed's items.
 */
public class FeedItemStatistics {
    private final long feedID;
    private final int numberOfItems;
    private final int numberOfNewItems;
    private final int numberOfInProgressItems;
    private final Date lastUpdate;
    private static final Date UNKNOWN_DATE = new Date(0);


    /**
     * Creates new FeedItemStatistics object.
     *
     * @param feedID                  ID of the feed.
     * @param numberOfItems           Number of items that this feed has.
     * @param numberOfNewItems        Number of unread items this feed has.
     * @param numberOfInProgressItems Number of items that the user has started listening to.
     * @param lastUpdate              pubDate of the latest episode. A lastUpdate value of 0 will be interpreted as DATE_UNKOWN if
     *                                numberOfItems is 0.
     */
    private FeedItemStatistics(long feedID, int numberOfItems, int numberOfNewItems, int numberOfInProgressItems, Date lastUpdate) {
        this.feedID = feedID;
        this.numberOfItems = numberOfItems;
        this.numberOfNewItems = numberOfNewItems;
        this.numberOfInProgressItems = numberOfInProgressItems;
        if (numberOfItems > 0) {
            this.lastUpdate = (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
        } else {
            this.lastUpdate = UNKNOWN_DATE;
        }
    }

    public static FeedItemStatistics fromCursor(Cursor cursor) {
        return new FeedItemStatistics(
                cursor.getLong(0),
                cursor.getInt(1),
                cursor.getInt(2),
                cursor.getInt(4),
                new Date(cursor.getLong(3)));
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

    /**
     * Returns the pubDate of the latest item in the feed. Users of this method
     * should check if this value is unkown or not by calling lastUpdateKnown() first.
     */
    public Date getLastUpdate() {
        return (lastUpdate != null) ? (Date) lastUpdate.clone() : null;
    }

    /**
     * Returns true if the lastUpdate value is known. The lastUpdate value is unkown if the
     * feed has no items.
     */
    public boolean lastUpdateKnown() {
        return lastUpdate != UNKNOWN_DATE;
    }
}
