package de.danoeh.antennapod.discovery.searchresultmapper;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import de.danoeh.antennapod.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapperTest extends TestCase {

    public void testGetPodcastSearchResult(){
        JSONObject jsonResponse = null;
        try {
            InputStream inputStream = getClass().getResourceAsStream("audiothek_search_response.json");
            String rawResponseJson = IOUtils.toString(inputStream, Charsets.UTF_8);
            jsonResponse = new JSONObject(rawResponseJson);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PodcastSearchResult podcastSearchResult = AudiothekSearchResultMapper.getPodcastSearchResult(jsonResponse);
        assertEquals("", podcastSearchResult.title);
        assertEquals(null, podcastSearchResult.author);
        assertEquals(null, podcastSearchResult.feedUrl);
        assertEquals(null, podcastSearchResult.imageUrl);
    }
}