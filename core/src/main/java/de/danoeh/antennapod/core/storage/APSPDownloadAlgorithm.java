package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * Implements the automatic download algorithm used by AntennaPodSP apps.
 */
public class APSPDownloadAlgorithm implements AutomaticDownloadAlgorithm {
    private static final String TAG = "APSPDownloadAlgorithm";

    private final int numberOfNewAutomaticallyDownloadedEpisodes;

    public APSPDownloadAlgorithm(int numberOfNewAutomaticallyDownloadedEpisodes) {
        this.numberOfNewAutomaticallyDownloadedEpisodes = numberOfNewAutomaticallyDownloadedEpisodes;
    }

    /**
     * Downloads the most recent episodes automatically. The exact number of
     * episodes that will be downloaded can be set in the AppPreferences.
     *
     * @param context Used for accessing the DB.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    @Override
    public Runnable autoDownloadUndownloadedItems(final Context context, final long... mediaIds) {
        return new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Performing auto-dl of undownloaded episodes");
                if (NetworkUtils.autodownloadNetworkAvailable(context)
                        && UserPreferences.isEnableAutodownload()) {

                    Arrays.sort(mediaIds);
                    List<FeedItem> itemsToDownload = DBReader.getRecentlyPublishedEpisodes(context,
                            numberOfNewAutomaticallyDownloadedEpisodes);
                    Iterator<FeedItem> it = itemsToDownload.iterator();

                    for (FeedItem item = it.next(); it.hasNext(); item = it.next()) {
                        if (!item.hasMedia()
                                || item.getMedia().isDownloaded()
                                || Arrays.binarySearch(mediaIds, item.getMedia().getId()) < 0) {
                            it.remove();
                        }
                    }
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Enqueueing " + itemsToDownload.size()
                                + " items for automatic download");
                    if (!itemsToDownload.isEmpty()) {
                        try {
                            DBTasks.downloadFeedItems(false, context,
                                    itemsToDownload.toArray(new FeedItem[itemsToDownload
                                            .size()]));
                        } catch (DownloadRequestException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }
}
