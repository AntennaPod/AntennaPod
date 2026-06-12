package de.danoeh.antennapod.storage.importexport;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Reads PortCast documents. See https://portcast.org/.
 * Unknown or unsupported sections (such as bookmarks or playback events) are ignored.
 */
public class PortcastReader {
    private static final String TAG = "PortcastReader";

    public PortcastDocument readDocument(Reader reader) throws IOException, JSONException {
        JSONObject root = new JSONObject(IOUtils.toString(reader));
        if (!root.has(PortcastSymbols.PORTCAST)) {
            throw new IOException("Not a PortCast file");
        }

        PortcastDocument document = new PortcastDocument();
        Map<String, PortcastSubscription> byRef = new HashMap<>();

        JSONArray subscriptions = root.optJSONArray(PortcastSymbols.SUBSCRIPTIONS);
        if (subscriptions != null) {
            for (int i = 0; i < subscriptions.length(); i++) {
                JSONObject json = subscriptions.optJSONObject(i);
                if (json == null) {
                    continue;
                }
                PortcastSubscription subscription = readSubscription(json);
                if (TextUtils.isEmpty(subscription.getFeedUrl())
                        && TextUtils.isEmpty(subscription.getPodcastGuid())) {
                    continue;
                }
                document.getSubscriptions().add(subscription);
                registerRefs(byRef, subscription, json.optString(PortcastSymbols.SUBSCRIPTION_ID, null));
            }
        }

        JSONArray episodes = root.optJSONArray(PortcastSymbols.EPISODES);
        if (episodes != null) {
            for (int i = 0; i < episodes.length(); i++) {
                JSONObject json = episodes.optJSONObject(i);
                if (json == null) {
                    continue;
                }
                PortcastSubscription subscription = findSubscription(byRef,
                        json.optJSONObject(PortcastSymbols.SUBSCRIPTION_REF));
                if (subscription != null) {
                    subscription.getEpisodes().add(readEpisode(json));
                }
            }
        }

        readPreferences(root, byRef);
        readQueue(root, document);
        return document;
    }

    private PortcastSubscription readSubscription(JSONObject json) {
        PortcastSubscription subscription = new PortcastSubscription();
        subscription.setFeedUrl(json.optString(PortcastSymbols.FEED_URL, null));
        subscription.setPodcastGuid(json.optString(PortcastSymbols.PODCAST_GUID, null));
        subscription.setTitle(json.optString(PortcastSymbols.TITLE, null));
        if (json.has(PortcastSymbols.NOTIFICATIONS_ENABLED)) {
            subscription.setNotificationsEnabled(json.optBoolean(PortcastSymbols.NOTIFICATIONS_ENABLED, true));
        }
        JSONArray tags = json.optJSONArray(PortcastSymbols.TAGS);
        if (tags != null) {
            for (int i = 0; i < tags.length(); i++) {
                String tag = tags.optString(i, null);
                if (!TextUtils.isEmpty(tag)) {
                    subscription.getTags().add(tag);
                }
            }
        }
        return subscription;
    }

    private PortcastEpisode readEpisode(JSONObject json) {
        PortcastEpisode episode = new PortcastEpisode();
        episode.setGuid(json.optString(PortcastSymbols.GUID, null));
        episode.setEnclosureUrl(json.optString(PortcastSymbols.ENCLOSURE_URL, null));
        episode.setStatus(json.optString(PortcastSymbols.STATUS, PortcastSymbols.STATUS_UNPLAYED));
        episode.setPositionSeconds(json.optInt(PortcastSymbols.POSITION_SECONDS, 0));
        episode.setDurationSeconds(json.optInt(PortcastSymbols.DURATION_SECONDS, 0));
        episode.setTitle(json.optString(PortcastSymbols.TITLE, null));
        episode.setLastPlayedAt(parseRfc3339(json.optString(PortcastSymbols.LAST_PLAYED_AT, null)));
        episode.setPublishedAt(parseRfc3339(json.optString(PortcastSymbols.PUBLISHED_AT, null)));
        episode.setStarred(json.optBoolean(PortcastSymbols.STARRED, false));
        return episode;
    }

    private void readPreferences(JSONObject root, Map<String, PortcastSubscription> byRef) {
        JSONObject preferences = root.optJSONObject(PortcastSymbols.PREFERENCES);
        if (preferences == null) {
            return;
        }
        JSONObject perFeed = preferences.optJSONObject(PortcastSymbols.PER_FEED);
        if (perFeed == null) {
            return;
        }
        Iterator<String> keys = perFeed.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            PortcastSubscription subscription = byRef.get(key);
            JSONObject json = perFeed.optJSONObject(key);
            if (subscription == null || json == null) {
                continue;
            }
            if (json.has(PortcastSymbols.PLAYBACK_RATE)) {
                subscription.setPlaybackSpeed((float) json.optDouble(PortcastSymbols.PLAYBACK_RATE));
            }
            if (json.has(PortcastSymbols.SKIP_INTRO_SECONDS)) {
                subscription.setSkipIntroSeconds(json.optInt(PortcastSymbols.SKIP_INTRO_SECONDS));
            }
            if (json.has(PortcastSymbols.SKIP_OUTRO_SECONDS)) {
                subscription.setSkipEndingSeconds(json.optInt(PortcastSymbols.SKIP_OUTRO_SECONDS));
            }
        }
    }

    private void readQueue(JSONObject root, PortcastDocument document) {
        JSONArray queue = root.optJSONArray(PortcastSymbols.QUEUE);
        if (queue == null) {
            return;
        }
        for (int i = 0; i < queue.length(); i++) {
            JSONObject json = queue.optJSONObject(i);
            JSONObject ref = (json != null) ? json.optJSONObject(PortcastSymbols.EPISODE_REF) : null;
            if (ref == null) {
                continue;
            }
            PortcastQueueEntry entry = new PortcastQueueEntry();
            entry.setGuid(ref.optString(PortcastSymbols.GUID, null));
            entry.setEnclosureUrl(ref.optString(PortcastSymbols.ENCLOSURE_URL, null));
            if (!TextUtils.isEmpty(entry.getGuid()) || !TextUtils.isEmpty(entry.getEnclosureUrl())) {
                document.getQueue().add(entry);
            }
        }
    }

    private PortcastSubscription findSubscription(Map<String, PortcastSubscription> byRef, JSONObject ref) {
        if (ref == null) {
            return null;
        }
        String guid = ref.optString(PortcastSymbols.PODCAST_GUID, null);
        if (!TextUtils.isEmpty(guid) && byRef.containsKey(guid)) {
            return byRef.get(guid);
        }
        return byRef.get(ref.optString(PortcastSymbols.FEED_URL, ""));
    }

    private void registerRefs(Map<String, PortcastSubscription> byRef,
                              PortcastSubscription subscription, String subscriptionId) {
        if (!TextUtils.isEmpty(subscriptionId)) {
            byRef.put(subscriptionId, subscription);
        }
        if (!TextUtils.isEmpty(subscription.getFeedUrl())) {
            byRef.put(subscription.getFeedUrl(), subscription);
        }
        if (!TextUtils.isEmpty(subscription.getPodcastGuid())) {
            byRef.put(subscription.getPodcastGuid(), subscription);
        }
    }

    private Date parseRfc3339(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(value);
        } catch (ParseException e) {
            Log.d(TAG, "Unable to parse date: " + value);
            return null;
        }
    }
}
