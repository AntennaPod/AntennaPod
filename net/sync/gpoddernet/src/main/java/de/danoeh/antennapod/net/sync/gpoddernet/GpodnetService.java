package de.danoeh.antennapod.net.sync.gpoddernet;

import android.util.Log;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.net.sync.HostnameParser;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeActionChanges;
import de.danoeh.antennapod.net.sync.serviceinterface.ISyncService;
import de.danoeh.antennapod.net.sync.serviceinterface.SubscriptionChanges;
import de.danoeh.antennapod.net.sync.serviceinterface.SyncServiceException;
import de.danoeh.antennapod.net.sync.serviceinterface.UploadChangesResponse;
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
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.net.sync.gpoddernet.mapper.ResponseMapper;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetDevice;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetEpisodeActionPostResponse;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetUploadChangesResponse;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Communicates with the gpodder.net service.
 */
public class GpodnetService implements ISyncService {
    public static final String TAG = "GpodnetService";
    private static final String DEFAULT_BASE_HOST = "gpodder.net";
    private static final int UPLOAD_BULK_SIZE = 30;
    private static final MediaType TEXT = MediaType.parse("plain/text; charset=utf-8");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private String baseScheme;
    private int basePort;
    private final String baseHost;
    private final String deviceId;
    private String username;
    private String password;
    private boolean loggedIn = false;

    private final OkHttpClient httpClient;

    public GpodnetService(OkHttpClient httpClient, String baseHostUrl,
                          String deviceId, String username, String password)  {
        this.httpClient = httpClient;
        this.deviceId = deviceId;
        this.username = username;
        this.password = password;
        HostnameParser hostname = new HostnameParser(baseHostUrl == null ? DEFAULT_BASE_HOST : baseHostUrl);
        this.baseHost = hostname.host;
        this.basePort = hostname.port;
        this.baseScheme = hostname.scheme;
    }

    private void requireLoggedIn() {
        if (!loggedIn) {
            throw new IllegalStateException("Not logged in");
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
            URL url = new URI(baseScheme, null, baseHost, basePort,
                    String.format("/api/2/devices/%s.json", username), null, null).toURL();
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
            URL url = new URI(baseScheme, null, baseHost, basePort,
                    String.format("/api/2/devices/%s/%s.json", username, deviceId), null, null).toURL();
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
            RequestBody body = RequestBody.create(content, JSON);
            Request.Builder request = new Request.Builder().post(body).url(url);
            executeRequest(request);
        } catch (JSONException | MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Updates the subscription list of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param added    Collection of feed URLs of added feeds. This Collection MUST NOT contain any duplicates
     * @param removed  Collection of feed URLs of removed feeds. This Collection MUST NOT contain any duplicates
     * @return a GpodnetUploadChangesResponse. See {@link GpodnetUploadChangesResponse}
     * for details.
     * @throws GpodnetServiceException            if added or removed contain duplicates or if there
     *                                            is an authentication error.
     */
    @Override
    public UploadChangesResponse uploadSubscriptionChanges(List<String> added, List<String> removed)
            throws GpodnetServiceException {
        requireLoggedIn();
        try {
            URL url = new URI(baseScheme, null, baseHost, basePort,
                    String.format("/api/2/subscriptions/%s/%s.json", username, deviceId), null, null).toURL();

            final JSONObject requestObject = new JSONObject();
            requestObject.put("add", new JSONArray(added));
            requestObject.put("remove", new JSONArray(removed));

            RequestBody body = RequestBody.create(requestObject.toString(), JSON);
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
     * @param timestamp A timestamp that can be used to receive all changes since a
     *                  specific point in time.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    @Override
    public SubscriptionChanges getSubscriptionChanges(long timestamp) throws GpodnetServiceException {
        requireLoggedIn();
        String params = String.format(Locale.US, "since=%d", timestamp);
        String path = String.format("/api/2/subscriptions/%s/%s.json", username, deviceId);
        try {
            URL url = new URI(baseScheme, null, baseHost, basePort, path, params, null).toURL();
            Request.Builder request = new Request.Builder().url(url);

            String response = executeRequest(request);
            JSONObject changes = new JSONObject(response);
            return ResponseMapper.readSubscriptionChangesFromJsonObject(changes);
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
            URL url = new URI(baseScheme, null, baseHost, basePort,
                    String.format("/api/2/episodes/%s.json", username), null, null).toURL();

            final JSONArray list = new JSONArray();
            for (int i = from; i < to; i++) {
                EpisodeAction episodeAction = episodeActions.get(i);
                JSONObject obj = episodeAction.writeToJsonObject();
                if (obj != null) {
                    obj.put("device", deviceId);
                    list.put(obj);
                }
            }

            RequestBody body = RequestBody.create(list.toString(), JSON);
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
            URL url = new URI(baseScheme, null, baseHost, basePort, path, params, null).toURL();
            Request.Builder request = new Request.Builder().url(url);

            String response = executeRequest(request);
            JSONObject json = new JSONObject(response);
            return ResponseMapper.readEpisodeActionsFromJsonObject(json);
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
    @Override
    public void login() throws GpodnetServiceException {
        URL url;
        try {
            url = new URI(baseScheme, null, baseHost, basePort,
                    String.format("/api/2/auth/%s/login.json", username), null, null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
        RequestBody requestBody = RequestBody.create("", TEXT);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try {
            String credential = Credentials.basic(username, password, Charset.forName("UTF-8"));
            Request authRequest = request.newBuilder().header("Authorization", credential).build();
            Response response = httpClient.newCall(authRequest).execute();
            checkStatusCode(response);
            response.body().close();
            this.loggedIn = true;
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
            return outputStream.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    private void checkStatusCode(@NonNull Response response) throws GpodnetServiceException {
        int responseCode = response.code();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new GpodnetServiceAuthenticationException("Wrong username or password");
            } else {
                if (BuildConfig.DEBUG) {
                    try {
                        Log.d(TAG, response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (responseCode >= 500) {
                    throw new GpodnetServiceBadStatusCodeException(this.baseHost + " is currently unavailable (code "
                            + responseCode + ")", responseCode);
                } else {
                    throw new GpodnetServiceBadStatusCodeException("Unable to connect to " + this.baseHost + " (code "
                            + responseCode + ": " + response.message() + ")", responseCode);
                }
            }
        }
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

    @Override
    public void logout() {

    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
