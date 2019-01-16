package de.danoeh.antennapod.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.StorageUtils;

/**
 * Lets the user start the single file import process from a path
 */
public class SingleFileAddActivity extends AppCompatActivity {

    private static final String TAG = "SingleFileAddAct";
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

        if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
            intentPickAction.setData(null);
            if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
                singleFileHeaderExplanation1.setVisibility(View.GONE);
                singleFileExplanation1.setVisibility(View.GONE);
                findViewById(R.id.divider1).setVisibility(View.GONE);
                butChooseFilesystem.setVisibility(View.GONE);
            }
        }
        if(singleFileExplanation1.getVisibility() == View.VISIBLE) {
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

    /*
     * Creates an implicit intent to launch a file manager which lets
     * the user choose a specific file to import.
     */
    private void chooseFileFromFilesystem() {
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }

        try {
            startActivityForResult(intentPickAction, CHOOSE_SINGLE_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    /**
      * Gets the path of the file chosen with chooseFileToImport()
      */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_SINGLE_FILE) {
            Uri uri = data.getData();
            if(uri != null && uri.toString().startsWith("/")) {
                uri = Uri.parse("file://" + uri.toString());
            }
            boolean ret = importUri(uri);
            if (ret == false) {
                Toast.makeText(this, "Could not import", Toast.LENGTH_LONG).show();
            }

            //go back to main
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Log.d(TAG, "result not ok or code not file: " + resultCode + "," + requestCode);
        }
    }

    private void requestPermission() {
        String[] permissions = { android.Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
    }

    boolean importUri(@Nullable Uri uri) {
        if(uri == null) {
            new MaterialDialog.Builder(this)
                    .content(R.string.opml_import_error_no_file)
                    .positiveText(android.R.string.ok)
                    .show();
            return false;
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                uri.toString().contains(Environment.getExternalStorageDirectory().toString())) {
            int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            startImport(uri);
            return true;
        }
        return false;
    }

    private long getFileDuration(File f) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(f.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }

    /** Starts the import process. */
    private void startImport(Uri uri) {
        try {
            String fileUrl = uri.toString();
            Feed theDummyFeed = new Feed("someDummyUrl", null, "Dummy feed (local files)");

            File f = new File(uri.getPath());

            //create item
            long globalId = 0;
            FeedItem item = new FeedItem(globalId, "Dummy Feed: Item " + f.getName(), "item" + Long.toString(new Date().getTime()),
                    fileUrl, new Date(), FeedItem.UNPLAYED, theDummyFeed);
            item.setAutoDownload(false);

            //add the media to the item
            long duration = getFileDuration(f);
            long size = f.length();
            FeedMedia media = new FeedMedia(0, item, (int)duration, 0, size, "audio/mp3", f.getAbsolutePath(), f.getAbsolutePath(), true, null, 0, 0);
            item.setMedia(media);

            //add to the feed
            List<FeedItem> items = new ArrayList<>();
            items.add(item);
            theDummyFeed.setItems(items);

            //add or merge to the db
            Feed[] feeds = DBTasks.updateFeed(this, theDummyFeed);

        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
            String message = getString(R.string.singlefile_import_error);
            new MaterialDialog.Builder(this)
                    .content(message + " " + e.getMessage())
                    .positiveText(android.R.string.ok)
                    .show();
        }
    }

}
