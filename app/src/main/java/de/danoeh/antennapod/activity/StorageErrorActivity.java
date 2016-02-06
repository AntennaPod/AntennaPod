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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.StorageUtils;

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
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
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
        File dataFolder =  UserPreferences.getDataFolder(null);
        if(dataFolder == null) {
            new MaterialDialog.Builder(this)
                    .title(R.string.error_label)
                    .content(R.string.external_storage_error_msg)
                    .neutralText(android.R.string.ok)
                    .show();
            return;
        }
        String dataFolderPath = dataFolder.getAbsolutePath();
        int selectedIndex = -1;
        File[] mediaDirs = ContextCompat.getExternalFilesDirs(this, null);
        List<String> folders = new ArrayList<>(mediaDirs.length);
        List<CharSequence> choices = new ArrayList<>(mediaDirs.length);
        for(int i=0; i < mediaDirs.length; i++) {
            if(mediaDirs[i] == null) {
                continue;
            }
            String path = mediaDirs[i].getAbsolutePath();
            folders.add(path);
            if(dataFolderPath.equals(path)) {
                selectedIndex = i;
            }
            int index = path.indexOf("Android");
            String choice;
            if(index >= 0) {
                choice = path.substring(0, index);
            } else {
                choice = path;
            }
            long bytes = StorageUtils.getFreeSpaceAvailable(path);
            String freeSpace = String.format(getString(R.string.free_space_label),
                    Converter.byteToString(bytes));
            choices.add(Html.fromHtml("<html><small>" + choice + " [" + freeSpace + "]"
                    + "</small></html>"));
        }
        if(choices.size() == 0) {
            new MaterialDialog.Builder(this)
                    .title(R.string.error_label)
                    .content(R.string.external_storage_error_msg)
                    .neutralText(android.R.string.ok)
                    .show();
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.choose_data_directory)
                .content(R.string.choose_data_directory_message)
                .items(choices.toArray(new CharSequence[choices.size()]))
                .itemsCallbackSingleChoice(selectedIndex, (dialog1, itemView, which, text) -> {
                    String folder = folders.get(which);
                    UserPreferences.setDataFolder(folder);
                    leaveErrorState();
                    return true;
                })
                .negativeText(R.string.cancel_label)
                .cancelable(true)
                .build();
        dialog.show();
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

	private BroadcastReceiver mediaUpdate = new BroadcastReceiver() {

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
