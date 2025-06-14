package de.danoeh.antennapod.ui.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.databinding.ShareEpisodeDialogBinding;
import de.danoeh.antennapod.model.feed.FeedItem;

public class ShareDialog extends BottomSheetDialogFragment {
    private static final String ARGUMENT_FEED_ITEM = "feedItem";
    private static final String PREF_NAME = "ShareDialog";
    private static final String PREF_SHARE_EPISODE_START_AT = "prefShareEpisodeStartAt";

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
        if (getArguments() == null) {
            return null;
        }
        FeedItem item = (FeedItem) getArguments().getSerializable(ARGUMENT_FEED_ITEM);
        ShareEpisodeDialogBinding viewBinding = ShareEpisodeDialogBinding.inflate(inflater);

        if (item.getMedia() != null && item.getMedia().isDownloaded()) {
            viewBinding.mediaFileCardCard.setOnClickListener(v -> {
                ShareUtils.shareFeedItemFile(getContext(), item.getMedia());
                dismiss();
            });
        } else {
            viewBinding.mediaFileCardCard.setVisibility(View.GONE);
        }

        if (item.getMedia() != null && item.getMedia().getDownloadUrl() != null) {
            viewBinding.mediaAddressText.setText(item.getMedia().getDownloadUrl());
            viewBinding.mediaAddressCard.setOnClickListener(v -> {
                ShareUtils.shareLink(getContext(), item.getMedia().getDownloadUrl());
                dismiss();
            });
        } else {
            viewBinding.mediaAddressCard.setVisibility(View.GONE);
        }

        SharedPreferences prefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        viewBinding.sharePositionCheckbox.setChecked(prefs.getBoolean(PREF_SHARE_EPISODE_START_AT, false));
        viewBinding.socialMessageText.setText(ShareUtils.getSocialFeedItemShareText(
                getContext(), item, viewBinding.sharePositionCheckbox.isChecked(), true));
        viewBinding.sharePositionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_SHARE_EPISODE_START_AT, isChecked).apply();
            viewBinding.socialMessageText.setText(
                    ShareUtils.getSocialFeedItemShareText(getContext(), item, isChecked, true));
        });
        viewBinding.socialMessageCard.setOnClickListener(v -> {
            ShareUtils.shareLink(getContext(), ShareUtils.getSocialFeedItemShareText(
                    getContext(), item, viewBinding.sharePositionCheckbox.isChecked(), false));
            dismiss();
        });

        return viewBinding.getRoot();
    }
}
