package de.danoeh.antennapod.parser.feed.parser;

import androidx.annotation.NonNull;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;


import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.util.MimeTypeNonStaticWrapper;

public class JsonFeedParserTest extends TestCase {


    public void testHydrateFeed() throws JSONException, IOException {
        MimeTypeNonStaticWrapper mock = mock(MimeTypeNonStaticWrapper.class);
        when(mock.getMimeTypeFromUrl(anyString())).thenReturn("ogg");
        JsonFeedParser jsonFeedParser = new JsonFeedParser(mock);
        JSONObject feedItemsResult = getFeedItemsResult();
        Feed feedContent = jsonFeedParser.hydrateFeed(feedItemsResult);
    }

    @NonNull
    private JSONObject getFeedItemsResult() throws IOException, JSONException {
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("audiothek_feed_content.json");

        String rawProgramDetail = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        return new JSONObject(rawProgramDetail);
    }
}