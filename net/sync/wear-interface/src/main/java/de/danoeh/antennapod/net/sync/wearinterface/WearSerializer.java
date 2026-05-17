package de.danoeh.antennapod.net.sync.wearinterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedMedia;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WearSerializer {
    private static final String KEY_EPISODE_ID = "episode_id";
    private static final String KEY_FEED_ID = "feed_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PUB_DATE = "pub_date";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_POSITION = "position";
    private static final String KEY_IS_PLAYING = "is_playing";

    private WearSerializer() {
    }

    @NonNull
    private static JSONObject episodeToJson(@NonNull FeedItem item) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(KEY_EPISODE_ID, item.getId());
        obj.put(KEY_TITLE, item.getTitle() != null ? item.getTitle() : "");
        obj.put(KEY_PUB_DATE, item.getPubDate() != null ? item.getPubDate().getTime() : 0);
        obj.put(KEY_DURATION, item.getMedia() != null ? item.getMedia().getDuration() : 0);
        obj.put(KEY_POSITION, item.getMedia() != null ? item.getMedia().getPosition() : 0);
        return obj;
    }

    @NonNull
    private static FeedItem episodeFromJson(@NonNull JSONObject obj) throws JSONException {
        FeedItem item = new FeedItem();
        item.setId(obj.optLong(KEY_EPISODE_ID, -1));
        item.setTitle(obj.optString(KEY_TITLE, ""));
        long pubDate = obj.optLong(KEY_PUB_DATE, 0);
        if (pubDate != 0) {
            item.setPubDate(new Date(pubDate));
        }
        int duration = obj.optInt(KEY_DURATION, 0);
        int position = obj.optInt(KEY_POSITION, 0);
        FeedMedia media = new FeedMedia(0, item, duration, position, 0, null, null, null, 0, null, 0, 0L);
        item.setMedia(media);
        return item;
    }

    @NonNull
    public static byte[] episodesToBytes(@NonNull List<FeedItem> items) {
        JSONArray array = new JSONArray();
        for (FeedItem item : items) {
            try {
                array.put(episodeToJson(item));
            } catch (JSONException e) {
                // skip malformed item
            }
        }
        return array.toString().getBytes(StandardCharsets.UTF_8);
    }

    @NonNull
    public static List<FeedItem> episodesFromBytes(@NonNull byte[] data) {
        List<FeedItem> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                items.add(episodeFromJson(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // return whatever we managed to parse
        }
        return items;
    }

    @NonNull
    public static byte[] feedsToBytes(@NonNull List<Feed> feeds) {
        JSONArray array = new JSONArray();
        for (Feed feed : feeds) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(KEY_FEED_ID, feed.getId());
                obj.put(KEY_TITLE, feed.getTitle() != null ? feed.getTitle() : "");
                array.put(obj);
            } catch (JSONException e) {
                // skip malformed feed
            }
        }
        return array.toString().getBytes(StandardCharsets.UTF_8);
    }

    @NonNull
    public static List<Feed> feedsFromBytes(@NonNull byte[] data) {
        List<Feed> feeds = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Feed feed = new Feed(null, null);
                feed.setId(obj.optLong(KEY_FEED_ID, -1));
                feed.setTitle(obj.optString(KEY_TITLE, ""));
                feeds.add(feed);
            }
        } catch (JSONException e) {
            // return whatever we managed to parse
        }
        return feeds;
    }

    @NonNull
    public static byte[] nowPlayingToBytes(@NonNull FeedItem item, boolean isPlaying) {
        try {
            JSONObject obj = episodeToJson(item);
            obj.put(KEY_IS_PLAYING, isPlaying);
            return obj.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            return new byte[0];
        }
    }

    @Nullable
    public static WearNowPlaying nowPlayingFromBytes(@NonNull byte[] data) {
        if (data.length == 0) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(new String(data, StandardCharsets.UTF_8));
            FeedItem item = episodeFromJson(obj);
            boolean isPlaying = obj.optBoolean(KEY_IS_PLAYING, false);
            return new WearNowPlaying(item, isPlaying);
        } catch (JSONException e) {
            return null;
        }
    }

}
