package de.danoeh.antennapod.core.feed;

public class FeedUrlNotFoundException extends RuntimeException {
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
}