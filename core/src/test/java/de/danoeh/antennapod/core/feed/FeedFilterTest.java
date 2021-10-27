package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeedFilterTest {

    @Test
    public void testNullFilter() {
        FeedFilter filter = new FeedFilter();
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        assertFalse(filter.excludeOnly());
        assertFalse(filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
    }

    @Test
    public void testBasicIncludeFilter() {
        String includeFilter = "Hello";
        FeedFilter filter = new FeedFilter(includeFilter, "");
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Don't include me");

        assertFalse(filter.excludeOnly());
        assertTrue(filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals(includeFilter, filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
        assertFalse(filter.shouldAutoDownload(item2));
    }

    @Test
    public void testBasicExcludeFilter() {
        String excludeFilter = "Hello";
        FeedFilter filter = new FeedFilter("", excludeFilter);
        FeedItem item = new FeedItem();
        item.setTitle("Hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Item2");

        assertTrue(filter.excludeOnly());
        assertFalse(filter.includeOnly());
        assertEquals(excludeFilter, filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertFalse(filter.shouldAutoDownload(item));
        assertTrue(filter.shouldAutoDownload(item2));
    }

    @Test
    public void testComplexIncludeFilter() {
        String includeFilter = "Hello \n\"Two words\"";
        FeedFilter filter = new FeedFilter(includeFilter, "");
        FeedItem item = new FeedItem();
        item.setTitle("hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("Two three words");

        FeedItem item3 = new FeedItem();
        item3.setTitle("One two words");

        assertFalse(filter.excludeOnly());
        assertTrue(filter.includeOnly());
        assertEquals("", filter.getExcludeFilter());
        assertEquals(includeFilter, filter.getIncludeFilter());
        assertTrue(filter.shouldAutoDownload(item));
        assertFalse(filter.shouldAutoDownload(item2));
        assertTrue(filter.shouldAutoDownload(item3));
    }

    @Test
    public void testComplexExcludeFilter() {
        String excludeFilter = "Hello \"Two words\"";
        FeedFilter filter = new FeedFilter("", excludeFilter);
        FeedItem item = new FeedItem();
        item.setTitle("hello world");

        FeedItem item2 = new FeedItem();
        item2.setTitle("One three words");

        FeedItem item3 = new FeedItem();
        item3.setTitle("One two words");

        assertTrue(filter.excludeOnly());
        assertFalse(filter.includeOnly());
        assertEquals(excludeFilter, filter.getExcludeFilter());
        assertEquals("", filter.getIncludeFilter());
        assertFalse(filter.shouldAutoDownload(item));
        assertTrue(filter.shouldAutoDownload(item2));
        assertFalse(filter.shouldAutoDownload(item3));
    }

    @Test
    public void testComboFilter() {
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
        assertFalse(filter.shouldAutoDownload(doNotDownload));
        assertFalse(filter.shouldAutoDownload(doNotDownload2));
    }

    @Test
    public void testMinimalDurationFilter() {
        FeedItem download = new FeedItem();
        download.setTitle("Hello friend!");
        FeedMedia downloadMedia = FeedMediaMother.anyFeedMedia();
        downloadMedia.setDuration(Converter.durationStringShortToMs("05:00", false));
        download.setMedia(downloadMedia);
        // because duration of the media in unknown
        FeedItem download2 = new FeedItem();
        download2.setTitle("Hello friend!");
        FeedMedia unknownDurationMedia = FeedMediaMother.anyFeedMedia();
        download2.setMedia(unknownDurationMedia);
        // because it is not long enough
        FeedItem doNotDownload = new FeedItem();
        doNotDownload.setTitle("Hello friend!");
        FeedMedia doNotDownloadMedia = FeedMediaMother.anyFeedMedia();
        doNotDownloadMedia.setDuration(Converter.durationStringShortToMs("02:00", false));
        doNotDownload.setMedia(doNotDownloadMedia);

        int minimalDurationFilter = 3 * 60;
        FeedFilter filter = new FeedFilter("", "", minimalDurationFilter);

        assertTrue(filter.hasMinimalDurationFilter());
        assertTrue(filter.shouldAutoDownload(download));
        assertFalse(filter.shouldAutoDownload(doNotDownload));
        assertTrue(filter.shouldAutoDownload(download2));
    }

}
