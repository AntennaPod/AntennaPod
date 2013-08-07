package de.danoeh.antennapod.activity;

import android.os.Bundle;

import android.support.v7.app.ActionBarActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DownloadLogAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.preferences.UserPreferences;

/**
 * Displays completed and failed downloads in a list. The data comes from the
 * FeedManager.
 */
public class DownloadLogActivity extends ActionBarActivity {
	private static final String TAG = "DownloadLogActivity";

	DownloadLogAdapter dla;
	FeedManager manager;

    private ListView listview;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
        setContentView(R.layout.listview_activity);
		manager = FeedManager.getInstance();

        listview = (ListView) findViewById(R.layout.listview_activity);

		dla = new DownloadLogAdapter(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		listview.setAdapter(dla);
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
