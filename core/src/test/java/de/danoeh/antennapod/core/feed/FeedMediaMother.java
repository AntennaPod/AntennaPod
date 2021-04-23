package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.model.feed.FeedMedia;

class FeedMediaMother {

    private static final String EPISODE_URL = "http://example.com/episode";
    private static final long SIZE = 42;
    private static final String MIME_TYPE = "audio/mp3";

    static FeedMedia anyFeedMedia() {
        return new FeedMedia(null, EPISODE_URL, SIZE, MIME_TYPE);
    }

}
