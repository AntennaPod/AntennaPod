package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;

public class PlaybackHistoryActivity extends SherlockFragmentActivity {
	private static final String TAG = "PlaybackHistoryActivity";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.clear_history_item, Menu.NONE,
				R.string.clear_history_label).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		case R.id.clear_history_item:
			FeedManager.getInstance().clearPlaybackHistory(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle arg0) {
		setTheme(PodcastApp.getThemeResourceId());
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
