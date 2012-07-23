package de.danoeh.antennapod.activity;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.StorageUtils;

public class OpmlImportActivity extends SherlockActivity {
	private static final String TAG = "OpmlImportActivity";

	private static final String IMPORT_DIR = "import/";

	private TextView txtvPath;
	private Button butStart;
	private String importPath;

	private OpmlImportWorker importWorker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.opml_import);

		txtvPath = (TextView) findViewById(R.id.txtvPath);
		butStart = (Button) findViewById(R.id.butStartImport);

		butStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startImport();
			}

		});
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
			if (AppConfig.DEBUG)
				Log.d(TAG, "Import directory doesn't exist. Creating...");
			success = importDir.mkdir();
			if (!success) {
				Log.e(TAG, "Could not create directory");
			}
		}
		if (success) {
			txtvPath.setText(importDir.toString());
			importPath = importDir.toString();
		} else {
			txtvPath.setText(R.string.opml_directory_error);
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
			finish();
			return true;
		default:
			return false;
		}
	}

	private void startImport() {
		File dir = new File(importPath);
		if (dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			if (fileList.length > 1) {
				Log.w(TAG,
						"Import directory contains more than one file. Might choose the wrong one");
			}
			if (fileList.length > 0) {
				importWorker = new OpmlImportWorker(this, fileList[0]) {

					@Override
					protected void onPostExecute(ArrayList<OpmlElement> result) {
						super.onPostExecute(result);

					}
				};
			} else {
				Log.e(TAG, "Import directory is empty");
			}
		}
	}
}
