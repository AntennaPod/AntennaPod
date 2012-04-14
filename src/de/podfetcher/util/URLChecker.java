package	de.podfetcher.util;

import android.webkit.URLUtil;
import android.util.Log;

/** Provides methods for checking and editing a URL */
public class URLChecker {

	private static final String TAG = "URLChecker";	
	private static final String FEEDBURNER_URL = "feeds.feedburner.com";
	private static final String FEEDBURNER_PREFIX = "?format=xml";

	/** Checks if URL is valid and modifies it if necessary */
	public static String prepareURL(String url) {
		StringBuilder builder = new StringBuilder();

		if(!url.startsWith("http")) {
			builder.append("http://");
			Log.d(TAG, "Missing http; appending");
		}
		builder.append(url);

		if(url.contains(FEEDBURNER_URL)) {
			Log.d(TAG, "URL seems to be Feedburner URL; appending prefix");
			builder.append(FEEDBURNER_PREFIX);
		}
		return builder.toString();	
	}
}
