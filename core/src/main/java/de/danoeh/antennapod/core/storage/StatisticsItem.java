package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.core.feed.Feed;

public class StatisticsItem {
    public final Feed feed;
    public final long time;

    /**
     * Respects speed, listening twice, ...
     */
    public final long timePlayed;

    /**
     * Simply sums up time of podcasts that are marked as played.
     */
    public final long timePlayedCountAll;

    /**
     * Number of episodes.
     */
    public final long episodes;

    /**
     * Episodes that are actually played.
     */
    public final long episodesStarted;

    /**
     * All episodes that are marked as played (or have position != 0).
     */
    public final long episodesStartedIncludingMarked;

    /**
     * Simply sums up the size of download podcasts.
     */
    public final long totalDownloadSize;

    public StatisticsItem(Feed feed, long time, long timePlayed, long timePlayedCountAll,
                          long episodes, long episodesStarted, long episodesStartedIncludingMarked,
                          long totalDownloadSize) {
        this.feed = feed;
        this.time = time;
        this.timePlayed = timePlayed;
        this.timePlayedCountAll = timePlayedCountAll;
        this.episodes = episodes;
        this.episodesStarted = episodesStarted;
        this.episodesStartedIncludingMarked = episodesStartedIncludingMarked;
        this.totalDownloadSize = totalDownloadSize;
    }
}
