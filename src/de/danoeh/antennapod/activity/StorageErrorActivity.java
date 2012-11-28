package de.danoeh.antennapod.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.StorageUtils;

/** Is show if there is now external storage available. */
public class StorageErrorActivity extends SherlockActivity {
	private static final String TAG = "StorageErrorActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
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
		if (StorageUtils.storageAvailable()) {
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
			if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				if (intent.getBooleanExtra("read-only", true)) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Media was mounted; Finishing activity");
					leaveErrorState();
				} else {
					if (AppConfig.DEBUG)
						Log.d(TAG,
								"Media seemed to have been mounted read only");
				}
			}
		}

	};

}
