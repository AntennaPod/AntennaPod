package de.danoeh.antennapod.view;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.ui.i18n.R;
import de.danoeh.antennapod.model.feed.FeedItem;

public class LocalDeleteModal {
    public static void showLocalFeedDeleteWarningIfNecessary(
            Context context,
            Iterable<FeedItem> items,
            Runnable deleteCommand
    ) {
        boolean anyLocalFeed = false;
        for (FeedItem item : items) {
            if (item.getFeed().isLocalFeed()) {
                anyLocalFeed = true;
                break;
            }
        }

        if (!anyLocalFeed) {
            deleteCommand.run();
            return;
        }

        new AlertDialog.Builder(context)
            .setTitle(R.string.delete_local_feed_warning_title)
            .setMessage(R.string.delete_local_feed_warning_body)
            .setPositiveButton(R.string.delete_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteCommand.run();
                }
            })
            .setNegativeButton(R.string.cancel_label, null)
            .show();
    }
}
