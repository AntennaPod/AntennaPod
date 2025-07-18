package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.Feed;

public class StatisticsItem {
    public final Feed feed;
    public final long time;

    /**
     * Respects speed, listening twice, ...
     */
    public final long timePlayed;

    /**
     * Number of episodes.
     */
    public final long episodes;

    /**
     * Episodes that are actually played.
     */
    public final long episodesStarted;

    /**
     * Simply sums up the size of download podcasts.
     */
    public final long totalDownloadSize;

    /**
     * Stores the number of episodes downloaded.
     */
    public final long episodesDownloadCount;

    public final boolean hasRecentUnplayed;

    public StatisticsItem(Feed feed, long time, long timePlayed,
                          long episodes, long episodesStarted,
                          long totalDownloadSize, long episodesDownloadCount, boolean hasRecentUnplayed) {
        this.feed = feed;
        this.time = time;
        this.timePlayed = timePlayed;
        this.episodes = episodes;
        this.episodesStarted = episodesStarted;
        this.totalDownloadSize = totalDownloadSize;
        this.episodesDownloadCount = episodesDownloadCount;
        this.hasRecentUnplayed = hasRecentUnplayed;
    }
}
