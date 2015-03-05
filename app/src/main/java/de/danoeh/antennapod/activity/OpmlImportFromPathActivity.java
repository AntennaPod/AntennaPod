package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.LangUtils;
import de.danoeh.antennapod.core.util.StorageUtils;

import java.io.*;

/**
 * Lets the user start the OPML-import process from a path
 */
public class OpmlImportFromPathActivity extends OpmlImportBaseActivity {
    private static final String TAG = "OpmlImportFromPathActivity";
    private static final int CHOOSE_OPML_FILE = 1;
    private TextView txtvPath;
    private Button butChoose;
    private Button butStart;
    private String importPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.opml_import);

        txtvPath = (TextView) findViewById(R.id.txtvPath);
        butChoose = (Button) findViewById(R.id.butChooseImport);
        butStart = (Button) findViewById(R.id.butStartImport);

        butChoose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFileToImport();
            }
        });

        butStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkFolderForFiles();
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
        if (txtvPath.getText().equals("")) {
            setImportPath();
        }
    }

    /**
     * Sets the importPath variable and makes txtvPath display the import
     * directory.
     */
    private void setImportPath() {
        File importDir = UserPreferences.getDataFolder(this, UserPreferences.IMPORT_DIR);
        boolean success = true;
        if (!importDir.exists()) {
            if (BuildConfig.DEBUG)
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

    /**
     * Looks at the contents of the import directory and decides what to do. If
     * more than one file is in the directory, a dialog will be created to let
     * the user choose which item to import
     */
    private void checkFolderForFiles() {
        File dir = new File(importPath);
        if (dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList.length == 1) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Found one file, choosing that one.");
                startImport(fileList[0]);
            } else if (fileList.length > 1) {
                Log.w(TAG, "Import directory contains more than one file.");
                askForFile(dir);
            } else {
                Log.e(TAG, "Import directory is empty");
                Toast toast = Toast
                        .makeText(this, R.string.opml_import_error_dir_empty,
                                Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (dir.isFile()) {
            startImport(dir);
        }
    }

    private void startImport(File file) {
        Reader mReader = null;
        try {
            mReader = new InputStreamReader(new FileInputStream(file),
                LangUtils.UTF_8);
            if (BuildConfig.DEBUG) Log.d(TAG, "Parsing " + file.toString());
            startImport(mReader);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found which really should be there");
            // this should never happen as it is a file we have just chosen
        }
    }

    /**
     * Asks the user to choose from a list of files in a directory and returns
     * his choice.
     */
    private void askForFile(File dir) {
        final File[] fileList = dir.listFiles();
        String[] fileNames = dir.list();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.choose_file_to_import_label);
        dialog.setNeutralButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Dialog was cancelled");
                        dialog.dismiss();
                    }
                });
        dialog.setItems(fileNames, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "File at index " + which + " was chosen");
                dialog.dismiss();
                startImport(fileList[which]);
            }
        });
        dialog.create().show();
    }

    /**
     * Creates an implicit intent to launch a file manager which lets
     * the user choose a specific OPML-file to import from.
     */
    private void chooseFileToImport() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(Uri.parse("file://"));
        try {
            startActivityForResult(intent, CHOOSE_OPML_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Trying without URI scheme.");
            intent.setData(null);
            startActivityForResult(intent, CHOOSE_OPML_FILE);
        }
    }

    /**
     * Gets the path of the file chosen with chooseFileToImport()
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_OPML_FILE) {
            importPath = data.getData().getPath();
            txtvPath.setText(importPath);
        }
    }

}
