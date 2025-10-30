package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.DialogInterface;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.screen.feed.RenameFeedDialog;

public class TagMenuHandler {
    private TagMenuHandler() {
    }

    public static boolean onMenuItemClicked(@NonNull Fragment fragment, NavDrawerData.TagItem selectedTag,
                                            MenuItem item, SubscriptionTagAdapter tagAdapter) {
        int itemId = item.getItemId();
        if (itemId == R.id.rename_folder_item) {
            new RenameFeedDialog(fragment.getActivity(), selectedTag).show();
            return true;
        } else if (itemId == R.id.delete_folder_item) {
            ConfirmationDialog dialog = new ConfirmationDialog(fragment.getContext(), R.string.delete_tag_label,
                    fragment.getString(R.string.delete_tag_confirmation, selectedTag.getTitle())) {

                @Override
                public void onConfirmButtonPressed(DialogInterface dialog) {
                    tagAdapter.setSelectedTag(FeedPreferences.TAG_ROOT);
                    for (Feed feed : selectedTag.getFeeds()) {
                        FeedPreferences preferences = feed.getPreferences();
                        preferences.getTags().remove(selectedTag.getTitle());
                        DBWriter.setFeedPreferences(preferences);
                    }
                }
            };
            dialog.createNewDialog().show();
            return true;
        }
        return false;
    }
}
