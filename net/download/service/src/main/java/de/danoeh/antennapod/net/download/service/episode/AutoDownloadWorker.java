package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AutoDownloadWorker extends Worker {
    public static final String TAG = "AutoDownloadWorker";

    public AutoDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        List<FeedItem> candidates = new ArrayList<>();

        List<Feed> feeds = DBReader.getFeedList();
        for (Feed feed : feeds) {
            FeedPreferences.AutoDownload autoDownload = feed.getPreferences().getAutoDownload();
            if (autoDownload == FeedPreferences.AutoDownload.GLOBAL) {
                autoDownload = UserPreferences.isEnableAutodownload();
            }
            if (autoDownload != FeedPreferences.AutoDownload.ENABLED) {
                continue;
            }
            List<FeedItem> items = DBReader.getFeedItemList(feed,
                    FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD, 0, 5);
            for (FeedItem newItem : items) {
                if (newItem.isPlayed()) {
                    continue;
                }
                FeedPreferences feedPrefs = newItem.getFeed().getPreferences();
                if (feedPrefs.getFilter().shouldAutoDownload(newItem)) {
                    candidates.add(newItem);
                }
            }
        }

        // TODO: This should be behind a setting
        candidates.addAll(DBReader.getQueue());

        // filter items that are not auto downloadable
        Iterator<FeedItem> it = candidates.iterator();
        int alreadyDownloading = 0;
        while (it.hasNext()) {
            FeedItem item = it.next();
            if (!item.isAutoDownloadEnabled()
                    || item.isDownloaded()
                    || !item.hasMedia()
                    || item.getFeed().isLocalFeed()
                    || item.getFeed().getState() != Feed.STATE_SUBSCRIBED
                    || DownloadServiceInterface.get().isDownloadingEpisode(item.getMedia().getDownloadUrl())) {
                it.remove();
                if (DownloadServiceInterface.get().isDownloadingEpisode(item.getMedia().getDownloadUrl())) {
                    alreadyDownloading++;
                }
            }
        }

        int downloadedEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
        int episodeCacheSize = UserPreferences.getEpisodeCacheSize();
        if (episodeCacheSize == UserPreferences.EPISODE_CACHE_SIZE_UNLIMITED) {
            episodeCacheSize = Integer.MAX_VALUE;
        }
        int episodeSpaceLeft = Math.max(0, episodeCacheSize - (downloadedEpisodes + alreadyDownloading));

        // This decision to download the first ones is arbitrary. The algorithm could do smart things here.
        List<FeedItem> itemsToDownload = candidates.subList(0, episodeSpaceLeft);

        Log.d(TAG, "Enqueueing " + itemsToDownload.size() + " items for automatic download");
        for (FeedItem episode : itemsToDownload) {
            DownloadServiceInterface.get().download(getApplicationContext(), episode);
        }

        return Result.success();
    }
}
