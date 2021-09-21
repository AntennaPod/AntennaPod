package de.danoeh.antennapod.discovery.audiothek;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.danoeh.antennapod.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapperTest extends TestCase {

    public void testGetPodcastSearchResult() {
        JSONObject jsonResponse = null;
        try {
            InputStream inputStream = getClass()
                    .getResourceAsStream("audiothek_search_response_single_programSet.json");
            String rawResponseJson = IOUtils.toString(inputStream, Charsets.UTF_8);
            jsonResponse = new JSONObject(rawResponseJson);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PodcastSearchResult podcastSearchResult = null;
        try {
            podcastSearchResult = AudiothekSearchResultMapper.getPodcastSearchResult(jsonResponse);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert podcastSearchResult != null;
        assertEquals("Klimazentrale - Der Talk zu Klima & Umwelt", podcastSearchResult.title);
        assertEquals("SWR", podcastSearchResult.author);
        assertEquals("https://api.ardaudiothek.de/./programsets/64922226", podcastSearchResult.feedUrl);
        assertEquals("https://img.ardmediathek.de/standard/00/74/40/73/32/-407010077/{ratio}/{width}?mandant=ard", podcastSearchResult.imageUrl);
    }

    public void testExtractPodcasts() throws IOException, JSONException {
        InputStream inputStream = getClass()
                .getResourceAsStream("audiothek_search_response.json");
        String rawResponseJson = null;
        JSONObject searchResponseJson = null;
        rawResponseJson = IOUtils.toString(inputStream, Charsets.UTF_8);
        searchResponseJson = new JSONObject(rawResponseJson);
        List<PodcastSearchResult> podcastSearchResults = AudiothekSearchResultMapper.extractPodcasts(searchResponseJson);
        assertSame(6, podcastSearchResults.size());

        PodcastSearchResult podcastSearchResult = podcastSearchResults.get(0);
        assertEquals("Klimazentrale - Der Talk zu Klima & Umwelt", podcastSearchResult.title);
        assertEquals("SWR", podcastSearchResult.author);
        assertEquals("https://api.ardaudiothek.de/./programsets/64922226", podcastSearchResult.feedUrl);
        assertEquals("https://img.ardmediathek.de/standard/00/74/40/73/32/-407010077/{ratio}/{width}?mandant=ard", podcastSearchResult.imageUrl);
    }
}