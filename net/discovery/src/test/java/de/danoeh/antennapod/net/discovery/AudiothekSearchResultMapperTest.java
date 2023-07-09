package de.danoeh.antennapod.net.discovery;

import androidx.annotation.NonNull;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.danoeh.antennapod.net.discovery.audiothek.AudiothekSearchResultMapper;

public class AudiothekSearchResultMapperTest extends TestCase {

    public void testExtractPodcasts() throws IOException, JSONException {

        JSONObject searchResponseJson = getSearchResult();

        List<PodcastSearchResult> podcastSearchResults = AudiothekSearchResultMapper
                .extractPodcasts(searchResponseJson);
        TestCase.assertSame(23, podcastSearchResults.size());

        PodcastSearchResult podcastSearchResult = podcastSearchResults.get(0);
        TestCase.assertEquals("Das war morgen", podcastSearchResult.title);
        TestCase.assertEquals("SWR", podcastSearchResult.author);
        TestCase.assertEquals("https://api.ardaudiothek.de/./programsets/12701131", podcastSearchResult.feedUrl);
        TestCase.assertEquals("https://api.ardmediathek.de/image-service/images/urn:ard:image:d949e2c7d6ff3b11?w=64&ch=f8e6e3500cd679cf", podcastSearchResult.imageUrl);

        PodcastSearchResult editorialCollectionSearchResult = podcastSearchResults.get(6);
        TestCase.assertEquals("SWR Aktuell Info-Date", editorialCollectionSearchResult.title);
        TestCase.assertEquals("SWR", editorialCollectionSearchResult.author);
        TestCase.assertEquals("https://api.ardaudiothek.de/./programsets/94703120", editorialCollectionSearchResult.feedUrl);
        TestCase.assertEquals("https://api.ardmediathek.de/image-service/images/urn:ard:image:c52f727faf86911b?w=64&ch=c3d831509180e565", editorialCollectionSearchResult.imageUrl);
    }

    @NonNull
    private JSONObject getSearchResult() throws IOException, JSONException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("audiothek_search_response.json");

        String rawProgramDetail = IOUtils.toString(inputStream, Charsets.UTF_8);
        return new JSONObject(rawProgramDetail);
    }

}