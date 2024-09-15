package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoDeleteWorker extends Worker {
    public static final String TAG = "AutoDeleteWorker";

    public AutoDeleteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        Log.d(TAG, "Starting auto-deletion");
        List<FeedItem> downloads = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        for (FeedItem item : downloads) {
            if (item.getFeed().isLocalFeed() && !UserPreferences.isAutoDeleteLocal()) {
                continue;
            }
            if (item.isTagged(FeedItem.TAG_FAVORITE) && UserPreferences.shouldFavoriteKeepEpisode()) {
                continue;
            }

            // Auto-delete after playback
            // TODO: Add delayed delete
            FeedPreferences.AutoDeleteAction action = item.getFeed().getPreferences().getCurrentAutoDelete();
            boolean autoDeleteEnabledGlobally = UserPreferences.isAutoDeletePlayed()
                    && (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal());
            boolean shouldAutoDelete = action == FeedPreferences.AutoDeleteAction.ALWAYS
                    || (action == FeedPreferences.AutoDeleteAction.GLOBAL && autoDeleteEnabledGlobally);
            if (UserPreferences.isAutoDeletePlayed() && item.isPlayed() && shouldAutoDelete
                    && item.getMedia().getDownloadDate() < item.getMedia().getLastPlayedTime()) {
                DBWriter.deleteFeedMediaOfItem(getApplicationContext(), item.getMedia());
                continue;
            }

            // Auto-delete after time
            int autoDeleteDays = UserPreferences.isAutoDeleteOldDownloads();
            if (autoDeleteDays > 0 && item.getMedia().getDownloadDate()
                    + TimeUnit.DAYS.toMillis(autoDeleteDays) < System.currentTimeMillis()) {
                DBWriter.deleteFeedMediaOfItem(getApplicationContext(), item.getMedia());
            }
        }
        return Result.success();
    }
}
