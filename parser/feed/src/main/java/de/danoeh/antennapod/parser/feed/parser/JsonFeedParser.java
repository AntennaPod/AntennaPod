package de.danoeh.antennapod.parser.feed.parser;

import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.util.TypeResolver;

public class JsonFeedParser implements FeedParser {
    public static final String AUDIOTHEK_BASE_URI = "https://api.ardaudiothek.de/";

    @Override
    public FeedHandlerResult createFeedHandlerResult(Feed feed, TypeResolver.Type type) throws JSONException {
        InputStream fileInputStream = null;
        Map<String, String> alternateFeedUrls = new HashMap<>();
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

    public Feed hydrateFeed(JSONObject jsonObject) throws JSONException {
        String title = jsonObject.getString("title");
        String url = AUDIOTHEK_BASE_URI + jsonObject.getJSONObject("_links").getJSONObject("self").getString("href");
        Feed feed = new Feed(url, Calendar.getInstance().getTime().toString(), title);

        JSONObject embedded = jsonObject.getJSONObject("_embedded");
        feed.setItems(extractFeedItems(embedded.getJSONArray("mt:items")));
        feed.setImageUrl(getProgramSetImageUrl(jsonObject));
        feed.setDescription(jsonObject.getString("synopsis"));
        feed.setAuthor(embedded.getJSONObject("mt:publicationService").getString("title"));

        return feed;
    }

    @NonNull
    private String getProgramSetImageUrl(JSONObject jsonObject) throws JSONException {
        String rawMediaUrl = jsonObject.getJSONObject("_links").getJSONObject("mt:image").getString("href");
        rawMediaUrl = rawMediaUrl.replace("{ratio}", "1x1");
        return rawMediaUrl.replace("{width}", "128");
    }

    private ArrayList<FeedItem> extractFeedItems(JSONArray programSetItems) throws JSONException {
        ArrayList<FeedItem> feedItems = new ArrayList<>();
        for (int i = 0; i < programSetItems.length(); i++) {
            JSONObject programSetItem = (JSONObject) programSetItems.get(i);
            FeedItem feedItem = new FeedItem();
            feedItem.setTitle(programSetItem.getString("title"));
            feedItem.setDescriptionIfLonger(programSetItem.getString("synopsis"));
            feedItem.setMedia(getFeedMedia(programSetItem, feedItem));
            feedItems.add(feedItem);
        }

        return feedItems;
    }

    @NonNull
    private FeedMedia getFeedMedia(JSONObject programSetItem, FeedItem feedItem) throws JSONException {
        String mediaUrl = programSetItem.getJSONObject("_links").getJSONObject("mt:bestQualityPlaybackUrl")
                .getString("href");
        return new FeedMedia(feedItem, mediaUrl, mediaUrl.length(), getMimeType(mediaUrl));
    }

    private String getMimeType(String mediaUrl) {
        String extension = FilenameUtils.getExtension(mediaUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
}
