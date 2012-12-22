package de.danoeh.antennapod.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import android.test.AndroidTestCase;
import android.util.Log;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.syndication.handler.FeedHandler;

/** Enqueues a list of Feeds and tests if they are parsed correctly */
public class FeedHandlerTest extends AndroidTestCase {
	private static final String TAG = "FeedHandlerTest";
	private static final String FEEDS_DIR = "testfeeds";

	private ArrayList<Feed> feeds;

	protected void setUp() throws Exception {
		super.setUp();
		feeds = new ArrayList<Feed>();
		for (int i = 0; i < TestFeeds.urls.length; i++) {
			Feed f = new Feed(TestFeeds.urls[i], new Date());
			f.setFile_url(new File(getContext().getExternalFilesDir(FEEDS_DIR)
					.getAbsolutePath(), "R" + i).getAbsolutePath());
			feeds.add(f);
		}
	}

	private InputStream getInputStream(String url)
			throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url))
				.openConnection();
		int rc = connection.getResponseCode();
		if (rc == HttpURLConnection.HTTP_OK) {
			return connection.getInputStream();
		} else {
			return null;
		}
	}

	private void downloadFeed(Feed feed) throws IOException {
		int num_retries = 20;
		boolean successful = false;

		for (int i = 0; i < num_retries; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getInputStream(feed.getDownload_url());
				assertNotNull(in);
				out = new BufferedOutputStream(new FileOutputStream(
						feed.getFile_url()));
				byte[] buffer = new byte[8 * 1024];
				int count = 0;
				while ((count = in.read(buffer)) != -1) {
					out.write(buffer, 0, count);
				}
				successful = true;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (successful) {
					break;
				}
			}
		}
		if (!successful) {
			Log.e(TAG, "Download failed after " + num_retries + " retries");
			throw new IOException();
		}
	}

	private boolean isFeedValid(Feed feed) {
		Log.i(TAG, "Checking if " + feed.getDownload_url() + " is valid");
		boolean result = false;
		if (feed.getTitle() == null) {
			Log.e(TAG, "Feed has no title");
			return false;
		}
		if (feed.getItems() == null) {
			Log.e(TAG, "Feed has no items");
			return false;
		}
		if (!hasValidFeedItems(feed)) {
			Log.e(TAG, "Feed has invalid items");
			return false;
		}
		if (feed.getLink() == null) {
			Log.e(TAG, "Feed has no link");
			return false;
		}
		if (feed.getLink() != null && feed.getLink().length() == 0) {
			Log.e(TAG, "Feed has empty link");
			return false;
		}
		if (feed.getIdentifyingValue() == null) {
			Log.e(TAG, "Feed has no identifying value");
			return false;
		}
		if (feed.getIdentifyingValue() != null
				&& feed.getIdentifyingValue().length() == 0) {
			Log.e(TAG, "Feed has empty identifying value");
			return false;
		}
		return true;
	}

	private boolean hasValidFeedItems(Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getTitle() == null) {
				Log.e(TAG, "Item has no title");
				return false;
			}
		}
		return true;
	}

	public void testParseFeeds() {
		Log.i(TAG, "Testing RSS feeds");
		while (!feeds.isEmpty()) {
			Feed feed = feeds.get(0);
			parseFeed(feed);
			feeds.remove(0);
		}

		Log.i(TAG, "RSS Test completed");
	}

	private void parseFeed(Feed feed) {
		try {
			Log.i(TAG, "Testing feed with url " + feed.getDownload_url());
			FeedHandler handler = new FeedHandler();
			downloadFeed(feed);
			handler.parseFeed(feed);
			assertTrue(isFeedValid(feed));
		} catch (Exception e) {
			Log.e(TAG, "Error when trying to test " + feed.getDownload_url());
			e.printStackTrace();
			fail();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Feed feed : feeds) {
			File f = new File(feed.getFile_url());
			f.delete();
		}
	}

}
