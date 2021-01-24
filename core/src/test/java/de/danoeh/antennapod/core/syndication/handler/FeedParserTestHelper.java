package de.danoeh.antennapod.core.syndication.handler;

import androidx.annotation.NonNull;

import java.io.File;

import de.danoeh.antennapod.core.feed.Feed;

/**
 * Tests for FeedHandler.
 */
public abstract class FeedParserTestHelper {

    /**
     * Returns the File object for a file in the resources folder.
     */
    @NonNull
    static File getFeedFile(@NonNull String fileName) {
        //noinspection ConstantConditions
        return new File(FeedParserTestHelper.class.getClassLoader().getResource(fileName).getFile());
    }

    /**
     * Runs the feed parser on the given file.
     */
    @NonNull
    static Feed runFeedParser(@NonNull File feedFile) throws Exception {
        FeedHandler handler = new FeedHandler();
        Feed parsedFeed = new Feed("http://example.com/feed", null);
        parsedFeed.setFile_url(feedFile.getAbsolutePath());
        parsedFeed.setDownloaded(true);
        handler.parseFeed(parsedFeed);
        return parsedFeed;
    }
}
