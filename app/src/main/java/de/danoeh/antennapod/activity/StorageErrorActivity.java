package de.danoeh.antennapod.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;

/** Is show if there is now external storage available. */
public class StorageErrorActivity extends AppCompatActivity {

	private static final String TAG = "StorageErrorActivity";

    private static final String[] EXTERNAL_STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 42;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);

		setContentView(R.layout.storage_error);

		Button btnChooseDataFolder = (Button) findViewById(R.id.btnChooseDataFolder);
		btnChooseDataFolder.setOnClickListener(v -> {
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                showChooseDataFolderDialog();
            } else {
                openDirectoryChooser();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int readPermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (readPermission != PackageManager.PERMISSION_GRANTED ||
                    writePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            }
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, EXTERNAL_STORAGE_PERMISSIONS,
                PERMISSION_REQUEST_EXTERNAL_STORAGE);
    }

    private void openDirectoryChooser() {
        Intent intent = new Intent(this, DirectoryChooserActivity.class);
        startActivityForResult(intent, DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_EXTERNAL_STORAGE || grantResults.length != 2) {
            return;
        }
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            new MaterialDialog.Builder(this)
                    .content(R.string.choose_data_directory_permission_rationale)
                    .positiveText(android.R.string.ok)
                    .onPositive((dialog, which) -> requestPermission())
                    .onNegative((dialog, which) -> finish())
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StorageUtils.storageAvailable()) {
            leaveErrorState();
        } else {
            registerReceiver(mediaUpdate, new IntentFilter(Intent.ACTION_MEDIA_MOUNTED));
        }
    }

	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(mediaUpdate);
		} catch (IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
		}
	}

    // see PreferenceController.showChooseDataFolderDialog()
    private void showChooseDataFolderDialog() {
        ChooseDataFolderDialog.showDialog(
                this, new ChooseDataFolderDialog.RunnableWithString() {
                    @Override
                    public void run(final String folder) {
                        UserPreferences.setDataFolder(folder);
                        leaveErrorState();
                    }
                });
    }

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK &&
				requestCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
            String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);

            File path;
            if (dir != null) {
                path = new File(dir);
            } else {
                path = getExternalFilesDir(null);
            }
            if(path == null) {
                return;
            }
            String message = null;
			if(!path.exists()) {
				message = String.format(getString(R.string.folder_does_not_exist_error), dir);
			} else if(!path.canRead()) {
				message = String.format(getString(R.string.folder_not_readable_error), dir);
			} else if(!path.canWrite()) {
				message = String.format(getString(R.string.folder_not_writable_error), dir);
			}

			if(message == null) {
				Log.d(TAG, "Setting data folder: " + dir);
				UserPreferences.setDataFolder(dir);
				leaveErrorState();
			} else {
				AlertDialog.Builder ab = new AlertDialog.Builder(this);
				ab.setMessage(message);
				ab.setPositiveButton(android.R.string.ok, null);
				ab.show();
			}
		}
	}

	private void leaveErrorState() {
		finish();
		startActivity(new Intent(this, MainActivity.class));
	}

	private final BroadcastReceiver mediaUpdate = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (TextUtils.equals(intent.getAction(), Intent.ACTION_MEDIA_MOUNTED)) {
				if (intent.getBooleanExtra("read-only", true)) {
					Log.d(TAG, "Media was mounted; Finishing activity");
					leaveErrorState();
				} else {
					Log.d(TAG, "Media seemed to have been mounted read only");
				}
			}
		}

	};

}
