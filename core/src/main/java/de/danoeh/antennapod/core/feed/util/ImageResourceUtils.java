package de.danoeh.antennapod.core.feed.util;

import de.danoeh.antennapod.core.asynctask.ImageResource;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Utility class to use the appropriate image resource based on {@link UserPreferences}
 */
public final class ImageResourceUtils {

    private ImageResourceUtils() {
    }

    /**
     * calls {@link ImageResourceUtils#getImageLocation(ImageResource, boolean)} without requesting a feed cover.
     */
    public static String getImageLocation(ImageResource resource) {
        return getImageLocation(resource, false);
    }


    public static String getImageLocation(ImageResource resource, boolean preferredFeedCover) {

        if (UserPreferences.getUseEpisodeCoverSetting() || !preferredFeedCover) {
            return resource.getImageLocation();
        } else {
            return getShowImageLocation(resource);
        }
    }

    public static String getFallbackImageLocation(ImageResource resource) {
        return getShowImageLocation(resource);
    }

    private static String getShowImageLocation(ImageResource resource) {

        if (resource instanceof FeedItem) {
            FeedItem item = (FeedItem) resource;
            if (item.getFeed() != null) {
                return item.getFeed().getImageLocation();
            } else {
                return null;
            }
        } else if (resource instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) resource;
            FeedItem item = media.getItem();
            if (item != null && item.getFeed() != null) {
                return item.getFeed().getImageLocation();
            } else {
                return null;
            }
        } else {
            return resource.getImageLocation();
        }
    }
}
