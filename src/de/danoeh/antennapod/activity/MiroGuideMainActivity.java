package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.miroguide.con.MiroException;
import de.danoeh.antennapod.miroguide.con.MiroService;

/** Shows a list of available categories and offers a search button. */
public class MiroGuideMainActivity extends SherlockListActivity {
	private static final String TAG = "MiroGuideMainActivity";

	private static String[] categories;
	private ArrayAdapter<String> listAdapter;

	private TextView txtvStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.miro_categorylist);

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
	
	private void createAdapter() {
		if (categories != null) {
			listAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, categories);
			txtvStatus.setText(R.string.no_items_label);
			setListAdapter(listAdapter);
		}
	}

	private void loadCategories() {
		AsyncTask<Void, Void, Void> listLoader = new AsyncTask<Void, Void, Void>() {

			private String[] c;
			private MiroException exception;
			
			@Override
			protected void onPostExecute(Void result) {
				if (exception != null) {
					if (AppConfig.DEBUG) Log.d(TAG, "Successfully loaded categories");
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

			@SuppressLint({ "NewApi", "NewApi" })
			@Override
			protected Void doInBackground(Void... params) {
				MiroService service = new MiroService();
				try {
					c = service.getCategories();
				} catch (MiroException e) {
					e.printStackTrace();
					exception = e;
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

}
