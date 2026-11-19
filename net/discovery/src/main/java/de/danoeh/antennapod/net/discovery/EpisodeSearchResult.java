package de.danoeh.antennapod.net.discovery;

import androidx.annotation.Nullable;
import java.util.Date;

public class EpisodeSearchResult {
    public final String title;
    public final String podcastTitle;
    @Nullable
    public final String imageUrl;
    @Nullable
    public final String feedUrl;
    @Nullable
    public final String episodeUrl;
    @Nullable
    public final String episodeGuid;
    @Nullable
    public final Date pubDate;
    public final long durationMs;
    @Nullable
    public final String description;
    @Nullable
    public final String author;

    public EpisodeSearchResult(String title,
                               String podcastTitle,
                               @Nullable String imageUrl,
                               @Nullable String feedUrl,
                               @Nullable String episodeUrl,
                               @Nullable String episodeGuid,
                               @Nullable Date pubDate,
                               long durationMs,
                               @Nullable String description,
                               @Nullable String author) {
        this.title = title;
        this.podcastTitle = podcastTitle;
        this.imageUrl = imageUrl;
        this.feedUrl = feedUrl;
        this.episodeUrl = episodeUrl;
        this.episodeGuid = episodeGuid;
        this.pubDate = pubDate;
        this.durationMs = durationMs;
        this.description = description;
        this.author = author;
    }
}
