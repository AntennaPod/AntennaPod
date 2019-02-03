package de.danoeh.antennapod.activity;

import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.StorageUtils;

/**
 * Lets the user start the file import process from a path
 */
public class AddLocalFilesActivity extends AppCompatActivity implements DialogSelectionListener {

    private static final String TAG = "AddLocalFilesAct";
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.singlefile_add);

        final TextView singleFileHeaderExplanation1 = findViewById(R.id.singleFileHeadingExplanation1);
        final TextView singleFileExplanation1 = findViewById(R.id.singleFileExplanation1);

        Button butChooseFilesystem = findViewById(R.id.butChooseFileFromFilesystem);
        butChooseFilesystem.setOnClickListener(v -> chooseFileFromFilesystem());

        int nextOption = 1;
        String optionLabel = getString(R.string.singlefile_import_option);
        if (singleFileExplanation1.getVisibility() == View.VISIBLE) {
            singleFileHeaderExplanation1.setText(String.format(optionLabel, nextOption));
            nextOption++;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseFileFromFilesystem(); // Retry
            }
        }
    }

    /*
     * Lets the user choose a specific file to import.
     */
    private void chooseFileFromFilesystem() {
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            return;
        }

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select a File");

        dialog.setDialogSelectionListener(this);

        dialog.show();
    }

    @Override
    public void onSelectedFilePaths(String[] files) {
        for (String f: files) {
            Uri uri = new Uri.Builder().scheme("file").path(f).build();
            Log.d(TAG, "path is " + uri.getPath());
            boolean ret = importUri(uri);
            if (ret == false) {
                Toast.makeText(this, "Could not import", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
            }

            //go back to main
            finish();
        }
    }

    private void requestPermission() {
        String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
    }

    boolean importUri(@Nullable Uri uri) {
        if (uri == null) {
            new MaterialDialog.Builder(this)
                    .content(R.string.opml_import_error_no_file)
                    .positiveText(android.R.string.ok)
                    .show();
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            try {
                LocalFeedUpdater.startImport(uri, this);
                return true;
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
                String message = getString(R.string.singlefile_import_error);
                new MaterialDialog.Builder(this)
                        .content(message + " " + e.getMessage())
                        .positiveText(android.R.string.ok)
                        .show();
            }
        }
        return false;
    }
}
