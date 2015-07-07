package de.danoeh.antennapod.core.feed;

import android.content.Context;
import de.danoeh.antennapod.core.storage.DBWriter;
import org.apache.commons.lang3.StringUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences {

    private long feedID;
    private boolean autoDownload;
    public enum AutoDeleteAction {
        GLOBAL,
        YES,
        NO
    }
    private AutoDeleteAction auto_delete_action;
    private String username;
    private String password;

    public FeedPreferences(long feedID, boolean autoDownload, AutoDeleteAction auto_delete_action, String username, String password) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
        this.auto_delete_action = auto_delete_action;
        this.username = username;
        this.password = password;
    }


    /**
     * Compare another FeedPreferences with this one. The feedID, autoDownload and AutoDeleteAction attribute are excluded from the
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
     * Update this FeedPreferences object from another one. The feedID, autoDownload and AutoDeleteAction attributes are excluded
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

    public AutoDeleteAction getAutoDeleteAction() {
        return auto_delete_action;
    }

    public void setAutoDeleteAction(AutoDeleteAction auto_delete_action) {
        this.auto_delete_action = auto_delete_action;
    }

    public boolean getCurrentAutoDelete() {
        switch (auto_delete_action) {
            case GLOBAL:
                return UserPreferences.isAutoDelete();

            case YES:
                return true;

            case NO:
                return false;
        }
        return false; // TODO - add exceptions here
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
