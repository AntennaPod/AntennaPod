package de.danoeh.antennapod.miroguide.conn;

import de.danoeh.antennapod.miroguide.model.MiroGuideChannel;
import de.danoeh.antennapod.miroguide.model.MiroGuideItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/** Provides methods to communicate with the Miroguide API on an abstract level. */
public class MiroGuideService {
	private static final String TAG = "MiroGuideService";
	
	public static final int DEFAULT_CHANNEL_LIMIT = 20;

	public static final String FILTER_CATEGORY = "category";
	public static final String FILTER_NAME = "name";
	public static final String SORT_NAME = "name";
	public static final String SORT_POPULAR = "popular";
	public static final String SORT_RATING = "rating";

	public static final String JSON_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

	private MiroGuideConnector connector;

	private static ThreadLocal<SimpleDateFormat> jSONDateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(JSON_DATE_FORMAT_STRING, Locale.US);
		}

	};

	public MiroGuideService() {
		connector = new MiroGuideConnector();
	}
	
	public void close() {
		connector.shutdown();
	}

	public String[] getCategories() throws MiroGuideException {
		JSONArray resultArray = connector.getArrayResponse(connector
				.createListCategoriesURI());
		String[] result = new String[resultArray.length()];
		for (int i = 0; i < resultArray.length(); i++) {
			try {
				result[i] = resultArray.getJSONObject(i).getString("name");
			} catch (JSONException e) {
				e.printStackTrace();
				throw new MiroGuideException();
			}
		}
		return result;
	}

	/** Get a list of MiroGuideChannel objects without their items. */
	public List<MiroGuideChannel> getChannelList(String filter, String filterValue,
			String sort, int limit, int offset) throws MiroGuideException {
		JSONArray resultArray = connector.getArrayResponse(connector
				.createGetChannelsUri(filter, filterValue, sort,
						Integer.toString(limit), Integer.toString(offset)));
		int resultLen = resultArray.length();
		List<MiroGuideChannel> channels = new ArrayList<MiroGuideChannel>(resultLen);
		for (int i = 0; i < resultLen; i++) {
			JSONObject content = null;
			try {
				content = resultArray.getJSONObject(i);
				MiroGuideChannel channel = extractMiroChannel(content, false);
				channels.add(channel);
			} catch (JSONException e) {
				e.printStackTrace();
				throw new MiroGuideException();
			}
		}

		return channels;
	}

	/**
	 * Get a single channel with its items.
	 * 
	 * @throws MiroGuideException
	 */
	public MiroGuideChannel getChannel(long id) throws MiroGuideException {
		JSONObject resultObject = connector.getSingleObjectResponse(connector
				.createGetChannelUri(Long.toString(id)));
		MiroGuideChannel result = null;
		try {
			result = extractMiroChannel(resultObject, true);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new MiroGuideException();
		}
		return result;
	}

	/**
	 * Get a MiroGuideChannel object from it's JSON source. The itemlist of the
	 * channel can be included or excluded
	 * 
	 * @throws JSONException
	 */
	private MiroGuideChannel extractMiroChannel(JSONObject content, boolean withItems)
			throws JSONException {
		long id = content.getLong("id");
		String name = content.getString("name");
		String description = content.getString("description");
		String thumbnailUrl = content.optString("thumbnail_url");
		String downloadUrl = content.getString("url");
		String websiteUrl = content.getString("website_url");
		if (!withItems) {
			return new MiroGuideChannel(id, name, thumbnailUrl, downloadUrl,
					websiteUrl, description);
		} else {
			JSONArray itemData = content.getJSONArray("item");
			int numItems = itemData.length();
			ArrayList<MiroGuideItem> items = new ArrayList<MiroGuideItem>(numItems);
			for (int i = 0; i < numItems; i++) {
				items.add(extractMiroItem(itemData.getJSONObject(i)));
			}

			return new MiroGuideChannel(id, name, thumbnailUrl, downloadUrl,
					websiteUrl, description, items);
		}
	}

	/** Get a MiroGuideItem from its JSON source. */
	private MiroGuideItem extractMiroItem(JSONObject content) throws JSONException {
		Date date = parseMiroItemDate(content.getString("date"));
		String description = content.getString("description");
		String name = content.getString("name");
		String url = content.getString("url");
		return new MiroGuideItem(name, description, date, url);
	}

	private Date parseMiroItemDate(String s) {
		try {
			return jSONDateFormat.get().parse(s);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

}
