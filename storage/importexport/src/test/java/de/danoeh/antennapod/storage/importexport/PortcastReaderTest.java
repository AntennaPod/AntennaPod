package de.danoeh.antennapod.storage.importexport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class PortcastReaderTest {

    private static final String SAMPLE = "{"
            + "\"portcast\":\"0.2.0\","
            + "\"generatedAt\":\"2026-01-01T00:00:00Z\","
            + "\"generator\":{\"name\":\"Other App\",\"version\":\"1.0\"},"
            + "\"subscriptions\":["
            + "  {\"subscriptionId\":\"https://example.com/feed.xml\",\"feedUrl\":\"https://example.com/feed.xml\","
            + "   \"podcastGuid\":\"guid-1\",\"title\":\"Test Podcast\",\"updatedAt\":\"2026-01-01T00:00:00Z\","
            + "   \"tags\":[\"News\",\"Tech\"],\"notificationsEnabled\":true}"
            + "],"
            + "\"episodes\":["
            + "  {\"episodeStateId\":\"e1\",\"subscriptionRef\":{\"podcastGuid\":\"guid-1\"},\"guid\":\"ep-1\","
            + "   \"enclosureUrl\":\"https://example.com/1.mp3\",\"status\":\"completed\","
            + "   \"updatedAt\":\"2026-01-02T03:04:05Z\",\"lastPlayedAt\":\"2026-01-02T03:04:05Z\","
            + "   \"title\":\"Episode 1\",\"durationSeconds\":1800,\"starred\":true},"
            + "  {\"episodeStateId\":\"e2\",\"subscriptionRef\":{\"feedUrl\":\"https://example.com/feed.xml\"},"
            + "   \"guid\":\"ep-2\",\"status\":\"in_progress\",\"positionSeconds\":42,"
            + "   \"updatedAt\":\"2026-01-02T03:04:05Z\"}"
            + "],"
            + "\"queue\":["
            + "  {\"position\":1,\"episodeRef\":{\"guid\":\"ep-2\",\"enclosureUrl\":\"https://example.com/2.mp3\"}}"
            + "],"
            + "\"preferences\":{"
            + "  \"global\":{\"playbackRate\":1.0,\"skipForwardSeconds\":30,\"skipBackwardSeconds\":10},"
            + "  \"perFeed\":{\"https://example.com/feed.xml\":{\"playbackRate\":1.5,"
            + "     \"skipIntroSeconds\":30,\"skipOutroSeconds\":20,\"trimSilence\":true}}"
            + "},"
            + "\"bookmarks\":[{\"bookmarkId\":\"b1\",\"episodeRef\":{\"guid\":\"ep-1\"},\"atSeconds\":10,"
            + "   \"createdAt\":\"2026-01-01T00:00:00Z\"}],"
            + "\"completeness\":[{\"section\":\"episodes\",\"source\":\"Other App\",\"level\":\"full\"}]"
            + "}";

    @Test
    public void testReadsSubscriptionAndTags() throws Exception {
        PortcastDocument document = new PortcastReader().readDocument(new StringReader(SAMPLE));
        assertEquals(1, document.getSubscriptions().size());
        PortcastSubscription subscription = document.getSubscriptions().get(0);
        assertEquals("Test Podcast", subscription.getTitle());
        assertEquals("https://example.com/feed.xml", subscription.getFeedUrl());
        assertEquals("guid-1", subscription.getPodcastGuid());
        assertEquals(2, subscription.getTags().size());
        assertTrue(subscription.getTags().contains("News"));
        assertEquals(Boolean.TRUE, subscription.getNotificationsEnabled());
    }

    @Test
    public void testEpisodesGroupedByObjectRef() throws Exception {
        PortcastDocument document = new PortcastReader().readDocument(new StringReader(SAMPLE));
        PortcastSubscription subscription = document.getSubscriptions().get(0);
        assertEquals(2, subscription.getEpisodes().size());

        PortcastEpisode completed = findByGuid(subscription, "ep-1");
        assertEquals(PortcastDocument.STATUS_COMPLETED, completed.getStatus());
        assertNotNull(completed.getLastPlayedAt());
        assertEquals("Episode 1", completed.getTitle());
        assertEquals(1800, completed.getDurationSeconds());
        assertTrue(completed.isStarred());

        PortcastEpisode inProgress = findByGuid(subscription, "ep-2");
        assertEquals(PortcastDocument.STATUS_IN_PROGRESS, inProgress.getStatus());
        assertEquals(42, inProgress.getPositionSeconds());
    }

    @Test
    public void testReadsQueueFromEpisodeRef() throws Exception {
        PortcastDocument document = new PortcastReader().readDocument(new StringReader(SAMPLE));
        assertEquals(1, document.getQueue().size());
        assertEquals("ep-2", document.getQueue().get(0).getGuid());
        assertEquals("https://example.com/2.mp3", document.getQueue().get(0).getEnclosureUrl());
    }

    @Test
    public void testReadsPerFeedPreferences() throws Exception {
        PortcastDocument document = new PortcastReader().readDocument(new StringReader(SAMPLE));
        PortcastSubscription subscription = document.getSubscriptions().get(0);
        assertEquals(1.5f, subscription.getPlaybackSpeed(), 0.001f);
        assertEquals(Integer.valueOf(30), subscription.getSkipIntroSeconds());
        assertEquals(Integer.valueOf(20), subscription.getSkipEndingSeconds());
    }

    @Test
    public void testNonPortcastFileThrows() {
        try {
            new PortcastReader().readDocument(new StringReader("{\"foo\":\"bar\"}"));
            fail("Expected IOException");
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
        }
    }

    private PortcastEpisode findByGuid(PortcastSubscription subscription, String guid) {
        for (PortcastEpisode episode : subscription.getEpisodes()) {
            if (guid.equals(episode.getGuid())) {
                return episode;
            }
        }
        return null;
    }
}
