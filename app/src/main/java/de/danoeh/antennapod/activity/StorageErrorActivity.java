package de.danoeh.antennapod.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.StorageUtils;

/** Is show if there is now external storage available. */
public class StorageErrorActivity extends ActionBarActivity {
	private static final String TAG = "StorageErrorActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		setContentView(R.layout.storage_error);
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(mediaUpdate);
		} catch (IllegalArgumentException e) {

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (StorageUtils.storageAvailable(this)) {
			leaveErrorState();
		} else {
			registerReceiver(mediaUpdate, new IntentFilter(
					Intent.ACTION_MEDIA_MOUNTED));
		}
	}

	private void leaveErrorState() {
		finish();
		startActivity(new Intent(this, MainActivity.class));
	}

	private BroadcastReceiver mediaUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (StringUtils.equals(intent.getAction(), Intent.ACTION_MEDIA_MOUNTED)) {
				if (intent.getBooleanExtra("read-only", true)) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, "Media was mounted; Finishing activity");
					leaveErrorState();
				} else {
					if (BuildConfig.DEBUG)
						Log.d(TAG,
								"Media seemed to have been mounted read only");
				}
			}
		}

	};

}
