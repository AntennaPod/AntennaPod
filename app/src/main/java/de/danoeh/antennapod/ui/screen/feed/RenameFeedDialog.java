package de.danoeh.antennapod.ui.screen.feed;

import android.app.Activity;

import java.lang.ref.WeakReference;

import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.model.feed.FeedPreferences;

public class RenameFeedDialog {

    private final WeakReference<Activity> activityRef;
    private Feed feed = null;
    private NavDrawerData.TagItem tag = null;

    public RenameFeedDialog(Activity activity, Feed feed) {
        this.activityRef = new WeakReference<>(activity);
        this.feed = feed;
    }

    public RenameFeedDialog(Activity activity, NavDrawerData.TagItem drawerItem) {
        this.activityRef = new WeakReference<>(activity);
        this.tag = drawerItem;
    }

    public void show() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        final EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(activity));
        String title = feed != null ? feed.getTitle() : tag.getTitle();

        binding.textInput.setText(title);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setTitle(feed != null ? R.string.rename_feed_label : R.string.rename_tag_label)
                .setPositiveButton(android.R.string.ok, (d, input) -> {
                    String newTitle = binding.textInput.getText().toString();
                    if (feed != null) {
                        feed.setCustomTitle(newTitle);
                        DBWriter.setFeedCustomTitle(feed);
                    } else {
                        renameTag(newTitle);
                    }
                })
                .setNeutralButton(R.string.reset, null)
                .setNegativeButton(R.string.cancel_label, null)
                .show();

        // To prevent cancelling the dialog on button click
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                (view) -> binding.textInput.setText(title));
    }

    private void renameTag(String title) {
        for (Feed feed : tag.getFeeds()) {
            FeedPreferences preferences = feed.getPreferences();
            preferences.getTags().remove(tag.getTitle());
            preferences.getTags().add(title);
            DBWriter.setFeedPreferences(preferences);
        }
    }

}
