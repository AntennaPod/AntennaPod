package de.danoeh.antennapod.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.MiroGuideChannellistFragment;

/**
 * Displays results when a search for miroguide channels has been performed. It
 * uses a MiroGuideChannelListFragment to display the results.
 */
public class MiroGuideSearchActivity extends SherlockFragmentActivity {
	private static final String TAG = "MiroGuideSearchActivity";

	private MiroGuideChannellistFragment listFragment;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.miroguidesearch);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			getSupportActionBar()
					.setSubtitle(
							getString(R.string.search_term_label) + "\""
									+ query + "\"");
			handleSearchRequest(query);
		}
	}

	private void handleSearchRequest(String query) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Performing search");
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		listFragment = MiroGuideChannellistFragment.newInstance("name", query,
				"name");
		ft.replace(R.id.channellistFragment, listFragment);
		ft.commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
				.setIcon(R.drawable.action_search)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.search_item:
			onSearchRequested();
			return true;
		default:
			return false;
		}
	}

}
