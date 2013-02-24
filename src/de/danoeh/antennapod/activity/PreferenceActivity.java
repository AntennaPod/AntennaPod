package de.danoeh.antennapod.activity;

import java.io.File;

import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.asynctask.OpmlExportWorker;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.flattr.FlattrUtils;

/** The main preference activity */
public class PreferenceActivity extends SherlockPreferenceActivity {
	private static final String TAG = "PreferenceActivity";

	private static final String PREF_FLATTR_THIS_APP = "prefFlattrThisApp";
	private static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
	private static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";
	private static final String PREF_OPML_EXPORT = "prefOpmlExport";
	private static final String PREF_ABOUT = "prefAbout";
	private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);
		findPreference(PREF_FLATTR_THIS_APP).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						new FlattrClickWorker(PreferenceActivity.this,
								FlattrUtils.APP_URL).executeAsync();

						return true;
					}
				});

		findPreference(PREF_FLATTR_REVOKE).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						FlattrUtils.revokeAccessToken(PreferenceActivity.this);
						checkItemVisibility();
						return true;
					}

				});

		findPreference(PREF_ABOUT).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						PreferenceActivity.this.startActivity(new Intent(
								PreferenceActivity.this, AboutActivity.class));
						return true;
					}

				});

		findPreference(PREF_OPML_EXPORT).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						if (!FeedManager.getInstance().getFeeds().isEmpty()) {
							new OpmlExportWorker(PreferenceActivity.this)
									.executeAsync();
						}
						return true;
					}
				});

		findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						startActivityForResult(
								new Intent(PreferenceActivity.this,
										DirectoryChooserActivity.class),
								DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
						return true;
					}
				});
		findPreference(UserPreferences.PREF_THEME).setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Intent i = getIntent();
						i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
								| Intent.FLAG_ACTIVITY_NEW_TASK);
						finish();
						startActivity(i);
						return true;
					}
				});

	}

	@Override
	protected void onResume() {
		super.onResume();
		checkItemVisibility();
		setDataFolderText();
	}

	@SuppressWarnings("deprecation")
	private void checkItemVisibility() {

		boolean hasFlattrToken = FlattrUtils.hasToken();

		findPreference(PREF_FLATTR_AUTH).setEnabled(!hasFlattrToken);
		findPreference(PREF_FLATTR_REVOKE).setEnabled(hasFlattrToken);

	}

	private void setDataFolderText() {
		File f = UserPreferences.getDataFolder(this, null);
		if (f != null) {
			findPreference(PREF_CHOOSE_DATA_DIR)
					.setSummary(f.getAbsolutePath());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
		theme.applyStyle(UserPreferences.getTheme(), true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
			String dir = data
					.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Setting data folder");
			UserPreferences.setDataFolder(dir);
		}
	}

}
