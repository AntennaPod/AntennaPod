package de.danoeh.antennapod.storage.importexport;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/** Writes PortCast documents. See https://portcast.org/. */
public class PortcastWriter {
    private static final String TAG = "PortcastWriter";

    /**
     * Writes the given feeds and queue into a PortCast document.
     *
     * @param feeds       The complete list of feeds, with their items loaded. Only subscribed feeds are exported.
     * @param queue       The current playback queue, in order.
     * @param favoriteIds The ids of all favorite (starred) feed items.
     * @param writer      Target writer.
     * @param context     Used to read the app version for the generator field.
     */
    public static void writeDocument(List<Feed> feeds, List<FeedItem> queue, Set<Long> favoriteIds,
                                     Writer writer, Context context) throws IOException {
        Log.d(TAG, "Starting to write document");
        try {
            Set<Long> queuedIds = new HashSet<>();
            for (FeedItem item : queue) {
                queuedIds.add(item.getId());
            }
            String now = formatRfc3339(new Date());
            JSONObject root = new JSONObject();
            root.put(PortcastSymbols.PORTCAST, PortcastSymbols.PROTOCOL_VERSION);
            root.put(PortcastSymbols.GENERATED_AT, now);
            root.put(PortcastSymbols.GENERATOR, buildGenerator(context));

            JSONArray subscriptions = new JSONArray();
            JSONArray episodes = new JSONArray();
            JSONObject perFeed = new JSONObject();
            for (Feed feed : feeds) {
                if (feed.getState() != Feed.STATE_SUBSCRIBED || feed.getDownloadUrl() == null) {
                    continue;
                }
                subscriptions.put(buildSubscription(feed, now));
                for (FeedItem item : feed.getItems()) {
                    JSONObject episode = buildEpisode(feed, item, favoriteIds, queuedIds, now);
                    if (episode != null) {
                        episodes.put(episode);
                    }
                }
                JSONObject feedPrefs = buildFeedPreferences(feed.getPreferences());
                if (feedPrefs.length() > 0) {
                    perFeed.put(feed.getDownloadUrl(), feedPrefs);
                }
            }
            root.put(PortcastSymbols.SUBSCRIPTIONS, subscriptions);
            root.put(PortcastSymbols.EPISODES, episodes);
            root.put(PortcastSymbols.QUEUE, buildQueue(queue));
            root.put(PortcastSymbols.PREFERENCES, buildPreferences(perFeed));
            root.put(PortcastSymbols.COMPLETENESS, buildCompleteness());

            writer.write(root.toString(2));
        } catch (JSONException e) {
            throw new IOException(e);
        }
        Log.d(TAG, "Finished writing document");
    }

