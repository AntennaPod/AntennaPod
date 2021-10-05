package de.danoeh.antennapod.net.sync.nextcloud;

import de.danoeh.antennapod.net.sync.HostnameParser;
import de.danoeh.antennapod.net.sync.gpoddernet.mapper.ResponseMapper;
import de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetUploadChangesResponse;
import de.danoeh.antennapod.net.sync.model.EpisodeAction;
import de.danoeh.antennapod.net.sync.model.EpisodeActionChanges;
import de.danoeh.antennapod.net.sync.model.ISyncService;
import de.danoeh.antennapod.net.sync.model.SubscriptionChanges;
import de.danoeh.antennapod.net.sync.model.SyncServiceException;
import de.danoeh.antennapod.net.sync.model.UploadChangesResponse;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

public class NextcloudSyncService implements ISyncService {
    private static final int UPLOAD_BULK_SIZE = 30;
    private final OkHttpClient httpClient;
    private final String baseScheme;
    private final int basePort;
    private final String baseHost;
    private final String username;
    private final String password;

    public NextcloudSyncService(OkHttpClient httpClient, String baseHosturl,
                          String username, String password)  {
        this.httpClient = httpClient;
        this.username = username;
        this.password = password;
        HostnameParser hostname = new HostnameParser(baseHosturl);
        this.baseHost = hostname.host;
        this.basePort = hostname.port;
        this.baseScheme = hostname.scheme;
    }

    @Override
    public void login() {
    }

    @Override
    public SubscriptionChanges getSubscriptionChanges(long lastSync) throws SyncServiceException {
        try {
            HttpUrl.Builder url = makeUrl("/index.php/apps/gpoddersync/subscriptions");
            url.addQueryParameter("since", "" + lastSync);
            String responseString = performRequest(url, "GET", null);
            JSONObject json = new JSONObject(responseString);
            return ResponseMapper.readSubscriptionChangesFromJsonObject(json);
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        }
    }

    @Override
    public UploadChangesResponse uploadSubscriptionChanges(List<String> addedFeeds,
                                                           List<String> removedFeeds)
            throws NextcloudSynchronizationServiceException {
        try {
            HttpUrl.Builder url = makeUrl("/index.php/apps/gpoddersync/subscription_change/create");
            final JSONObject requestObject = new JSONObject();
            requestObject.put("add", new JSONArray(addedFeeds));
            requestObject.put("remove", new JSONArray(removedFeeds));
            RequestBody requestBody = RequestBody.create(
                    MediaType.get("application/json"), requestObject.toString());
            performRequest(url, "POST", requestBody);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NextcloudSynchronizationServiceException(e);
        }

        return new GpodnetUploadChangesResponse(System.currentTimeMillis() / 1000, new HashMap<>());
    }

    @Override
    public EpisodeActionChanges getEpisodeActionChanges(long timestamp) throws SyncServiceException {
        try {
            HttpUrl.Builder uri = makeUrl("/index.php/apps/gpoddersync/episode_action");
            uri.addQueryParameter("since", "" + timestamp);
            String responseString = performRequest(uri, "GET", null);
            JSONObject json = new JSONObject(responseString);
            return ResponseMapper.readEpisodeActionsFromJsonObject(json);
        } catch (JSONException | MalformedURLException e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SyncServiceException(e);
        }
    }

    @Override
    public UploadChangesResponse uploadEpisodeActions(List<EpisodeAction> queuedEpisodeActions)
            throws NextcloudSynchronizationServiceException {
        for (int i = 0; i < queuedEpisodeActions.size(); i += UPLOAD_BULK_SIZE) {
            uploadEpisodeActionsPartial(queuedEpisodeActions,
                    i, Math.min(queuedEpisodeActions.size(), i + UPLOAD_BULK_SIZE));
        }
        return new NextcloudGpodderEpisodeActionPostResponse(System.currentTimeMillis() / 1000);
    }

    private void uploadEpisodeActionsPartial(List<EpisodeAction> queuedEpisodeActions, int from, int to)
            throws NextcloudSynchronizationServiceException {
        try {
            final JSONArray list = new JSONArray();
            for (int i = from; i < to; i++) {
                EpisodeAction episodeAction = queuedEpisodeActions.get(i);
                JSONObject obj = episodeAction.writeToJsonObject();
                if (obj != null) {
                    list.put(obj);
                }
            }
            HttpUrl.Builder url = makeUrl("/index.php/apps/gpoddersync/episode_action/create");
            RequestBody requestBody = RequestBody.create(
                    MediaType.get("application/json"), list.toString());
            performRequest(url, "POST", requestBody);
        } catch (Exception e) {
            e.printStackTrace();
            throw new NextcloudSynchronizationServiceException(e);
        }
    }

    private String performRequest(HttpUrl.Builder url, String method, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(url.build())
                .header("Authorization", Credentials.basic(username, password))
                .header("Accept", "application/json")
                .method(method, body)
                .build();
        Response response = httpClient.newCall(request).execute();
        if (response.code() != 200) {
            throw new IOException("Response code: " + response.code());
        }
        return response.body().string();
    }

    private HttpUrl.Builder makeUrl(String path) {
        return new HttpUrl.Builder()
                .scheme(baseScheme)
                .host(baseHost)
                .port(basePort)
                .addPathSegments(path);
    }

    @Override
    public void logout() {
    }

    private static class NextcloudGpodderEpisodeActionPostResponse extends UploadChangesResponse {
        public NextcloudGpodderEpisodeActionPostResponse(long epochSecond) {
            super(epochSecond);
        }
    }
}

