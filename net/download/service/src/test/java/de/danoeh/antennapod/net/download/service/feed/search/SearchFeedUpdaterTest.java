package de.danoeh.antennapod.net.download.service.feed.search;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.discovery.EpisodeSearchResult;
import de.danoeh.antennapod.net.discovery.SearchFeedBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class SearchFeedUpdaterTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testGetQuery() {
        assertEquals("", SearchFeedUpdater.getQuery(null));

        Feed feedWithNullUrl = new Feed(null, null, "Null URL");
        assertEquals("", SearchFeedUpdater.getQuery(feedWithNullUrl));

        Feed searchFeed = new Feed(Feed.PREFIX_SEARCH_FEED + "android news", null, "Search");
        assertEquals("android news", SearchFeedUpdater.getQuery(searchFeed));

        Feed normalFeed = new Feed("https://example.com/feed.xml", null, "Normal");
        assertEquals("https://example.com/feed.xml", SearchFeedUpdater.getQuery(normalFeed));
    }

    @Test
    public void testCreateFeedItem() {
        Feed feed = new Feed(Feed.PREFIX_SEARCH_FEED + "tech", null, "Tech Search");
        Date now = new Date();
        EpisodeSearchResult episodeWithGuid = new EpisodeSearchResult(
                "Episode Title", "Podcast Title", "https://example.com/art.jpg",
                "https://example.com/feed.xml", "https://example.com/ep.mp3",
                "guid-12345", now, 60000L, "Desc", "Author");

        FeedItem item = SearchFeedUpdater.createFeedItem(feed, episodeWithGuid);
        assertEquals("Episode Title", item.getTitle());
        assertEquals("guid-12345", item.getItemIdentifier());
        assertEquals("https://example.com/ep.mp3", item.getLink());
        assertEquals(now, item.getPubDate());
        assertEquals("Desc", item.getDescription());
        assertEquals("https://example.com/art.jpg", item.getImageUrl());
        assertNotNull(item.getMedia());
        assertEquals("https://example.com/ep.mp3", item.getMedia().getDownloadUrl());
        assertEquals(60000, item.getMedia().getDuration());

        EpisodeSearchResult episodeWithoutGuid = new EpisodeSearchResult(
                "Episode Title 2", "Podcast Title", "https://example.com/art.jpg",
                "https://example.com/feed.xml", "https://example.com/ep2.mp3",
                null, now, 30000L, "Desc", "Author");

        FeedItem item2 = SearchFeedUpdater.createFeedItem(feed, episodeWithoutGuid);
        assertEquals("https://example.com/ep2.mp3", item2.getItemIdentifier());
    }

    @Test
    public void testBuildFeed() {
        Date now = new Date();
        EpisodeSearchResult episode = new EpisodeSearchResult(
                "Episode 1", "Podcast Title", "https://example.com/art.jpg",
                "https://example.com/feed.xml", "https://example.com/ep1.mp3",
                "guid-1", now, 45000L, "Desc", "Author");

        Feed feed = SearchFeedBuilder.buildFeed(context, "science", Collections.singletonList(episode));
        assertEquals(Feed.PREFIX_SEARCH_FEED + "science", feed.getDownloadUrl());
        assertEquals(Feed.PREFIX_SEARCH_COVER + "science", feed.getImageUrl());
        assertEquals(Feed.STATE_SUBSCRIBED, feed.getState());
        assertEquals(1, feed.getItems().size());
        assertEquals("Episode 1", feed.getItems().get(0).getTitle());
    }
}
