package de.danoeh.antennapod.storage.importexport;

import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PortcastWriterTest {
    private long nextId = 1;

    @Before
    public void setUp() {
        UserPreferences.init(ApplicationProvider.getApplicationContext());
    }

    private Feed createFeed(String title, String downloadUrl, String guid) {
        Feed feed = new Feed(0, null, title, null, null, null, null, null, "rss", guid, null, null, downloadUrl, 0);
        feed.setPreferences(new FeedPreferences(0, FeedPreferences.AutoDownloadSetting.GLOBAL,
                FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF,
                FeedPreferences.NewEpisodesAction.GLOBAL, null, null));
        feed.setItems(new ArrayList<>());
        return feed;
    }

    private FeedItem addItem(Feed feed, String guid, String enclosureUrl, int state, int positionMs) {
        FeedItem item = new FeedItem(nextId++, guid, guid, null, null, state, feed);
        FeedMedia media = new FeedMedia(item, enclosureUrl, 0, "audio/mpeg");
        media.setPosition(positionMs);
        item.setMedia(media);
        feed.getItems().add(item);
        return item;
    }

    private PortcastDocument writeAndRead(List<Feed> feeds, List<FeedItem> queue, Set<Long> favoriteIds)
            throws Exception {
        StringWriter writer = new StringWriter();
        PortcastWriter.writeDocument(feeds, queue, favoriteIds, writer, ApplicationProvider.getApplicationContext());
        return new PortcastReader().readDocument(new StringReader(writer.toString()));
    }

    @Test
    public void testWritesSubscriptionWithGuid() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), Collections.emptySet());

        assertEquals(1, document.getSubscriptions().size());
        PortcastSubscription subscription = document.getSubscriptions().get(0);
        assertEquals("My Podcast", subscription.getTitle());
        assertEquals("https://example.com/feed.xml", subscription.getFeedUrl());
        assertEquals("guid-123", subscription.getPodcastGuid());
    }

    @Test
    public void testGuidNotWrittenWhenEqualToUrl() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "https://example.com/feed.xml");
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), Collections.emptySet());

        assertNull(document.getSubscriptions().get(0).getPodcastGuid());
    }

    @Test
    public void testOnlyEpisodesWithStateAreWritten() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        addItem(feed, "played", "https://example.com/1.mp3", FeedItem.PLAYED, 0);
        addItem(feed, "in-progress", "https://example.com/2.mp3", FeedItem.UNPLAYED, 65000);
        addItem(feed, "untouched", "https://example.com/3.mp3", FeedItem.UNPLAYED, 0);
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), Collections.emptySet());

        List<PortcastEpisode> episodes = document.getSubscriptions().get(0).getEpisodes();
        assertEquals(2, episodes.size());

        PortcastEpisode played = findByGuid(episodes, "played");
        assertEquals(PortcastDocument.STATUS_COMPLETED, played.getStatus());

        PortcastEpisode inProgress = findByGuid(episodes, "in-progress");
        assertEquals(PortcastDocument.STATUS_IN_PROGRESS, inProgress.getStatus());
        assertEquals(65, inProgress.getPositionSeconds());
    }

    @Test
    public void testStarredUntouchedEpisodeIsWritten() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        FeedItem favorite = addItem(feed, "fav", "https://example.com/fav.mp3", FeedItem.UNPLAYED, 0);
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), new HashSet<>(Collections.singletonList(favorite.getId())));

        List<PortcastEpisode> episodes = document.getSubscriptions().get(0).getEpisodes();
        assertEquals(1, episodes.size());
        assertEquals(PortcastDocument.STATUS_UNPLAYED, episodes.get(0).getStatus());
        assertTrue(episodes.get(0).isStarred());
    }

    @Test
    public void testQueueIsWrittenInOrder() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        FeedItem first = addItem(feed, "first", "https://example.com/1.mp3", FeedItem.UNPLAYED, 0);
        FeedItem second = addItem(feed, "second", "https://example.com/2.mp3", FeedItem.UNPLAYED, 0);
        List<FeedItem> queue = new ArrayList<>();
        queue.add(first);
        queue.add(second);
        PortcastDocument document = writeAndRead(Collections.singletonList(feed), queue, Collections.emptySet());

        assertEquals(2, document.getQueue().size());
        assertEquals("first", document.getQueue().get(0).getGuid());
        assertEquals("second", document.getQueue().get(1).getGuid());
    }

    @Test
    public void testQueuedUnplayedEpisodeIsExportedAsEpisode() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        FeedItem queued = addItem(feed, "queued", "https://example.com/q.mp3", FeedItem.UNPLAYED, 0);
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.singletonList(queued), Collections.emptySet());

        List<PortcastEpisode> episodes = document.getSubscriptions().get(0).getEpisodes();
        assertEquals(1, episodes.size());
        assertEquals("queued", episodes.get(0).getGuid());
        assertEquals(1, document.getQueue().size());
        assertEquals("queued", document.getQueue().get(0).getGuid());
    }

    @Test
    public void testOnlySubscribedFeedsExported() throws Exception {
        Feed subscribed = createFeed("Subscribed", "https://example.com/sub.xml", null);
        Feed archived = createFeed("Archived", "https://example.com/archived.xml", null);
        archived.setState(Feed.STATE_ARCHIVED);
        PortcastDocument document = writeAndRead(Arrays.asList(subscribed, archived),
                Collections.emptyList(), Collections.emptySet());

        assertEquals(1, document.getSubscriptions().size());
        assertEquals("Subscribed", document.getSubscriptions().get(0).getTitle());
    }

    @Test
    public void testTagsAreWritten() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        feed.getPreferences().getTags().add("News");
        feed.getPreferences().getTags().add("#root");
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), Collections.emptySet());

        assertTrue(document.getSubscriptions().get(0).getTags().contains("News"));
        assertEquals(1, document.getSubscriptions().get(0).getTags().size());
    }

    @Test
    public void testPerFeedPreferencesRoundTrip() throws Exception {
        Feed feed = createFeed("My Podcast", "https://example.com/feed.xml", "guid-123");
        feed.getPreferences().setFeedPlaybackSpeed(1.5f);
        feed.getPreferences().setFeedSkipIntro(15);
        feed.getPreferences().setFeedSkipEnding(40);
        PortcastDocument document = writeAndRead(Collections.singletonList(feed),
                Collections.emptyList(), Collections.emptySet());

        PortcastSubscription subscription = document.getSubscriptions().get(0);
        assertEquals(1.5f, subscription.getPlaybackSpeed(), 0.001f);
        assertEquals(Integer.valueOf(15), subscription.getSkipIntroSeconds());
        assertEquals(Integer.valueOf(40), subscription.getSkipEndingSeconds());
    }

    private PortcastEpisode findByGuid(List<PortcastEpisode> episodes, String guid) {
        for (PortcastEpisode episode : episodes) {
            if (guid.equals(episode.getGuid())) {
                return episode;
            }
        }
        return null;
    }
}
