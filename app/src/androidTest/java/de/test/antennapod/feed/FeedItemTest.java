package de.test.antennapod.feed;

import android.test.AndroidTestCase;
import de.danoeh.antennapod.core.feed.FeedItem;

public class FeedItemTest extends AndroidTestCase {

    public void testShownoteLength() throws Exception {
        FeedItem item = new FeedItem();

        item.setDescription(null);
        item.setContentEncoded("Hello world");
        assertEquals("Hello world", item.loadShownotes().call());

        item.setDescription("");
        item.setContentEncoded("Hello world");
        assertEquals("Hello world", item.loadShownotes().call());

        item.setDescription("Hello world");
        item.setContentEncoded(null);
        assertEquals("Hello world", item.loadShownotes().call());

        item.setDescription("Hello world");
        item.setContentEncoded("");
        assertEquals("Hello world", item.loadShownotes().call());

        item.setDescription("Hi");
        item.setContentEncoded("Hello world");
        assertEquals("Hello world", item.loadShownotes().call());

        item.setDescription("Hello world");
        item.setContentEncoded("Hi");
        assertEquals("Hello world", item.loadShownotes().call());
    }
}
