package de.danoeh.antennapod.core.storage;

import android.content.Context;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.FeedPreferences.AutoDeleteAction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APDownloadAlgorithmTest {

    private static final boolean IN_AUTO_DL = true;
    private static final boolean NOT_AUTO_DL = false;
    private static final int CACHE_SIZE_UNLIMITED = -1;
    private static final int CACHE_SIZE_DEFAULT = 5;

    private APDownloadAlgorithm.ItemProvider stubItemProvider;
    private EpisodeCleanupAlgorithm stubCleanupAlgorithm;
    private APDownloadAlgorithm.DownloadPreferences stubDownloadPreferences;

    // Setup test data
    private Feed f1 = feed(1, IN_AUTO_DL);
    private Feed f2 = feed(2, IN_AUTO_DL);
    private Feed f3 = feed(3, IN_AUTO_DL);
    private Feed fNotAdl = feed(1, NOT_AUTO_DL);

    private FeedItem fi1_1 = feedItem(f1, 11);
    private FeedItem fi1_2 = feedItem(f1, 12);
    private FeedItem fi1_3 = feedItem(f1, 13);

    private FeedItem fi2_1 = feedItem(f2, 21);
    private FeedItem fi2_2 = feedItem(f2, 22);
    private FeedItem fi2_3 = feedItem(f2, 23);

    private FeedItem fi3_1 = feedItem(f3, 31);
    private FeedItem fi3_2 = feedItem(f3, 32);
    private FeedItem fi3_3 = feedItem(f3, 33);

    private FeedItem fiNotADl_1 = feedItem(fNotAdl, 91);

    @Test
    public void episodic_Average_AllAutoDownloadable() {
        stubs(CACHE_SIZE_DEFAULT,
                fis(fi1_3, fi2_1, fi2_2), // queue
                fis(fi1_1, fi1_2), // played and downloaded
                fis(fi3_1, fi2_3, fi3_2, fi3_3) // new list
        );
        expecting(fis(fi3_1, fi2_3));
    }

    @Test
    public void episodic_Average_SomeNotAutoDownloadable() {
        stubs(CACHE_SIZE_DEFAULT,
                fis(fi1_3, fi2_1, fi2_2), // queue
                fis(fi1_1, fi1_2), // played and downloaded
                fis(fi3_1, fiNotADl_1, fi2_3, fi3_2, fi3_3) // new list
        );
        expecting(fis(fi3_1, fi2_3));
    }

    @Test
    public void episodic_CacheUnlimited() {
        stubs(CACHE_SIZE_UNLIMITED,
                fis(fi1_3, fi2_1, fi2_2), // queue
                fis(fi1_1, fi1_2), // played and downloaded
                fis(fi3_1, fi2_3, fi3_2, fi3_3) // new list
        );
        expecting(fis(fi3_1, fi2_3, fi3_2, fi3_3));
    }

    @Test
    public void episodic_NotEnoughInNewList() {
        stubs(CACHE_SIZE_UNLIMITED,
                fis(fi1_3, fi2_1, fi2_2), // queue
                fis(fi1_1, fi1_2), // played and downloaded
                fis(fi3_1) // new list
        );
        expecting(fis(fi3_1));
    }

    // Run actual test, and comparing the result with the named expected.
    private void expecting(List<? extends FeedItem> expected) {
        APDownloadAlgorithm algorithm = new APDownloadAlgorithm(
                stubItemProvider, stubCleanupAlgorithm, stubDownloadPreferences);
        List<? extends FeedItem> actual = algorithm.getItemsToDownload(mock(Context.class));

        assertEquals(expected, actual);
    }


    //
    // test data generation helpers
    //

    private void stubs(int cacheSize,
                       List<? extends FeedItem> itemsInQueue,
                       List<? extends FeedItem> itemsDownloadedAndPlayed,
                       List<? extends FeedItem> itemsInNewList
    ) {
        // In the test cases, we assume all items in the queue has been downloaded (the typical case)
        for (FeedItem item : itemsInQueue) {
            item.setPlayed(false);
            item.getMedia().setDownloaded(true);
            item.getMedia().setFile_url("file://path/media-" + item.getId());
        }

        for (FeedItem item : itemsDownloadedAndPlayed) {
            item.setPlayed(true);
            item.getMedia().setDownloaded(true);
            item.getMedia().setFile_url("file://path/media-" + item.getId());
        }

        for (FeedItem item : itemsDownloadedAndPlayed) {
            item.setNew();
            item.getMedia().setDownloaded(false);
        }

        List<FeedItem> downloaded = new ArrayList<>();
        downloaded.addAll(itemsDownloadedAndPlayed);
        downloaded.addAll(itemsInQueue);

        stubItemProvider = mock(APDownloadAlgorithm.ItemProvider.class);
        when(stubItemProvider.getQueue()).then(answer(itemsInQueue));
        when(stubItemProvider.getNumberOfDownloadedEpisodes()).thenReturn(downloaded.size());
        when(stubItemProvider.getNewItemsList()).then(answer(itemsInNewList));

        stubCleanupAlgorithm = mock(EpisodeCleanupAlgorithm.class);
        when(stubCleanupAlgorithm.makeRoomForEpisodes(any(Context.class), anyInt()))
                .then(invocation -> {
                    int amountOfRoomNeeded = invocation.getArgumentAt(1, Integer.class);
                    int numItemsToDelete = Math.min(amountOfRoomNeeded, itemsDownloadedAndPlayed.size());
                    for(int i = 0; i < numItemsToDelete; i++) {
                        // here we assume that in the downloaded list
                        // the items played are at the head of the list, that can be cleaned-up
                        downloaded.remove(0);
                    }
                    return numItemsToDelete;
                });


        stubDownloadPreferences = mock(APDownloadAlgorithm.DownloadPreferences.class);
        when(stubDownloadPreferences.getEpisodeCacheSize()).thenReturn(cacheSize);
        when(stubDownloadPreferences.isCacheUnlimited()).thenReturn(cacheSize < 0);
    }


    private static Feed feed(long id, boolean isAutoDownload) {
        FeedFilter filter = new FeedFilter("", "");
        FeedPreferences fPrefs = new FeedPreferences(id, isAutoDownload, AutoDeleteAction.GLOBAL, "", "");
        fPrefs.setFilter(filter);

        Feed feed = FeedMother.anyFeed();
        feed.setId(id);
        feed.setTitle("feed-title-" + id);
        feed.setPreferences(fPrefs);

        return feed;
    }

    private static FeedItem feedItem(Feed feed, long id) {
        FeedItem item = new FeedItem(id, "Item-" + id, "Item", "url", new Date(), FeedItem.UNPLAYED, feed);
        item.setAutoDownload(feed.getPreferences().getAutoDownload());
        FeedMedia media = new FeedMedia(item, "http://example.com/episode", 42, "audio/mp3") {
            // Stub out methods that rely on static UserPreferences
            @Override
            public boolean isPlaying() {
                return false;
            }
        };
        media.setItem(item);
        item.setMedia(media);
        return item;
    }

    private static Answer<List<? extends FeedItem>> answer(List<? extends FeedItem> result) {
        return invocation -> result;
    }

    private static List<? extends FeedItem> fis(FeedItem... feedItems) {
        return Arrays.asList(feedItems);
    }

}

