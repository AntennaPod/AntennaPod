package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.ShareUtils;

public class ShareDialog extends DialogFragment {

    private static final String TAG = "ShareDialog";
    private final Context ctx;
    private FeedItem item;

    private RadioGroup radioGroup;
    private RadioButton radioEpisodeWebsite;
    private RadioButton radioMediaFile;
    private Switch switchStartAt;

    public ShareDialog(Context ctx, FeedItem item) {
        this.ctx = ctx;
        this.item = item;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = View.inflate(ctx, R.layout.share_episode_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.share_episode_label);
        builder.setView(content);

        radioGroup = content.findViewById(R.id.share_dialog_radio_group);
        radioEpisodeWebsite = content.findViewById(R.id.share_episode_website_radio);
        radioMediaFile = content.findViewById(R.id.share_media_file_radio);
        switchStartAt = content.findViewById(R.id.share_start_at_timer_dialog);

        setupOptions();

        builder
                .setPositiveButton(R.string.share_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (radioEpisodeWebsite.isChecked()) {
                            if (switchStartAt.isChecked()) {
                                ShareUtils.shareFeedItemLink(ctx, item, true);
                            } else {
                                ShareUtils.shareFeedItemLink(ctx, item);
                            }
                        } else {
                            if (switchStartAt.isChecked()) {
                                ShareUtils.shareFeedItemDownloadLink(ctx, item, true);
                            } else {
                                ShareUtils.shareFeedItemDownloadLink(ctx, item);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
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
    }
}
