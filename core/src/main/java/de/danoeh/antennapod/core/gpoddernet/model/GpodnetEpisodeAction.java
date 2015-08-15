package de.danoeh.antennapod.core.gpoddernet.model;


import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
            GpodnetEpisodeAction result = new Builder(podcast, episode, action)
                    .deviceId(deviceId)
                    .timestamp(new Date(Long.valueOf(fields[4])))
                    .started(Integer.valueOf(fields[5]))
                    .position(Integer.valueOf(fields[6]))
                    .total(Integer.valueOf(fields[7]))
                    .build();
            return result;
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
        if(StringUtils.isEmpty(podcast) || StringUtils.isEmpty(episode) || StringUtils.isEmpty(actionString)) {
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
        if(StringUtils.isNotEmpty(utcTimestamp)) {
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

    public String getActionString() {
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
        if(o == null) return false;
        if(this == o) return true;
        if(this.getClass() != o.getClass()) return false;
        GpodnetEpisodeAction that = (GpodnetEpisodeAction)o;
        return new EqualsBuilder()
                .append(this.podcast, that.podcast)
                .append(this.episode, that.episode)
                .append(this.deviceId, that.deviceId)
                .append(this.action, that.action)
                .append(this.timestamp, that.timestamp)
                .append(this.started, that.started)
                .append(this.position, that.position)
                .append(this.total, that.total)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.podcast)
                .append(this.episode)
                .append(this.deviceId)
                .append(this.action)
                .append(this.timestamp)
                .append(this.started)
                .append(this.position)
                .append(this.total)
                .toHashCode();
    }

    public String writeToString() {
        StringBuilder result = new StringBuilder();
        result.append(this.podcast).append("\t");
        result.append(this.episode).append("\t");
        result.append(this.deviceId).append("\t");
        result.append(this.action).append("\t");
        result.append(this.timestamp.getTime()).append("\t");
        result.append(String.valueOf(this.started)).append("\t");
        result.append(String.valueOf(this.position)).append("\t");
        result.append(String.valueOf(this.total));
        return result.toString();
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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
