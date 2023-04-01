package de.danoeh.antennapod.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.export.opml.OpmlElement;
import de.danoeh.antennapod.core.export.opml.OpmlReader;
import de.danoeh.antennapod.core.preferences.ThemeSwitcher;

import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.databinding.OpmlSelectionBinding;
import de.danoeh.antennapod.model.feed.Feed;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity for Opml Import.
 * */
public class OpmlImportActivity extends AppCompatActivity {
    private static final String TAG = "OpmlImportBaseActivity";
    @Nullable private Uri uri;
    private OpmlSelectionBinding viewBinding;
    private ArrayAdapter<String> listAdapter;
    private MenuItem selectAll;
    private MenuItem deselectAll;
    private ArrayList<OpmlElement> readElements;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getTheme(this));
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewBinding = OpmlSelectionBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.feedlist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        viewBinding.feedlist.setOnItemClickListener((parent, view, position, id) -> {
            SparseBooleanArray checked = viewBinding.feedlist.getCheckedItemPositions();
            int checkedCount = 0;
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    checkedCount++;
                }
            }
            if (checkedCount == listAdapter.getCount()) {
                selectAll.setVisible(false);
                deselectAll.setVisible(true);
            } else {
                deselectAll.setVisible(false);
                selectAll.setVisible(true);
            }
        });
        viewBinding.butCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        viewBinding.butConfirm.setOnClickListener(v -> {
            viewBinding.progressBar.setVisibility(View.VISIBLE);
            Completable.fromAction(() -> {
                SparseBooleanArray checked = viewBinding.feedlist.getCheckedItemPositions();
                for (int i = 0; i < checked.size(); i++) {
                    if (!checked.valueAt(i)) {
                        continue;
                    }
                    OpmlElement element = readElements.get(checked.keyAt(i));
                    Feed feed = new Feed(element.getXmlUrl(), null,
                            element.getText() != null ? element.getText() : "Unknown podcast");
                    feed.setItems(Collections.emptyList());
                    DBTasks.updateFeed(this, feed, false);
                }
                FeedUpdateManager.runOnce(this);
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            () -> {
                                viewBinding.progressBar.setVisibility(View.GONE);
                                Intent intent = new Intent(OpmlImportActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }, e -> {
                                e.printStackTrace();
                                viewBinding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                            });
        });

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith("/")) {
            uri = Uri.parse("file://" + uri.toString());
        } else {
            String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                uri = Uri.parse(extraText);
            }
        }
        importUri(uri);
    }

    void importUri(@Nullable Uri uri) {
        if (uri == null) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.opml_import_error_no_file)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        this.uri = uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && uri.toString().contains(Environment.getExternalStorageDirectory().toString())) {
            int permission = ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
                return;
            }
        }
        startImport();
    }

    private List<String> getTitleList() {
        List<String> result = new ArrayList<>();
        if (readElements != null) {
            for (OpmlElement element : readElements) {
                result.add(element.getText());
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opml_selection_options, menu);
        selectAll = menu.findItem(R.id.select_all_item);
        deselectAll = menu.findItem(R.id.deselect_all_item);
        deselectAll.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.select_all_item) {
            selectAll.setVisible(false);
            selectAllItems(true);
            deselectAll.setVisible(true);
            return true;
        } else if (itemId == R.id.deselect_all_item) {
            deselectAll.setVisible(false);
            selectAllItems(false);
            selectAll.setVisible(true);
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
        }
        return false;
    }

    private void selectAllItems(boolean b) {
        for (int i = 0; i < viewBinding.feedlist.getCount(); i++) {
            viewBinding.feedlist.setItemChecked(i, b);
        }
    }

    private void requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    startImport();
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.opml_import_ask_read_permission)
                            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                    requestPermission())
                            .setNegativeButton(R.string.cancel_label, (dialog, which) ->
                                    finish())
                            .show();
                }
            });

    /** Starts the import process. */
    private void startImport() {
        viewBinding.progressBar.setVisibility(View.VISIBLE);

        Observable.fromCallable(() -> {
            InputStream opmlFileStream = getContentResolver().openInputStream(uri);
            BOMInputStream bomInputStream = new BOMInputStream(opmlFileStream);
            ByteOrderMark bom = bomInputStream.getBOM();
            String charsetName = (bom == null) ? "UTF-8" : bom.getCharsetName();
            Reader reader = new InputStreamReader(bomInputStream, charsetName);
            OpmlReader opmlReader = new OpmlReader();
            ArrayList<OpmlElement> result = opmlReader.readDocument(reader);
            reader.close();
            return result;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            viewBinding.progressBar.setVisibility(View.GONE);
                            Log.d(TAG, "Parsing was successful");
                            readElements = result;
                            listAdapter = new ArrayAdapter<>(OpmlImportActivity.this,
                                    android.R.layout.simple_list_item_multiple_choice,
                                    getTitleList());
                            viewBinding.feedlist.setAdapter(listAdapter);
                        }, e -> {
                            viewBinding.progressBar.setVisibility(View.GONE);
                            MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
                            alert.setTitle(R.string.error_label);
                            alert.setMessage(getString(R.string.opml_reader_error) + e.getMessage());
                            alert.setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                            alert.create().show();
                        });
    }
}
