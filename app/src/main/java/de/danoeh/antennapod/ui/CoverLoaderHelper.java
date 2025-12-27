package de.danoeh.antennapod.ui;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.GenerativeUrlBuilder;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;

public class CoverLoaderHelper {
    /**
     * Creates an ImageModel for a podcast feed's cover image.
     *
     * @param feed The feed to create an ImageModel for
     * @return ImageModel with the feed's image URL as primary source and feed title as fallback text.
     *     If the feed has no image URL, a generated placeholder will be created automatically.
     */
    public static GenerativeUrlBuilder fromFeed(Feed feed) {
        return fromFeed(feed, false);
    }

    public static GenerativeUrlBuilder fromFeed(Feed feed, boolean noFallbackText) {
        String imageUrl = feed.getImageUrl();
        boolean initialized = feed.getType() != null;

        // Don't use imageUrl if it's not a valid URL (relative paths, local files, etc.)
        if (imageUrl != null && !imageUrl.startsWith("http")) {
            imageUrl = null; // Force placeholder generation for invalid URLs
            if (feed.isLocalFeed()) {
                initialized = true;
            }
        }

        return new GenerativeUrlBuilder(
                imageUrl,
                null,
                feed.getTitle(),
                feed.getDownloadUrl(),
                initialized,
                noFallbackText);
    }

    public static GenerativeUrlBuilder fromFeedItem(FeedItem item) {
        String imageUrl = ImageResourceUtils.getEpisodeListImageLocation(item);
        String fallbackUrl = item.getFeed() != null ? item.getFeed().getImageUrl() : null;
        String fallbackText = item.getFeed() != null ? item.getFeed().getTitle() : item.getTitle();
        String feedDownloadUrl = item.getFeed() != null ? item.getFeed().getDownloadUrl() : null;
        return new GenerativeUrlBuilder(imageUrl, fallbackUrl, fallbackText, feedDownloadUrl);
    }

    public static GenerativeUrlBuilder fromMedia(Playable playable, Chapter chapter) {
        return new GenerativeUrlBuilder(
                chapter.getImageUrl(),
                playable.getImageLocation(),
                chapter.getTitle(),
                playable.getFeedDownloadUrl());
    }
}
