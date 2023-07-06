package de.danoeh.antennapod.parser.feed.parser;


import androidx.annotation.NonNull;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.type.TypeResolver;
import de.danoeh.antennapod.parser.feed.util.MimeTypeNonStaticWrapper;

public class JsonFeedParser implements FeedParser {
    public static final String AUDIOTHEK_BASE_URI = "https://api.ardaudiothek.de/";
    private MimeTypeNonStaticWrapper mimeTypeUtils;

    public JsonFeedParser(MimeTypeNonStaticWrapper mimeTypeUtils) {

        this.mimeTypeUtils = mimeTypeUtils;
    }

    @Override
    public FeedHandlerResult createFeedHandlerResult(Feed feed, TypeResolver.Type type) throws JSONException {
        Map<String, String> alternateFeedUrls = new HashMap<>();
        Feed hydratedFeed = null;
        try {
            InputStream fileInputStream = new FileInputStream(feed.getFile_url());
            String jsonTxt = IOUtils.toString(fileInputStream, Charsets.UTF_8);
            fileInputStream.close();
            JSONObject jsonObject = new JSONObject(jsonTxt);
            hydratedFeed = hydrateFeed(jsonObject.getJSONObject("data").getJSONObject("programSet"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert hydratedFeed != null;
        return new FeedHandlerResult(hydratedFeed, alternateFeedUrls, hydratedFeed.getDownload_url());
    }

    public Feed hydrateFeed(JSONObject jsonObject) throws JSONException {
        String title = jsonObject.getString("title");
        String url = AUDIOTHEK_BASE_URI + jsonObject.getJSONObject("_links").getJSONObject("self").getString("href");
        url = url.replace("{?order,offset,limit}", "");

        Feed feed = new Feed(url, Calendar.getInstance().getTime().toString(), title);

        JSONObject items = jsonObject.getJSONObject("items");
        ArrayList<FeedItem> episodes = extractFeedItems(items.getJSONArray("nodes"), feed);
        feed.setItems(episodes);
        feed.setImageUrl(getProgramSetImageUrl(jsonObject));
        feed.setDescription(jsonObject.getString("synopsis"));
        feed.setAuthor(jsonObject.getJSONObject("publicationService").getString("organizationName"));

        return feed;
    }

    @NonNull
    private String getProgramSetImageUrl(JSONObject jsonObject) throws JSONException {
        String rawMediaUrl = jsonObject.getJSONObject("image").getString("url");
        return rawMediaUrl.replace("{width}", "128");
    }

    private ArrayList<FeedItem> extractFeedItems(JSONArray programSetItems, Feed feed) throws JSONException {
        ArrayList<FeedItem> feedItems = new ArrayList<>();
        for (int i = 0; i < programSetItems.length(); i++) {
            JSONObject programSetItem = (JSONObject) programSetItems.get(i);
            FeedItem feedItem = new FeedItem();
            feedItem.setTitle(programSetItem.getString("title"));
            feedItem.setDescriptionIfLonger(programSetItem.getString("synopsis"));
            feedItem.setMedia(getFeedMedia(programSetItem, feedItem));
            feedItem.setFeed(feed);
            feedItem.setItemIdentifier(createUuid(programSetItem));
            feedItem.setPubDate(getPubDate(programSetItem.getString("publicationStartDateAndTime")));
            feedItems.add(feedItem);
        }

        return feedItems;
    }

    @NonNull
    private static Date getPubDate(String publicationStartDateAndTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        try {
            assert publicationStartDateAndTime != null;
            return format.parse(publicationStartDateAndTime);
        } catch (ParseException e) {
            return new Date();
        }
    }

    @NonNull
    private String createUuid(JSONObject programSetItem) throws JSONException {
        return "audiothek_" + programSetItem.getString("id");
    }

    @NonNull
    private FeedMedia getFeedMedia(JSONObject programSetItem, FeedItem feedItem) throws JSONException {
        JSONArray audios = programSetItem.getJSONArray("audios");
        JSONObject audioSource = (JSONObject) audios.get(0);
        String mediaUrl = audioSource.getString("url");
        return new FeedMedia(feedItem, mediaUrl, mediaUrl.length(), mimeTypeUtils.getMimeTypeFromUrl(mediaUrl));
    }

}
