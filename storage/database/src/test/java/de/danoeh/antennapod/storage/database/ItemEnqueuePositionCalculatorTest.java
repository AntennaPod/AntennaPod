package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterfaceStub;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences.EnqueueLocation;
import de.danoeh.antennapod.model.playback.Playable;

import static de.danoeh.antennapod.storage.preferences.UserPreferences.EnqueueLocation.AFTER_CURRENTLY_PLAYING;
import static de.danoeh.antennapod.storage.preferences.UserPreferences.EnqueueLocation.BACK;
import static de.danoeh.antennapod.storage.preferences.UserPreferences.EnqueueLocation.FRONT;
import static de.danoeh.antennapod.storage.database.CollectionTestUtil.concat;
import static de.danoeh.antennapod.storage.database.CollectionTestUtil.list;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class ItemEnqueuePositionCalculatorTest {

    @RunWith(Parameterized.class)
    public static class BasicTest {
        @Parameters(name = "{index}: case<{0}>, expected:{1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"case default, i.e., add to the end",
                            concat(QUEUE_DEFAULT_IDS, TFI_ID),
                            BACK, QUEUE_DEFAULT},
                    {"case option enqueue at front",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            FRONT, QUEUE_DEFAULT},
                    {"case empty queue, option default",
                            list(TFI_ID),
                            BACK, QUEUE_EMPTY},
                    {"case empty queue, option enqueue at front",
                            list(TFI_ID),
                            FRONT, QUEUE_EMPTY},
            });
        }

        @Parameter
        public String message;

        @Parameter(1)
        public List<Long> idsExpected;

        @Parameter(2)
        public EnqueueLocation options;

        @Parameter(3)
        public List<FeedItem> curQueue;

        public static final long TFI_ID = 101;

        /**
         * Add a FeedItem with ID {@link #TFI_ID} with the setup
         */
        @Test
        public void test() {
            DownloadServiceInterface.setImpl(new DownloadServiceInterfaceStub());
            ItemEnqueuePositionCalculator calculator = new ItemEnqueuePositionCalculator(options);

            // shallow copy to which the test will add items
            List<FeedItem> queue = new ArrayList<>(curQueue);
            FeedItem tFI = createFeedItem(TFI_ID);
            doAddToQueueAndAssertResult(message,
                    calculator, tFI, queue, getCurrentlyPlaying(),
                    idsExpected);
        }

        Playable getCurrentlyPlaying() {
            return null;
        }
    }

    @RunWith(Parameterized.class)
    public static class AfterCurrentlyPlayingTest extends BasicTest {
        @Parameters(name = "{index}: case<{0}>, expected:{1}")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"case option after currently playing",
                            list(11L, TFI_ID, 12L, 13L, 14L),
                            AFTER_CURRENTLY_PLAYING, QUEUE_DEFAULT, 11L},
                    {"case option after currently playing, currently playing in the middle of the queue",
                            list(11L, 12L, 13L, TFI_ID, 14L),
                            AFTER_CURRENTLY_PLAYING, QUEUE_DEFAULT, 13L},
                    {"case option after currently playing, currently playing is not in queue",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, QUEUE_DEFAULT, 99L},
                    {"case option after currently playing, no currentlyPlaying is null",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, QUEUE_DEFAULT, ID_CURRENTLY_PLAYING_NULL},
                    {"case option after currently playing, currentlyPlaying is not a feedMedia",
                            concat(TFI_ID, QUEUE_DEFAULT_IDS),
                            AFTER_CURRENTLY_PLAYING, QUEUE_DEFAULT, ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA},
                    {"case empty queue, option after currently playing",
                            list(TFI_ID),
                            AFTER_CURRENTLY_PLAYING, QUEUE_EMPTY, ID_CURRENTLY_PLAYING_NULL},
            });
        }

        @Parameter(4)
        public long idCurrentlyPlaying;

        @Override
        Playable getCurrentlyPlaying() {
            return ItemEnqueuePositionCalculatorTest.getCurrentlyPlaying(idCurrentlyPlaying);
        }

        private static final long ID_CURRENTLY_PLAYING_NULL = -1L;
        private static final long ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA = -9999L;

    }

    static void doAddToQueueAndAssertResult(String message,
                                            ItemEnqueuePositionCalculator calculator,
                                            FeedItem itemToAdd,
                                            List<FeedItem> queue,
                                            Playable currentlyPlaying,
                                            List<Long> idsExpected) {
        int posActual = calculator.calcPosition(queue, currentlyPlaying);
        queue.add(posActual, itemToAdd);
        assertEquals(message, idsExpected.size(), queue.size());
        for (int i = 0; i < idsExpected.size(); i++) {
            assertEquals(message, (long) idsExpected.get(i), queue.get(i).getId());
        }
    }

    static final List<FeedItem> QUEUE_EMPTY = Collections.unmodifiableList(emptyList());

    static final List<FeedItem> QUEUE_DEFAULT = 
            Collections.unmodifiableList(Arrays.asList(
                    createFeedItem(11), createFeedItem(12), createFeedItem(13), createFeedItem(14)));
    static final List<Long> QUEUE_DEFAULT_IDS =
            QUEUE_DEFAULT.stream().map(FeedItem::getId).collect(Collectors.toList());


    static Playable getCurrentlyPlaying(long idCurrentlyPlaying) {
        if (ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA == idCurrentlyPlaying) {
            return externalMedia();
        }
        if (ID_CURRENTLY_PLAYING_NULL == idCurrentlyPlaying) {
            return null;
        }
        return createFeedItem(idCurrentlyPlaying).getMedia();
    }

    static Playable externalMedia() {
        return new RemoteMedia(createFeedItem(0));
    }

    static final long ID_CURRENTLY_PLAYING_NULL = -1L;
    static final long ID_CURRENTLY_PLAYING_NOT_FEEDMEDIA = -9999L;


    static FeedItem createFeedItem(long id) {
        Feed feed = new Feed(0, null, "title", "http://example.com", "This is the description",
                "http://example.com/payment", "Daniel", "en", null, "http://example.com/feed",
                "http://example.com/image", null, "http://example.com/feed", System.currentTimeMillis());
        FeedItem item = new FeedItem(id, "Item" + id, "ItemId" + id, "url",
                new Date(), FeedItem.PLAYED, feed);
        FeedMedia media = new FeedMedia(item, "http://download.url.net/" + id, 1234567, "audio/mpeg");
        media.setId(item.getId());
        item.setMedia(media);
        return item;
    }

}
