package de.danoeh.antennapod.net.discovery;

import androidx.annotation.Nullable;

public class PodcastSearchResult {

    /**
     * The name of the podcast
     */
    public final String title;

    /**
     * URL of the podcast image
     */
    @Nullable
    public final String imageUrl;
    /**
     * URL of the podcast feed
     */
    @Nullable
    public final String feedUrl;

    /**
     * artistName of the podcast feed
     */
    @Nullable
    public final String author;


    public PodcastSearchResult(String title, @Nullable String imageUrl, @Nullable String feedUrl, @Nullable String author) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.feedUrl = feedUrl;
        this.author = author;
    }

    public static PodcastSearchResult dummy() {
        return new PodcastSearchResult("", "", "", "");
    }
}
