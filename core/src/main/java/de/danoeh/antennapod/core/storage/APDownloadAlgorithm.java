package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.PowerUtils;
import de.danoeh.antennapod.core.util.comparator.FeedItemPubdateComparator;

/**
 * Implements the automatic download algorithm used by AntennaPod. This class assumes that
 * the client uses the APEpisodeCleanupAlgorithm.
 */
public class APDownloadAlgorithm implements AutomaticDownloadAlgorithm {
    private static final String TAG = "APDownloadAlgorithm";

    // Subset of DBReader static methods, for ease of stubbing in tests
    interface DBAccess {
        int getNumberOfDownloadedEpisodes();
        @NonNull EpisodicSerialPair getEpisodicToSerialRatio();
        @NonNull List<? extends FeedItem> getDownloadedItems();
    }

    // Subset of UserPreferences static methods, for ease of stubbing in tests
    interface DownloadPreferences {
        int getEpisodeCacheSize();
        boolean isCacheUnlimited();
    }

    @NonNull
    private final DBAccess dbAccess;

    @NonNull
    private final DownloadItemSelector selectorEpisodic;

    @NonNull
    private final DownloadItemSelector selectorSerial;

    @NonNull
    private final EpisodeCleanupAlgorithm cleanupAlgorithm;
    @NonNull
    private final DownloadPreferences downloadPreferences;

    @VisibleForTesting
    APDownloadAlgorithm(@NonNull DBAccess dbAccess,
                        @NonNull DownloadItemSelector selectorEpisodic,
                        @NonNull DownloadItemSelector selectorSerial,
                        @NonNull EpisodeCleanupAlgorithm cleanupAlgorithm,
                        @NonNull DownloadPreferences downloadPreferences) {
        this.dbAccess = dbAccess;
        this.selectorEpisodic = selectorEpisodic;
        this.selectorSerial = selectorSerial;
        this.cleanupAlgorithm = cleanupAlgorithm;
        this.downloadPreferences = downloadPreferences;
    }

    public APDownloadAlgorithm() {
        this(new DBAccessDefaultImpl()
                , new DownloadItemSelectorEpisodicImpl()
                , new DownloadItemSelectorSerialImpl()
                , UserPreferences.getEpisodeCleanupAlgorithm()
                , new DownloadPreferencesDefaultImpl());
    }

    /**
     * Looks for undownloaded episodes in the queue or list of new items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    @Override
    public Runnable autoDownloadUndownloadedItems(final Context context) {
        return () -> {

            // true if we should auto download based on network status
            boolean networkShouldAutoDl = NetworkUtils.autodownloadNetworkAvailable()
                    && UserPreferences.isEnableAutodownload();

            // true if we should auto download based on power status
            boolean powerShouldAutoDl = PowerUtils.deviceCharging(context)
                    || UserPreferences.isEnableAutodownloadOnBattery();

            // we should only auto download if both network AND power are happy
            if (networkShouldAutoDl && powerShouldAutoDl) {

                Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                List<? extends FeedItem> itemsToDownloadList = getItemsToDownload(context);

                FeedItem[] itemsToDownload = itemsToDownloadList
                        .toArray(new FeedItem[0]);

                Log.d(TAG, "Enqueueing " + itemsToDownload.length + " items for download");

                try {
                    DBTasks.downloadFeedItems(false, context, itemsToDownload);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    @VisibleForTesting
    @NonNull
    List<? extends FeedItem> getItemsToDownload(@NonNull Context context) {
        List<? extends FeedItem> candidatesEpisodic =
                selectorEpisodic.getAutoDownloadableEpisodes();
        Log.v(TAG, "num. of episodic candidates: " + candidatesEpisodic.size());
        List<? extends FeedItem> candidatesSerial =
                selectorSerial.getAutoDownloadableEpisodes();
        Log.v(TAG, "num. of serial candidates: " + candidatesSerial.size());

        int autoDownloadableEpisodes = candidatesEpisodic.size() + candidatesSerial.size();
        int downloadedEpisodes = dbAccess.getNumberOfDownloadedEpisodes();
        int deletedEpisodes = cleanupAlgorithm.makeRoomForEpisodes(context, autoDownloadableEpisodes);
        boolean cacheIsUnlimited = downloadPreferences.isCacheUnlimited();
        int episodeCacheSize = downloadPreferences.getEpisodeCacheSize();

        List<FeedItem> candidates;
        if (cacheIsUnlimited ||
                episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
            candidates = new ArrayList<>(autoDownloadableEpisodes);
            candidates.addAll(candidatesEpisodic);
            candidates.addAll(candidatesSerial);
        } else {
            // determine the space allocated for episodic and serial feeds
            EpisodicSerialPair episodeSpaceLeft =
                    calcEpisodeSpaceLeftEpisodicAndSerial(candidatesEpisodic.size(), candidatesSerial.size());
            Log.v(TAG, "num. items to download: " + episodeSpaceLeft);

            candidates = new ArrayList<>(episodeSpaceLeft.episodic + episodeSpaceLeft.serial);
            candidates.addAll(candidatesEpisodic.subList(0, episodeSpaceLeft.episodic));
            candidates.addAll(candidatesSerial.subList(0, episodeSpaceLeft.serial));
        }

        // sort them by pubDate descending, to avoid the serial ones being at the back of the download
        // all the time (they are most likely still at the back though, as they tend to have older pubDates)
        Collections.sort(candidates, new FeedItemPubdateComparator());

        return candidates;
    }

    /**
     * Divide the space available between episodic and serial, based on:
     * - ratio between the number of episodic feeds and serial feeds
     * - maintain the ratio among all the downloaded
     */
    private EpisodicSerialPair calcEpisodeSpaceLeftEpisodicAndSerial(int numDownloadablesEpisodic, int numDownloadablesSerial) {
        EpisodicSerialPair episodicToSerialTarget = dbAccess.getEpisodicToSerialRatio()
                .times(downloadPreferences.getEpisodeCacheSize());
        Log.v(TAG, "Episodic to Serial Target: " + episodicToSerialTarget);

        EpisodicSerialPair episodicToSerialDownloaded;
        {
            List<? extends FeedItem> downloaded = dbAccess.getDownloadedItems();
            int numEpisodic = 0;
            for (FeedItem item : downloaded) {
                if (SemanticType.EPISODIC == item.getFeed().getPreferences().getSemanticType()) {
                    numEpisodic++;
                }
            }
            episodicToSerialDownloaded = new EpisodicSerialPair(numEpisodic, downloaded.size() - numEpisodic);
        }

        EpisodicSerialPair episodeSpaceLeftInitial = episodicToSerialTarget.minus(episodicToSerialDownloaded);

        // if there aren't enough downloadables, reduce the spaceLeft.
        EpisodicSerialPair episodeSpaceLeft = episodeSpaceLeftInitial.min(
                new EpisodicSerialPair(numDownloadablesEpisodic, numDownloadablesSerial));

        // after the above reduction (if any),
        // allocate the freed space in, say, serial to episodic (and vice versa) to fill up the cache
        if (episodeSpaceLeft.serial < episodeSpaceLeftInitial.serial) {
            int freeSpace = episodeSpaceLeftInitial.serial - episodeSpaceLeft.serial;
            int newSpaceLeftForEpisodic = Math.min(episodeSpaceLeft.episodic + freeSpace,
                    numDownloadablesEpisodic);
            episodeSpaceLeft = new EpisodicSerialPair(newSpaceLeftForEpisodic, episodeSpaceLeft.serial);
        }
        if (episodeSpaceLeft.episodic < episodeSpaceLeftInitial.episodic) {
            int freeSpace = episodeSpaceLeftInitial.episodic - episodeSpaceLeft.episodic;
            int newSpaceLeftForSerial = Math.min(episodeSpaceLeft.serial + freeSpace,
                    numDownloadablesSerial);
            episodeSpaceLeft = new EpisodicSerialPair(episodeSpaceLeft.episodic, newSpaceLeftForSerial);
        }

        return episodeSpaceLeft;
    }

