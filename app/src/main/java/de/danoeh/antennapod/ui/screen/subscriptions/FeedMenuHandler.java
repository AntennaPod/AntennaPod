package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.screen.feed.RemoveFeedDialog;
import de.danoeh.antennapod.ui.screen.feed.RenameFeedDialog;
import de.danoeh.antennapod.ui.screen.feed.preferences.TagSettingsDialog;
import de.danoeh.antennapod.ui.share.ShareUtils;

import java.util.Collections;
import java.util.List;

/**
 * Handles interactions with the FeedItemMenu.
 */
public abstract class FeedMenuHandler {
    public static boolean onPrepareMenu(Menu menu, List<Feed> selectedItems) {
        if (menu == null || selectedItems == null || selectedItems.isEmpty() || selectedItems.get(0) == null) {
            return false;
        }
        boolean allSubscribed = true;
        boolean allArchived = true;
        for (Feed feed : selectedItems) {
            if (feed.getState() != Feed.STATE_SUBSCRIBED) {
                allSubscribed = false;
            }
            if (feed.getState() != Feed.STATE_ARCHIVED) {
                allArchived = false;
            }
        }
        setItemVisibility(menu, R.id.remove_all_inbox_item, allSubscribed);
        setItemVisibility(menu, R.id.remove_archive_feed, !allArchived && allSubscribed);
        setItemVisibility(menu, R.id.remove_restore_feed, allArchived);
        boolean singleNonLocalFeedSelected = selectedItems.size() == 1 && !selectedItems.get(0).isLocalFeed();
        setItemVisibility(menu, R.id.share_feed, singleNonLocalFeedSelected);
        return true;
    }

    private static void setItemVisibility(Menu menu, int menuId, boolean visibility) {
        MenuItem item = menu.findItem(menuId);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull Feed selectedFeed) {
        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.rename_folder_item) {
            new RenameFeedDialog(fragment.getActivity(), selectedFeed).show();
        } else if (menuItemId == R.id.remove_all_inbox_item) {
            new FeedMultiSelectActionHandler(fragment.getActivity(), Collections.singletonList(selectedFeed))
                    .handleAction(R.id.remove_all_inbox_item);
        } else if (menuItemId == R.id.edit_tags) {
            TagSettingsDialog.newInstance(Collections.singletonList(selectedFeed.getPreferences()))
                    .show(fragment.getChildFragmentManager(), TagSettingsDialog.TAG);
        } else if (menuItemId == R.id.remove_archive_feed || menuItemId == R.id.remove_restore_feed) {
            new RemoveFeedDialog(Collections.singletonList(selectedFeed))
                    .show(fragment.getChildFragmentManager(), null);
        } else if (menuItemId == R.id.share_feed) {
            ShareUtils.shareFeedLink(context, selectedFeed);
        } else {
            return false;
        }
        return true;
    }
}
