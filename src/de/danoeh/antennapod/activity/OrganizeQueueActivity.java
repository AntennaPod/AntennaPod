package de.danoeh.antennapod.activity;

import android.content.res.TypedArray;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

public class OrganizeQueueActivity extends SherlockListActivity {
	private static final String TAG = "OrganizeQueueActivity";

	private static final int MENU_ID_ACCEPT = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		TypedArray drawables = obtainStyledAttributes(new int[] { R.attr.navigation_accept });
		menu.add(Menu.NONE, MENU_ID_ACCEPT, Menu.NONE, R.string.confirm_label)
				.setIcon(drawables.getDrawable(0))
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_IF_ROOM
								| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case MENU_ID_ACCEPT:
			finish();
			return true;
		default:
			return false;
		}
	}

}
