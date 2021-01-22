package de.danoeh.antennapod.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RemoveFeedDialog {
    private static final String TAG = "RemoveFeedDialog";

    public static void show(Context context, Feed feed, Runnable onSuccess) {
        int messageId = feed.isLocalFeed() ? R.string.feed_delete_confirmation_local_msg
                : R.string.feed_delete_confirmation_msg;
        String message = context.getString(messageId, feed.getTitle());

        ConfirmationDialog dialog = new ConfirmationDialog(context, R.string.remove_feed_label, message) {
            @Override
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();

                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(context.getString(R.string.feed_remover_msg));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.show();

                Completable.fromCallable(() -> DBWriter.deleteFeed(context, feed.getId()).get())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            () -> {
                                Log.d(TAG, "Feed was deleted");
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                                progressDialog.dismiss();
                            }, error -> {
                                Log.e(TAG, Log.getStackTraceString(error));
                                progressDialog.dismiss();
                            });
            }
        };
        dialog.createNewDialog().show();
    }
}
