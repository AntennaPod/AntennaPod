package de.danoeh.antennapod.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FeedItemUtilTest {
    private static final String FEED_LINK = "http://example.com";
    private static final String ITEM_LINK = "http://example.com/feedItem1";

    private final String msg;
    private final String feedLink;
    private final String itemLink;
    private final String expected;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"average", FEED_LINK, ITEM_LINK, ITEM_LINK},
                {"null item link - fallback to feed", FEED_LINK, null, FEED_LINK},
                {"empty item link - same as null", FEED_LINK, "", FEED_LINK},
                {"blank item link - same as null", FEED_LINK, "  ", FEED_LINK},
                {"fallback, but feed link is null too", null, null, null},
                {"fallback - but empty feed link - same as null", "", null, null},
                {"fallback - but blank feed link - same as null", "  ", null, null}
        });
    }

    public FeedItemUtilTest(String msg, String feedLink, String itemLink, String expected) {
        this.msg = msg;
        this.feedLink = feedLink;
        this.itemLink = itemLink;
        this.expected = expected;
    }


    // Test the getIds() method
    @Test
    public void testGetIds() {
        List<FeedItem> feedItemsList = new ArrayList<FeedItem>(5);
        List<Integer> idList = new ArrayList<Integer>();

        idList.add(980);
        idList.add(324);
        idList.add(226);
        idList.add(164);
        idList.add(854);

        for (int i = 0; i < 5; i++) {
            FeedItem item = createFeedItem(feedLink, itemLink);
            item.setId(idList.get(i));
            feedItemsList.add(item);
        }

        long[] actual = FeedItemUtil.getIds(feedItemsList);

        // covers edge case for getIds() method
        List<FeedItem> emptyList = new ArrayList<FeedItem>();
        long[] testEmptyList = FeedItemUtil.getIds(emptyList);
        assertEquals(msg, 0, testEmptyList.length);
        assertEquals(msg, 980, actual[0]);
        assertEquals(msg, 324, actual[1]);
        assertEquals(msg, 226, actual[2]);
        assertEquals(msg, 164, actual[3]);
        assertEquals(msg, 854, actual[4]);

    }

    // Tests the Null value for getLinkWithFallback() method
    @Test
    public void testLinkWithFallbackNullValue() {
        String actual = FeedItemUtil.getLinkWithFallback(null);
        assertEquals(msg, null, actual);
    }


    @Test
    public void testLinkWithFallback() {
        String actual = FeedItemUtil.getLinkWithFallback(createFeedItem(feedLink, itemLink));
        assertEquals(msg, expected, actual);
    }

    private static FeedItem createFeedItem(String feedLink, String itemLink) {
        Feed feed = new Feed();
        feed.setLink(feedLink);
        FeedItem feedItem = new FeedItem();
        feedItem.setLink(itemLink);
        feedItem.setFeed(feed);
        feed.setItems(Collections.singletonList(feedItem));
        return feedItem;
    }
}
