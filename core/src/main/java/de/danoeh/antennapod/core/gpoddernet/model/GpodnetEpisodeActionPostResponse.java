package de.danoeh.antennapod.core.gpoddernet.model;

import android.support.v4.util.ArrayMap;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class GpodnetEpisodeActionPostResponse {

    /**
     * timestamp/ID that can be used for requesting changes since this upload.
     */
    public final long timestamp;

    /**
     * URLs that should be updated. The key of the map is the original URL, the value of the map
     * is the sanitized URL.
     */
    private final Map<String, String> updatedUrls;

    private GpodnetEpisodeActionPostResponse(long timestamp, Map<String, String> updatedUrls) {
        this.timestamp = timestamp;
        this.updatedUrls = updatedUrls;
    }

    /**
     * Creates a new GpodnetUploadChangesResponse-object from a JSON object that was
     * returned by an uploadChanges call.
     *
     * @throws org.json.JSONException If the method could not parse the JSONObject.
     */
    public static GpodnetEpisodeActionPostResponse fromJSONObject(String objectString) throws JSONException {
        final JSONObject object = new JSONObject(objectString);
        final long timestamp = object.getLong("timestamp");
        JSONArray urls = object.getJSONArray("update_urls");
        Map<String, String> updatedUrls = new ArrayMap<>(urls.length());
        for (int i = 0; i < urls.length(); i++) {
            JSONArray urlPair = urls.getJSONArray(i);
            updatedUrls.put(urlPair.getString(0), urlPair.getString(1));
        }
        return new GpodnetEpisodeActionPostResponse(timestamp, updatedUrls);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

