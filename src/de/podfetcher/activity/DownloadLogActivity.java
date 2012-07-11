package de.podfetcher.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.adapter.DownloadLogAdapter;
import de.podfetcher.feed.FeedManager;

public class DownloadLogActivity extends SherlockListActivity {
	private static final String TAG = "DownloadLogActivity";

	DownloadLogAdapter dla;
	FeedManager manager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manager = FeedManager.getInstance();

		dla = new DownloadLogAdapter(this, 0, manager.getDownloadLog());
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setListAdapter(dla);
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

}
