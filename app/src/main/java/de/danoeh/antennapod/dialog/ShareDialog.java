package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.databinding.ShareEpisodeDialogBinding;
import de.danoeh.antennapod.model.feed.FeedItem;

public class ShareDialog extends BottomSheetDialogFragment {
    private static final String ARGUMENT_FEED_ITEM = "feedItem";
    private static final String PREF_NAME = "ShareDialog";
    private static final String PREF_SHARE_EPISODE_START_AT = "prefShareEpisodeStartAt";
    private static final String PREF_SHARE_EPISODE_TYPE = "prefShareEpisodeType";

    private Context ctx;
    private FeedItem item;
    private SharedPreferences prefs;

    private ShareEpisodeDialogBinding viewBinding;

    public ShareDialog() {
        // Empty constructor required for DialogFragment
    }

    public static ShareDialog newInstance(FeedItem item) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARGUMENT_FEED_ITEM, item);
        ShareDialog dialog = new ShareDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ctx = getActivity();
            item = (FeedItem) getArguments().getSerializable(ARGUMENT_FEED_ITEM);
            prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        viewBinding = ShareEpisodeDialogBinding.inflate(inflater);
        viewBinding.shareDialogRadioGroup.setOnCheckedChangeListener((group, checkedId) ->
                viewBinding.sharePositionCheckbox.setEnabled(checkedId == viewBinding.shareSocialRadio.getId()));

        setupOptions();

        viewBinding.shareButton.setOnClickListener((v) -> {
            boolean includePlaybackPosition = viewBinding.sharePositionCheckbox.isChecked();
            int position;
            if (viewBinding.shareSocialRadio.isChecked()) {
                ShareUtils.shareFeedItemLinkWithDownloadLink(ctx, item, includePlaybackPosition);
                position = 1;
            } else if (viewBinding.shareMediaReceiverRadio.isChecked()) {
                ShareUtils.shareMediaDownloadLink(ctx, item.getMedia());
                position = 2;
            } else if (viewBinding.shareMediaFileRadio.isChecked()) {
                ShareUtils.shareFeedItemFile(ctx, item.getMedia());
                position = 3;
            } else {
                throw new IllegalStateException("Unknown share method");
            }
            prefs.edit()
                    .putBoolean(PREF_SHARE_EPISODE_START_AT, includePlaybackPosition)
                    .putInt(PREF_SHARE_EPISODE_TYPE, position)
                    .apply();
            dismiss();
        });
        return viewBinding.getRoot();
    }

    private void setupOptions() {
        final boolean hasMedia = item.getMedia() != null;
        boolean downloaded = hasMedia && item.getMedia().isDownloaded();
        viewBinding.shareMediaFileRadio.setVisibility(downloaded ? View.VISIBLE : View.GONE);

        boolean hasDownloadUrl = hasMedia && item.getMedia().getDownload_url() != null;
        if (!hasDownloadUrl) {
            viewBinding.shareMediaReceiverRadio.setVisibility(View.GONE);
        }
        int type = prefs.getInt(PREF_SHARE_EPISODE_TYPE, 1);
        if ((type == 2 && !hasDownloadUrl) || (type == 3 && !downloaded)) {
            type = 1;
        }
        viewBinding.shareSocialRadio.setChecked(type == 1);
        viewBinding.shareMediaReceiverRadio.setChecked(type == 2);
        viewBinding.shareMediaFileRadio.setChecked(type == 3);

        boolean switchIsChecked = prefs.getBoolean(PREF_SHARE_EPISODE_START_AT, false);
        viewBinding.sharePositionCheckbox.setChecked(switchIsChecked);
    }
}
