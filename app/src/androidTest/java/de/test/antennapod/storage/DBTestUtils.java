package de.test.antennapod.storage;

import android.content.Context;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Utility methods for DB* tests.
 */
public class DBTestUtils {

    public static List<Feed> saveFeedlist(Context context, int numFeeds, int numItems, boolean withMedia) {
        if (numFeeds <= 0) {
            throw new IllegalArgumentException("numFeeds<=0");
        }
        if (numItems < 0) {
            throw new IllegalArgumentException("numItems<0");
        }

        List<Feed> feeds = new ArrayList<Feed>();
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        for (int i = 0; i < numFeeds; i++) {
            Feed f = new Feed(0, new Date(), "feed " + i, "link" + i, "descr", null, null,
                    null, null, "id" + i, null, null, "url" + i, false, new FlattrStatus(), false, null);
            f.setItems(new ArrayList<FeedItem>());
            for (int j = 0; j < numItems; j++) {
                FeedItem item = new FeedItem(0, "item " + j, "id" + j, "link" + j, new Date(),
                        true, f);
                if (withMedia) {
                    FeedMedia media = new FeedMedia(item, "url" + j, 1, "audio/mp3");
                    item.setMedia(media);
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
