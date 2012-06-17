package de.podfetcher.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.podfetcher.R;

public class PreferenceActivity extends SherlockPreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
