package de.podfetcher.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;

import de.podfetcher.R;

public class StorageErrorActivity extends SherlockActivity {
	private static final String TAG = "StorageErrorActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.storage_error);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mediaUpdate);
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mediaUpdate, new IntentFilter(Intent.ACTION_MEDIA_MOUNTED));
	}
	
	private BroadcastReceiver mediaUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				if (intent.getBooleanExtra("read-only", true)) {
					Log.d(TAG, "Media was mounted; Finishing activity");
					finish();
				} else {
					Log.d(TAG, "Media seemed to have been mounted read only");
				}
			}
		}
		
	};
	
}
