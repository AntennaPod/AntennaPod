package de.danoeh.antennapod.core.feed;

import android.content.Context;
import de.danoeh.antennapod.core.storage.DBWriter;
import org.apache.commons.lang3.StringUtils;

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


    /**
     * Compare another FeedPreferences with this one. The feedID and autoDownload attribute are excluded from the
     * comparison.
     *
     * @return True if the two objects are different.
     */
    public boolean compareWithOther(FeedPreferences other) {
        if (other == null)
            return true;
        if (!StringUtils.equals(username, other.username)) {
            return true;
        }
        if (!StringUtils.equals(password, other.password)) {
            return true;
        }
        return false;
    }

    /**
     * Update this FeedPreferences object from another one. The feedID and autoDownload attributes are excluded
     * from the update.
     */
    public void updateFromOther(FeedPreferences other) {
        if (other == null)
            return;
        this.username = other.username;
        this.password = other.password;
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
