package de.danoeh.antennapod.feed;

import android.content.Context;

import de.danoeh.antennapod.storage.DBWriter;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences {

    private long feedID;
    private boolean autoDownload;

    public FeedPreferences(long feedID, boolean autoDownload) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
    }

    public long getFeedID() {
        return feedID;
    }

    public void setFeedID(long feedID) {
        this.feedID = feedID;
    }

    public boolean getAutoDownload() {
        return autoDownload;
    }

    public void setAutoDownload(boolean autoDownload) {
        this.autoDownload = autoDownload;
    }

    public void save(Context context) {
        DBWriter.setFeedPreferences(context, this);
    }
}
