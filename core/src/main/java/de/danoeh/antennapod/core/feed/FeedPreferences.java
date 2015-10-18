package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.database.Cursor;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

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
    private String playback_speed;
    private String username;
    private String password;

    public FeedPreferences(long feedID, boolean autoDownload, AutoDeleteAction auto_delete_action, String playback_speed, String username, String password) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
        this.auto_delete_action = auto_delete_action;
        this.playback_speed = playback_speed;
        this.username = username;
        this.password = password;
    }

    public static FeedPreferences fromCursor(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexAutoDownload = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DOWNLOAD);
        int indexAutoDeleteAction = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DELETE_ACTION);
        int indexUsername = cursor.getColumnIndex(PodDBAdapter.KEY_USERNAME);
        int indexPassword = cursor.getColumnIndex(PodDBAdapter.KEY_PASSWORD);
        int indexPlaybackSpeed = cursor.getColumnIndex(PodDBAdapter.KEY_PLAYBACK_SPEED);

        long feedId = cursor.getLong(indexId);
        boolean autoDownload = cursor.getInt(indexAutoDownload) > 0;
        int autoDeleteActionIndex = cursor.getInt(indexAutoDeleteAction);
        AutoDeleteAction autoDeleteAction = AutoDeleteAction.values()[autoDeleteActionIndex];
        String username = cursor.getString(indexUsername);
        String password = cursor.getString(indexPassword);
        String playbackSpeed = cursor.getString(indexPlaybackSpeed);
        return new FeedPreferences(feedId, autoDownload, autoDeleteAction, playbackSpeed, username, password);
    }



    /**
     * Compare another FeedPreferences with this one. The feedID, autoDownload, AutoDeleteAction and PlaybackSpeed attribute are excluded from the
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
     * Update this FeedPreferences object from another one. The feedID, autoDownload, AutoDeleteAction and PlaybackSpeed attributes are excluded
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

    public String getPlaybackSpeed() {
        return playback_speed;
    }

    public void setPlaybackSpeed(String playback_speed) {
        this.playback_speed = playback_speed;
    }

    public String getCurrentPlaybackSpeed() {
        if (playback_speed.equals("GLOBAL")) {
            return UserPreferences.getPlaybackSpeed();
        }
        return playback_speed;
    }

    public void save(Context context) {
        DBWriter.setFeedPreferences(this);
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