    private static JSONObject buildGenerator(Context context) throws JSONException {
        JSONObject generator = new JSONObject();
        generator.put(PortcastSymbols.NAME, PortcastSymbols.GENERATOR_NAME);
        generator.put(PortcastSymbols.URL, PortcastSymbols.GENERATOR_URL);
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            if (version != null) {
                generator.put(PortcastSymbols.VERSION, version);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return generator;
    }

    private static JSONObject buildSubscription(Feed feed, String now) throws JSONException {
        JSONObject subscription = new JSONObject();
        subscription.put(PortcastSymbols.SUBSCRIPTION_ID, feed.getDownloadUrl());
        subscription.put(PortcastSymbols.FEED_URL, feed.getDownloadUrl());
        subscription.put(PortcastSymbols.TITLE, feed.getTitle() != null ? feed.getTitle() : feed.getDownloadUrl());
        subscription.put(PortcastSymbols.UPDATED_AT, feed.getLastRefreshAttempt() > 0
                ? formatRfc3339(new Date(feed.getLastRefreshAttempt())) : now);
        String guid = feed.getFeedIdentifier();
        if (guid != null && !guid.isEmpty() && !guid.equals(feed.getDownloadUrl())) {
            subscription.put(PortcastSymbols.PODCAST_GUID, guid);
        }
        if (feed.getAuthor() != null) {
            subscription.put(PortcastSymbols.AUTHOR, feed.getAuthor());
        }
        if (feed.getImageUrl() != null) {
            subscription.put(PortcastSymbols.IMAGE_URL, feed.getImageUrl());
        }
        if (feed.getPreferences() != null) {
            subscription.put(PortcastSymbols.NOTIFICATIONS_ENABLED,
                    feed.getPreferences().getShowEpisodeNotification());
            JSONArray tags = new JSONArray();
            for (String tag : feed.getPreferences().getTags()) {
                if (!tag.startsWith("#")) {
                    tags.put(tag);
                }
            }
            if (tags.length() > 0) {
                subscription.put(PortcastSymbols.TAGS, tags);
            }
        }
        return subscription;
    }

    private static JSONObject buildEpisode(Feed feed, FeedItem item, Set<Long> favoriteIds,
                                           Set<Long> queuedIds, String now) throws JSONException {
        FeedMedia media = item.getMedia();
        int positionSeconds = (media != null) ? media.getPosition() / 1000 : 0;
        boolean played = item.getPlayState() == FeedItem.PLAYED;
        boolean starred = favoriteIds.contains(item.getId());
        boolean queued = queuedIds.contains(item.getId());
        if (!played && positionSeconds <= 0 && !starred && !queued) {
            return null;
        }
        JSONObject episode = new JSONObject();
        episode.put(PortcastSymbols.EPISODE_STATE_ID, episodeStateId(feed, item));
        episode.put(PortcastSymbols.SUBSCRIPTION_REF, buildSubscriptionRef(feed));
        if (played) {
            episode.put(PortcastSymbols.STATUS, PortcastSymbols.STATUS_COMPLETED);
        } else if (positionSeconds > 0) {
            episode.put(PortcastSymbols.STATUS, PortcastSymbols.STATUS_IN_PROGRESS);
            episode.put(PortcastSymbols.POSITION_SECONDS, positionSeconds);
        } else {
            episode.put(PortcastSymbols.STATUS, PortcastSymbols.STATUS_UNPLAYED);
        }
        Date lastPlayed = (media != null) ? media.getLastPlayedTimeHistory() : null;
        episode.put(PortcastSymbols.UPDATED_AT, lastPlayed != null ? formatRfc3339(lastPlayed) : now);
        if (item.getItemIdentifier() != null) {
            episode.put(PortcastSymbols.GUID, item.getItemIdentifier());
        }
        if (media != null && media.getDownloadUrl() != null) {
            episode.put(PortcastSymbols.ENCLOSURE_URL, media.getDownloadUrl());
        }
        if (item.getTitle() != null) {
            episode.put(PortcastSymbols.TITLE, item.getTitle());
        }
        if (item.getPubDate() != null) {
            episode.put(PortcastSymbols.PUBLISHED_AT, formatRfc3339(item.getPubDate()));
        }
        if (media != null && media.getDuration() > 0) {
            episode.put(PortcastSymbols.DURATION_SECONDS, media.getDuration() / 1000);
        }
        if (lastPlayed != null) {
            episode.put(PortcastSymbols.LAST_PLAYED_AT, formatRfc3339(lastPlayed));
        }
        if (starred) {
            episode.put(PortcastSymbols.STARRED, true);
        }
        return episode;
    }

    private static String episodeStateId(Feed feed, FeedItem item) {
        String suffix = item.getItemIdentifier() != null ? item.getItemIdentifier()
                : (item.getMedia() != null ? item.getMedia().getDownloadUrl() : String.valueOf(item.getId()));
        return feed.getDownloadUrl() + "#" + suffix;
    }

    private static JSONObject buildSubscriptionRef(Feed feed) throws JSONException {
        JSONObject ref = new JSONObject();
        ref.put(PortcastSymbols.FEED_URL, feed.getDownloadUrl());
        String guid = feed.getFeedIdentifier();
        if (guid != null && !guid.isEmpty() && !guid.equals(feed.getDownloadUrl())) {
            ref.put(PortcastSymbols.PODCAST_GUID, guid);
        }
        return ref;
    }

    private static JSONArray buildQueue(List<FeedItem> queue) throws JSONException {
        JSONArray result = new JSONArray();
        int position = 1;
        for (FeedItem item : queue) {
            JSONObject ref = new JSONObject();
            if (item.getItemIdentifier() != null) {
                ref.put(PortcastSymbols.GUID, item.getItemIdentifier());
            }
            if (item.getMedia() != null && item.getMedia().getDownloadUrl() != null) {
                ref.put(PortcastSymbols.ENCLOSURE_URL, item.getMedia().getDownloadUrl());
            }
            if (ref.length() == 0) {
                continue;
            }
            JSONObject entry = new JSONObject();
            entry.put(PortcastSymbols.POSITION, position);
            entry.put(PortcastSymbols.EPISODE_REF, ref);
            result.put(entry);
            position++;
        }
        return result;
    }

    private static JSONObject buildFeedPreferences(FeedPreferences preferences) throws JSONException {
        JSONObject feedPrefs = new JSONObject();
        if (preferences == null) {
            return feedPrefs;
        }
        if (preferences.getFeedPlaybackSpeed() != FeedPreferences.SPEED_USE_GLOBAL) {
            feedPrefs.put(PortcastSymbols.PLAYBACK_RATE, preferences.getFeedPlaybackSpeed());
        }
        if (preferences.getFeedSkipIntro() > 0) {
            feedPrefs.put(PortcastSymbols.SKIP_INTRO_SECONDS, preferences.getFeedSkipIntro());
        }
        if (preferences.getFeedSkipEnding() > 0) {
            feedPrefs.put(PortcastSymbols.SKIP_OUTRO_SECONDS, preferences.getFeedSkipEnding());
        }
        if (preferences.getFeedSkipSilence() == FeedPreferences.SkipSilence.AGGRESSIVE) {
            feedPrefs.put(PortcastSymbols.TRIM_SILENCE, true);
        }
        if (isBoost(preferences.getVolumeAdaptionSetting())) {
            feedPrefs.put(PortcastSymbols.BOOST_VOICE, true);
        }
        return feedPrefs;
    }

    private static boolean isBoost(VolumeAdaptionSetting setting) {
        return setting == VolumeAdaptionSetting.LIGHT_BOOST
                || setting == VolumeAdaptionSetting.MEDIUM_BOOST
                || setting == VolumeAdaptionSetting.HEAVY_BOOST;
    }

    private static JSONObject buildPreferences(JSONObject perFeed) throws JSONException {
        JSONObject global = new JSONObject();
        global.put(PortcastSymbols.PLAYBACK_RATE, UserPreferences.getPlaybackSpeed());
        global.put(PortcastSymbols.SKIP_FORWARD_SECONDS, UserPreferences.getFastForwardSecs());
        global.put(PortcastSymbols.SKIP_BACKWARD_SECONDS, UserPreferences.getRewindSecs());
        JSONObject preferences = new JSONObject();
        preferences.put(PortcastSymbols.GLOBAL, global);
        preferences.put(PortcastSymbols.PER_FEED, perFeed);
        return preferences;
    }

    private static JSONArray buildCompleteness() throws JSONException {
        JSONArray completeness = new JSONArray();
        completeness.put(completenessEntry(PortcastSymbols.SUBSCRIPTIONS, PortcastSymbols.LEVEL_FULL));
        completeness.put(completenessEntry(PortcastSymbols.EPISODES, PortcastSymbols.LEVEL_CURRENT_STATE_ONLY));
        completeness.put(completenessEntry(PortcastSymbols.QUEUE, PortcastSymbols.LEVEL_FULL));
        completeness.put(completenessEntry(PortcastSymbols.PREFERENCES, PortcastSymbols.LEVEL_FULL));
        return completeness;
    }

    private static JSONObject completenessEntry(String section, String level) throws JSONException {
        JSONObject entry = new JSONObject();
        entry.put(PortcastSymbols.SECTION, section);
        entry.put(PortcastSymbols.SOURCE, PortcastSymbols.GENERATOR_NAME);
        entry.put(PortcastSymbols.LEVEL, level);
        return entry;
    }

    private static String formatRfc3339(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }
}
