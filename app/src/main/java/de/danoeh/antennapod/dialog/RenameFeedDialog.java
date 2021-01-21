package de.danoeh.antennapod.dialog;

import android.app.Activity;

import java.lang.ref.WeakReference;

import android.view.View;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;

public class RenameFeedDialog {

    private final WeakReference<Activity> activityRef;
    private final Feed feed;

    public RenameFeedDialog(Activity activity, Feed feed) {
        this.activityRef = new WeakReference<>(activity);
        this.feed = feed;
    }

    public void show() {
        Activity activity = activityRef.get();
        if(activity == null) {
            return;
        }

        View content = View.inflate(activity, R.layout.edit_text_dialog, null);
        EditTextDialogBinding alertViewBinding = EditTextDialogBinding.bind(content);

        alertViewBinding.urlEditText.setText(feed.getTitle());
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setTitle(de.danoeh.antennapod.core.R.string.rename_feed_label)
                .setPositiveButton(android.R.string.ok, (d, input) -> {
                    feed.setCustomTitle(alertViewBinding.urlEditText.getText().toString());
                    DBWriter.setFeedCustomTitle(feed);
                })
                .setNeutralButton(de.danoeh.antennapod.core.R.string.reset, null)
                .setNegativeButton(de.danoeh.antennapod.core.R.string.cancel_label, null)
                .show();

        // To prevent cancelling the dialog on button click
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                (view) -> alertViewBinding.urlEditText.setText(feed.getFeedTitle()));
    }

}
