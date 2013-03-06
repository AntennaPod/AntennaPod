package de.danoeh.antennapod.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.preferences.UserPreferences;

/**
 * Displays completed and failed downloads in a list. The data comes from the
 * FeedManager.
 */
public class DownloadLogActivity extends SherlockListActivity {
	private static final String TAG = "DownloadLogActivity";

	DownloadLogAdapter dla;
	FeedManager manager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();

		dla = new DownloadLogAdapter(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setListAdapter(dla);
	}

	@Override
	protected void onPause() {
		super.onPause();
		EventDistributor.getInstance().unregister(contentUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		EventDistributor.getInstance().register(contentUpdate);
		dla.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			return false;
		}
		return true;
	}

	private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
		
		@Override
		public void update(EventDistributor eventDistributor, Integer arg) {
			if ((arg & EventDistributor.DOWNLOADLOG_UPDATE) != 0) {
				dla.notifyDataSetChanged();
			}
		}
	};

}
