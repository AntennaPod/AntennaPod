package de.danoeh.antennapod.model.feed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FeedItemFallbackLinkTest {
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

    public FeedItemFallbackLinkTest(String msg, String feedLink, String itemLink, String expected) {
        this.msg = msg;
        this.feedLink = feedLink;
        this.itemLink = itemLink;
        this.expected = expected;
    }

    @Test
    public void testLinkWithFallback() {
        String actual = createFeedItem(feedLink, itemLink).getLinkWithFallback();
        assertEquals(msg, expected, actual);
    }

    private static FeedItem createFeedItem(String feedLink, String itemLink) {
        Feed feed = new Feed("http://example.com/feed", null);
        feed.setLink(feedLink);
        FeedItem feedItem = new FeedItem();
        feedItem.setLink(itemLink);
        feedItem.setFeed(feed);
        feed.setItems(Collections.singletonList(feedItem));
        return feedItem;
    }
}
