package de.danoeh.antennapod.core.storage;

import android.content.Context;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class APKeepNNewestDownloadAlgorithmTest {

    @Mock
    Context mockContext;
    @Captor
    private ArgumentCaptor<List<FeedItem>> feedItemsCaptor;

    @Spy
    APKeepNNewestDownloadAlgorithm algorithm = new APKeepNNewestDownloadAlgorithm();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        doNothing().when(algorithm).downloadFeedItems(any(), any());
        doReturn(0).when(algorithm).makeRoomForEpisodes(any(), anyInt());
        doReturn(true).when(algorithm).isAutoDownloadEnabled();
        doReturn(true).when(algorithm).isAutoDownloadNetworkAvailable();
        doReturn(true).when(algorithm).isAutoDownloadPowerAvailable(any());
        doReturn(-1).when(algorithm).getEpisodeCacheSize();
        doReturn(true).when(algorithm).isEpisodeCacheUnlimited();
        doReturn(0).when(algorithm).getNumberOfDownloadedEpisodes();
    }

    @Test
    public void notNull() {
        // when
        Runnable result = algorithm.autoDownloadUndownloadedItems(mockContext);

        // then
        assertNotNull(result);
    }

    @Test
    public void downloadsItem() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        assertFalse(feedItemsCaptor.getValue().isEmpty());
    }

    @Test
    public void doesNotDownloadItemIfNotEnabledUpdates() throws Exception {
        doReturn(false).when(algorithm).isAutoDownloadEnabled();

        given()
                .feeds(1)
                .itemsPerFeed(1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void doesNotDownloadItemIfNotOnSuitableNetwork() throws Exception {
        doReturn(false).when(algorithm).isAutoDownloadNetworkAvailable();

        given()
                .feeds(1)
                .itemsPerFeed(1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void doesNotDownloadItemIfNotOnSuitablePower() throws Exception {
        doReturn(false).when(algorithm).isAutoDownloadPowerAvailable(any());

        given()
                .feeds(1)
                .itemsPerFeed(1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void doesNotDownloadAlreadyDownloadedMedia() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(1, (f, i) -> {
                    FeedMedia media = i.getMedia();
                    doReturn(true).when(media).isDownloaded();
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void doesNotDownloadNotAutoDownloadable() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(1, (f, item) -> {
                    doReturn(false).when(item).isAutoDownloadable();
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void nonAutoDownloadableCountsAgainstLimit() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(algorithm.getKeepCount() + 1, (f, item) -> {
                    long id = item.getId();
                    doReturn(id == algorithm.getKeepCount()).when(item).isAutoDownloadable();
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void doesNotDownloadPlayedEpisode() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(algorithm.getKeepCount(), (f, item) -> {
                    long id = item.getId();
                    doReturn(id == 2).when(item).isPlayed();
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        assertThat(feedItemsCaptor.getValue().size(), equalTo(algorithm.getKeepCount() - 1));
    }

    @Test
    public void downloadsAtMostNForFeed() throws Exception {
        given()
                .feeds(1)
                .itemsPerFeed(algorithm.getKeepCount() + 1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        assertThat(feedItemsCaptor.getValue().size(), equalTo(algorithm.getKeepCount()));
    }

    @Test
    public void respectsEpisodeCache() throws Exception {
        final int cacheSize = 1;
        given()
                .episodeCacheSize(cacheSize)
                .feeds(1)
                .itemsPerFeed(algorithm.getKeepCount() + 10)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        assertThat(feedItemsCaptor.getValue().size(), equalTo(cacheSize));
    }

    @Test
    public void checksIfCacheIsAlreadyFull() throws Exception {
        given()
                .episodeCacheSize(1)
                .feeds(1)
                .itemsPerFeed(algorithm.getKeepCount() + 10, (f, i) -> {
                    FeedMedia media = i.getMedia();
                    long id = i.getId();
                    // First item is already in the cache
                    doReturn(id == 0).when(media).isDownloaded();
                })
                .then();

        assertEquals(1, algorithm.getNumberOfDownloadedEpisodes());
        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm, never()).downloadFeedItems(any(), any());
    }

    @Test
    public void downloadsNewestItemFirst() throws Exception {
        final long expectedId = 3;
        given()
                .episodeCacheSize(1)
                .feeds(1)
                .itemsPerFeed(7, (f, item) -> {
                    // Set middle item in the future
                    long id = item.getId();
                    if (id == expectedId) {
                        Date date = item.getPubDate();
                        doReturn(new Date(date.getTime() + 10_000_000L)).when(item).getPubDate();
                    }
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        List<FeedItem> downloadedItems = feedItemsCaptor.getValue();
        assertThat(downloadedItems.size(), is(equalTo(1)));
        assertThat(downloadedItems.get(0).getId(), is(equalTo(expectedId)));
    }

    @Test
    public void downloadsNewestItemAmongSeveralFeedsFirst() throws Exception {
        final long expectedFeedId = 2;
        final long expectedItemId = 3;
        given()
                .episodeCacheSize(1)
                .feeds(3)
                .itemsPerFeed(7, (feed, item) -> {
                    // Set middle item in the future
                    long feedId = feed.getId();
                    long itemId = item.getId();
                    if (itemId == expectedItemId && feedId == expectedFeedId) {
                        Date date = item.getPubDate();
                        doReturn(new Date(date.getTime() + 10_000_000L)).when(item).getPubDate();
                    }
                })
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        List<FeedItem> downloadedItems = feedItemsCaptor.getValue();
        assertThat(downloadedItems.size(), is(equalTo(1)));
        assertThat(downloadedItems.get(0).getId(), is(equalTo(expectedItemId)));
    }

    @Test
    public void downloadsNForAllFeeds() throws Exception {
        final int feedCount = 3;
        given()
                .feeds(feedCount)
                .itemsPerFeed(algorithm.getKeepCount() + 1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        List<FeedItem> downloadedItems = feedItemsCaptor.getValue();
        assertThat(downloadedItems.size(), is(equalTo(feedCount * algorithm.getKeepCount())));
    }

    @Test
    public void makesRoomForEpisodes() throws Exception {
        final int feedCount = 2;
        final int keepCount = algorithm.getKeepCount();
        given()
                .feeds(feedCount)
                .itemsPerFeed(keepCount + 1)
                .then();

        algorithm.autoDownloadUndownloadedItems(mockContext).run();

        InOrder inOrder = inOrder(algorithm);

        inOrder.verify(algorithm).makeRoomForEpisodes(any(), eq(feedCount * keepCount));
        inOrder.verify(algorithm).downloadFeedItems(any(), feedItemsCaptor.capture());

        List<FeedItem> downloadedItems = feedItemsCaptor.getValue();
        assertThat(downloadedItems.size(), is(equalTo(feedCount * keepCount)));
    }

    SetupBuilder given() {
        return new SetupBuilder();
    }

    class SetupBuilder {
        int feedCount = 0;
        int itemCount = 0;
        Consumer<Feed> feedConsumer = (f) -> {
        };
        BiConsumer<Feed, FeedItem> itemConsumer = (f, i) -> {
        };

        SetupBuilder() {
        }

        SetupBuilder episodeCacheSize(int size) {
            doReturn(size).when(algorithm).getEpisodeCacheSize();
            doReturn(size < 0).when(algorithm).isEpisodeCacheUnlimited();

            return this;
        }

        SetupBuilder feeds(int count) {
            return feeds(count, this.feedConsumer);
        }

        SetupBuilder feeds(int count, Consumer<Feed> consumer) {
            this.feedCount = count;
            this.feedConsumer = consumer;

            return this;
        }

        SetupBuilder itemsPerFeed(int count) {
            return itemsPerFeed(count, this.itemConsumer);
        }

        SetupBuilder itemsPerFeed(int count, BiConsumer<Feed, FeedItem> consumer) {
            this.itemCount = count;
            this.itemConsumer = consumer;

            return this;
        }

        void then() {
            List<Feed> feeds = IntStream
                    .range(0, feedCount)
                    .mapToObj(i -> {
                        Feed feed = mock(Feed.class);
                        doReturn((long) i).when(feed).getId();
                        return feed;
                    })
                    .collect(Collectors.toList());

            doReturn(feeds).when(algorithm).getFeedList();

            final long now = new Date().getTime();

            feeds.stream()
                 .forEach(feed -> {
                     feedConsumer.accept(feed);

                     List<FeedItem> items = IntStream
                             .range(0, itemCount)
                             .mapToObj(i -> {
                                 FeedMedia media = mock(FeedMedia.class);
                                 doReturn(false).when(media).isDownloaded();

                                 Date date = new Date(now - i * 10_000L);

                                 FeedItem item = mock(FeedItem.class);

                                 doReturn((long) i).when(item).getId();
                                 doReturn(media).when(item).getMedia();
                                 doReturn(true).when(item).isAutoDownloadable();
                                 doReturn(date).when(item).getPubDate();

                                 itemConsumer.accept(feed, item);

                                 return item;
                             })
                             .collect(Collectors.toList());

                     doReturn(items).when(algorithm).getFeedItemList(feed);
                 });

            int downloadedCount = 0;

            for (Feed feed : feeds) {
                for (FeedItem item : algorithm.getFeedItemList(feed)) {
                    if (item.getMedia() != null && item.getMedia().isDownloaded()) {
                        downloadedCount += 1;
                    }
                }
            }

            doReturn(downloadedCount).when(algorithm).getNumberOfDownloadedEpisodes();
        }
    }
}
