package de.danoeh.antennapod.core.sync.gpoddernet;

import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetDevice;
import de.danoeh.antennapod.core.sync.model.EpisodeAction;
import de.danoeh.antennapod.core.sync.model.EpisodeActionChanges;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetEpisodeActionPostResponse;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.core.sync.model.ISyncService;
import de.danoeh.antennapod.core.sync.model.SubscriptionChanges;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetTag;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetUploadChangesResponse;
import de.danoeh.antennapod.core.sync.model.SyncServiceException;
import de.danoeh.antennapod.core.sync.model.UploadChangesResponse;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Communicates with the gpodder.net service.
 */
public class GpodnetService implements ISyncService {
    public static final String TAG = "GpodnetService";
    public static final String DEFAULT_BASE_HOST = "gpodder.net";
    private static final String BASE_SCHEME = "https";
    private static final int UPLOAD_BULK_SIZE = 30;
    private static final MediaType TEXT = MediaType.parse("plain/text; charset=utf-8");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String baseHost;
    private final OkHttpClient httpClient;
    private String username = null;

    public GpodnetService(OkHttpClient httpClient, String baseHost) {
        this.httpClient = httpClient;
        this.baseHost = baseHost;
    }

    private void requireLoggedIn() {
        if (username == null) {
            throw new IllegalStateException("Not logged in");
        }
    }

