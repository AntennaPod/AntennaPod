package de.danoeh.antennapod.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

/** Let's the user choose a directory on the storage device. */
public abstract class DirectoryChooserActivity extends SherlockActivity {
	private static final String TAG = "DirectoryChooserActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
}
