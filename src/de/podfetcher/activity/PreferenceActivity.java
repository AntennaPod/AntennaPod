package de.podfetcher.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.util.FlattrUtils;

public class PreferenceActivity extends SherlockPreferenceActivity {
	private static final String TAG = "PreferenceActivity";

	private static final String PREF_FLATTR_THIS_APP = "prefFlattrThisApp";
	private static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
	private static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);
		findPreference(PREF_FLATTR_THIS_APP).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						Log.d(TAG, "Flattring this app");
						return true;
					}
				});
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkItemVisibility();
	}

	@SuppressWarnings("deprecation")
	private void checkItemVisibility() {
		boolean hasFlattrToken = FlattrUtils.hasToken();
		findPreference(PREF_FLATTR_AUTH).setEnabled(!hasFlattrToken);
		findPreference(PREF_FLATTR_REVOKE).setEnabled(hasFlattrToken);

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
