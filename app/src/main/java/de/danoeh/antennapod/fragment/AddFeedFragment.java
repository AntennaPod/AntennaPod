package de.danoeh.antennapod.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.fragment.gpodnet.GpodnetMainFragment;

import java.io.File;

/**
 * Provides actions for adding new podcast subscriptions
 */
public class AddFeedFragment extends Fragment implements DialogSelectionListener {
    public static final String TAG = "AddFeedFragment";
    private static final int PERMISSION_REQUEST_ADD_LOCAL_FOLDER = 5;

    /**
     * Preset value for url text field.
     */
    private static final String ARG_FEED_URL = "feedurl";

    private EditText combinedFeedSearchBox;
    private MainActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // So, we certainly *don't* have an options menu,
        // but unless we say we do, old options menus sometimes
        // persist.  mfietz thinks this causes the ActionBar to be invalidated
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.addfeed, container, false);

        activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.add_feed_label);

        setupAdvancedSearchButtons(root);
        setupSeachBox(root);

        View butOpmlImport = root.findViewById(R.id.btn_opml_import);
        butOpmlImport.setOnClickListener(v -> startActivity(new Intent(getActivity(),
                OpmlImportFromPathActivity.class)));

        View butAddLocalFolder = root.findViewById(R.id.btn_add_local_folder);
        butAddLocalFolder.setOnClickListener(v -> addLocalFolder());

        return root;
    }

    private void setupSeachBox(View root) {
        final EditText etxtFeedurl = root.findViewById(R.id.etxtFeedurl);

        Bundle args = getArguments();
        if (args != null && args.getString(ARG_FEED_URL) != null) {
            etxtFeedurl.setText(args.getString(ARG_FEED_URL));
        }

        Button butConfirmAddUrl = root.findViewById(R.id.butConfirm);
        butConfirmAddUrl.setOnClickListener(v -> {
            addUrl(etxtFeedurl.getText().toString());
        });

        combinedFeedSearchBox = root.findViewById(R.id.combinedFeedSearchBox);
        combinedFeedSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void setupAdvancedSearchButtons(View root) {
        View butAdvancedSearch = root.findViewById(R.id.advanced_search);
        registerForContextMenu(butAdvancedSearch);
        butAdvancedSearch.setOnClickListener(v -> butAdvancedSearch.showContextMenu());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.advanced_search, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_fyyd:
                activity.loadChildFragment(new FyydSearchFragment());
                return true;
            case R.id.search_gpodder:
                activity.loadChildFragment(new GpodnetMainFragment());
                return true;
            case R.id.search_itunes:
                activity.loadChildFragment(new ItunesSearchFragment());
                return true;
        }
        return false;
    }

    private void addUrl(String url) {
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, url);
        intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, getString(R.string.add_feed_label));
        startActivity(intent);
    }

    private void performSearch() {
        String query = combinedFeedSearchBox.getText().toString();

        if (query.startsWith("http")) {
            addUrl(query);
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(CombinedSearchFragment.ARGUMENT_QUERY, query);
        CombinedSearchFragment fragment = new CombinedSearchFragment();
        fragment.setArguments(bundle);
        activity.loadChildFragment(fragment);
    }

    /**
     * Lets the user choose a specific folder to import.
     */
    private void addLocalFolder() {
        int permission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
            requestPermissions(permissions, PERMISSION_REQUEST_ADD_LOCAL_FOLDER);
            return;
        }

        DialogProperties properties = new DialogProperties();
        properties.selection_type = DialogConfigs.DIR_SELECT;
        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties);
        dialog.setTitle(R.string.folder_import_label);
        dialog.setDialogSelectionListener(this);

        dialog.show();
    }

    @Override
    public void onSelectedFilePaths(String[] files) {
        for (String f: files) {
            File dir = new File(f);
            Log.d(TAG, "Importing folder: " + dir.getAbsolutePath());

            try {
                Feed feed = LocalFeedUpdater.startImport(dir, getContext());
                DBTasks.forceRefreshFeed(getContext(), feed);
                Snackbar.make(getView(), R.string.folder_import_success, BaseTransientBottomBar.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
                new MaterialDialog.Builder(getContext())
                        .content(getString(R.string.folder_import_error, e.getMessage()))
                        .positiveText(android.R.string.ok)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ADD_LOCAL_FOLDER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addLocalFolder(); // Retry
            }
        }
    }
}
