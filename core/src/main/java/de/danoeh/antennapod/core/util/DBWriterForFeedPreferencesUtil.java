package de.danoeh.antennapod.core.util;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;

public class DBWriterForFeedPreferencesUtil {
    public static void saveFeedPreferences(Feed feed) {
        DBWriter.setFeedPreferences(feed.getPreferences());
    }
}
