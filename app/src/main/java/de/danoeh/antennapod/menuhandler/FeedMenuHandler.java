package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.dialog.FilterDialog;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedMenuHandler {

    private FeedMenuHandler(){ }

    private static final String TAG = "FeedMenuHandler";

    public static boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.feedlist, menu);
        return true;
    }

    public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
        if (selectedFeed == null) {
            return true;
        }

        Log.d(TAG, "Preparing options menu");

        menu.findItem(R.id.refresh_complete_item).setVisible(selectedFeed.isPaged());

        return true;
    }

    /**
     * NOTE: This method does not handle clicks on the 'remove feed' - item.
     *
     * @throws DownloadRequestException
     */
    public static boolean onOptionsItemClicked(final Context context, final MenuItem item,
                                               final Feed selectedFeed) throws DownloadRequestException {
        switch (item.getItemId()) {
            case R.id.refresh_item:
                DBTasks.forceRefreshFeed(context, selectedFeed);
                break;
            case R.id.refresh_complete_item:
                DBTasks.forceRefreshCompleteFeed(context, selectedFeed);
                break;
            case R.id.filter_items:
                showFilterDialog(context, selectedFeed);
                break;
            case R.id.mark_all_read_item:
                ConfirmationDialog conDialog = new ConfirmationDialog(context,
                        R.string.mark_all_read_label,
                        R.string.mark_all_read_feed_confirmation_msg) {

                    @Override
                    public void onConfirmButtonPressed(
                            DialogInterface dialog) {
                        dialog.dismiss();
                        DBWriter.markFeedRead(selectedFeed.getId());
                    }
                };
                conDialog.createNewDialog().show();
                break;
            case R.id.visit_website_item:
                Uri uri = Uri.parse(selectedFeed.getLink());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if(IntentUtils.isCallable(context, intent)) {
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, context.getString(R.string.download_error_malformed_url),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.share_link_item:
                ShareUtils.shareFeedlink(context, selectedFeed);
                break;
            case R.id.share_download_url_item:
                ShareUtils.shareFeedDownloadLink(context, selectedFeed);
                break;
            default:
                return false;
        }
        return true;
    }

    private static void showFilterDialog(Context context, Feed selectedFeed) {
        FilterDialog filterDialog = new FilterDialog(context, selectedFeed.getItemFilter()) {
            @Override
            protected void updateFilter(Set<String> filterValues) {
                selectedFeed.setItemFilter(filterValues.toArray(new String[filterValues.size()]));
                DBWriter.setFeedItemsFilter(selectedFeed.getId(), filterValues);
            }
        };

        filterDialog.openDialog();
    }
}
