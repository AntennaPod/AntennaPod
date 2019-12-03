package de.danoeh.antennapodSA.core.feed.util;

import de.danoeh.antennapodSA.core.asynctask.ImageResource;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.feed.FeedMedia;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;

/**
 * Utility class to use the appropriate image resource based on {@link UserPreferences}
 */
public final class ImageResourceUtils {

    private ImageResourceUtils() {
    }

    public static String getImageLocation(ImageResource resource) {
        if (UserPreferences.getUseEpisodeCoverSetting()) {
            return resource.getImageLocation();
        } else {
            return getShowImageLocation(resource);
        }
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
