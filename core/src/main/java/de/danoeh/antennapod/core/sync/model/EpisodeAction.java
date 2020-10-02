package de.danoeh.antennapod.core.sync.model;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EpisodeAction {
    private static final String TAG = "EpisodeAction";
    public static final Action NEW = Action.NEW;
    public static final Action DOWNLOAD = Action.DOWNLOAD;
    public static final Action PLAY = Action.PLAY;
    public static final Action DELETE = Action.DELETE;

    private final String podcast;
    private final String episode;
    private final Action action;
    private final Date timestamp;
    private final int started;
    private final int position;
    private final int total;

    private EpisodeAction(Builder builder) {
        this.podcast = builder.podcast;
        this.episode = builder.episode;
        this.action = builder.action;
        this.timestamp = builder.timestamp;
        this.started = builder.started;
        this.position = builder.position;
        this.total = builder.total;
    }

    /**
     * Create an episode action object from JSON representation. Mandatory fields are "podcast",
     * "episode" and "action".
     *
     * @param object JSON representation
     * @return episode action object, or null if mandatory values are missing
     */
    public static EpisodeAction readFromJsonObject(JSONObject object) {
        String podcast = object.optString("podcast", null);
        String episode = object.optString("episode", null);
        String actionString = object.optString("action", null);
        if (TextUtils.isEmpty(podcast) || TextUtils.isEmpty(episode) || TextUtils.isEmpty(actionString)) {
            return null;
        }
        EpisodeAction.Action action;
        try {
            action = EpisodeAction.Action.valueOf(actionString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        EpisodeAction.Builder builder = new EpisodeAction.Builder(podcast, episode, action);
        String utcTimestamp = object.optString("timestamp", null);
        if (!TextUtils.isEmpty(utcTimestamp)) {
            builder.timestamp(DateUtils.parse(utcTimestamp));
        }
        if (action == EpisodeAction.Action.PLAY) {
            int started = object.optInt("started", -1);
            int position = object.optInt("position", -1);
            int total = object.optInt("total", -1);
            if (started >= 0 && position > 0 && total > 0) {
                builder
                        .started(started)
                        .position(position)
                        .total(total);
            }
        }
        return builder.build();
    }

    public String getPodcast() {
        return this.podcast;
    }

    public String getEpisode() {
        return this.episode;
    }

    public Action getAction() {
        return this.action;
    }

    private String getActionString() {
        return this.action.name().toLowerCase();
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    /**
     * Returns the position (in seconds) at which the client started playback.
     *
     * @return start position (in seconds)
     */
    public int getStarted() {
        return this.started;
    }

    /**
     * Returns the position (in seconds) at which the client stopped playback.
     *
     * @return stop position (in seconds)
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Returns the total length of the file in seconds.
     *
     * @return total length in seconds
     */
    public int getTotal() {
        return this.total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EpisodeAction)) {
            return false;
        }

        EpisodeAction that = (EpisodeAction) o;
        return started == that.started && position == that.position && total == that.total && action != that.action
                && ObjectsCompat.equals(podcast, that.podcast)
                && ObjectsCompat.equals(episode, that.episode)
                && ObjectsCompat.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int result = podcast != null ? podcast.hashCode() : 0;
        result = 31 * result + (episode != null ? episode.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + started;
        result = 31 * result + position;
        result = 31 * result + total;
        return result;
    }

    /**
     * Returns a JSON object representation of this object.
     *
     * @return JSON object representation, or null if the object is invalid
     */
    public JSONObject writeToJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.putOpt("podcast", this.podcast);
            obj.putOpt("episode", this.episode);
            obj.put("action", this.getActionString());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            obj.put("timestamp", formatter.format(this.timestamp));
            if (this.getAction() == Action.PLAY) {
                obj.put("started", this.started);
                obj.put("position", this.position);
                obj.put("total", this.total);
            }
        } catch (JSONException e) {
            Log.e(TAG, "writeToJSONObject(): " + e.getMessage());
            return null;
        }
        return obj;
    }

    @NonNull
    @Override
    public String toString() {
        return "EpisodeAction{"
                + "podcast='" + podcast + '\''
                + ", episode='" + episode + '\''
                + ", action=" + action
                + ", timestamp=" + timestamp
                + ", started=" + started
                + ", position=" + position
                + ", total=" + total
                + '}';
    }

    public enum Action {
        NEW, DOWNLOAD, PLAY, DELETE
    }

    public static class Builder {

        // mandatory
        private final String podcast;
        private final String episode;
        private final Action action;

        // optional
        private Date timestamp;
        private int started = -1;
        private int position = -1;
        private int total = -1;

        public Builder(FeedItem item, Action action) {
            this(item.getFeed().getDownload_url(), item.getMedia().getDownload_url(), action);
        }

        public Builder(String podcast, String episode, Action action) {
            this.podcast = podcast;
            this.episode = episode;
            this.action = action;
        }

        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder currentTimestamp() {
            return timestamp(new Date());
        }

        public Builder started(int seconds) {
            if (action == Action.PLAY) {
                this.started = seconds;
            }
            return this;
        }

        public Builder position(int seconds) {
            if (action == Action.PLAY) {
                this.position = seconds;
            }
            return this;
        }

        public Builder total(int seconds) {
            if (action == Action.PLAY) {
                this.total = seconds;
            }
            return this;
        }

        public EpisodeAction build() {
            return new EpisodeAction(this);
        }

    }

}
