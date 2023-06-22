package de.danoeh.antennapod.net.discovery.audiothek;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import de.danoeh.antennapod.net.discovery.PodcastSearchResult;

public class AudiothekSearchResultMapperTest extends TestCase {

    public void testExtractPodcasts() throws IOException, JSONException {

        //parse string to jsonObject
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("audiothek_search_response.json");

        String rawProgramDetail = IOUtils.toString(inputStream, Charsets.UTF_8);
        JSONObject searchResponseJson = new JSONObject(rawProgramDetail);

        List<PodcastSearchResult> podcastSearchResults = AudiothekSearchResultMapper
                .extractPodcasts(searchResponseJson);
        assertSame(23, podcastSearchResults.size());

        PodcastSearchResult podcastSearchResult = podcastSearchResults.get(0);
        assertEquals("Das war morgen", podcastSearchResult.title);
        assertEquals("SWR", podcastSearchResult.author);
        assertEquals("https://api.ardaudiothek.de/./programsets/12701131", podcastSearchResult.feedUrl);
        assertEquals("https://api.ardmediathek.de/image-service/images/urn:ard:image:7180d61cfbc579f8?w=64&ch=99bdf1f0bb4a71df", podcastSearchResult.imageUrl);

        PodcastSearchResult editorialCollectionSearchResult = podcastSearchResults.get(6);
        assertEquals("SWR Aktuell Info-Date", editorialCollectionSearchResult.title);
        assertEquals("SWR", editorialCollectionSearchResult.author);
        assertEquals("https://api.ardaudiothek.de/./publicationservices/9209284", editorialCollectionSearchResult.feedUrl);
        assertEquals("https://img.ardmediathek.de/standard/00/42/92/12/78/-2114473875/1x1/64?mandant=ard", editorialCollectionSearchResult.imageUrl);
    }

    public void testExtractPodcastsOnlyOneResult() throws IOException, JSONException {
        InputStream inputStream = getClass()
                .getResourceAsStream("audiothek_search_response_only_one_result.json");
        String rawResponseJson = null;
        JSONObject searchResponseJson = null;
        rawResponseJson = IOUtils.toString(inputStream, Charsets.UTF_8);
        searchResponseJson = new JSONObject(rawResponseJson);
        List<PodcastSearchResult> podcastSearchResults = AudiothekSearchResultMapper
                .extractPodcasts(searchResponseJson);
        assertSame(1, podcastSearchResults.size());

        PodcastSearchResult podcastSearchResult = podcastSearchResults.get(0);
        assertEquals("Stanisław Lem: Frieden auf Erden", podcastSearchResult.title);
        assertEquals("MDR", podcastSearchResult.author);
        assertEquals("https://api.ardaudiothek.de/./programsets/92726154", podcastSearchResult.feedUrl);
        assertEquals("https://img.ardmediathek.de/standard/00/92/72/61/64/-1774185891/1x1/64?mandant=ard", podcastSearchResult.imageUrl);
    }

    public static File getFileFromPath(Object obj, String fileName) {
        ClassLoader classLoader = obj.getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        return new File(resource.getPath());
    }

}