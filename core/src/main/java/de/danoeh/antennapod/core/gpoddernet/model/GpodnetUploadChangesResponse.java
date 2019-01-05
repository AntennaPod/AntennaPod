package de.danoeh.antennapod.core.gpoddernet.model;

import android.support.v4.util.ArrayMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Object returned by {@link de.danoeh.antennapod.core.gpoddernet.GpodnetService} in uploadChanges method.
 */
public class GpodnetUploadChangesResponse {

    /**
     * timestamp/ID that can be used for requesting changes since this upload.
     */
    public final long timestamp;

    /**
     * URLs that should be updated. The key of the map is the original URL, the value of the map
     * is the sanitized URL.
     */
    private final Map<String, String> updatedUrls;

    private GpodnetUploadChangesResponse(long timestamp, Map<String, String> updatedUrls) {
        this.timestamp = timestamp;
        this.updatedUrls = updatedUrls;
    }

    /**
     * Creates a new GpodnetUploadChangesResponse-object from a JSON object that was
     * returned by an uploadChanges call.
     *
     * @throws org.json.JSONException If the method could not parse the JSONObject.
     */
    public static GpodnetUploadChangesResponse fromJSONObject(String objectString) throws JSONException {
        final JSONObject object = new JSONObject(objectString);
        final long timestamp = object.getLong("timestamp");
        Map<String, String> updatedUrls = new ArrayMap<>();
        JSONArray urls = object.getJSONArray("update_urls");
        for (int i = 0; i < urls.length(); i++) {
            JSONArray urlPair = urls.getJSONArray(i);
            updatedUrls.put(urlPair.getString(0), urlPair.getString(1));
        }
        return new GpodnetUploadChangesResponse(timestamp, updatedUrls);
    }

    @Override
    public String toString() {
        return "GpodnetUploadChangesResponse{" +
                "timestamp=" + timestamp +
                ", updatedUrls=" + updatedUrls +
                '}';
    }
}
