package de.danoeh.antennapod.core.storage;

import android.content.Context;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;


public class APKeepNNewestCleanupAlgorithmTest {

    APKeepNNewestCleanupAlgorithm algorithm;
    int keepCount;

    @Before
    public void setup() {
        algorithm = spy(new APKeepNNewestCleanupAlgorithm());

        doReturn(mock(Future.class)).when(algorithm).deleteFeedMediaOfItem(any(), anyLong());

        keepCount = algorithm.getKeepCount();
    }

    @Test
    public void cleansNothingIfNoFeeds() {
        doReturn(emptyList()).when(algorithm).getFeedList();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(0)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(0)));
        verify(algorithm, never()).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void cleansNothingIfNoFeedItems() {
        given()
                .feeds(1)
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(0)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(0)));
        verify(algorithm, never()).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void cleansNothingIfNotTooManyFeedItems() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount)
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(0)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(0)));
        verify(algorithm, never()).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void cleansWhenFeedHasMoreThanNItems() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 1)
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(1)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(1)));
        verify(algorithm, times(1)).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void cleansNullDatedItemsFirst() {
        // An item in the middle
        final long expectedId = 2;

        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 1, (f, item) -> {
                    if (item.getId() == expectedId) {
                        doReturn(null).when(item).getPubDate();
                    }
                })
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(1)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(1)));
        verify(algorithm, times(1)).deleteFeedMediaOfItem(any(), eq(expectedId));
    }

    @Test
    public void cleansOldestItemFirstWhenAllDated() {
        final long expectedId = 2;
        final Date oldestDate = new Date(0);

        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 1, (f, item) -> {
                    if (item.getId() == expectedId) {
                        doReturn(oldestDate).when(item).getPubDate();
                    }
                })
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(1)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(1)));
        verify(algorithm, times(1)).deleteFeedMediaOfItem(any(), eq(expectedId));
    }

    @Test
    public void doesNotCleanFavorite() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 1, (f, item) -> {
                    // oldest item is favorited
                    long id = item.getId();
                    doReturn(id == keepCount).when(item).isTagged(TAG_FAVORITE);
                })
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(0)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(0)));
        verify(algorithm, never()).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void onlyCleansDownloadedItems() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 2, (f, item) -> {
                    // oldest item is not downloaded
                    long id = item.getId();
                    FeedMedia media = item.getMedia();
                    doReturn(id <= keepCount).when(media).isDownloaded();
                })
                .then();


        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(1)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(1)));
        verify(algorithm, times(1)).deleteFeedMediaOfItem(any(), eq((long) keepCount));
    }

    @Test
    public void doesNotCleanPlayingItem() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 1, (f, item) -> {
                    // oldest item is currently playing
                    long id = item.getId();
                    FeedMedia media = item.getMedia();
                    doReturn(id == keepCount).when(media).isPlaying();
                })
                .then();


        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(0)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(0)));
        verify(algorithm, never()).deleteFeedMediaOfItem(any(), anyLong());
    }

    @Test
    public void cleansNoMoreThanRequestedNumber() {
        given()
                .feeds(1)
                .itemsPerFeed(keepCount + 10)
                .then();

        int reclaimable = algorithm.getReclaimableItems();

        assertThat(reclaimable, is(equalTo(10)));

        assertThat(algorithm.performCleanup(mock(Context.class), 1), is(equalTo(1)));
        verify(algorithm, times(1)).deleteFeedMediaOfItem(any(), anyLong());
    }

    /*
      Tests to add:
      Manually downloaded items should not be immediately deleted again
Episode cache should be respected?
     */

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
                                 doReturn((long) i).when(media).getId();
                                 doReturn(true).when(media).isDownloaded();

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
        }
    }
}
