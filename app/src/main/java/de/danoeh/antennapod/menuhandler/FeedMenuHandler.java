package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ShareUtils;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.dialog.IntraFeedSortDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.SortOrder;
import org.apache.commons.lang3.StringUtils;
import android.content.DialogInterface;
import android.annotation.SuppressLint;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameItemDialog;
import de.danoeh.antennapod.dialog.TagSettingsDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
            FeedUpdateManager.runOnceOrAsk(context, selectedFeed);
        } else if (itemId == R.id.refresh_complete_item) {
            new Thread(() -> {
                selectedFeed.setNextPageLink(selectedFeed.getDownload_url());
                selectedFeed.setPageNr(0);
                try {
                    DBWriter.resetPagedFeedPage(selectedFeed).get();
                    FeedUpdateManager.runOnce(context, selectedFeed);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
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

    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull Feed selectedFeed, Runnable callback) {
        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.rename_folder_item) {
            new RenameItemDialog(fragment.getActivity(), selectedFeed).show();
        } else if (menuItemId == R.id.remove_all_inbox_item) {
            ConfirmationDialog dialog = new ConfirmationDialog(fragment.getActivity(),
                    R.string.remove_all_inbox_label,  R.string.remove_all_inbox_confirmation_msg) {
                @Override
                @SuppressLint("CheckResult")
                public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                    clickedDialog.dismiss();
                    Observable.fromCallable((Callable<Future>) () -> DBWriter.removeFeedNewFlag(selectedFeed.getId()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(result -> callback.run(),
                                    error -> Log.e(TAG, Log.getStackTraceString(error)));
                }
            };
            dialog.createNewDialog().show();

        } else if (menuItemId == R.id.edit_tags) {
            TagSettingsDialog.newInstance(Collections.singletonList(selectedFeed.getPreferences()))
                    .show(fragment.getChildFragmentManager(), TagSettingsDialog.TAG);
        } else if (menuItemId == R.id.rename_item) {
            new RenameItemDialog(fragment.getActivity(), selectedFeed).show();
        } else if (menuItemId == R.id.remove_feed) {
            RemoveFeedDialog.show(context, selectedFeed, null);
        } else {
            return false;
        }
        return true;
    }

}
