package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences {

    public static final float SPEED_USE_GLOBAL = -1;

    @NonNull
    private FeedFilter filter;
    private long feedID;
    private boolean autoDownload;
    private boolean keepUpdated;

    public enum AutoDeleteAction {
        GLOBAL,
        YES,
        NO
    }
    private AutoDeleteAction auto_delete_action;

    private VolumeAdaptionSetting volumeAdaptionSetting;

    private String username;
    private String password;
    private float feedPlaybackSpeed;

    public FeedPreferences(long feedID, boolean autoDownload, AutoDeleteAction auto_delete_action, VolumeAdaptionSetting volumeAdaptionSetting, String username, String password) {
        this(feedID, autoDownload, true, auto_delete_action, volumeAdaptionSetting, username, password, new FeedFilter(), SPEED_USE_GLOBAL);
    }

    private FeedPreferences(long feedID, boolean autoDownload, boolean keepUpdated, AutoDeleteAction auto_delete_action, VolumeAdaptionSetting volumeAdaptionSetting, String username, String password, @NonNull FeedFilter filter, float feedPlaybackSpeed) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
        this.keepUpdated = keepUpdated;
        this.auto_delete_action = auto_delete_action;
        this.volumeAdaptionSetting = volumeAdaptionSetting;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.feedPlaybackSpeed = feedPlaybackSpeed;
    }

    public static FeedPreferences fromCursor(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexAutoDownload = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DOWNLOAD);
        int indexAutoRefresh = cursor.getColumnIndex(PodDBAdapter.KEY_KEEP_UPDATED);
        int indexAutoDeleteAction = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DELETE_ACTION);
        int indexVolumeAdaption = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_VOLUME_ADAPTION);
        int indexUsername = cursor.getColumnIndex(PodDBAdapter.KEY_USERNAME);
        int indexPassword = cursor.getColumnIndex(PodDBAdapter.KEY_PASSWORD);
        int indexIncludeFilter = cursor.getColumnIndex(PodDBAdapter.KEY_INCLUDE_FILTER);
        int indexExcludeFilter = cursor.getColumnIndex(PodDBAdapter.KEY_EXCLUDE_FILTER);
        int indexFeedPlaybackSpeed = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_PLAYBACK_SPEED);

        long feedId = cursor.getLong(indexId);
        boolean autoDownload = cursor.getInt(indexAutoDownload) > 0;
        boolean autoRefresh = cursor.getInt(indexAutoRefresh) > 0;
        int autoDeleteActionIndex = cursor.getInt(indexAutoDeleteAction);
        AutoDeleteAction autoDeleteAction = AutoDeleteAction.values()[autoDeleteActionIndex];
        int volumeAdaptionValue = cursor.getInt(indexVolumeAdaption);
        VolumeAdaptionSetting volumeAdaptionSetting = VolumeAdaptionSetting.fromInteger(volumeAdaptionValue);
        String username = cursor.getString(indexUsername);
        String password = cursor.getString(indexPassword);
        String includeFilter = cursor.getString(indexIncludeFilter);
        String excludeFilter = cursor.getString(indexExcludeFilter);
        float feedPlaybackSpeed = cursor.getFloat(indexFeedPlaybackSpeed);
        return new FeedPreferences(feedId, autoDownload, autoRefresh, autoDeleteAction, volumeAdaptionSetting, username, password, new FeedFilter(includeFilter, excludeFilter), feedPlaybackSpeed);
    }

    /**
     * @return the filter for this feed
     */
    @NonNull public FeedFilter getFilter() {
        return filter;
    }

    public void setFilter(@NonNull FeedFilter filter) {
        this.filter = filter;
    }

    /**
     * @return true if this feed should be refreshed when everything else is being refreshed
     *         if false the feed should only be refreshed if requested directly.
     */
    public boolean getKeepUpdated() {
        return keepUpdated;
    }

    public void setKeepUpdated(boolean keepUpdated) {
        this.keepUpdated = keepUpdated;
    }

    /**
     * Compare another FeedPreferences with this one. The feedID, autoDownload and AutoDeleteAction attribute are excluded from the
     * comparison.
     *
     * @return True if the two objects are different.
     */
    public boolean compareWithOther(FeedPreferences other) {
        if (other == null) {
            return true;
        }
        if (!TextUtils.equals(username, other.username)) {
            return true;
        }
        if (!TextUtils.equals(password, other.password)) {
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

    public VolumeAdaptionSetting getVolumeAdaptionSetting() {
        return volumeAdaptionSetting;
    }

    public void setAutoDeleteAction(AutoDeleteAction auto_delete_action) {
        this.auto_delete_action = auto_delete_action;
    }

    public void setVolumeAdaptionSetting(VolumeAdaptionSetting volumeAdaptionSetting) {
        this.volumeAdaptionSetting = volumeAdaptionSetting;
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

    public float getFeedPlaybackSpeed() {
        return feedPlaybackSpeed;
    }

    public void setFeedPlaybackSpeed(float playbackSpeed) {
        feedPlaybackSpeed = playbackSpeed;
    }
}
