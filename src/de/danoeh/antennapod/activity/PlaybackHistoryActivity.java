package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBWriter;

public class PlaybackHistoryActivity extends ActionBarActivity {
	private static final String TAG = "PlaybackHistoryActivity";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
		MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, R.id.clear_history_item, Menu.NONE,
                R.string.clear_history_label),
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.clear_history_item:
			DBWriter.clearPlaybackHistory(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle arg0) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(arg0);

		if (AppConfig.DEBUG)
			Log.d(TAG, "Activity created");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.playbackhistory_activity);

		FragmentTransaction fT = getSupportFragmentManager().beginTransaction();
		fT.replace(R.id.playbackhistory_fragment, new PlaybackHistoryFragment());
		fT.commit();
	}

}
