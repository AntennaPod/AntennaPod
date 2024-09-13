package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NonSubscribedFeedsCleanerTest {

    @Test
    public void testSubscribed() {
        Feed feed = createFeed();
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));

        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    @Test
    public void testOldDate() {
        Feed feed = createFeed();
        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        feed.setLastRefreshAttempt(System.currentTimeMillis());
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));

        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));

        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));

        feed.setLastRefreshAttempt(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    @Test
    public void testPlayedItem() {
        Feed feed = createFeed();
        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        FeedItem item = createItem(feed);
        feed.getItems().add(item);

        item.setPlayed(false);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.DAYS));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));

        item.setPlayed(true);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(10, TimeUnit.DAYS));
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));

        item.setPlayed(true);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(100, TimeUnit.DAYS));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    @Test
    public void testQueuedItem() {
        Feed feed = createFeed();
        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        feed.getItems().add(createItem(feed));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));

        FeedItem queuedItem = createItem(feed);
        queuedItem.addTag(FeedItem.TAG_QUEUE);
        feed.getItems().add(queuedItem);
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    @Test
    public void testFavoriteItem() {
        Feed feed = createFeed();
        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        feed.getItems().add(createItem(feed));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));

        FeedItem queuedItem = createItem(feed);
        queuedItem.addTag(FeedItem.TAG_FAVORITE);
        feed.getItems().add(queuedItem);
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    @Test
    public void testDownloadedItem() {
        Feed feed = createFeed();
        feed.setState(Feed.STATE_NOT_SUBSCRIBED);
        feed.setLastRefreshAttempt(System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(200, TimeUnit.DAYS));
        feed.getItems().add(createItem(feed));
        assertTrue(NonSubscribedFeedsCleaner.shouldDelete(feed));

        FeedItem queuedItem = createItem(feed);
        queuedItem.getMedia().setDownloaded(true, System.currentTimeMillis());
        feed.getItems().add(queuedItem);
        assertFalse(NonSubscribedFeedsCleaner.shouldDelete(feed));
    }

    private Feed createFeed() {
        Feed feed = new Feed(0, null, "title", "http://example.com", "This is the description",
                "http://example.com/payment", "Daniel", "en", null, "http://example.com/feed",
                "http://example.com/image", null, "http://example.com/feed", System.currentTimeMillis());
        feed.setItems(new ArrayList<>());
        return feed;
    }

    private FeedItem createItem(Feed feed) {
        FeedItem item = new FeedItem(0, "Item", "ItemId", "url", new Date(), FeedItem.PLAYED, feed);
        FeedMedia media = new FeedMedia(item, "http://download.url.net/", 1234567, "audio/mpeg");
        media.setId(item.getId());
        item.setMedia(media);
        return item;
    }
}
