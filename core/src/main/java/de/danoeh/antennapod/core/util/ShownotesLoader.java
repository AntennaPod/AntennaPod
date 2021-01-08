package de.danoeh.antennapod.core.util;

import java.util.concurrent.Callable;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;

public class ShownotesLoader {

    public static Callable<String> loadShownotes(ShownotesProvider provider) {
        if(provider instanceof FeedItem)
            return loadForFeedItem((FeedItem) provider);
        else if(provider instanceof FeedMedia)
            return loadForFeedMedia((FeedMedia) provider);
        else
            return provider.loadShownotes();
    }

    private static Callable<String> loadForFeedMedia(FeedMedia provider) {
        return () -> {
            if (!provider.hasItem()) {
                provider.setItem(DBReader.getFeedItem(provider.getItemID()));
            }
            return ShownotesLoader.loadForFeedItem(provider.getItem()).call();
        };
    }

    private static Callable<String> loadForFeedItem(FeedItem provider) {
        return () -> {
            if (provider.shouldLoadItemDescription()) {
                DBReader.loadDescriptionOfFeedItem(provider);
            }
            return provider.getShownotes();
        };
    }
}
