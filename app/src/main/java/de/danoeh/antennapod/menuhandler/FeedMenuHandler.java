package de.danoeh.antennapod.menuhandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameItemDialog;
import de.danoeh.antennapod.dialog.TagSettingsDialog;
import de.danoeh.antennapod.model.feed.Feed;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Handles interactions with the FeedItemMenu.
 */
public abstract class FeedMenuHandler {
    private static final String TAG = "FeedMenuHandler";

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
