package de.danoeh.antennapod.core.feed;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import de.danoeh.antennapod.core.storage.DBReader;

import static org.junit.Assert.assertEquals;

public class FeedItemTest2 {
    private static final String TEXT_LONG = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String TEXT_SHORT = "Lorem ipsum";

    @BeforeClass
    public static void beforeClass() {
        //noinspection ResultOfMethodCallIgnored
        Mockito.mockStatic(DBReader.class);
    }

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
