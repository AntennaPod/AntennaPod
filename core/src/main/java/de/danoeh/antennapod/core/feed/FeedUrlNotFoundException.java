package de.danoeh.antennapod.core.feed;

import java.io.IOException;

public class FeedUrlNotFoundException extends IOException {
    private final String artistName;
    private final String trackName;

    public FeedUrlNotFoundException(String url, String trackName) {
        this.artistName = url;
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getTrackName() {
        return trackName;
    }

    @Override
    public String getMessage() {
        return "Result does not specify a feed url";
    }
}