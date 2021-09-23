package de.danoeh.antennapod.parser.feed.parser;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import de.danoeh.antennapod.model.feed.Feed;

public class JsonFeedParserTest extends TestCase {

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
        assertEquals("./programsets/92726154?order=desc&offset=0&limit=12", feed.getDownload_url());
        assertNull(feed.getItems());
    }
}