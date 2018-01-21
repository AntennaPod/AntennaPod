package de.danoeh.antennapod.core.gpoddernet.model;


import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.util.DateUtils;

public class GpodnetEpisodeAction {

    private static final String TAG = "GpodnetEpisodeAction";

    public enum Action {
        NEW, DOWNLOAD, PLAY, DELETE
    }

    private final String podcast;
    private final String episode;
    private final String deviceId;
    private final Action action;
    private final Date timestamp;
    private final int started;
    private final int position;
    private final int total;

    private GpodnetEpisodeAction(Builder builder) {
        this.podcast = builder.podcast;
        this.episode = builder.episode;
        this.action = builder.action;
        this.deviceId = builder.deviceId;
        this.timestamp = builder.timestamp;
        this.started = builder.started;
        this.position = builder.position;
        this.total = builder.total;
    }

    /**
     * Creates an episode action object from a String representation. The representation includes
     * all mandatory and optional attributes
     *
     * @param s String representation (output from {@link #writeToString()})
     * @return episode action object, or null if s is invalid
     */
    public static GpodnetEpisodeAction readFromString(String s) {
        String[] fields = s.split("\t");
        if(fields.length != 8) {
            return null;
        }
        String podcast = fields[0];
        String episode = fields[1];
        String deviceId = fields[2];
        try {
            Action action = Action.valueOf(fields[3]);
            return new Builder(podcast, episode, action)
                    .deviceId(deviceId)
                    .timestamp(new Date(Long.parseLong(fields[4])))
                    .started(Integer.parseInt(fields[5]))
                    .position(Integer.parseInt(fields[6]))
                    .total(Integer.parseInt(fields[7]))
                    .build();
        } catch(IllegalArgumentException e) {
            Log.e(TAG, "readFromString(" + s + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Create an episode action object from JSON representation. Mandatory fields are "podcast",
     * "episode" and "action".
     *
     *  @param object JSON representation
     *  @return episode action object, or null if mandatory values are missing
     */
    public static GpodnetEpisodeAction readFromJSONObject(JSONObject object) {
        String podcast = object.optString("podcast", null);
        String episode = object.optString("episode", null);
        String actionString = object.optString("action", null);
        if(TextUtils.isEmpty(podcast) || TextUtils.isEmpty(episode) || TextUtils.isEmpty(actionString)) {
            return null;
        }
        GpodnetEpisodeAction.Action action;
        try {
            action = GpodnetEpisodeAction.Action.valueOf(actionString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String deviceId = object.optString("device", "");
        GpodnetEpisodeAction.Builder builder = new GpodnetEpisodeAction.Builder(podcast, episode, action)
                .deviceId(deviceId);
        String utcTimestamp = object.optString("timestamp", null);
        if(!TextUtils.isEmpty(utcTimestamp)) {
            builder.timestamp(DateUtils.parse(utcTimestamp));
        }
        if(action == GpodnetEpisodeAction.Action.PLAY) {
            int started = object.optInt("started", -1);
            int position = object.optInt("position", -1);
            int total = object.optInt("total", -1);
            if(started >= 0 && position > 0 && total > 0) {
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

    public String getDeviceId() {
        return this.deviceId;
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
     * Returns the position (in seconds) at which the client started playback
     *
     * @return start position (in seconds)
     */
    public int getStarted() {
        return this.started;
    }

    /**
     * Returns the position (in seconds) at which the client stopped playback
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GpodnetEpisodeAction that = (GpodnetEpisodeAction) o;

        if (started != that.started) return false;
        if (position != that.position) return false;
        if (total != that.total) return false;
        if (podcast != null ? !podcast.equals(that.podcast) : that.podcast != null) return false;
        if (episode != null ? !episode.equals(that.episode) : that.episode != null) return false;
        if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null)
            return false;
        if (action != that.action) return false;
        return !(timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null);

    }

    @Override
    public int hashCode() {
        int result = podcast != null ? podcast.hashCode() : 0;
        result = 31 * result + (episode != null ? episode.hashCode() : 0);
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + started;
        result = 31 * result + position;
        result = 31 * result + total;
        return result;
    }

    public String writeToString() {
        return this.podcast + "\t" +
                this.episode + "\t" +
                this.deviceId + "\t" +
                this.action + "\t" +
                this.timestamp.getTime() + "\t" +
                String.valueOf(this.started) + "\t" +
                String.valueOf(this.position) + "\t" +
                String.valueOf(this.total);
    }

    /**
     * Returns a JSON object representation of this object
     *
     * @return JSON object representation, or null if the object is invalid
     */
    public JSONObject writeToJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.putOpt("podcast", this.podcast);
            obj.putOpt("episode", this.episode);
            obj.put("device", this.deviceId);
            obj.put("action", this.getActionString());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            obj.put("timestamp",formatter.format(this.timestamp));
            if (this.getAction() == Action.PLAY) {
                obj.put("started", this.started);
                obj.put("position", this.position);
                obj.put("total", this.total);
            }
        } catch(JSONException e) {
            Log.e(TAG, "writeToJSONObject(): " + e.getMessage());
            return null;
        }
        return obj;
    }

    @Override
    public String toString() {
        return "GpodnetEpisodeAction{" +
                "podcast='" + podcast + '\'' +
                ", episode='" + episode + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", action=" + action +
                ", timestamp=" + timestamp +
                ", started=" + started +
                ", position=" + position +
                ", total=" + total +
                '}';
    }

    public static class Builder {

        // mandatory
        private final String podcast;
        private final String episode;
        private final Action action;

        // optional
        private String deviceId = "";
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

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder currentDeviceId() {
            return deviceId(GpodnetPreferences.getDeviceID());
        }

        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder currentTimestamp() {
            return timestamp(new Date());
        }

        public Builder started(int seconds) {
            if(action == Action.PLAY) {
                this.started = seconds;
            }
            return this;
        }

        public Builder position(int seconds) {
            if(action == Action.PLAY) {
                this.position = seconds;
            }
            return this;
        }

        public Builder total(int seconds) {
            if(action == Action.PLAY) {
                this.total = seconds;
            }
            return this;
        }

        public GpodnetEpisodeAction build() {
            return new GpodnetEpisodeAction(this);
        }

    }

}
