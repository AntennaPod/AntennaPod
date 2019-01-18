package de.danoeh.antennapod.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.StorageUtils;

/**
 * Lets the user start the file import process from a path
 */
public class AddLocalFilesActivity extends AppCompatActivity {

    private static final String TAG = "AddLocalFilesAct";
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 5;

    private static final int CHOOSE_SINGLE_FILE = 1;

    private Intent intentPickAction;

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
        intentPickAction = new Intent(Intent.ACTION_PICK);

        if (!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
            intentPickAction.setData(null);
            if (!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
                singleFileHeaderExplanation1.setVisibility(View.GONE);
                singleFileExplanation1.setVisibility(View.GONE);
                findViewById(R.id.divider1).setVisibility(View.GONE);
                butChooseFilesystem.setVisibility(View.GONE);
            }
        }
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
     * Creates an implicit intent to launch a file manager which lets
     * the user choose a specific file to import.
     */
    private void chooseFileFromFilesystem() {
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            return;
        }

        try {
            startActivityForResult(intentPickAction, CHOOSE_SINGLE_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    /**
     * With the path, attempt to start the import process
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHOOSE_SINGLE_FILE) {
            Uri uri = data.getData();
            if (uri != null && uri.toString().startsWith("/")) {
                uri = Uri.parse("file://" + uri.toString());
            }
            boolean ret = importUri(uri);
            if (ret == false) {
                Toast.makeText(this, "Could not import", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Success!", Toast.LENGTH_LONG).show();
            }

            //go back to main
            finish();
        } else {
            Log.d(TAG, "result not ok or code not file: " + resultCode + "," + requestCode);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                uri.toString().contains(Environment.getExternalStorageDirectory().toString())) {
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
