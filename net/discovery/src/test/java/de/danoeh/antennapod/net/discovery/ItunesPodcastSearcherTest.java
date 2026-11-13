package de.danoeh.antennapod.net.discovery;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ItunesPodcastSearcherTest {

    @Test
    public void testUrlNeedsLookup() {
        ItunesPodcastSearcher searcher = new ItunesPodcastSearcher();
        assertTrue(searcher.urlNeedsLookup("https://itunes.apple.com/us/podcast/id123456"));
        assertTrue(searcher.urlNeedsLookup("https://podcasts.apple.com/us/podcast/my-podcast/id123456"));
        assertFalse(searcher.urlNeedsLookup("https://example.com/feed.xml"));
    }

    @Test
    public void testGetName() {
        ItunesPodcastSearcher searcher = new ItunesPodcastSearcher();
        assertEquals("Apple", searcher.getName());
    }

    @Test
    public void testParseEpisodesJson() throws Exception {
        String json = "{\"results\": ["
                + "{"
                + "\"trackName\": \"Episode 1\","
                + "\"collectionName\": \"My Podcast\","
                + "\"feedUrl\": \"https://example.com/feed.xml\","
                + "\"episodeUrl\": \"https://example.com/ep1.mp3\","
                + "\"artworkUrl600\": \"https://example.com/art600.jpg\","
                + "\"episodeGuid\": \"guid-123\","
                + "\"trackTimeMillis\": 120000,"
                + "\"description\": \"Full description\","
                + "\"artistName\": \"Author Name\","
                + "\"releaseDate\": \"2023-01-01T12:00:00Z\""
                + "},"
                + "{"
                + "\"trackName\": \"Episode 2 Fallback\","
                + "\"collectionName\": \"My Podcast\","
                + "\"feedUrl\": \"https://example.com/feed.xml\","
                + "\"previewUrl\": \"https://example.com/ep2-preview.mp3\","
                + "\"artworkUrl160\": \"https://example.com/art160.jpg\","
                + "\"shortDescription\": \"Short description\","
                + "\"artistName\": \"Author Name\""
                + "},"
                + "{"
                + "\"trackName\": \"Skipped - No Feed Url\","
                + "\"episodeUrl\": \"https://example.com/ep3.mp3\""
                + "},"
                + "{"
                + "\"trackName\": \"Skipped - No Episode Url\","
                + "\"feedUrl\": \"https://example.com/feed.xml\""
                + "}"
                + "]}";

        List<EpisodeSearchResult> results = ItunesPodcastSearcher.parseEpisodesJson(new JSONObject(json));
        assertEquals(2, results.size());

        EpisodeSearchResult first = results.get(0);
        assertEquals("Episode 1", first.title);
        assertEquals("My Podcast", first.podcastTitle);
        assertEquals("https://example.com/feed.xml", first.feedUrl);
        assertEquals("https://example.com/ep1.mp3", first.episodeUrl);
        assertEquals("https://example.com/art600.jpg", first.imageUrl);
        assertEquals("guid-123", first.episodeGuid);
        assertEquals(120000L, first.durationMs);
        assertEquals("Full description", first.description);
        assertEquals("Author Name", first.author);
        assertNotNull(first.pubDate);

        EpisodeSearchResult second = results.get(1);
        assertEquals("Episode 2 Fallback", second.title);
        assertEquals("https://example.com/ep2-preview.mp3", second.episodeUrl);
        assertEquals("https://example.com/art160.jpg", second.imageUrl);
        assertEquals("Short description", second.description);
        assertNull(second.pubDate);
    }
}
