package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OpmlImportActivity;
import de.danoeh.antennapod.databinding.AddfeedBinding;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.discovery.CombinedSearcher;
import de.danoeh.antennapod.discovery.FyydPodcastSearcher;
import de.danoeh.antennapod.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.fragment.gpodnet.GpodnetMainFragment;

/**
 * Provides actions for adding new podcast subscriptions.
 */
public class AddFeedFragment extends Fragment {

    public static final String TAG = "AddFeedFragment";
    private static final int REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH = 1;

    private MainActivity activity;

    AddfeedBinding addFeedViewBinding;
    EditTextDialogBinding editTextDialogViewBinding;

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.addfeed, container, false);

        addFeedViewBinding = AddfeedBinding.bind(root);

        activity = (MainActivity) getActivity();

        ((AppCompatActivity) getActivity()).setSupportActionBar(addFeedViewBinding.toolbar);

        addFeedViewBinding.searchItunesButton.setOnClickListener(v
                -> activity.loadChildFragment(OnlineSearchFragment.newInstance(ItunesPodcastSearcher.class)));
        addFeedViewBinding.searchFyydButton.setOnClickListener(v
                -> activity.loadChildFragment(OnlineSearchFragment.newInstance(FyydPodcastSearcher.class)));
        addFeedViewBinding.searchGpodderButton.setOnClickListener(v
                -> activity.loadChildFragment(new GpodnetMainFragment()));

        addFeedViewBinding.podcastSearchButton.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        addFeedViewBinding.addViaUrlButton.setOnClickListener(v
                -> showAddViaUrlDialog());

        addFeedViewBinding.opmlImportButton.setOnClickListener(v -> {
            try {
                Intent intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
                intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
                intentGetContentAction.setType("*/*");
                startActivityForResult(intentGetContentAction, REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found. Should never happen...");
            }
        });
        addFeedViewBinding.searchButton.setOnClickListener(view -> performSearch());
        return addFeedViewBinding.getRoot();
    }

    private void showAddViaUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.add_podcast_by_url);

        View content = View.inflate(getContext(), R.layout.edit_text_dialog, null);
        editTextDialogViewBinding = EditTextDialogBinding.bind(content);

        editTextDialogViewBinding.text.setHint(R.string.add_podcast_by_url_hint);
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        String clipboardContent = clipboard.getText() != null ? clipboard.getText().toString() : "";
        if (clipboardContent.trim().startsWith("http")) {
            editTextDialogViewBinding.text.setText(clipboardContent.trim());
        }

        builder.setView(editTextDialogViewBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> addUrl(editTextDialogViewBinding.text.getText().toString()));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }

    private void addUrl(String url) {
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, url);
        startActivity(intent);
    }

    private void performSearch() {
        String query = addFeedViewBinding.podcastSearchButton.getText().toString();

        if (query.matches("http[s]?://.*")) {
            addUrl(query);
            return;
        }
        activity.loadChildFragment(OnlineSearchFragment.newInstance(CombinedSearcher.class, query));
    }

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();

        if (requestCode == REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH) {
            Intent intent = new Intent(getContext(), OpmlImportActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
    }
}
