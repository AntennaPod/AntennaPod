package de.danoeh.antennapod.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import android.test.AndroidTestCase;
import android.util.Log;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.syndication.handler.FeedHandler;


/** Enqueues a list of Feeds and tests if they are parsed correctly*/
public class FeedHandlerTest extends AndroidTestCase {
	private static final String TAG = "FeedHandlerTest";
	
	private static final String[] rssUrls = {
		
		
	};
	
	private static final String[] atomUrls = {
		
		
	};
	
	private ArrayList<Feed> rssFeeds;
	private ArrayList<Feed> atomFeeds;
	
	protected void setUp() throws Exception {
		super.setUp();
		rssFeeds = new ArrayList<Feed>();
		for (String url : rssUrls) {
			Feed f = new Feed(url, new Date());
			rssFeeds.add(f);
		}
		
		atomFeeds = new ArrayList<Feed>();
		for (String url : atomUrls) {
			Feed f = new Feed(url, new Date());
			atomFeeds.add(f);
		}
		
		Log.d(TAG, "Setup completed");		
	}
	
	public InputStream getInputStream(String url) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
		int rc = connection.getResponseCode();
		if (rc == HttpURLConnection.HTTP_OK) {
			return connection.getInputStream();
		} else {
			return null;
		}
	}
	
	public void testParseRSS() {
		Log.i(TAG, "Testing RSS feeds");
		for (Feed feed : rssFeeds) {
			Log.i(TAG, "Testing feed with url " + feed.getDownload_url());
			FeedHandler handler = new FeedHandler();
			
		}
		
		Log.i(TAG, "RSS Test completed");
	}

}