    @VisibleForTesting
    static class EpisodicSerialPair {
        public final int episodic;
        public final int serial;

        public EpisodicSerialPair(int episodic, int serial) {
            if (episodic < 0 || serial < 0) {
                throw new IllegalArgumentException("EpisodicSerialPair() the numbers must be >= 0. Arguments: "
                        + toString(episodic, serial));
            }
            this.episodic = episodic;
            this.serial = serial;
        }

        public EpisodicSerialPair minus(@NonNull EpisodicSerialPair other) {
            return new EpisodicSerialPair(
                    Math.max(0, this.episodic - other.episodic),
                    Math.max(0, this.serial - other.serial)
            );
        }

        /**
         * Break the supplied number according to the episodic : serial ratio.
         */
        public EpisodicSerialPair times(int number) {
            if (number < 0) {
                throw new IllegalArgumentException("Argument number must be >= 0. Actual: " + number);
            }
            int total = episodic + serial;

            int newEpisodic = Math.round((number * this.episodic + 0f) / total);

            // boundary case handling
            // - #serial rounded to 0: make it 1
            if (newEpisodic == number && serial > 0) {
                newEpisodic = number - 1;
            }
            // - #episodic rounded to 0: make it 1
            if (newEpisodic == 0 && episodic > 0) {
                newEpisodic = 1;
            }

            return new EpisodicSerialPair(newEpisodic, number - newEpisodic);
        }

        public EpisodicSerialPair min(EpisodicSerialPair other) {
            return new EpisodicSerialPair(Math.min(this.episodic, other.episodic),
                    Math.min(this.serial, other.serial));
        }

        @NonNull
        @Override
        public String toString() {
            return toString(episodic, serial);
        }

        @NonNull
        private static String toString(int episodic, int serial) {
            return "{episodic=" + episodic +
                    ", serial=" + serial +
                    '}';
        }
    }

    private static class DBAccessDefaultImpl implements DBAccess {
        @Override
        public int getNumberOfDownloadedEpisodes() {
            return DBReader.getNumberOfDownloadedEpisodes();
        }

        @NonNull
        @Override
        public List<? extends FeedItem> getDownloadedItems() {
            return DBReader.getDownloadedItems();
        }

        @NonNull
        @Override
        public EpisodicSerialPair getEpisodicToSerialRatio() {
            Pair<Integer, Integer> result = DBReader.getFeedEpisodicToSerialRatio();
            return new EpisodicSerialPair(result.first, result.second);
        }
    }

    private static class DownloadPreferencesDefaultImpl implements DownloadPreferences {
        @Override
        public int getEpisodeCacheSize() {
            return UserPreferences.getEpisodeCacheSize();
        }

        @Override
        public boolean isCacheUnlimited() {
            return UserPreferences.getEpisodeCacheSize() == UserPreferences
                    .getEpisodeCacheSizeUnlimited();
        }
    }
}
