package de.danoeh.antennapod.storage.importexport;

import java.util.Date;

/**
 * Represents the stored state of a single episode in a PortCast file.
 */
public class PortcastEpisode {
    private String guid;
    private String enclosureUrl;
    private String status;
    private int positionSeconds;
    private Date lastPlayedAt;
    private String title;
    private Date publishedAt;
    private int durationSeconds;
    private boolean starred;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getEnclosureUrl() {
        return enclosureUrl;
    }

    public void setEnclosureUrl(String enclosureUrl) {
        this.enclosureUrl = enclosureUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPositionSeconds() {
        return positionSeconds;
    }

    public void setPositionSeconds(int positionSeconds) {
        this.positionSeconds = positionSeconds;
    }

    public Date getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(Date lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }
}
