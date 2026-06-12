package de.danoeh.antennapod.storage.importexport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a single subscription in a PortCast file, together with the episode
 * states and per-feed preferences that belong to it.
 */
public class PortcastSubscription {
    private String feedUrl;
    private String podcastGuid;
    private String title;
    private final Set<String> tags = new HashSet<>();
    private final List<PortcastEpisode> episodes = new ArrayList<>();
    private Float playbackSpeed;
    private Integer skipIntroSeconds;
    private Integer skipEndingSeconds;
    private Boolean autoUpdate;
    private Boolean notificationsEnabled;

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public String getPodcastGuid() {
        return podcastGuid;
    }

    public void setPodcastGuid(String podcastGuid) {
        this.podcastGuid = podcastGuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<String> getTags() {
        return tags;
    }

    public List<PortcastEpisode> getEpisodes() {
        return episodes;
    }

    public Float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(Float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public Integer getSkipIntroSeconds() {
        return skipIntroSeconds;
    }

    public void setSkipIntroSeconds(Integer skipIntroSeconds) {
        this.skipIntroSeconds = skipIntroSeconds;
    }

    public Integer getSkipEndingSeconds() {
        return skipEndingSeconds;
    }

    public void setSkipEndingSeconds(Integer skipEndingSeconds) {
        this.skipEndingSeconds = skipEndingSeconds;
    }

    public Boolean getAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(Boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}
