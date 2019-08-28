package de.test.antennapod.feed;

import android.support.test.filters.SmallTest;
import de.danoeh.antennapod.core.feed.FeedItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SmallTest
public class FeedItemTest {
    private static final String TEXT_LONG = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String TEXT_SHORT = "Lorem ipsum";

    /**
     * If one of `description` or `content:encoded` is null, use the other one.
     */
    @Test
    public void testShownotesNullValues() throws Exception {
        testShownotes(null, TEXT_LONG);
        testShownotes(TEXT_LONG, null);
    }

    /**
     * If `description` is reasonably longer than `content:encoded`, use `description`.
     */
    @Test
    public void testShownotesLength() throws Exception {
        testShownotes(TEXT_SHORT, TEXT_LONG);
        testShownotes(TEXT_LONG, TEXT_SHORT);
    }

    /**
     * Checks if the shownotes equal TEXT_LONG, using the given `description` and `content:encoded`
     * @param description Description of the feed item
     * @param contentEncoded `content:encoded` of the feed item
     */
    private void testShownotes(String description, String contentEncoded) throws Exception {
        FeedItem item = new FeedItem();
        item.setDescription(description);
        item.setContentEncoded(contentEncoded);
        assertEquals(TEXT_LONG, item.loadShownotes().call());
    }
}
