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
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedMenuHandler {
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
        if (selectedFeed.getPaymentLink() != null && selectedFeed.getFlattrStatus().flattrable()) {
            menu.findItem(R.id.support_item).setVisible(true);
        } else {
            menu.findItem(R.id.support_item).setVisible(false);
        }

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
            case R.id.support_item:
                DBTasks.flattrFeedIfLoggedIn(context, selectedFeed);
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

    private static void showFilterDialog(final Context context, final Feed feed) {
        final String[] items = context.getResources().getStringArray(R.array.episode_filter_options);
        final String[] values = context.getResources().getStringArray(R.array.episode_filter_values);
        final boolean[] checkedItems = new boolean[items.length];

        final Set<String> filter = new HashSet<>(Arrays.asList(feed.getItemFilter().getValues()));
        Iterator<String> it = filter.iterator();
        while(it.hasNext()) {
            // make sure we have no empty strings in the filter list
            if(TextUtils.isEmpty(it.next())) {
                it.remove();
            }
        }
        for(int i=0; i < values.length; i++) {
            String value = values[i];
            if(filter.contains(value)) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.filter);
        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                filter.add(values[which]);
            } else {
                filter.remove(values[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            feed.setItemFilter(filter.toArray(new String[filter.size()]));
            DBWriter.setFeedItemsFilter(feed.getId(), filter);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();

    }

}
