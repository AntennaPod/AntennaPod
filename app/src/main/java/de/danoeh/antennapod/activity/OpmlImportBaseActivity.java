package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.OpmlFeedQueuer;
import de.danoeh.antennapod.asynctask.OpmlImportWorker;
import de.danoeh.antennapod.core.export.opml.OpmlElement;
import de.danoeh.antennapod.core.util.LangUtils;

/**
 * Base activity for Opml Import - e.g. with code what to do afterwards
 * */
public class OpmlImportBaseActivity extends AppCompatActivity {

    private static final String TAG = "OpmlImportBaseActivity";
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 5;
    private OpmlImportWorker importWorker;
    @Nullable private Uri uri;

    /**
     * Handles the choices made by the user in the OpmlFeedChooserActivity and
     * starts the OpmlFeedQueuer if necessary.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Received result");
        if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Activity was cancelled");
            if (finishWhenCanceled()) {
                finish();
            }
        } else {
            int[] selected = data.getIntArrayExtra(OpmlFeedChooserActivity.EXTRA_SELECTED_ITEMS);
            if (selected != null && selected.length > 0) {
                OpmlFeedQueuer queuer = new OpmlFeedQueuer(this, selected) {

                    @Override
                    protected void onPostExecute(Void result) {
                        super.onPostExecute(result);
                        Intent intent = new Intent(OpmlImportBaseActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                };
                queuer.executeAsync();
            } else {
                Log.d(TAG, "No items were selected");
            }
        }
    }

    void importUri(@Nullable Uri uri) {
        if(uri == null) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.opml_import_error_no_file)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        this.uri = uri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                uri.toString().contains(Environment.getExternalStorageDirectory().toString())) {
            int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
                return;
            }
        }
        startImport();
    }

    private void requestPermission() {
        String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            return;
        }
        if (grantResults.length > 0 && ArrayUtils.contains(grantResults, PackageManager.PERMISSION_GRANTED)) {
            startImport();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.opml_import_ask_read_permission)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermission())
                    .setNegativeButton(R.string.cancel_label, (dialog, which) -> finish())
                    .show();
        }
    }

    /** Starts the import process. */
    private void startImport() {
        try {
            Reader mReader = new InputStreamReader(getContentResolver().openInputStream(uri), LangUtils.UTF_8);
            importWorker = new OpmlImportWorker(this, mReader) {

                @Override
                protected void onPostExecute(ArrayList<OpmlElement> result) {
                    super.onPostExecute(result);
                    if (result != null) {
                        Log.d(TAG, "Parsing was successful");
                        OpmlImportHolder.setReadElements(result);
                        startActivityForResult(new Intent(
                                OpmlImportBaseActivity.this,
                                OpmlFeedChooserActivity.class), 0);
                    } else {
                        Log.d(TAG, "Parser error occurred");
                    }
                }
            };
            importWorker.executeAsync();
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
            String message = getString(R.string.opml_reader_error);
            new AlertDialog.Builder(this)
                    .setMessage(message + " " + e.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    boolean finishWhenCanceled() {
        return false;
    }


}
