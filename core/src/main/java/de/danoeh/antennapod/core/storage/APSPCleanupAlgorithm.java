package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Implementation of the EpisodeCleanupAlgorithm interface used by AntennaPodSP apps.
 */
public class APSPCleanupAlgorithm implements EpisodeCleanupAlgorithm<Integer> {
    private static final String TAG = "APSPCleanupAlgorithm";

    final int numberOfNewAutomaticallyDownloadedEpisodes;

    public APSPCleanupAlgorithm(int numberOfNewAutomaticallyDownloadedEpisodes) {
        this.numberOfNewAutomaticallyDownloadedEpisodes = numberOfNewAutomaticallyDownloadedEpisodes;
    }

    /**
     * Performs an automatic cleanup. Episodes that have been downloaded first will also be deleted first.
     * The episode that is currently playing as well as the n most recent episodes (the exact value is determined
     * by AppPreferences.numberOfNewAutomaticallyDownloadedEpisodes) will never be deleted.
     *
     * @param context
     * @param episodeSize The maximum amount of space that should be freed by this method
     * @return The number of episodes that have been deleted
     */
    @Override
    public int performCleanup(Context context, Integer episodeSize) {
        Log.i(TAG, String.format("performAutoCleanup(%d)", episodeSize));
        if (episodeSize <= 0) {
            return 0;
        }

        List<FeedItem> candidates = getAutoCleanupCandidates(context);
        List<FeedItem> deleteList = new ArrayList<FeedItem>();
        long deletedEpisodesSize = 0;
        Collections.sort(candidates, new Comparator<FeedItem>() {
            @Override
            public int compare(FeedItem lhs, FeedItem rhs) {
                File lFile = new File(lhs.getMedia().getFile_url());
                File rFile = new File(rhs.getMedia().getFile_url());
                if (!lFile.exists() || !rFile.exists()) {
                    return 0;
                }
                if (FileUtils.isFileOlder(lFile, rFile)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        // listened episodes will be deleted first
        Iterator<FeedItem> it = candidates.iterator();
        if (it.hasNext()) {
            for (FeedItem i = it.next(); it.hasNext() && deletedEpisodesSize <= episodeSize; i = it.next()) {
                if (!i.getMedia().isPlaying() && i.getMedia().getPlaybackCompletionDate() != null) {
                    it.remove();
                    deleteList.add(i);
                    deletedEpisodesSize += i.getMedia().getSize();
                }
            }
        }

        // delete unlistened old episodes if necessary
        it = candidates.iterator();
        if (it.hasNext()) {
            for (FeedItem i = it.next(); it.hasNext() && deletedEpisodesSize <= episodeSize; i = it.next()) {
                if (!i.getMedia().isPlaying()) {
                    it.remove();
                    deleteList.add(i);
                    deletedEpisodesSize += i.getMedia().getSize();
                }
            }
        }
        for (FeedItem item : deleteList) {
            try {
                DBWriter.deleteFeedMediaOfItem(context, item.getMedia().getId()).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, String.format("performAutoCleanup(%d) deleted %d episodes and freed %d bytes of memory",
                episodeSize, deleteList.size(), deletedEpisodesSize));
        return deleteList.size();
    }

    @Override
    public Integer getDefaultCleanupParameter(Context context) {
        return 0;
    }

    @Override
    public Integer getPerformCleanupParameter(Context context, List<FeedItem> items) {
        int episodeSize = 0;
        for (FeedItem item : items) {
            if (item.hasMedia() && !item.getMedia().isDownloaded()) {
                episodeSize += item.getMedia().getSize();
            }
        }
        return episodeSize;
    }

    /**
     * Returns list of FeedItems that have been downloaded, but are not one of the
     * [numberOfNewAutomaticallyDownloadedEpisodes] most recent items.
     */
    private List<FeedItem> getAutoCleanupCandidates(Context context) {
        List<FeedItem> downloaded = new ArrayList<FeedItem>(DBReader.getDownloadedItems(context));
        List<FeedItem> recent = new ArrayList<FeedItem>(DBReader.getRecentlyPublishedEpisodes(context,
                numberOfNewAutomaticallyDownloadedEpisodes));
        for (FeedItem r : recent) {
            if (r.hasMedia() && r.getMedia().isDownloaded()) {
                for (int i = 0; i < downloaded.size(); i++) {
                    if (downloaded.get(i).getId() == r.getId()) {
                        downloaded.remove(i);
                        break;
                    }
                }
            }
        }

        return downloaded;

    }
}
