package de.danoeh.antennapod.core.storage;

import android.content.Context;

import androidx.collection.ArraySet;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedMother;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.FeedPreferences.AutoDeleteAction;
import de.danoeh.antennapod.core.feed.FeedPreferences.SemanticType;
import de.danoeh.antennapod.core.storage.APDownloadAlgorithm.EpisodicSerialPair;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APDownloadAlgorithmTest {

    private static final boolean IN_AUTO_DL = true;
    private static final boolean NOT_AUTO_DL = false;
    private static final int CACHE_SIZE_UNLIMITED = -1;
    private static final int CACHE_SIZE_5 = 5;

    private APDownloadAlgorithm.DBAccess stubDBAccess;
    private DownloadItemSelector stubSelectorEpisodic;
    private DownloadItemSelector stubSelectorSerial;
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
    public void episodic_Average() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(3,2), fi(3,3)) // new list, average
        );
        doTest("Average case - download some from new list",
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

    @Test
    public void episodic_Boundary_DownloadedMoreThanTarget() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2), fi(3,1)), // played and downloaded, average
                fis(fi(2,3), fi(3,2)),  // new list, average
                0 // don't cleanup any of the downloaded
        );
        doTest("Case num. of downloaded (6) is more than the target(5)",
                fis());
    }

    @Test
    public void mixed_Average_1SerialFeed() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1), fi(2,2)), // queue, average
                fis(fi(1,1), fi(1,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(3,2), fi(100,2), fi(3,3), fi(100,1)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:1, thus episode cache allocation is 4:1
        // the serial item fi(100,1) has older pubDate, so it is ahead in the to download list
        doTest("Mixed Average case - 1 serial feed - download both episodic and serial ones",
                fis(fi(3,1), fi(100,1)));
    }

    @Test
    public void mixed_Average_MultipleSerialFeeds() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1)), // queue, average
                fis(fi(1,1), fi(100,1), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(100,2), fi(3,3), fi(200,1), fi(1,2), fi(300,1)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:3, thus episode cache allocation is 3:2 (tiebreak to episodic)
        // output:
        // 1. choose the oldest ones (thus fi(100,2 is cut)
        // 2. but they are sorted with pubDate descending
        doTest("Mixed Average case - 3 serial feeds - download serial ones; no space left for episodic",
                fis(fi(3,1), fi(200,1), fi(300,1)));
    }

    @Test
    public void mixed_Average_CacheUnlimited() {
        withStubs(CACHE_SIZE_UNLIMITED,
                fis(fi(1,3), fi(2,1)), // queue, average
                fis(fi(1,1), fi(100,1), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(100,2), fi(3,3), fi(200,1), fi(1,2), fi(300,1)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:3, thus episode cache allocation is 3:2 (tiebreak to episodic)
        // output:
        // 1. choose the oldest ones (thus fi(100,2 is cut)
        // 2. but they are sorted with pubDate descending
        doTest("Mixed , unlimited cache - download all",
                fis(fi(3,1), fi(2,3), fi(100,2), fi(3,3), fi(200,1), fi(1,2), fi(300,1)));
    }

    @Test
    public void mixed_Boundary_StillDownloadSerialFeed_RatioNearZero() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1)), // queue, average
                fis(fi(1,1), fi(100,1), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(100,3), fi(4,1), fi(5,1), fi(6,1), fi(7,1), fi(8,1),
                        fi(9,1), fi(10,1), fi(11,1), fi(100,2)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 11:1, thus episode cache allocation is 4:1
        // (simple rounded would make the allocation be 5:0, but we won't do it)
        // output:
        // 1. choose the oldest ones (thus fi(100,2 is cut)
        // 2. but they are sorted with pubDate descending
        doTest("Mixed Boundary case - still allocate 1 slot for serial when simple rounded ratio would make it 0",
                fis(fi(3,1), fi(2,3), fi(100,2)));
    }

    @Test
    public void mixed_Boundary_NotEnoughSerial() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(2,1)), // queue, average
                fis(fi(1,1), fi(100,1), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(2,3), fi(200,1), fi(3,3), fi(1,2)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:2,
        // thus episode cache allocation is 3:2, space left should be 1:2
        // but there is only 1 serial feedItem auto-downloadable
        doTest("Mixed Boundary case - not enough serial items to download, extra space used by episodic",
                fis(fi(3,1), fi(2,3), fi(200,1)));
    }

    @Test
    public void mixed_Boundary_NotEnoughEpisodic() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3), fi(100,2)), // queue, average
                fis(fi(1,1), fi(100,1), fi(2,2)), // played and downloaded, average
                fis(fi(200,3), fi(200,2), fi(3,1), fi(200,1)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:2,
        // thus episode cache allocation is 3:2, space left should be 2:1
        // but there is only 1 episodic feedItem auto-downloadable
        doTest("Mixed Boundary case - not enough episodic items to download, extra space used by serial",
                fis(fi(200,2), fi(3,1), fi(200,1)));
    }

    @Test
    public void mixed_Boundary_NotEnoughBoth() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3)), // queue, average
                fis(fi(1,1), fi(100,1), fi(100,2), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(200,1)) // auto-Dl items (episodic and serial)
        );
        // episodic to serial feed ratio = 3:2,
        // thus episode cache allocation is 3:2, space left should be 2:2
        // but there is only 1 feedItem for episodic and serial respectively
        doTest("Mixed Boundary case - not enough items to download, extra space left unused",
                fis(fi(3,1), fi(200,1)));
    }

    @Test
    public void mixed_Boundary_DownloadedMoreThanTarget_Both() {
        withStubs(CACHE_SIZE_5,
                fis(fi(1,3)), // queue, average
                fis(fi(1,1), fi(100,1), fi(100,2), fi(100,3), fi(1,2), fi(2,2)), // played and downloaded, average
                fis(fi(3,1), fi(200,1)), // auto-Dl items (episodic and serial)
                0 // don't cleanup any of the downloaded
        );
        // episodic to serial feed ratio = 3:2, episode cache target allocation is 3:2,
        // space left is 0 however.
        // ensure the calculation does not become negative
        // - episodic (target is 3, but 4 have been downloaded)
        // - serial (target is 2, but 3 have been downloaded)
        doTest("Mixed Boundary case - more downloaded than target - both",
                fis());
    }

    // Run actual test, and comparing the result with the named expected.
    private void doTest(String msg, List<? extends FeedItem> expected) {
        APDownloadAlgorithm algorithm = new APDownloadAlgorithm(
                stubDBAccess, stubSelectorEpisodic, stubSelectorSerial, stubCleanupAlgorithm, stubDownloadPreferences);
        List<? extends FeedItem> actual = algorithm.getItemsToDownload(mock(Context.class));

        assertEquals(msg, expected, actual);
    }


    private void withStubs(int cacheSize,
                           List<? extends FeedItem> itemsInQueue,
                           List<? extends FeedItem> itemsDownloadedAndPlayed,
                           List<? extends FeedItem> itemsAutoDownloadablePubDateDesc
    ) {
        withStubs(cacheSize, itemsInQueue, itemsDownloadedAndPlayed, itemsAutoDownloadablePubDateDesc,
                Integer.MAX_VALUE);
    }

    private void withStubs(int cacheSize,
                           List<? extends FeedItem> itemsInQueue,
                           List<? extends FeedItem> itemsDownloadedAndPlayed,
                           List<? extends FeedItem> itemsAutoDownloadablePubDateDesc,
                           int maxNumToCleanup
    ) {
        Set<Feed> feedSet = new ArraySet<>();

        // In the test cases, we assume all items in the queue has been downloaded (the typical case)
        for (FeedItem item : itemsInQueue) {
            item.setPlayed(false);
            item.getMedia().setDownloaded(true);
            item.getMedia().setFile_url("file://path/media-" + item.getId());
            feedSet.add(item.getFeed());
        }

        for (FeedItem item : itemsDownloadedAndPlayed) {
            item.setPlayed(true);
            item.getMedia().setDownloaded(true);
            item.getMedia().setFile_url("file://path/media-" + item.getId());
            feedSet.add(item.getFeed());
        }

        List<FeedItem> itemsAutoDownloadableEpisodic = new ArrayList<>();
        List<FeedItem> itemsAutoDownloadableSerial = new ArrayList<>();
        final long pubDateBase = System.currentTimeMillis();
        for (int i = 0; i < itemsAutoDownloadablePubDateDesc.size(); i++) {
            final FeedItem item = itemsAutoDownloadablePubDateDesc.get(i);
            item.getMedia().setDownloaded(false);
            item.setPubDate(new Date(pubDateBase - i * 1000)); // descending pubDate in the list
            if (SemanticType.EPISODIC == item.getFeed().getPreferences().getSemanticType()) {
                item.setNew();
                itemsAutoDownloadableEpisodic.add(item);
            } else {
                item.setPlayed(false); // set to new is okay too
                itemsAutoDownloadableSerial.add(item);
            }
            feedSet.add(item.getFeed());
        }
        Collections.reverse(itemsAutoDownloadableSerial); // for serial, oldest ones come first

        EpisodicSerialPair episodicSerialFeedRatio;
        {
            int numEpisodic = 0;
            for (Feed feed : feedSet) {
                if (SemanticType.EPISODIC == feed.getPreferences().getSemanticType()) {
                    numEpisodic++;
                }
            }
            episodicSerialFeedRatio = new EpisodicSerialPair(numEpisodic, feedSet.size() - numEpisodic);
        }

        List<FeedItem> downloaded = new ArrayList<>();
        downloaded.addAll(itemsDownloadedAndPlayed);
        downloaded.addAll(itemsInQueue);

        stubDBAccess = mock(APDownloadAlgorithm.DBAccess.class);
        when(stubDBAccess.getNumberOfDownloadedEpisodes()).thenReturn(downloaded.size());
        when(stubDBAccess.getDownloadedItems()).then(answer(downloaded));
        when(stubDBAccess.getEpisodicToSerialRatio()).thenReturn(episodicSerialFeedRatio);

        stubSelectorEpisodic = mock(DownloadItemSelector.class);

        when(stubSelectorEpisodic.getAutoDownloadableEpisodes()).then(answer(itemsAutoDownloadableEpisodic));

        stubSelectorSerial = mock(DownloadItemSelector.class);
        when(stubSelectorSerial.getAutoDownloadableEpisodes()).then(answer(itemsAutoDownloadableSerial));

        stubCleanupAlgorithm = mock(EpisodeCleanupAlgorithm.class);
        when(stubCleanupAlgorithm.makeRoomForEpisodes(any(Context.class), anyInt()))
                .then(invocation -> {
                    int amountOfRoomNeeded = invocation.getArgumentAt(1, Integer.class);
                    int numItemsToDelete = Math.min(maxNumToCleanup,
                            Math.min(amountOfRoomNeeded, itemsDownloadedAndPlayed.size()));
                    for (int i = 0; i < numItemsToDelete; i++) {
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
        for (int i = 1; i <= 11; i++) {
            feeds.put((long)i, feedWithItems(i, SemanticType.EPISODIC, IN_AUTO_DL, numFeedItemsPerFeed));
        }

        // convention: feed ids of 100, 200, etc. are serial feeds
        for (int i = 1; i <= 11; i++) {
            int feedId = i * 100;
            feeds.put((long)feedId, feedWithItems(feedId, SemanticType.SERIAL, IN_AUTO_DL, numFeedItemsPerFeed));
        }

        return feeds;
    }

    private static Feed feedWithItems(long id, SemanticType semanticType, boolean isAutoDownload, int numFeedItems) {
        Feed feed = feed(id, semanticType, isAutoDownload);
        for (int i = 1; i <= numFeedItems; i++) {
            feedItem(feed, 10 * id + i);
        }
        return feed;
    }

    private static Feed feed(long id, SemanticType semanticType, boolean isAutoDownload) {
        FeedFilter filter = new FeedFilter("", "");
        FeedPreferences fPrefs = new FeedPreferences(id, isAutoDownload, AutoDeleteAction.GLOBAL, "", "");
        fPrefs.setFilter(filter);
        fPrefs.setSemanticType(semanticType);

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
