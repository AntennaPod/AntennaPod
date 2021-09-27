package de.danoeh.antennapod.parser.feed.parser;

import static junit.framework.TestCase.assertEquals;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import de.danoeh.antennapod.model.feed.Feed;

public class JsonFeedParserTest {

    @Test
    public void testHydrateFeed() {
        JsonFeedParser jsonFeedParser = new JsonFeedParser();
        Feed feed = null;
        try {
            InputStream inputStream = getClass()
                    .getResourceAsStream("audiothek_programset_detail.json");
            String rawProgramDetail = IOUtils.toString(inputStream, Charsets.UTF_8);
            feed = jsonFeedParser.hydrateFeed(new JSONObject(rawProgramDetail));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

        assertEquals("Stanisław Lem: Frieden auf Erden", feed.getTitle());
        assertEquals(JsonFeedParser.AUDIOTHEK_BASE_URI + "./programsets/92726154?order=desc&offset=0&limit=12",
                feed.getDownload_url());
        assertEquals(12, feed.getItems().size());

        assertEquals("audiothek_92766886", feed.getItems().get(0).getItemIdentifier());
    }
}