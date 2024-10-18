package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains preferences for a single feed.
 */
public class FeedPreferences implements Serializable {

    public static final float SPEED_USE_GLOBAL = -1;
    public static final String TAG_ROOT = "#root";
    public static final String TAG_SEPARATOR = "\u001e";

    public enum AutoDeleteAction {
        GLOBAL(0),
        ALWAYS(1),
        NEVER(2);

        public final int code;

        AutoDeleteAction(int code) {
            this.code = code;
        }

        public static AutoDeleteAction fromCode(int code) {
            for (AutoDeleteAction action : values()) {
                if (code == action.code) {
                    return action;
                }
            }
            return NEVER;
        }
    }

    public enum NewEpisodesAction {
        GLOBAL(0),
        ADD_TO_INBOX(1),
        ADD_TO_QUEUE(3),
        NOTHING(2);

        public final int code;

        NewEpisodesAction(int code) {
            this.code = code;
        }

        public static NewEpisodesAction fromCode(int code) {
            for (NewEpisodesAction action : values()) {
                if (code == action.code) {
                    return action;
                }
            }
            return ADD_TO_INBOX;
        }
    }

    public enum SkipSilence {
        GLOBAL(0), OFF(1), MILD(2), MEDIUM(3), AGGRESSIVE(4);

        public final int code;

        SkipSilence(int code) {
            this.code = code;
        }

        public static SkipSilence fromCode(int code) {
            for (SkipSilence s : values()) {
                if (s.code == code) {
                    return s;
                }
            }
            return GLOBAL;
        }
    }

    @NonNull
    private FeedFilter filter;
    private long feedID;
    private boolean autoDownload;
    private boolean keepUpdated;
    private AutoDeleteAction autoDeleteAction;
    private VolumeAdaptionSetting volumeAdaptionSetting;
    private NewEpisodesAction newEpisodesAction;
    private String username;
    private String password;
    private float feedPlaybackSpeed;
    private int feedSkipIntro;
    private int feedSkipEnding;
    private SkipSilence feedSkipSilence;
    private boolean showEpisodeNotification;
    private final Set<String> tags = new HashSet<>();

    public FeedPreferences(long feedID, boolean autoDownload, AutoDeleteAction autoDeleteAction,
                           VolumeAdaptionSetting volumeAdaptionSetting, NewEpisodesAction newEpisodesAction,
                           String username, String password) {
        this(feedID, autoDownload, true, autoDeleteAction, volumeAdaptionSetting, username, password,
                new FeedFilter(), SPEED_USE_GLOBAL, 0, 0, SkipSilence.GLOBAL,
                false, newEpisodesAction, new HashSet<>());
    }

    public FeedPreferences(long feedID, boolean autoDownload, boolean keepUpdated,
                            AutoDeleteAction autoDeleteAction, VolumeAdaptionSetting volumeAdaptionSetting,
                            String username, String password, @NonNull FeedFilter filter,
                            float feedPlaybackSpeed, int feedSkipIntro, int feedSkipEnding, SkipSilence feedSkipSilence,
                            boolean showEpisodeNotification, NewEpisodesAction newEpisodesAction,
                            Set<String> tags) {
        this.feedID = feedID;
        this.autoDownload = autoDownload;
        this.keepUpdated = keepUpdated;
        this.autoDeleteAction = autoDeleteAction;
        this.volumeAdaptionSetting = volumeAdaptionSetting;
        this.username = username;
        this.password = password;
        this.filter = filter;
        this.feedPlaybackSpeed = feedPlaybackSpeed;
        this.feedSkipIntro = feedSkipIntro;
        this.feedSkipEnding = feedSkipEnding;
        this.feedSkipSilence = feedSkipSilence;
        this.showEpisodeNotification = showEpisodeNotification;
        this.newEpisodesAction = newEpisodesAction;
        this.tags.addAll(tags);
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
        return autoDeleteAction;
    }

    public VolumeAdaptionSetting getVolumeAdaptionSetting() {
        return volumeAdaptionSetting;
    }

    public NewEpisodesAction getNewEpisodesAction() {
        return newEpisodesAction;
    }

    public void setAutoDeleteAction(AutoDeleteAction autoDeleteAction) {
        this.autoDeleteAction = autoDeleteAction;
    }

    public void setVolumeAdaptionSetting(VolumeAdaptionSetting volumeAdaptionSetting) {
        this.volumeAdaptionSetting = volumeAdaptionSetting;
    }

    public void setNewEpisodesAction(NewEpisodesAction newEpisodesAction) {
        this.newEpisodesAction = newEpisodesAction;
    }

    public AutoDeleteAction getCurrentAutoDelete() {
        return autoDeleteAction;
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

    public void setFeedSkipIntro(int skipIntro) {
        feedSkipIntro = skipIntro;
    }

    public int getFeedSkipIntro() {
        return feedSkipIntro;
    }

    public void setFeedSkipEnding(int skipEnding) {
        feedSkipEnding = skipEnding;
    }

    public int getFeedSkipEnding() {
        return feedSkipEnding;
    }

    public void setFeedSkipSilence(SkipSilence skipSilence) {
        feedSkipSilence = skipSilence;
    }

    public SkipSilence getFeedSkipSilence() {
        if (feedPlaybackSpeed == SPEED_USE_GLOBAL) {
            return SkipSilence.GLOBAL;
        }
        return feedSkipSilence;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getTagsAsString() {
        return TextUtils.join(TAG_SEPARATOR, tags);
    }

    /**
     * getter for preference if notifications should be display for new episodes.
     * @return true for displaying notifications
     */
    public boolean getShowEpisodeNotification() {
        return showEpisodeNotification;
    }

    public void setShowEpisodeNotification(boolean showEpisodeNotification) {
        this.showEpisodeNotification = showEpisodeNotification;
    }
}
