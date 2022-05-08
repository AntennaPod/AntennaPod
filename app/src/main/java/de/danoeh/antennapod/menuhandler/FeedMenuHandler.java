package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.dialog.IntraFeedSortDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.SortOrder;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedMenuHandler {

    private FeedMenuHandler(){ }

    private static final String TAG = "FeedMenuHandler";

    public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
        if (selectedFeed == null) {
            return true;
        }

        Log.d(TAG, "Preparing options menu");

        menu.findItem(R.id.refresh_complete_item).setVisible(selectedFeed.isPaged());
        if (StringUtils.isBlank(selectedFeed.getLink())) {
            menu.findItem(R.id.visit_website_item).setVisible(false);
        }
        if (selectedFeed.isLocalFeed()) {
            // hide complete submenu "Share..." as both sub menu items are not visible
            menu.findItem(R.id.share_item).setVisible(false);
        }

        return true;
    }

    /**
     * NOTE: This method does not handle clicks on the 'remove feed' - item.
     */
    public static boolean onOptionsItemClicked(final Context context, final MenuItem item, final Feed selectedFeed) {
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            DBTasks.forceRefreshFeed(context, selectedFeed, true);
        } else if (itemId == R.id.refresh_complete_item) {
            DBTasks.forceRefreshCompleteFeed(context, selectedFeed);
        } else if (itemId == R.id.sort_items) {
            showSortDialog(context, selectedFeed);
        } else if (itemId == R.id.visit_website_item) {
            IntentUtils.openInBrowser(context, selectedFeed.getLink());
        } else if (itemId == R.id.share_item) {
            ShareUtils.shareFeedLink(context, selectedFeed);
        } else {
            return false;
        }
        return true;
    }

    private static void showSortDialog(Context context, Feed selectedFeed) {
        IntraFeedSortDialog sortDialog = new IntraFeedSortDialog(context, selectedFeed.getSortOrder(), selectedFeed.isLocalFeed()) {
            @Override
            protected void updateSort(@NonNull SortOrder sortOrder) {
                selectedFeed.setSortOrder(sortOrder);
                DBWriter.setFeedItemSortOrder(selectedFeed.getId(), sortOrder);
            }
        };
        sortDialog.openDialog();
    }

}
