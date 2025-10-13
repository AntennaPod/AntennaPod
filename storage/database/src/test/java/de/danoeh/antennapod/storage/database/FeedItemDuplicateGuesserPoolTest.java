package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class FeedItemDuplicateGuesserPoolTest {

    @Test
    public void testDuplicateIsConsistent() {
        Feed feed = new Feed("url", null, null);
        FeedItem item1 = createItem("id1", "Title", feed);
        FeedItem item2 = createItem("id2", "Title", feed);

        FeedItemDuplicateGuesserPool pool = new FeedItemDuplicateGuesserPool(new ArrayList<>());
        pool.add(item1);
        assertSame(item1, pool.guessDuplicate(item1));
        assertSame(item1, pool.guessDuplicate(item2));
        pool.add(item2);
        assertSame(item1, pool.guessDuplicate(item1));
        assertSame(item1, pool.guessDuplicate(item2));
    }

    private FeedItem createItem(String identifier, String title, Feed feed) {
        FeedItem item = new FeedItem();
        item.setItemIdentifier(identifier);
        item.setTitle(title);
        item.setMedia(new FeedMedia(item, "url-" + title, 2, "mime"));
        item.setFeed(feed);
        return item;
    }
}
