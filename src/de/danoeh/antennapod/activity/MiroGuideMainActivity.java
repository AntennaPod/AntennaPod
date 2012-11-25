package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.miroguide.conn.MiroGuideException;
import de.danoeh.antennapod.miroguide.conn.MiroGuideService;

/**
 * Shows a list of available categories and offers a search button. If the user
 * selects a category, the MiroGuideCategoryActivity is started.
 */
public class MiroGuideMainActivity extends SherlockListActivity {
	private static final String TAG = "MiroGuideMainActivity";

	private static String[] categories;
	private ArrayAdapter<String> listAdapter;

	private TextView txtvStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(PodcastApp.getThemeResourceId());
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.miroguide_categorylist);

		txtvStatus = (TextView) findViewById(android.R.id.empty);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (categories != null) {
			createAdapter();
		} else {
			loadCategories();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String selection = listAdapter.getItem(position);
		Intent launchIntent = new Intent(this, MiroGuideCategoryActivity.class);
		launchIntent.putExtra(MiroGuideCategoryActivity.EXTRA_CATEGORY,
				selection);
		startActivity(launchIntent);
	}

	private void createAdapter() {
		if (categories != null) {
			listAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, categories);
			txtvStatus.setText(R.string.no_items_label);
			setListAdapter(listAdapter);
		}
	}

	/**
	 * Launches an AsyncTask to load the available categories in the background.
	 */
	@SuppressLint("NewApi")
	private void loadCategories() {
		AsyncTask<Void, Void, Void> listLoader = new AsyncTask<Void, Void, Void>() {

			private String[] c;
			private MiroGuideException exception;

			@Override
			protected void onPostExecute(Void result) {
				if (exception == null) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Successfully loaded categories");
					categories = c;
					createAdapter();
				} else {
					Log.e(TAG, "Error happened while trying to load categories");
					txtvStatus.setText(exception.getMessage());
				}
			}

			@Override
			protected void onPreExecute() {
				txtvStatus.setText(R.string.loading_categories_label);
			}

			@Override
			protected Void doInBackground(Void... params) {
				MiroGuideService service = new MiroGuideService();
				try {
					c = service.getCategories();
				} catch (MiroGuideException e) {
					e.printStackTrace();
					exception = e;
				} finally {
					service.close();
				}
				return null;
			}

		};

		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			listLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			listLoader.execute();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
				.setIcon(
						obtainStyledAttributes(
								new int[] { R.attr.action_search })
								.getDrawable(0))
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