    /**
     * Returns the [count] most used tags.
     */
    public List<GpodnetTag> getTopTags(int count) throws GpodnetServiceException {
        URL url;
        try {
            url = new URI(BASE_SCHEME, baseHost, String.format(Locale.US, "/api/2/tags/%d.json", count), null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }

        Request.Builder request = new Request.Builder().url(url);
        String response = executeRequest(request);
        try {
            JSONArray jsonTagList = new JSONArray(response);
            List<GpodnetTag> tagList = new ArrayList<>(jsonTagList.length());
            for (int i = 0; i < jsonTagList.length(); i++) {
                JSONObject jsonObject = jsonTagList.getJSONObject(i);
                String title = jsonObject.getString("title");
                String tag = jsonObject.getString("tag");
                int usage = jsonObject.getInt("usage");
                tagList.add(new GpodnetTag(title, tag, usage));
            }
            return tagList;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns the [count] most subscribed podcasts for the given tag.
     *
     * @throws IllegalArgumentException if tag is null
     */
    public List<GpodnetPodcast> getPodcastsForTag(@NonNull GpodnetTag tag, int count)
            throws GpodnetServiceException {
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(Locale.US,
                    "/api/2/tag/%s/%d.json", tag.getTag(), count), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJsonArray(jsonArray);

        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns the toplist of podcast.
     *
     * @param count of elements that should be returned. Must be in range 1..100.
     * @throws IllegalArgumentException if count is out of range.
     */
    public List<GpodnetPodcast> getPodcastToplist(int count) throws GpodnetServiceException {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Count must be in range 1..100");
        }

        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(Locale.US, "/toplist/%d.json", count), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJsonArray(jsonArray);

        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns a list of suggested podcasts for the user that is currently
     * logged in.
     * <p/>
     * This method requires authentication.
     *
     * @param count The
     *              number of elements that should be returned. Must be in range
     *              1..100.
     * @throws IllegalArgumentException              if count is out of range.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public List<GpodnetPodcast> getSuggestions(int count) throws GpodnetServiceException {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Count must be in range 1..100");
        }

        try {
            URL url = new URI(BASE_SCHEME, baseHost,
                    String.format(Locale.US, "/suggestions/%d.json", count), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJsonArray(jsonArray);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Searches the podcast directory for a given string.
     *
     * @param query          The search query
     * @param scaledLogoSize The size of the logos that are returned by the search query.
     *                       Must be in range 1..256. If the value is out of range, the
     *                       default value defined by the gpodder.net API will be used.
     */
    public List<GpodnetPodcast> searchPodcasts(String query, int scaledLogoSize) throws GpodnetServiceException {
        String parameters = (scaledLogoSize > 0 && scaledLogoSize <= 256) ? String
                .format(Locale.US, "q=%s&scale_logo=%d", query, scaledLogoSize) : String
                .format("q=%s", query);
        try {
            URL url = new URI(BASE_SCHEME, null, baseHost, -1, "/search.json",
                    parameters, null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJsonArray(jsonArray);

        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns all devices of a given user.
     * <p/>
     * This method requires authentication.
     *
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public List<GpodnetDevice> getDevices() throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format("/api/2/devices/%s.json", username), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            String response = executeRequest(request);
            JSONArray devicesArray = new JSONArray(response);
            return readDeviceListFromJsonArray(devicesArray);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Configures the device of a given user.
     * <p/>
     * This method requires authentication.
     *
     * @param deviceId The ID of the device that should be configured.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public void configureDevice(@NonNull String deviceId, String caption, GpodnetDevice.DeviceType type)
            throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(
                    "/api/2/devices/%s/%s.json", username, deviceId), null).toURL();
            String content;
            if (caption != null || type != null) {
                JSONObject jsonContent = new JSONObject();
                if (caption != null) {
                    jsonContent.put("caption", caption);
                }
                if (type != null) {
                    jsonContent.put("type", type.toString());
                }
                content = jsonContent.toString();
            } else {
                content = "";
            }
            RequestBody body = RequestBody.create(JSON, content);
            Request.Builder request = new Request.Builder().post(body).url(url);
            executeRequest(request);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns the subscriptions of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param deviceId The ID of the device whose subscriptions should be returned.
     * @return A list of subscriptions in OPML format.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public String getSubscriptionsOfDevice(@NonNull String deviceId) throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(
                    "/subscriptions/%s/%s.opml", username, deviceId), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            return executeRequest(request);
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns all subscriptions of a specific user.
     * <p/>
     * This method requires authentication.
     *
     * @return A list of subscriptions in OPML format.
     * @throws IllegalArgumentException              If username is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public String getSubscriptionsOfUser() throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format("/subscriptions/%s.opml", username), null).toURL();
            Request.Builder request = new Request.Builder().url(url);
            return executeRequest(request);
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Uploads the subscriptions of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param deviceId      The ID of the device whose subscriptions should be updated.
     * @param subscriptions A list of feed URLs containing all subscriptions of the
     *                      device.
     * @throws IllegalArgumentException              If username, deviceId or subscriptions is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public void uploadSubscriptions(@NonNull String deviceId, @NonNull List<String> subscriptions)
            throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(
                    "/subscriptions/%s/%s.txt", username, deviceId), null).toURL();
            StringBuilder builder = new StringBuilder();
            for (String s : subscriptions) {
                builder.append(s);
                builder.append("\n");
            }
            RequestBody body = RequestBody.create(TEXT, builder.toString());
            Request.Builder request = new Request.Builder().put(body).url(url);
            executeRequest(request);
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }

    }

    /**
     * Updates the subscription list of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param deviceId The ID of the device whose subscriptions should be updated.
     * @param added    Collection of feed URLs of added feeds. This Collection MUST NOT contain any duplicates
     * @param removed  Collection of feed URLs of removed feeds. This Collection MUST NOT contain any duplicates
     * @return a GpodnetUploadChangesResponse. See {@link GpodnetUploadChangesResponse}
     * for details.
     * @throws GpodnetServiceException            if added or removed contain duplicates or if there
     *                                            is an authentication error.
     */
    public GpodnetUploadChangesResponse uploadChanges(@NonNull String deviceId, @NonNull Collection<String> added,
            @NonNull Collection<String> removed) throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(BASE_SCHEME, baseHost, String.format(
                    "/api/2/subscriptions/%s/%s.json", username, deviceId), null).toURL();

            final JSONObject requestObject = new JSONObject();
            requestObject.put("add", new JSONArray(added));
            requestObject.put("remove", new JSONArray(removed));

            RequestBody body = RequestBody.create(JSON, requestObject.toString());
            Request.Builder request = new Request.Builder().post(body).url(url);

            final String response = executeRequest(request);
            return GpodnetUploadChangesResponse.fromJSONObject(response);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }

    }

    /**
     * Returns all subscription changes of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param deviceId  The ID of the device whose subscription changes should be
     *                  downloaded.
     * @param timestamp A timestamp that can be used to receive all changes since a
     *                  specific point in time.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public SubscriptionChanges getSubscriptionChanges(@NonNull String deviceId, long timestamp)
            throws GpodnetServiceException {
        requireLoggedIn();
        String params = String.format(Locale.US, "since=%d", timestamp);
        String path = String.format("/api/2/subscriptions/%s/%s.json", username, deviceId);
        try {
            URL url = new URI(BASE_SCHEME, null, baseHost, -1, path, params,
                    null).toURL();
            Request.Builder request = new Request.Builder().url(url);

            String response = executeRequest(request);
            JSONObject changes = new JSONObject(response);
            return readSubscriptionChangesFromJsonObject(changes);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }

    }

    /**
     * Updates the episode actions
     * <p/>
     * This method requires authentication.
     *
     * @param episodeActions Collection of episode actions.
     * @return a GpodnetUploadChangesResponse. See {@link GpodnetUploadChangesResponse}
     * for details.
     * @throws GpodnetServiceException            if added or removed contain duplicates or if there
     *                                            is an authentication error.
     */
    @Override
    public UploadChangesResponse uploadEpisodeActions(List<EpisodeAction> episodeActions) throws SyncServiceException {
        requireLoggedIn();
        UploadChangesResponse response = null;
        for (int i = 0; i < episodeActions.size(); i += UPLOAD_BULK_SIZE) {
            response = uploadEpisodeActionsPartial(episodeActions,
                    i, Math.min(episodeActions.size(), i + UPLOAD_BULK_SIZE));
        }
        return response;
    }

    private UploadChangesResponse uploadEpisodeActionsPartial(List<EpisodeAction> episodeActions, int from, int to)
            throws SyncServiceException {
        try {
            Log.d(TAG, "Uploading partial actions " + from + " to " + to + " of " + episodeActions.size());
            URL url = new URI(BASE_SCHEME, baseHost, String.format(
                    "/api/2/episodes/%s.json", username), null).toURL();

            final JSONArray list = new JSONArray();
            for (int i = from; i < to; i++) {
                EpisodeAction episodeAction = episodeActions.get(i);
                JSONObject obj = episodeAction.writeToJsonObject();
                if (obj != null) {
                    obj.put("device", GpodnetPreferences.getDeviceID());
                    list.put(obj);
                }
            }

            RequestBody body = RequestBody.create(JSON, list.toString());
            Request.Builder request = new Request.Builder().post(body).url(url);

            final String response = executeRequest(request);
            return GpodnetEpisodeActionPostResponse.fromJSONObject(response);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        }
    }

    /**
     * Returns all subscription changes of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param timestamp A timestamp that can be used to receive all changes since a
     *                  specific point in time.
     * @throws SyncServiceException If there is an authentication error.
     */
    @Override
    public EpisodeActionChanges getEpisodeActionChanges(long timestamp) throws SyncServiceException {
        requireLoggedIn();
        String params = String.format(Locale.US, "since=%d", timestamp);
        String path = String.format("/api/2/episodes/%s.json", username);
        try {
            URL url = new URI(BASE_SCHEME, null, baseHost, -1, path, params, null).toURL();
            Request.Builder request = new Request.Builder().url(url);

            String response = executeRequest(request);
            JSONObject json = new JSONObject(response);
            return readEpisodeActionsFromJsonObject(json);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        }

    }


    /**
     * Logs in a specific user. This method must be called if any of the methods
     * that require authentication is used.
     *
     * @throws IllegalArgumentException If username or password is null.
     */
    public void authenticate(@NonNull String username, @NonNull String password) throws GpodnetServiceException {
        URL url;
        try {
            url = new URI(BASE_SCHEME, baseHost, String.format("/api/2/auth/%s/login.json", username), null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
        RequestBody requestBody = RequestBody.create(TEXT, "");
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try {
            String credential = Credentials.basic(username, password, Charset.forName("UTF-8"));
            Request authRequest = request.newBuilder().header("Authorization", credential).build();
            Response response = httpClient.newCall(authRequest).execute();
            checkStatusCode(response);
            response.body().close();
            this.username = username;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    private String executeRequest(@NonNull Request.Builder requestB) throws GpodnetServiceException {
        Request request = requestB.build();
        String responseString;
        Response response;
        ResponseBody body = null;
        try {

            response = httpClient.newCall(request).execute();
            checkStatusCode(response);
            body = response.body();
            responseString = getStringFromResponseBody(body);
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } finally {
            if (body != null) {
                body.close();
            }
        }
        return responseString;
    }

    private String getStringFromResponseBody(@NonNull ResponseBody body) throws GpodnetServiceException {
        ByteArrayOutputStream outputStream;
        int contentLength = (int) body.contentLength();
        if (contentLength > 0) {
            outputStream = new ByteArrayOutputStream(contentLength);
        } else {
            outputStream = new ByteArrayOutputStream();
        }
        try {
            byte[] buffer = new byte[8 * 1024];
            InputStream in = body.byteStream();
            int count;
            while ((count = in.read(buffer)) > 0) {
                outputStream.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
        return outputStream.toString();
    }

    private void checkStatusCode(@NonNull Response response) throws GpodnetServiceException {
        int responseCode = response.code();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new GpodnetServiceAuthenticationException("Wrong username or password");
            } else {
                throw new GpodnetServiceBadStatusCodeException("Bad response code: " + responseCode, responseCode);
            }
        }
    }

    private List<GpodnetPodcast> readPodcastListFromJsonArray(@NonNull JSONArray array) throws JSONException {
        List<GpodnetPodcast> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(readPodcastFromJsonObject(array.getJSONObject(i)));
        }
        return result;
    }

    private GpodnetPodcast readPodcastFromJsonObject(JSONObject object) throws JSONException {
        String url = object.getString("url");

        String title;
        Object titleObj = object.opt("title");
        if (titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            title = url;
        }

        String description;
        Object descriptionObj = object.opt("description");
        if (descriptionObj instanceof String) {
            description = (String) descriptionObj;
        } else {
            description = "";
        }

        int subscribers = object.getInt("subscribers");

        Object logoUrlObj = object.opt("logo_url");
        String logoUrl = (logoUrlObj instanceof String) ? (String) logoUrlObj : null;
        if (logoUrl == null) {
            Object scaledLogoUrl = object.opt("scaled_logo_url");
            if (scaledLogoUrl instanceof String) {
                logoUrl = (String) scaledLogoUrl;
            }
        }

        String website = null;
        Object websiteObj = object.opt("website");
        if (websiteObj instanceof String) {
            website = (String) websiteObj;
        }
        String mygpoLink = object.getString("mygpo_link");

        String author = null;
        Object authorObj = object.opt("author");
        if (authorObj instanceof String) {
            author = (String) authorObj;
        }
        return new GpodnetPodcast(url, title, description, subscribers, logoUrl, website, mygpoLink, author);
    }

    private List<GpodnetDevice> readDeviceListFromJsonArray(@NonNull JSONArray array) throws JSONException {
        List<GpodnetDevice> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(readDeviceFromJsonObject(array.getJSONObject(i)));
        }
        return result;
    }

    private GpodnetDevice readDeviceFromJsonObject(JSONObject object) throws JSONException {
        String id = object.getString("id");
        String caption = object.getString("caption");
        String type = object.getString("type");
        int subscriptions = object.getInt("subscriptions");
        return new GpodnetDevice(id, caption, type, subscriptions);
    }

    private SubscriptionChanges readSubscriptionChangesFromJsonObject(@NonNull JSONObject object)
            throws JSONException {

        List<String> added = new LinkedList<>();
        JSONArray jsonAdded = object.getJSONArray("add");
        for (int i = 0; i < jsonAdded.length(); i++) {
            String addedUrl = jsonAdded.getString(i);
            // gpodder escapes colons unnecessarily
            addedUrl = addedUrl.replace("%3A", ":");
            added.add(addedUrl);
        }

        List<String> removed = new LinkedList<>();
        JSONArray jsonRemoved = object.getJSONArray("remove");
        for (int i = 0; i < jsonRemoved.length(); i++) {
            String removedUrl = jsonRemoved.getString(i);
            // gpodder escapes colons unnecessarily
            removedUrl = removedUrl.replace("%3A", ":");
            removed.add(removedUrl);
        }

        long timestamp = object.getLong("timestamp");
        return new SubscriptionChanges(added, removed, timestamp);
    }

    private EpisodeActionChanges readEpisodeActionsFromJsonObject(@NonNull JSONObject object)
            throws JSONException {

        List<EpisodeAction> episodeActions = new ArrayList<>();

        long timestamp = object.getLong("timestamp");
        JSONArray jsonActions = object.getJSONArray("actions");
        for (int i = 0; i < jsonActions.length(); i++) {
            JSONObject jsonAction = jsonActions.getJSONObject(i);
            EpisodeAction episodeAction = EpisodeAction.readFromJsonObject(jsonAction);
            if (episodeAction != null) {
                episodeActions.add(episodeAction);
            }
        }
        return new EpisodeActionChanges(episodeActions, timestamp);
    }

    @Override
    public void login() throws GpodnetServiceException {
        authenticate(GpodnetPreferences.getUsername(), GpodnetPreferences.getPassword());
    }

    @Override
    public SubscriptionChanges getSubscriptionChanges(long lastSync) throws GpodnetServiceException {
        return getSubscriptionChanges(GpodnetPreferences.getDeviceID(), lastSync);
    }

    @Override
    public UploadChangesResponse uploadSubscriptionChanges(List<String> addedFeeds, List<String> removedFeeds)
            throws GpodnetServiceException {
        return uploadChanges(GpodnetPreferences.getDeviceID(), addedFeeds, removedFeeds);
    }

    @Override
    public void logout() {

    }
}
