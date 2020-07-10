package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.ShareUtils;

public class ShareDialog extends DialogFragment {

    private static final String ARGUMENT_FEED_ITEM = "feedItem";

    private static final String TAG = "ShareDialog";
    private Context ctx;
    private FeedItem item;

    private static final String PREF_SHARE_DIALOG_OPTION = "prefShareDialogOption";
    private static final String PREF_SHARE_EPISODE_START_AT = "prefShareEpisodeStartAt";

    private RadioGroup radioGroup;
    private RadioButton radioEpisodeWebsite;
    private RadioButton radioMediaFile;
    private CheckBox checkBoxStartAt;
    private SharedPreferences prefs;

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
            prefs = getActivity().getSharedPreferences("ShareDialog", Context.MODE_PRIVATE);
        }

        View content = View.inflate(ctx, R.layout.share_episode_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.share_label);
        builder.setView(content);

        radioGroup = content.findViewById(R.id.share_dialog_radio_group);
        radioEpisodeWebsite = content.findViewById(R.id.share_episode_website_radio);
        radioMediaFile = content.findViewById(R.id.share_media_file_radio);
        checkBoxStartAt = content.findViewById(R.id.share_start_at_timer_dialog);

        setupOptions();

        builder.setPositiveButton(R.string.share_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                boolean includePlaybackPosition = checkBoxStartAt.isChecked();
                if (radioEpisodeWebsite.isChecked()) {
                    ShareUtils.shareFeedItemLink(ctx, item, includePlaybackPosition);
                    prefs.edit().putString(PREF_SHARE_DIALOG_OPTION, "website").apply();
                } else {
                    ShareUtils.shareFeedItemDownloadLink(ctx, item, includePlaybackPosition);
                    prefs.edit().putString(PREF_SHARE_DIALOG_OPTION, "media").apply();
                }
                prefs.edit().putBoolean(PREF_SHARE_EPISODE_START_AT, includePlaybackPosition).apply();
            }
        }).setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    private void setupOptions() {
        final boolean hasMedia = item.getMedia() != null;

        if (!ShareUtils.hasLinkToShare(item)) {
            radioEpisodeWebsite.setVisibility(View.GONE);
            radioMediaFile.setChecked(true);
        }

        if (!hasMedia || item.getMedia().getDownload_url() == null) {
            radioMediaFile.setVisibility(View.GONE);
            radioEpisodeWebsite.setChecked(true);
        }

        if (radioEpisodeWebsite.getVisibility() == View.VISIBLE && radioMediaFile.getVisibility() == View.VISIBLE) {
            String option = prefs.getString(PREF_SHARE_DIALOG_OPTION, "website");
            if (option.equals("website")) {
                radioEpisodeWebsite.setChecked(true);
                radioMediaFile.setChecked(false);
            } else {
                radioEpisodeWebsite.setChecked(false);
                radioMediaFile.setChecked(true);
            }
        }

        boolean switchIsChecked = prefs.getBoolean(PREF_SHARE_EPISODE_START_AT, false);
        checkBoxStartAt.setChecked(switchIsChecked);
    }
}
