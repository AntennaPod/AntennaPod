package de.danoeh.antennapod.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.core.storage.DBWriter;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RemoveFeedDialog {
    private static final String TAG = "RemoveFeedDialog";

    public static void show(Context context, Feed feed, Runnable onSuccess) {
        List<Feed> feeds = Collections.singletonList(feed);
        String message = getMessageId(context, feeds);
        showDialog(context, feeds, message, onSuccess);
    }

    public static void show(Context context, List<Feed> feeds, Runnable onSuccess) {
        String message = getMessageId(context, feeds);
        showDialog(context, feeds, message, onSuccess);
    }

    private static void showDialog(Context context, List<Feed> feeds, String message, Runnable onSuccess) {
        ConfirmationDialog dialog = new ConfirmationDialog(context, R.string.remove_feed_label, message) {
            @Override
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();

                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(context.getString(R.string.feed_remover_msg));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.show();

                Completable.fromCallable(() -> {
                    for (Feed feed : feeds) {
                        DBWriter.deleteFeed(context, feed.getId()).get();
                    }
                    return null;
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            () -> {
                                Log.d(TAG, "Feed(s) deleted");
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

    private static String getMessageId(Context context, List<Feed> feeds) {
        if (feeds.size() == 1) {
            if (feeds.get(0).isLocalFeed()) {
                return context.getString(R.string.feed_delete_confirmation_local_msg, feeds.get(0).getTitle());
            } else {
                return context.getString(R.string.feed_delete_confirmation_msg, feeds.get(0).getTitle());
            }
        } else {
            return context.getString(R.string.feed_delete_confirmation_msg_batch);
        }

    }
}
