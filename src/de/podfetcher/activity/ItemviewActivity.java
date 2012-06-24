package de.podfetcher.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.asynctask.DownloadObserver;
import de.podfetcher.asynctask.DownloadStatus;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.fragment.FeedItemlistFragment;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.service.PlaybackService;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.util.FeedItemMenuHandler;

/** Displays a single FeedItem and provides various actions */
public class ItemviewActivity extends SherlockActivity {
	private static final String TAG = "ItemviewActivity";

	private FeedManager manager;
	private FeedItem item;

	// Widgets
	private TextView txtvTitle;
	private TextView txtvPublished;
	private WebView webvDescription;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();
		extractFeeditem();
		populateUI();

	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "Stopping Activity");
	}

	/** Extracts FeedItem object the activity is supposed to display */
	private void extractFeeditem() {
		long itemId = getIntent().getLongExtra(
				FeedItemlistFragment.EXTRA_SELECTED_FEEDITEM, -1);
		long feedId = getIntent().getLongExtra(
				FeedlistFragment.EXTRA_SELECTED_FEED, -1);
		if (itemId == -1 || feedId == -1) {
			Log.e(TAG, "Received invalid selection of either feeditem or feed.");
		}
		Feed feed = manager.getFeed(feedId);
		item = manager.getFeedItem(itemId, feed);
		Log.d(TAG, "Title of item is " + item.getTitle());
		Log.d(TAG, "Title of feed is " + item.getFeed().getTitle());
	}

	private void populateUI() {
		setContentView(R.layout.feeditemview);
		txtvTitle = (TextView) findViewById(R.id.txtvItemname);
		txtvPublished = (TextView) findViewById(R.id.txtvPublished);
		webvDescription = (WebView) findViewById(R.id.webvDescription);
		setTitle(item.getFeed().getTitle());

		txtvPublished.setText(DateUtils.formatSameDayTime(item.getPubDate()
				.getTime(), System.currentTimeMillis(), DateFormat.MEDIUM,
				DateFormat.SHORT));
		txtvTitle.setText(item.getTitle());
		String url = "";
		try {
			if (item.getContentEncoded() == null) {
				url = URLEncoder.encode(item.getDescription(), "utf-8")
						.replaceAll("\\+", " ");
			} else {
				url = URLEncoder.encode(item.getContentEncoded(), "utf-8")
						.replaceAll("\\+", " ");
			}
			
		} catch (UnsupportedEncodingException e) {
			url = "Page could not be loaded";
			e.printStackTrace();
		}

		webvDescription.loadData(url, "text/html", "utf-8");

	}

	/*
	 * TODO implement final DownloadObserver downloadObserver = new
	 * DownloadObserver(this) {
	 * 
	 * @Override protected void onProgressUpdate( DownloadStatus... values) {
	 * 
	 * }
	 * 
	 * @Override protected void onPostExecute(Boolean result) { boolean r =
	 * getStatusList()[0].isSuccessful(); if (r) { //setDownloadedState(); }
	 * else { //setNotDownloadedState(); } } };
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return FeedItemMenuHandler.onCreateMenu(new MenuInflater(this), menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		FeedItemMenuHandler.onMenuItemClicked(this, menuItem, item);
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return FeedItemMenuHandler.onPrepareMenu(menu, item);
	}
}
