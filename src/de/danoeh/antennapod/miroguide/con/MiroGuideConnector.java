package de.danoeh.antennapod.miroguide.con;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.http.AndroidHttpClient;

/** Executes HTTP requests and returns the results. */
public class MiroGuideConnector {
	private HttpClient httpClient;

	private static final String HOST_URL = "https://www.miroguide.com/api/";
	private static final String PATH_GET_CHANNELS = "get_channels";
	private static final String PATH_LIST_CATEGORIES = "list_categories";
	private static final String PATH_GET_CHANNEL = "get_channel";

	public MiroGuideConnector() {
		httpClient = new DefaultHttpClient();
	}

	public void shutdown() {
		httpClient.getConnectionManager().shutdown();
	}

	private Uri.Builder getBaseURIBuilder(String path) {
		Uri.Builder builder = Uri.parse(HOST_URL).buildUpon();
		builder.appendPath(path).appendQueryParameter("datatype", "json");
		return builder;
	}

	public JSONArray getArrayResponse(Uri uri) throws MiroGuideException {
		try {
			JSONArray result = new JSONArray(executeRequest(uri));
			return result;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new MiroGuideException();
		}
	}

	public JSONObject getSingleObjectResponse(Uri uri) throws MiroGuideException {
		try {
			JSONObject result = new JSONObject(executeRequest(uri));
			return result;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new MiroGuideException();
		}
	}

	/**
	 * Executes a HTTP GET request with the given URI and returns the content of
	 * the return value.
	 * 
	 * @throws MiroGuideException
	 */
	private String executeRequest(Uri uri) throws MiroGuideException {
		HttpGet httpGet = new HttpGet(uri.toString());
		String result = null;
		try {
			HttpResponse response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream in = entity.getContent();

					BufferedReader reader = new BufferedReader(
							new InputStreamReader(in));
					result = reader.readLine();
					in.close();
				}
			} else {
				throw new MiroGuideException(response.getStatusLine()
						.getReasonPhrase());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new MiroGuideException(e.getMessage());
		}
		return result;

	}

	public Uri createGetChannelsUri(String filter, String filterValue,
			String sort, String limit, String offset) throws MiroGuideException {
		Uri.Builder resultBuilder = getBaseURIBuilder(PATH_GET_CHANNELS);
		resultBuilder.appendQueryParameter("filter", filter)
				.appendQueryParameter("filter_value", filterValue);

		if (sort != null) {
			resultBuilder.appendQueryParameter("sort", sort);
		}
		if (limit != null) {
			resultBuilder.appendQueryParameter("limit", limit);
		}
		if (offset != null) {
			resultBuilder.appendQueryParameter("offset", offset);
		}
		Uri result = resultBuilder.build();
		return result;
	}

	public Uri createListCategoriesURI() throws MiroGuideException {
		Uri.Builder resultBuilder = getBaseURIBuilder(PATH_LIST_CATEGORIES);
		Uri result = resultBuilder.build();

		return result;
	}

	public Uri createGetChannelUri(String id) throws MiroGuideException {
		Uri.Builder resultBuilder = getBaseURIBuilder(PATH_GET_CHANNEL)
				.appendQueryParameter("id", id);
		Uri result = resultBuilder.build();
		return result;
	}

}
