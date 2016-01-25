package de.test.antennapod.feed;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;

public class FeedFilterTest extends AndroidTestCase {

    public void testNullFilter() throws Exception {
        FeedFilter filter = new FeedFilter();
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        assertTrue(!filter.excludeOnly());
        assertTrue(!filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
    }

    public void testBasicIncludeFilter() throws Exception {
        String includeFilter = "Hello";
        FeedFilter filter = new FeedFilter(includeFilter, "");
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Don't include me");

        assertTrue(!filter.excludeOnly());
        assertTrue(filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals(includeFilter, filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
        assertTrue(!filter.shouldAutoDownload(item2));
    }

    public void testBasicExcludeFilter() throws Exception {
        String excludeFilter = "Hello";
        FeedFilter filter = new FeedFilter("", excludeFilter);
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Item2");

        assertTrue(filter.excludeOnly());
        assertTrue(!filter.includeOnly());
        assertEquals(excludeFilter, filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertTrue(!filter.shouldAutoDownload(item));
        assertTrue(filter.shouldAutoDownload(item2));
    }

    public void testComplexIncludeFilter() throws Exception {
        String includeFilter = "Hello \n\"Two words\"";
        FeedFilter filter = new FeedFilter(includeFilter, "");
        FeedItem item = new FeedItem();
        item.setTitle("hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Two three words");

        FeedItem item3 = new FeedItem();
        item3.setTitle("One two words");

        assertTrue(!filter.excludeOnly());
        assertTrue(filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals(includeFilter, filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
        assertTrue(!filter.shouldAutoDownload(item2));
        assertTrue(filter.shouldAutoDownload(item3));
    }

    public void testComplexExcludeFilter() throws Exception {
        String excludeFilter = "Hello \"Two words\"";
        FeedFilter filter = new FeedFilter("", excludeFilter);
        FeedItem item = new FeedItem();
        item.setTitle("hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("One three words");

        FeedItem item3 = new FeedItem();
        item3.setTitle("One two words");

        assertTrue(filter.excludeOnly());
        assertTrue(!filter.includeOnly());
        assertEquals(excludeFilter, filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertTrue(!filter.shouldAutoDownload(item));
        assertTrue(filter.shouldAutoDownload(item2));
        assertTrue(!filter.shouldAutoDownload(item3));
    }

    public void testComboFilter() throws Exception {
        String includeFilter = "Hello world";
        String excludeFilter = "dislike";
        FeedFilter filter = new FeedFilter(includeFilter, excludeFilter);

        FeedItem download = new FeedItem();
        download.setTitle("Hello everyone!");
        // because, while it has words from the include filter it also has exclude words
        FeedItem doNotDownload = new FeedItem();
        doNotDownload.setTitle("I dislike the world");
        // because it has no words from the include filter
        FeedItem doNotDownload2 = new FeedItem();
        doNotDownload2.setTitle("no words to include");

        assertTrue(filter.hasExcludeFilter());
        assertTrue(filter.hasIncludeFilter());
        assertTrue(filter.shouldAutoDownload(download));
        assertTrue(!filter.shouldAutoDownload(doNotDownload));
        assertTrue(!filter.shouldAutoDownload(doNotDownload2));
    }

}
