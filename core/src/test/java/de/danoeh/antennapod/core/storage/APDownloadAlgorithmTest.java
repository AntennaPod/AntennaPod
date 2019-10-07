package de.danoeh.antennapod.core.storage;

import android.content.Context;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final long NotAdl1 = 9; // the constant is not all cap to make test codes look more natural.

    private APDownloadAlgorithm.ItemProvider stubItemProvider;
    private EpisodeCleanupAlgorithm stubCleanupAlgorithm;
    private APDownloadAlgorithm.DownloadPreferences stubDownloadPreferences;

    private Map<Long, Feed> _feeds; // test logic would access with fi() convenience method

    public APDownloadAlgorithmTest() {
        _feeds = createTestFeeds();
    }

    // Convenience method to access test feed items
    private FeedItem fi(long feedId, int itemOffset) {
        return _feeds.get(feedId).getItems().get(itemOffset - 1);
    }

    @Test
    public void episodic_Average_AllAutoDownloadable() {
        withStubs(CACHE_SIZE_DEFAULT,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(3,2), fi(3,3)) // new list, average
        );
        doTest("Average case - download some from new list",
                fis(fi(3,1), fi(2,3)));
    }

    @Test
    public void episodic_Average_SomeNotAutoDownloadable() {
        withStubs(CACHE_SIZE_DEFAULT,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1), fi(NotAdl1,1), fi(2,3), fi(3,2), fi(3,3)) // new list, with item not downloadable
        );
        doTest("Average case - some in new list not auto downloadable",
                fis(fi(3,1), fi(2,3)));
    }

    @Test
    public void episodic_CacheUnlimited() {
        withStubs(CACHE_SIZE_UNLIMITED,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(3,2), fi(3,3)) // new list, average
        );
        doTest("Case unlimited cache - download all from new list",
                fis(fi(3,1), fi(2,3), fi(3,2), fi(3,3)));
    }

    @Test
    public void episodic_NotEnoughInNewList() {
        withStubs(CACHE_SIZE_UNLIMITED,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1)) // new list, too few to fill the queue
        );
        doTest("Case new list is not enough to fill the queue - use only all of the new list",
                fis(fi(3,1)));
    }

    // Run actual test, and comparing the result with the named expected.
    private void doTest(String msg, List<? extends FeedItem> expected) {
        APDownloadAlgorithm algorithm = new APDownloadAlgorithm(
                stubItemProvider, stubCleanupAlgorithm, stubDownloadPreferences);
        List<? extends FeedItem> actual = algorithm.getItemsToDownload(mock(Context.class));

        assertEquals(msg, expected, actual);
    }


    private void withStubs(int cacheSize,
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

    private static Answer<List<? extends FeedItem>> answer(List<? extends FeedItem> result) {
        return invocation -> result;
    }

    private static List<? extends FeedItem> fis(FeedItem... feedItems) {
        return Arrays.asList(feedItems);
    }

    //
    // test feeds generation helpers
    //

    private static Map<Long, Feed> createTestFeeds() {
        final int numFeedItemsPerFeed = 3;

        Map<Long, Feed> feeds = new HashMap<>();
        feeds.put(1L, feedWithItems(1, IN_AUTO_DL, numFeedItemsPerFeed));
        feeds.put(2L, feedWithItems(2, IN_AUTO_DL, numFeedItemsPerFeed));
        feeds.put(3L, feedWithItems(3, IN_AUTO_DL, numFeedItemsPerFeed));
        feeds.put(NotAdl1, feedWithItems(NotAdl1, NOT_AUTO_DL, numFeedItemsPerFeed));

        return feeds;

    }
    private static Feed feedWithItems(long id, boolean isAutoDownload, int numFeedItems) {
        Feed feed = feed(id, isAutoDownload);
        for(int i = 1; i <= numFeedItems; i++) {
            feedItem(feed, 10 * id + i);
        }
        return feed;
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

        feed.getItems().add(item);

        return item;
    }
}
