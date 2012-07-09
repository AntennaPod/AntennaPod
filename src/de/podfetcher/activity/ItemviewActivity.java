package de.podfetcher.activity;

import java.text.DateFormat;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import de.podfetcher.R;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedItem;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.fragment.FeedlistFragment;
import de.podfetcher.fragment.ItemDescriptionFragment;
import de.podfetcher.fragment.ItemlistFragment;
import de.podfetcher.util.FeedItemMenuHandler;
import de.podfetcher.util.StorageUtils;

/** Displays a single FeedItem and provides various actions */
public class ItemviewActivity extends SherlockFragmentActivity {
	private static final String TAG = "ItemviewActivity";

	private FeedManager manager;
	private FeedItem item;

	// Widgets
	private TextView txtvTitle;
	private TextView txtvPublished;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StorageUtils.checkStorageAvailability(this);
		manager = FeedManager.getInstance();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		extractFeeditem();
		populateUI();
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);

	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "Stopping Activity");
	}

	/** Extracts FeedItem object the activity is supposed to display */
	private void extractFeeditem() {
		long itemId = getIntent().getLongExtra(
				ItemlistFragment.EXTRA_SELECTED_FEEDITEM, -1);
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
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.feeditemview);
		txtvTitle = (TextView) findViewById(R.id.txtvItemname);
		txtvPublished = (TextView) findViewById(R.id.txtvPublished);
		setTitle(item.getFeed().getTitle());

		txtvPublished.setText(DateUtils.formatSameDayTime(item.getPubDate()
				.getTime(), System.currentTimeMillis(), DateFormat.MEDIUM,
				DateFormat.SHORT));
		txtvTitle.setText(item.getTitle());

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		ItemDescriptionFragment fragment = ItemDescriptionFragment
				.newInstance(item);
		fragmentTransaction.add(R.id.description_fragment, fragment);
		fragmentTransaction.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return FeedItemMenuHandler.onCreateMenu(new MenuInflater(this), menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		if (!FeedItemMenuHandler.onMenuItemClicked(this, menuItem, item)) {
			switch (menuItem.getItemId()) {
			case android.R.id.home:
				finish();
				break;
			}
		}
		invalidateOptionsMenu();
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return FeedItemMenuHandler.onPrepareMenu(menu, item);
	}

}
