package de.danoeh.antennapod.parser.feed.parser;

import static java.time.Instant.now;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.util.TypeResolver;

public class JsonFeedParser implements FeedParser {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public FeedHandlerResult createFeedHandlerResult(Feed feed, TypeResolver.Type type) throws JSONException {
        InputStream fileInputStream = null;
        Map<String, String> alternateFeedUrls = null;
        Feed hydratedFeed = null;
        try {
            fileInputStream = new FileInputStream(feed.getFile_url());
            String jsonTxt = IOUtils.toString(fileInputStream, Charsets.UTF_8);
            hydratedFeed = hydrateFeed(new JSONObject(jsonTxt));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new FeedHandlerResult(hydratedFeed, alternateFeedUrls);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Feed hydrateFeed(JSONObject jsonObject) throws JSONException {
        String title = jsonObject.getString("title");
        //@todo prefix audiothek host
        String url = jsonObject.getJSONObject("_links").getJSONObject("self").getString("href");
        return new Feed(url, now().toString(), title);
    }
}
