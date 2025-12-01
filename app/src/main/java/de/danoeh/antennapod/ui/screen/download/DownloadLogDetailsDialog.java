package de.danoeh.antennapod.ui.screen.download;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.DownloadLogDetailsDialogBinding;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.common.ClipboardUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Shows a dialog with Feed title (and FeedItem title if possible).
 * Can show a button to jump to the feed details view.
 */
public class DownloadLogDetailsDialog extends DialogFragment {
    public static final String TAG = "DownloadLogDetails";
    private static final String EXTRA_IS_JUMP_TO_FEED = "isJumpToFeed";
    private static final String EXTRA_DOWNLOAD_RESULT = "downloadResult";
    private DownloadLogDetailsDialogBinding viewBinding;
    private Disposable disposable;
    private boolean isJumpToFeed;
    private DownloadResult downloadResult;
    private Feed feed = null;
    private String podcastName = null;
    private String episodeName = null;
    private String url = "unknown";
    private String clipboardContent = "";

    public static DownloadLogDetailsDialog newInstance(DownloadResult downloadResult, boolean isJumpToFeed) {
        DownloadLogDetailsDialog dialog = new DownloadLogDetailsDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DOWNLOAD_RESULT, downloadResult);
        args.putBoolean(EXTRA_IS_JUMP_TO_FEED, isJumpToFeed);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadResult = (DownloadResult) getArguments().getSerializable(EXTRA_DOWNLOAD_RESULT);
        isJumpToFeed = getArguments().getBoolean(EXTRA_IS_JUMP_TO_FEED, true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext());
        dialog.setTitle(R.string.download_error_details);
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNeutralButton(R.string.copy_to_clipboard, (copyDialog, which) ->
                ClipboardUtils.copyText(viewBinding.getRoot(), R.string.download_error_details, clipboardContent));

        viewBinding = DownloadLogDetailsDialogBinding.inflate(getLayoutInflater());
        dialog.setView(viewBinding.getRoot());

        viewBinding.goToPodcastButton.setVisibility(View.GONE);
        viewBinding.goToPodcastButton.setOnClickListener(v -> {
            goToFeed();
            dismiss();
            Fragment downloadLog = getParentFragmentManager().findFragmentByTag(DownloadLogFragment.TAG);
            if (downloadLog instanceof DownloadLogFragment) {
                ((DownloadLogFragment) downloadLog).dismiss();
            }
        });
        viewBinding.fileUrlLabel.setOnClickListener(v ->
                ClipboardUtils.copyText(viewBinding.fileUrlLabel, R.string.download_log_details_file_url_title));
        viewBinding.technicalReasonLabel.setOnClickListener(v ->
                ClipboardUtils.copyText(viewBinding.technicalReasonLabel,
                        R.string.download_log_details_technical_reason_title));

        loadData();
        return dialog.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadData() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Single.create(emitter -> {
            if (downloadResult.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                FeedMedia media = DBReader.getFeedMedia(downloadResult.getFeedfileId());
                if (media != null) {
                    if (media.getItem() != null && media.getItem().getFeed() != null) {
                        feed = media.getItem().getFeed();
                        podcastName = feed.getTitle();
                    }
                    episodeName = media.getEpisodeTitle();
                    url = media.getDownloadUrl();
                } else {
                    episodeName = downloadResult.getTitle();
                }
            } else if (downloadResult.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                feed = DBReader.getFeed(downloadResult.getFeedfileId(), false, 0, 0);
                if (feed != null) {
                    podcastName = feed.getTitle();
                    url = feed.getDownloadUrl();
                } else {
                    podcastName = downloadResult.getTitle();
                }
            }
            emitter.onSuccess(true);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(obj -> updateUi(),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void updateUi() {
        String message = getString(R.string.download_successful);
        if (!downloadResult.isSuccessful()) {
            message = downloadResult.getReasonDetailed();
        }
        viewBinding.goToPodcastButton.setVisibility((isJumpToFeed && feed != null) ? View.VISIBLE : View.GONE);
        viewBinding.podcastNameLabel.setText(podcastName);
        viewBinding.podcastContainer.setVisibility(podcastName == null ? View.GONE : View.VISIBLE);
        viewBinding.episodeNameLabel.setText(episodeName);
        viewBinding.episodeContainer.setVisibility(episodeName == null ? View.GONE : View.VISIBLE);

        final String humanReadableReason = getString(DownloadErrorLabel.from(downloadResult.getReason()));
        viewBinding.humanReadableReasonLabel.setText(humanReadableReason);
        viewBinding.technicalReasonLabel.setText(message);
        viewBinding.fileUrlLabel.setText(url);

        final String humanReadableReasonTitle = getString(R.string.download_log_details_human_readable_reason_title);
        final String technicalReasonTitle = getString(R.string.download_log_details_technical_reason_title);
        final String urlTitle = getString(R.string.download_log_details_file_url_title);
        clipboardContent = String.format("%s: \n%s \n\n%s: \n%s \n\n%s: \n%s",
                humanReadableReasonTitle, humanReadableReason, technicalReasonTitle, message, urlTitle, url);
    }

    void goToFeed() {
        if (feed == null) {
            return;
        }
        Intent intent;
        if (feed.getState() == Feed.STATE_SUBSCRIBED) {
            intent = new MainActivityStarter(getContext()).withOpenFeed(feed.getId()).getIntent();
        } else {
            intent = new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl()).getIntent();
        }
        getContext().startActivity(intent);
    }
}