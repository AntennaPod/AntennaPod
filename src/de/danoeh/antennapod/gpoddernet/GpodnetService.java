package de.danoeh.antennapod.gpoddernet;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.gpoddernet.model.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Communicates with the gpodder.net service.
 */
public class GpodnetService {

    private static final String BASE_SCHEME = "https";
    private static final String BASE_HOST = "gpodder.net";

    private static final int TIMEOUT_MILLIS = 20000;

    private final GpodnetClient httpClient;

    public GpodnetService() {
        httpClient = new GpodnetClient();
        final HttpParams params = httpClient.getParams();
        params.setParameter(CoreProtocolPNames.USER_AGENT, AppConfig.USER_AGENT);
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT_MILLIS);
    }

    /**
     * Returns the [count] most used tags.
     */
    public List<GpodnetTag> getTopTags(int count)
            throws GpodnetServiceException {
        URI uri;
        try {
            uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/tags/%d.json", count), null);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
            throw new IllegalStateException(e1);
        }

        HttpGet request = new HttpGet(uri);
        String response = executeRequest(request);
        try {
            JSONArray jsonTagList = new JSONArray(response);
            List<GpodnetTag> tagList = new ArrayList<GpodnetTag>(
                    jsonTagList.length());
            for (int i = 0; i < jsonTagList.length(); i++) {
                JSONObject jObj = jsonTagList.getJSONObject(i);
                String name = jObj.getString("tag");
                int usage = jObj.getInt("usage");
                tagList.add(new GpodnetTag(name, usage));
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
    public List<GpodnetPodcast> getPodcastsForTag(GpodnetTag tag, int count)
            throws GpodnetServiceException {
        if (tag == null) {
            throw new IllegalArgumentException(
                    "Tag and title of tag must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/tag/%s/%d.json", tag.getName(), count), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJSONArray(jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (URISyntaxException e) {
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
    public List<GpodnetPodcast> getPodcastToplist(int count)
            throws GpodnetServiceException {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("Count must be in range 1..100");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/toplist/%d.json", count), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJSONArray(jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);

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
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/suggestions/%d.json", count), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJSONArray(jsonArray);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException e) {
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
    public List<GpodnetPodcast> searchPodcasts(String query, int scaledLogoSize)
            throws GpodnetServiceException {
        String parameters = (scaledLogoSize > 0 && scaledLogoSize <= 256) ? String
                .format("q=%s&scale_logo=%d", query, scaledLogoSize) : String
                .format("q=%s", query);
        try {
            URI uri = new URI(BASE_SCHEME, null, BASE_HOST, -1, "/search.json",
                    parameters, null);
            System.out.println(uri.toASCIIString());
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);

            JSONArray jsonArray = new JSONArray(response);
            return readPodcastListFromJSONArray(jsonArray);

        } catch (JSONException e) {
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
     * @param username The username. Must be the same user as the one which is
     *                 currently logged in.
     * @throws IllegalArgumentException              If username is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public List<GpodnetDevice> getDevices(String username)
            throws GpodnetServiceException {
        if (username == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/devices/%s.json", username), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);
            JSONArray devicesArray = new JSONArray(response);
            List<GpodnetDevice> result = readDeviceListFromJSONArray(devicesArray);

            return result;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Configures the device of a given user.
     * <p/>
     * This method requires authentication.
     *
     * @param username The username. Must be the same user as the one which is
     *                 currently logged in.
     * @param deviceId The ID of the device that should be configured.
     * @throws IllegalArgumentException              If username or deviceId is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public void configureDevice(String username, String deviceId,
                                String caption, GpodnetDevice.DeviceType type)
            throws GpodnetServiceException {
        if (username == null || deviceId == null) {
            throw new IllegalArgumentException(
                    "Username and device ID must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/devices/%s/%s.json", username, deviceId), null);
            HttpPost request = new HttpPost(uri);
            if (caption != null || type != null) {
                JSONObject jsonContent = new JSONObject();
                if (caption != null) {
                    jsonContent.put("caption", caption);
                }
                if (type != null) {
                    jsonContent.put("type", type.toString());
                }
                StringEntity strEntity = new StringEntity(
                        jsonContent.toString(), "UTF-8");
                strEntity.setContentType("application/json");
                request.setEntity(strEntity);
            }
            executeRequest(request);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
    }

    /**
     * Returns the subscriptions of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param username The username. Must be the same user as the one which is
     *                 currently logged in.
     * @param deviceId The ID of the device whose subscriptions should be returned.
     * @return A list of subscriptions in OPML format.
     * @throws IllegalArgumentException              If username or deviceId is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public String getSubscriptionsOfDevice(String username, String deviceId)
            throws GpodnetServiceException {
        if (username == null || deviceId == null) {
            throw new IllegalArgumentException(
                    "Username and device ID must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/subscriptions/%s/%s.opml", username, deviceId), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);
            return response;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns all subscriptions of a specific user.
     * <p/>
     * This method requires authentication.
     *
     * @param username The username. Must be the same user as the one which is
     *                 currently logged in.
     * @return A list of subscriptions in OPML format.
     * @throws IllegalArgumentException              If username is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public String getSubscriptionsOfUser(String username)
            throws GpodnetServiceException {
        if (username == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/subscriptions/%s.opml", username), null);
            HttpGet request = new HttpGet(uri);
            String response = executeRequest(request);
            return response;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Uploads the subscriptions of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param username      The username. Must be the same user as the one which is
     *                      currently logged in.
     * @param deviceId      The ID of the device whose subscriptions should be updated.
     * @param subscriptions A list of feed URLs containing all subscriptions of the
     *                      device.
     * @throws IllegalArgumentException              If username, deviceId or subscriptions is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public void uploadSubscriptions(String username, String deviceId,
                                    List<String> subscriptions) throws GpodnetServiceException {
        if (username == null || deviceId == null || subscriptions == null) {
            throw new IllegalArgumentException(
                    "Username, device ID and subscriptions must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/subscriptions/%s/%s.txt", username, deviceId), null);
            HttpPut request = new HttpPut(uri);
            StringBuilder builder = new StringBuilder();
            for (String s : subscriptions) {
                builder.append(s);
                builder.append("\n");
            }
            StringEntity entity = new StringEntity(builder.toString(), "UTF-8");
            request.setEntity(entity);

            executeRequest(request);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Updates the subscription list of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param username The username. Must be the same user as the one which is
     *                 currently logged in.
     * @param deviceId The ID of the device whose subscriptions should be updated.
     * @param added    Collection of feed URLs of added feeds. This Collection MUST NOT contain any duplicates
     * @param removed  Collection of feed URLs of removed feeds. This Collection MUST NOT contain any duplicates
     * @return a GpodnetUploadChangesResponse. See {@link de.danoeh.antennapod.gpoddernet.model.GpodnetUploadChangesResponse}
     * for details.
     * @throws java.lang.IllegalArgumentException                      if username, deviceId, added or removed is null.
     * @throws de.danoeh.antennapod.gpoddernet.GpodnetServiceException if added or removed contain duplicates or if there
     *                                                                 is an authentication error.
     */
    public GpodnetUploadChangesResponse uploadChanges(String username, String deviceId, Collection<String> added,
                                                      Collection<String> removed) throws GpodnetServiceException {
        if (username == null || deviceId == null || added == null || removed == null) {
            throw new IllegalArgumentException(
                    "Username, device ID, added and removed must not be null");
        }
        try {
            URI uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/subscriptions/%s/%s.json", username, deviceId), null);

            final JSONObject requestObject = new JSONObject();
            requestObject.put("add", new JSONArray(added));
            requestObject.put("remove", new JSONArray(removed));

            HttpPost request = new HttpPost(uri);
            StringEntity entity = new StringEntity(requestObject.toString(), "UTF-8");
            request.setEntity(entity);

            final String response = executeRequest(request);
            return GpodnetUploadChangesResponse.fromJSONObject(response);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }

    /**
     * Returns all subscription changes of a specific device.
     * <p/>
     * This method requires authentication.
     *
     * @param username  The username. Must be the same user as the one which is
     *                  currently logged in.
     * @param deviceId  The ID of the device whose subscription changes should be
     *                  downloaded.
     * @param timestamp A timestamp that can be used to receive all changes since a
     *                  specific point in time.
     * @throws IllegalArgumentException              If username or deviceId is null.
     * @throws GpodnetServiceAuthenticationException If there is an authentication error.
     */
    public GpodnetSubscriptionChange getSubscriptionChanges(String username,
                                                            String deviceId, long timestamp) throws GpodnetServiceException {
        if (username == null || deviceId == null) {
            throw new IllegalArgumentException(
                    "Username and device ID must not be null");
        }
        String params = String.format("since=%d", timestamp);
        String path = String.format("/api/2/subscriptions/%s/%s.json",
                username, deviceId);
        try {
            URI uri = new URI(BASE_SCHEME, null, BASE_HOST, -1, path, params,
                    null);
            HttpGet request = new HttpGet(uri);

            String response = executeRequest(request);
            JSONObject changes = new JSONObject(response);
            return readSubscriptionChangesFromJSONObject(changes);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }

    }

    /**
     * Logs in a specific user. This method must be called if any of the methods
     * that require authentication is used.
     *
     * @throws IllegalArgumentException If username or password is null.
     */
    public void authenticate(String username, String password)
            throws GpodnetServiceException {
        if (username == null || password == null) {
            throw new IllegalArgumentException(
                    "Username and password must not be null");
        }
        URI uri;
        try {
            uri = new URI(BASE_SCHEME, BASE_HOST, String.format(
                    "/api/2/auth/%s/login.json", username), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new GpodnetServiceException();
        }
        HttpPost request = new HttpPost(uri);
        executeRequestWithAuthentication(request, username, password);
    }

    /**
     * Shuts down the GpodnetService's HTTP client. The service will be shut down in a separate thread to avoid
     * NetworkOnMainThreadExceptions.
     */
    public void shutdown() {
        new Thread() {
            @Override
            public void run() {
                httpClient.getConnectionManager().shutdown();
            }
        }.start();
    }

    private String executeRequest(HttpRequestBase request)
            throws GpodnetServiceException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String responseString = null;
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
            checkStatusCode(response);
            responseString = getStringFromEntity(response.getEntity());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } finally {
            if (response != null) {
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new GpodnetServiceException(e);
                }
            }

        }
        return responseString;
    }

    private String executeRequestWithAuthentication(HttpRequestBase request,
                                                    String username, String password) throws GpodnetServiceException {
        if (request == null || username == null || password == null) {
            throw new IllegalArgumentException(
                    "request and credentials must not be null");
        }
        String result = null;
        HttpResponse response = null;
        try {
            Header auth = new BasicScheme().authenticate(
                    new UsernamePasswordCredentials(username, password),
                    request);
            request.addHeader(auth);
            response = httpClient.execute(request);
            checkStatusCode(response);
            result = getStringFromEntity(response.getEntity());
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        } finally {
            if (response != null) {
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new GpodnetServiceException(e);
                }
            }
        }
        return result;
    }

    private String getStringFromEntity(HttpEntity entity)
            throws GpodnetServiceException {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        ByteArrayOutputStream outputStream;
        int contentLength = (int) entity.getContentLength();
        if (contentLength > 0) {
            outputStream = new ByteArrayOutputStream(contentLength);
        } else {
            outputStream = new ByteArrayOutputStream();
        }
        try {
            byte[] buffer = new byte[8 * 1024];
            InputStream in = entity.getContent();
            int count;
            while ((count = in.read(buffer)) > 0) {
                outputStream.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new GpodnetServiceException(e);
        }
        // System.out.println(outputStream.toString());
        return outputStream.toString();
    }

    private void checkStatusCode(HttpResponse response)
            throws GpodnetServiceException {
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != HttpStatus.SC_OK) {
            if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new GpodnetServiceAuthenticationException("Wrong username or password");
            } else {
                throw new GpodnetServiceBadStatusCodeException(
                        "Bad response code: " + responseCode, responseCode);
            }
        }
    }

    private List<GpodnetPodcast> readPodcastListFromJSONArray(JSONArray array)
            throws JSONException {
        if (array == null) {
            throw new IllegalArgumentException("array must not be null");
        }
        List<GpodnetPodcast> result = new ArrayList<GpodnetPodcast>(
                array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(readPodcastFromJSONObject(array.getJSONObject(i)));
        }
        return result;

    }

    private GpodnetPodcast readPodcastFromJSONObject(JSONObject object)
            throws JSONException {
        String url = object.getString("url");

        String title;
        Object titleObj = object.opt("title");
        if (titleObj != null && titleObj instanceof String) {
            title = (String) titleObj;
        } else {
            title = url;
        }

        String description;
        Object descriptionObj = object.opt("description");
        if (descriptionObj != null && descriptionObj instanceof String) {
            description = (String) descriptionObj;
        } else {
            description = "";
        }

        int subscribers = object.getInt("subscribers");

        Object logoUrlObj = object.opt("logo_url");
        String logoUrl = (logoUrlObj instanceof String) ? (String) logoUrlObj
                : null;
        if (logoUrl == null) {
            Object scaledLogoUrl = object.opt("scaled_logo_url");
            if (scaledLogoUrl != null && scaledLogoUrl instanceof String) {
                logoUrl = (String) scaledLogoUrl;
            }
        }

        String website = null;
        Object websiteObj = object.opt("website");
        if (websiteObj != null && websiteObj instanceof String) {
            website = (String) websiteObj;
        }
        String mygpoLink = object.getString("mygpo_link");
        return new GpodnetPodcast(url, title, description, subscribers,
                logoUrl, website, mygpoLink);
    }

    private List<GpodnetDevice> readDeviceListFromJSONArray(JSONArray array)
            throws JSONException {
        if (array == null) {
            throw new IllegalArgumentException("array must not be null");
        }
        List<GpodnetDevice> result = new ArrayList<GpodnetDevice>(
                array.length());
        for (int i = 0; i < array.length(); i++) {
            result.add(readDeviceFromJSONObject(array.getJSONObject(i)));
        }
        return result;
    }

    private GpodnetDevice readDeviceFromJSONObject(JSONObject object)
            throws JSONException {
        String id = object.getString("id");
        String caption = object.getString("caption");
        String type = object.getString("type");
        int subscriptions = object.getInt("subscriptions");
        return new GpodnetDevice(id, caption, type, subscriptions);
    }

    private GpodnetSubscriptionChange readSubscriptionChangesFromJSONObject(
            JSONObject object) throws JSONException {
        if (object == null) {
            throw new IllegalArgumentException("object must not be null");
        }
        List<String> added = new LinkedList<String>();
        JSONArray jsonAdded = object.getJSONArray("add");
        for (int i = 0; i < jsonAdded.length(); i++) {
            added.add(jsonAdded.getString(i));
        }

        List<String> removed = new LinkedList<String>();
        JSONArray jsonRemoved = object.getJSONArray("remove");
        for (int i = 0; i < jsonRemoved.length(); i++) {
            removed.add(jsonRemoved.getString(i));
        }

        long timestamp = object.getLong("timestamp");
        return new GpodnetSubscriptionChange(added, removed, timestamp);
    }
}
