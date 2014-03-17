package de.danoeh.antennapod.feed;

import android.content.Context;
import de.danoeh.antennapod.storage.DBWriter;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences {

    private long feedID;
    private boolean autoDownload;
    private String username;
    private String password;

    public FeedPreferences(long feedID, boolean autoDownload, String username, String password) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
        this.username = username;
        this.password = password;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
