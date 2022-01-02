package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.util.ShareUtils;

public class ShareDialog extends DialogFragment {
    private static final String ARGUMENT_FEED_ITEM = "feedItem";
    private static final String PREF_NAME = "ShareDialog";
    private static final String PREF_SHARE_EPISODE_START_AT = "prefShareEpisodeStartAt";

    private Context ctx;
    private FeedItem item;
    private SharedPreferences prefs;

    private RadioButton radioMediaFile;
    private RadioButton radioLinkToEpisode;
    private CheckBox checkBoxStartAt;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ctx = getActivity();
            item = (FeedItem) getArguments().getSerializable(ARGUMENT_FEED_ITEM);
            prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        View content = View.inflate(ctx, R.layout.share_episode_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.share_label);
        builder.setView(content);

        RadioGroup radioGroup = content.findViewById(R.id.share_dialog_radio_group);
        radioGroup.setOnCheckedChangeListener((group, checkedId) ->
                checkBoxStartAt.setEnabled(checkedId != R.id.share_media_file_radio));

        radioLinkToEpisode = content.findViewById(R.id.share_link_to_episode_radio);
        radioMediaFile = content.findViewById(R.id.share_media_file_radio);
        checkBoxStartAt = content.findViewById(R.id.share_start_at_timer_dialog);

        setupOptions();

        builder.setPositiveButton(R.string.share_label, (dialog, id) -> {
            boolean includePlaybackPosition = checkBoxStartAt.isChecked();
            if (radioLinkToEpisode.isChecked()) {
                ShareUtils.shareFeedItemLinkWithDownloadLink(ctx, item, includePlaybackPosition);
            } else if (radioMediaFile.isChecked()) {
                ShareUtils.shareFeedItemFile(ctx, item.getMedia());
            } else {
                throw new IllegalStateException("Unknown share method");
            }
            prefs.edit().putBoolean(PREF_SHARE_EPISODE_START_AT, includePlaybackPosition).apply();
        }).setNegativeButton(R.string.cancel_label, (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void setupOptions() {
        final boolean hasMedia = item.getMedia() != null;

        boolean downloaded = hasMedia && item.getMedia().isDownloaded();
        radioMediaFile.setVisibility(downloaded ? View.VISIBLE : View.GONE);

        boolean hasDownloadUrl = hasMedia && item.getMedia().getDownload_url() != null;
        if (!ShareUtils.hasLinkToShare(item) && !hasDownloadUrl) {
            radioLinkToEpisode.setVisibility(View.GONE);
        }

        radioMediaFile.setChecked(false);

        boolean switchIsChecked = prefs.getBoolean(PREF_SHARE_EPISODE_START_AT, false);
        checkBoxStartAt.setChecked(switchIsChecked);
    }
}
