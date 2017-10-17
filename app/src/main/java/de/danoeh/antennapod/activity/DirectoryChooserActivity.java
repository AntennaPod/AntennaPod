package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Let's the user choose a directory on the storage device. The selected folder
 * will be sent back to the starting activity as an activity result.
 */
public class DirectoryChooserActivity extends AppCompatActivity {

    private static final String TAG = "DirectoryChooserActivit";

    private static final String CREATE_DIRECTORY_NAME = "AntennaPod";

    public static final String RESULT_SELECTED_DIR = "selected_dir";
    public static final int RESULT_CODE_DIR_SELECTED = 1;

    private Button butConfirm;
    private Button butCancel;
    private ImageButton butNavUp;
    private TextView txtvSelectedFolder;
    private ListView listDirectories;

    private ArrayAdapter<String> listDirectoriesAdapter;
    private ArrayList<String> filenames;
    /** The directory that is currently being shown. */
    private File selectedDir;
    private File[] filesInDir;

    private FileObserver fileObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.directory_chooser);
        butConfirm = (Button) findViewById(R.id.butConfirm);
        butCancel = (Button) findViewById(R.id.butCancel);
        butNavUp = (ImageButton) findViewById(R.id.butNavUp);
        txtvSelectedFolder = (TextView) findViewById(R.id.txtvSelectedFolder);
        listDirectories = (ListView) findViewById(R.id.directory_list);

        butConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isValidFile(selectedDir)) {
                    if (selectedDir.list().length == 0) {
                        returnSelectedFolder();
                    } else {
                        showNonEmptyDirectoryWarning();
                    }
                }
            }

            private void showNonEmptyDirectoryWarning() {
                AlertDialog.Builder adb = new AlertDialog.Builder(
                        DirectoryChooserActivity.this);
                adb.setTitle(R.string.folder_not_empty_dialog_title);
                adb.setMessage(R.string.folder_not_empty_dialog_msg);
                adb.setNegativeButton(R.string.cancel_label,
                        (dialog, which) -> dialog.dismiss());
                adb.setPositiveButton(R.string.confirm_label,
                        (dialog, which) -> {
                            dialog.dismiss();
                            returnSelectedFolder();
                        });
                adb.create().show();
            }
        });

        butCancel.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        listDirectories.setOnItemClickListener((adapter, view, position, id) -> {
            Log.d(TAG, "Selected index: " + position);
            if (filesInDir != null && position >= 0
                    && position < filesInDir.length) {
                changeDirectory(filesInDir[position]);
            }
        });

        butNavUp.setOnClickListener(v -> {
            File parent = null;
            if (selectedDir != null
                    && (parent = selectedDir.getParentFile()) != null) {
                changeDirectory(parent);
            }
        });

        filenames = new ArrayList<>();
        listDirectoriesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, filenames);
        listDirectories.setAdapter(listDirectoriesAdapter);
        changeDirectory(Environment.getExternalStorageDirectory());
    }

    /**
     * Finishes the activity and returns the selected folder as a result. The
     * selected folder can also be null.
     */
    private void returnSelectedFolder() {
        if (selectedDir != null && BuildConfig.DEBUG)
            Log.d(TAG, "Returning " + selectedDir.getAbsolutePath()
                    + " as result");
        Intent resultData = new Intent();
        if (selectedDir != null) {
            resultData.putExtra(RESULT_SELECTED_DIR,
                    selectedDir.getAbsolutePath());
        }
        setResult(Activity.RESULT_OK, resultData);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fileObserver != null) {
            fileObserver.startWatching();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        listDirectoriesAdapter = null;
        fileObserver = null;
    }

    /**
     * Change the directory that is currently being displayed.
     * 
     * @param dir
     *            The file the activity should switch to. This File must be
     *            non-null and a directory, otherwise the displayed directory
     *            will not be changed
     */
    private void changeDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] contents = dir.listFiles();
            if (contents != null) {
                int numDirectories = 0;
                for (File f : contents) {
                    if (f.isDirectory()) {
                        numDirectories++;
                    }
                }
                filesInDir = new File[numDirectories];
                filenames.clear();
                for (int i = 0, counter = 0; i < numDirectories; counter++) {
                    if (contents[counter].isDirectory()) {
                        filesInDir[i] = contents[counter];
                        filenames.add(contents[counter].getName());
                        i++;
                    }
                }
                Arrays.sort(filesInDir);
                Collections.sort(filenames);
                selectedDir = dir;
                txtvSelectedFolder.setText(dir.getAbsolutePath());
                listDirectoriesAdapter.notifyDataSetChanged();
                fileObserver = createFileObserver(dir.getAbsolutePath());
                fileObserver.startWatching();
                Log.d(TAG, "Changed directory to " + dir.getAbsolutePath());
            } else {
                Log.d(TAG, "Could not change folder: contents of dir were null");
            }
        } else {
            if (dir == null) {
                Log.d(TAG, "Could not change folder: dir was null");
            } else {
                Log.d(TAG, "Could not change folder: dir is no directory");
            }
        }
        refreshButtonState();
    }

    /**
     * Changes the state of the buttons depending on the currently selected file
     * or folder.
     */
    private void refreshButtonState() {
        if (selectedDir != null) {
            butConfirm.setEnabled(isValidFile(selectedDir));
            supportInvalidateOptionsMenu();
        }
    }

    /** Refresh the contents of the directory that is currently shown. */
    private void refreshDirectory() {
        if (selectedDir != null) {
            changeDirectory(selectedDir);
        }
    }

    /** Sets up a FileObserver to watch the current directory. */
    private FileObserver createFileObserver(String path) {
        return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {

            @Override
            public void onEvent(int event, String path) {
                Log.d(TAG, "FileObserver received event " + event);
                runOnUiThread(DirectoryChooserActivity.this::refreshDirectory);
            }
        };
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.new_folder_item).setVisible(isValidFile(selectedDir));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.directory_chooser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        case R.id.new_folder_item:
            openNewFolderDialog();
            return true;
        case R.id.set_to_default_folder_item:
            selectedDir = null;
            returnSelectedFolder();
            return true;
        default:
            return false;
        }
    }

    /**
     * Shows a confirmation dialog that asks the user if he wants to create a
     * new folder.
     */
    private void openNewFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_folder_label);
        builder.setMessage(String.format(getString(R.string.create_folder_msg),
                CREATE_DIRECTORY_NAME));
        builder.setNegativeButton(R.string.cancel_label,
                (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton(R.string.confirm_label,
                (dialog, which) -> {
                    dialog.dismiss();
                    int msg = createFolder();
                    Toast t = Toast.makeText(DirectoryChooserActivity.this,
                            msg, Toast.LENGTH_SHORT);
                    t.show();
                });
        builder.create().show();
    }

    /**
     * Creates a new folder in the current directory with the name
     * CREATE_DIRECTORY_NAME.
     */
    private int createFolder() {
        if (selectedDir == null) {
            return R.string.create_folder_error;
        } else if (selectedDir.canWrite()) {
            File newDir = new File(selectedDir, CREATE_DIRECTORY_NAME);
            if (!newDir.exists()) {
                boolean result = newDir.mkdir();
                if (result) {
                    return R.string.create_folder_success;
                } else {
                    return R.string.create_folder_error;
                }
            } else {
                return R.string.create_folder_error_already_exists;
            }
        } else {
            return R.string.create_folder_error_no_write_access;
        }
    }

    /** Returns true if the selected file or directory would be valid selection. */
    private boolean isValidFile(File file) {
        return file != null && file.isDirectory() && file.canRead() && file.canWrite();
    }

}
