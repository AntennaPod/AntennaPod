package de.danoeh.antennapod.core.util;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class FeedUtil {
    public static boolean shouldAutoDeleteItemsOnThatFeed(Feed feed) {
        if (!UserPreferences.isAutoDelete()) {
            return false;
        }
        return !feed.isLocalFeed() || UserPreferences.isAutoDeleteLocal();
    }
}
