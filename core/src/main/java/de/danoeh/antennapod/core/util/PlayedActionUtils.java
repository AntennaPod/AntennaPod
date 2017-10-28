package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.util.Log;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;

public class PlayedActionUtils {
    private static final String TAG = "PlayedActionUtils";

    public static void performPlayedAction(final Context context, final FeedMedia media) {
        final FeedItem item = media.getItem();
        final boolean keep = item.isTagged(FeedItem.TAG_FAVORITE) && UserPreferences.shouldFavoriteKeepEpisode();

        switch (item.getFeed().getPreferences().getCurrentPlayedAction()) {
            case DELETE:
                if (!keep) {
                    Log.d(TAG, "Delete: " + media.toString());
                    DBWriter.deleteFeedMediaOfItem(context, media.getId());
                }
                break;
            case ARCHIVE:
                if (!keep) {
                    Log.d(TAG, "Archive: " + media.toString());
                    DBWriter.archiveFeedMediaOfItem(context, media.getId());
                }
                break;
            case NONE:
                Log.d(TAG, "No action: " + media.toString());
                break;
            default:
                Log.e(TAG, "performPlayedAction: unhandled case = "
                        + media.getItem().getFeed().getPreferences().getCurrentPlayedAction());
                break;
        }
    }
}
