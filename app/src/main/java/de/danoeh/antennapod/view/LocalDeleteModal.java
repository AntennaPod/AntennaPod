package de.danoeh.antennapod.view;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.ui.i18n.R;
import de.danoeh.antennapod.model.feed.FeedItem;

public class LocalDeleteModal {
    public static void showLocalFeedDeleteWarningIfNecessary(Context context, Iterable<FeedItem> items,
                                                             Runnable deleteCommand) {
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

        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_episode_label)
            .setMessage(R.string.delete_local_feed_warning_body)
            .setPositiveButton(R.string.delete_label, (dialog, which) -> deleteCommand.run())
            .setNegativeButton(R.string.cancel_label, null)
            .show();
    }
}
