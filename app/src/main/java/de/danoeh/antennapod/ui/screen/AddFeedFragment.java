package de.danoeh.antennapod.ui.screen;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.core.widget.NestedScrollView;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OpmlImportActivity;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.databinding.AddfeedBinding;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.net.discovery.CombinedSearcher;
import de.danoeh.antennapod.net.discovery.FyydPodcastSearcher;
import de.danoeh.antennapod.net.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.net.discovery.PodcastIndexPodcastSearcher;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.discovery.OnlineSearchFragment;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import java.util.Collections;

/**
 * Provides actions for adding new podcast subscriptions.
 */
public class AddFeedFragment extends Fragment {

    public static final String TAG = "AddFeedFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private AddfeedBinding viewBinding;
    private MainActivity activity;
    private boolean displayUpArrow;

    private final ActivityResultLauncher<String> chooseOpmlImportPathLauncher =
            registerForActivityResult(new GetContent(), this::chooseOpmlImportPathResult);
    private final ActivityResultLauncher<Uri> addLocalFolderLauncher =
            registerForActivityResult(new AddLocalFolder(), this::addLocalFolderResult);

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = AddfeedBinding.inflate(inflater);
        activity = (MainActivity) getActivity();

        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(viewBinding.toolbar, displayUpArrow);

        NestedScrollView scrollView = viewBinding.getRoot().findViewById(R.id.scrollView);
        scrollView.setOnScrollChangeListener(new LiftOnScrollListener(viewBinding.appbar));

        viewBinding.searchItunesButton.setOnClickListener(v
                -> activity.loadChildFragment(OnlineSearchFragment.newInstance(ItunesPodcastSearcher.class)));
        viewBinding.searchFyydButton.setOnClickListener(v
                -> activity.loadChildFragment(OnlineSearchFragment.newInstance(FyydPodcastSearcher.class)));
        viewBinding.searchPodcastIndexButton.setOnClickListener(v
                -> activity.loadChildFragment(OnlineSearchFragment.newInstance(PodcastIndexPodcastSearcher.class)));

        viewBinding.combinedFeedSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        viewBinding.addViaUrlButton.setOnClickListener(v
                -> showAddViaUrlDialog());

        viewBinding.opmlImportButton.setOnClickListener(v -> {
            try {
                chooseOpmlImportPathLauncher.launch("*/*");
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                EventBus.getDefault().post(new MessageEvent(getString(R.string.unable_to_start_system_file_manager)));
            }
        });

        viewBinding.addLocalFolderButton.setOnClickListener(v -> {
            try {
                addLocalFolderLauncher.launch(null);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                EventBus.getDefault().post(new MessageEvent(getString(R.string.unable_to_start_system_file_manager)));
            }
        });
        viewBinding.searchButton.setOnClickListener(view -> performSearch());

        return viewBinding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private void showAddViaUrlDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.add_podcast_by_url);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        dialogBinding.textInput.setHint(R.string.rss_address);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_URI);

        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0 && clipData.getItemAt(0).getText() != null) {
            final String clipboardContent = clipData.getItemAt(0).getText().toString();
            if (clipboardContent.trim().startsWith("http")) {
                dialogBinding.textInput.setText(clipboardContent.trim());
            }
        }
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label,
                (dialog, which) -> addUrl(dialogBinding.textInput.getText().toString()));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }

    private void addUrl(String url) {
        startActivity(new OnlineFeedviewActivityStarter(getContext(), url).withManualUrl().getIntent());
    }

    private void performSearch() {
        viewBinding.combinedFeedSearchEditText.clearFocus();
        InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(viewBinding.combinedFeedSearchEditText.getWindowToken(), 0);
        String query = viewBinding.combinedFeedSearchEditText.getText().toString();
        if (query.matches("http[s]?://.*")) {
            addUrl(query);
            return;
        }
        activity.loadChildFragment(OnlineSearchFragment.newInstance(CombinedSearcher.class, query));
        viewBinding.combinedFeedSearchEditText.post(() -> viewBinding.combinedFeedSearchEditText.setText(""));
    }

    private void chooseOpmlImportPathResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        final Intent intent = new Intent(getContext(), OpmlImportActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    private void addLocalFolderResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        Observable.fromCallable(() -> addLocalFolder(uri))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        feed -> {
                            Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
                            ((MainActivity) getActivity()).loadChildFragment(fragment);
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            EventBus.getDefault().post(new MessageEvent(error.getLocalizedMessage()));
                        });
    }

    private Feed addLocalFolder(Uri uri) {
        getActivity().getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), uri);
        if (documentFile == null) {
            throw new IllegalArgumentException("Unable to retrieve document tree");
        }
        String title = documentFile.getName();
        if (title == null) {
            title = getString(R.string.local_folder);
        }
        Feed dirFeed = new Feed(Feed.PREFIX_LOCAL_FOLDER + uri.toString(), null, title);
        dirFeed.setItems(Collections.emptyList());
        dirFeed.setSortOrder(SortOrder.EPISODE_TITLE_A_Z);
        Feed fromDatabase = FeedDatabaseWriter.updateFeed(getContext(), dirFeed, false);
        FeedUpdateManager.getInstance().runOnce(requireContext(), fromDatabase);
        return fromDatabase;
    }

    private static class AddLocalFolder extends ActivityResultContracts.OpenDocumentTree {
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @Nullable final Uri input) {
            return super.createIntent(context, input)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}
