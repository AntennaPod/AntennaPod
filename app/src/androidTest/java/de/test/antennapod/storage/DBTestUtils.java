package de.test.antennapod.storage;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;

/**
 * Utility methods for DB* tests.
 */
class DBTestUtils {

    /**
     * Use this method when tests don't involve chapters.
     */
    public static List<Feed> saveFeedlist(int numFeeds, int numItems, boolean withMedia) {
        return saveFeedlist(numFeeds, numItems, withMedia, false, 0);
    }

    /**
     * Use this method when tests involve chapters.
     */
    public static List<Feed> saveFeedlist(int numFeeds, int numItems, boolean withMedia,
                                          boolean withChapters, int numChapters) {
        if (numFeeds <= 0) {
            throw new IllegalArgumentException("numFeeds<=0");
        }
        if (numItems < 0) {
            throw new IllegalArgumentException("numItems<0");
        }

        List<Feed> feeds = new ArrayList<>();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        for (int i = 0; i < numFeeds; i++) {
            Feed f = new Feed(0, null, "feed " + i, null, "link" + i, "descr", null, null,
                    null, null, "id" + i, null, null, "url" + i, false, new FlattrStatus(), false, null, null, false);
            f.setItems(new ArrayList<>());
            for (int j = 0; j < numItems; j++) {
                FeedItem item = new FeedItem(0, "item " + j, "id" + j, "link" + j, new Date(),
                        FeedItem.PLAYED, f, withChapters);
                if (withMedia) {
                    FeedMedia media = new FeedMedia(item, "url" + j, 1, "audio/mp3");
                    item.setMedia(media);
                }
                if (withChapters) {
                    List<Chapter> chapters = new ArrayList<>();
                    item.setChapters(chapters);
                    for (int k = 0; k < numChapters; k++) {
                        chapters.add(new SimpleChapter(k, "item " + j + " chapter " + k, item, "http://example.com"));
                    }
                }
                f.getItems().add(item);
            }
            Collections.sort(f.getItems(), new FeedItemPubdateComparator());
            adapter.setCompleteFeed(f);
            Assert.assertTrue(f.getId() != 0);
            for (FeedItem item : f.getItems()) {
                Assert.assertTrue(item.getId() != 0);
            }
            feeds.add(f);
        }
        adapter.close();

        return feeds;
    }
}
