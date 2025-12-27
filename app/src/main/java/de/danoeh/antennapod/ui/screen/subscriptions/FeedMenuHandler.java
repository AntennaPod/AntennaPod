package de.danoeh.antennapod.ui.screen.subscriptions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.screen.feed.RemoveFeedDialog;
import de.danoeh.antennapod.ui.screen.feed.RenameFeedDialog;
import de.danoeh.antennapod.ui.screen.feed.preferences.TagSettingsDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.share.ShareUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Handles interactions with the FeedItemMenu.
 */
public abstract class FeedMenuHandler {
    private static final String TAG = "FeedMenuHandler";

    public static boolean onPrepareMenu(Menu menu, List<Feed> selectedItems) {
        if (menu == null || selectedItems == null || selectedItems.isEmpty() || selectedItems.get(0) == null) {
            return false;
        }
        boolean allNotSubscribed = true;
        boolean allArchived = true;
        for (Feed feed : selectedItems) {
            if (feed.getState() != Feed.STATE_NOT_SUBSCRIBED) {
                allNotSubscribed = false;
            }
            if (feed.getState() != Feed.STATE_ARCHIVED) {
                allArchived = false;
            }
        }
        if (allNotSubscribed) {
            setItemVisibility(menu, R.id.remove_archive_feed, false);
            setItemVisibility(menu, R.id.remove_all_inbox_item, false);
        }
        setItemVisibility(menu, R.id.remove_archive_feed, !allArchived);
        setItemVisibility(menu, R.id.remove_restore_feed, allArchived);
        if (selectedItems.size() != 1 || selectedItems.get(0).isLocalFeed()) {
            setItemVisibility(menu, R.id.share_feed, false);
        }
        return true;
    }

    private static void setItemVisibility(Menu menu, int menuId, boolean visibility) {
        MenuItem item = menu.findItem(menuId);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull Feed selectedFeed, @Nullable Runnable removeFromInboxCallback) {
        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.rename_folder_item) {
            new RenameFeedDialog(fragment.getActivity(), selectedFeed).show();
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
                            .subscribe(result -> {
                                if (removeFromInboxCallback != null) {
                                    removeFromInboxCallback.run();
                                }
                            }, error -> Log.e(TAG, Log.getStackTraceString(error)));
                }
            };
            dialog.createNewDialog().show();

        } else if (menuItemId == R.id.edit_tags) {
            TagSettingsDialog.newInstance(Collections.singletonList(selectedFeed.getPreferences()))
                    .show(fragment.getChildFragmentManager(), TagSettingsDialog.TAG);
        } else if (menuItemId == R.id.rename_item) {
            new RenameFeedDialog(fragment.getActivity(), selectedFeed).show();
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
