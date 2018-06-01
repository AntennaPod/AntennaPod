package de.danoeh.antennapod.core.feed;

class FeedImageMother {

    public static FeedImage anyFeedImage() {
        return new FeedImage(0, "image", null, "http://example.com/picture", false);
    }

}
