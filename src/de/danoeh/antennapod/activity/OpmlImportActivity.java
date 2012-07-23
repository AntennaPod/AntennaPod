package de.danoeh.antennapod.activity;

import java.io.File;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.StorageUtils;

public class OpmlImportActivity extends SherlockActivity {
	private static final String TAG = "OpmlImportActivity";
	
	private static final String IMPORT_DIR = "import/";
	
	private TextView txtvPath;
	private Button butStart;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.opml_import);
		
		txtvPath = (TextView) findViewById(R.id.txtvPath);
		butStart = (Button) findViewById(R.id.butStartImport);
	}

	@Override
	protected void onResume() {
		super.onResume();
		StorageUtils.checkStorageAvailability(this);
		setImportPath();
	}
	
	private void setImportPath() {
		File importDir = getExternalFilesDir(IMPORT_DIR);
		boolean success = true;
		if (!importDir.exists()) {
			if (AppConfig.DEBUG) Log.d(TAG, "Import directory doesn't exist. Creating...");
			success = importDir.mkdir();
			if (!success) {
				Log.e(TAG, "Could not create directory");
			}	
		}
		if (success) {
			txtvPath.setText(importDir.toString());
		} else {
			txtvPath.setText(R.string.opml_directory_error);
		}
	}
}
