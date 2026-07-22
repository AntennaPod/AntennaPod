package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.FeedItem;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ForYouBlenderTest {

    @Test
    public void inProgressEpisodesComeFirstThenFresh() {
        List<FeedItem> result = ForYouBlender.blend(items(1, 2), items(3, 4));
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), ids(result));
    }

    @Test
    public void freshEpisodesAlreadyInProgressAreNotDuplicated() {
        List<FeedItem> result = ForYouBlender.blend(items(1, 2), items(2, 3));
        assertEquals(Arrays.asList(1L, 2L, 3L), ids(result));
    }

    @Test
    public void duplicatesWithinFreshAreRemoved() {
        List<FeedItem> result = ForYouBlender.blend(items(1), items(2, 2, 3));
        assertEquals(Arrays.asList(1L, 2L, 3L), ids(result));
    }

    @Test
    public void emptyInProgressReturnsFreshOnly() {
        List<FeedItem> result = ForYouBlender.blend(Collections.emptyList(), items(5, 6));
        assertEquals(Arrays.asList(5L, 6L), ids(result));
    }

    @Test
    public void emptyFreshReturnsInProgressOnly() {
        List<FeedItem> result = ForYouBlender.blend(items(5, 6), Collections.emptyList());
        assertEquals(Arrays.asList(5L, 6L), ids(result));
    }

    @Test
    public void bothEmptyReturnsEmpty() {
        List<FeedItem> result = ForYouBlender.blend(
                Collections.emptyList(), Collections.emptyList());
        assertEquals(Collections.emptyList(), ids(result));
    }

    private static List<FeedItem> items(long... episodeIds) {
        List<FeedItem> list = new ArrayList<>();
        for (long id : episodeIds) {
            FeedItem item = new FeedItem();
            item.setId(id);
            list.add(item);
        }
        return list;
    }

    private static List<Long> ids(List<FeedItem> items) {
        List<Long> result = new ArrayList<>();
        for (FeedItem item : items) {
            result.add(item.getId());
        }
        return result;
    }
}
