package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.text.InputType;

import java.lang.ref.WeakReference;

import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;

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
        EditText editText = content.findViewById(R.id.text);
        editText.setText(feed.getTitle());
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setTitle(de.danoeh.antennapod.core.R.string.rename_feed_label)
                .setPositiveButton(android.R.string.ok, (d, input) -> {
                    feed.setCustomTitle(editText.getText().toString());
                    DBWriter.setFeedCustomTitle(feed);
                })
                .setNeutralButton(de.danoeh.antennapod.core.R.string.reset, null)
                .setNegativeButton(de.danoeh.antennapod.core.R.string.cancel_label, null)
                .show();

        // To prevent cancelling the dialog on button click
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                (view) -> editText.setText(feed.getFeedTitle()));
    }

}
