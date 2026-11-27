package de.danoeh.antennapod.ui.screen.playback.audio;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class CoverFragmentChaptersTest {

    @Test
    public void reloadingSameEpisodeKeepsLoadedChapters() {
        List<Chapter> chapters = Collections.singletonList(new Chapter(0, "Intro", null, null));
        FeedMedia previous = createMedia(1);
        previous.setChapters(chapters);
        FeedMedia reloaded = createMedia(1);

        CoverFragment.keepLoadedChapters(previous, reloaded);

        assertSame(chapters, reloaded.getChapters());
    }

    @Test
    public void reloadingSameEpisodeKeepsEmptyChapterList() {
        List<Chapter> noChapters = Collections.emptyList();
        FeedMedia previous = createMedia(1);
        previous.setChapters(noChapters);
        FeedMedia reloaded = createMedia(1);

        CoverFragment.keepLoadedChapters(previous, reloaded);

        assertSame(noChapters, reloaded.getChapters());
    }

    @Test
    public void switchingEpisodeDoesNotReuseChapters() {
        FeedMedia previous = createMedia(1);
        previous.setChapters(Collections.singletonList(new Chapter(0, "Intro", null, null)));
        FeedMedia other = createMedia(2);

        CoverFragment.keepLoadedChapters(previous, other);

        assertNull(other.getChapters());
    }

    @Test
    public void nothingLoadedYetLeavesChaptersUnloaded() {
        FeedMedia reloaded = createMedia(1);

        CoverFragment.keepLoadedChapters(null, reloaded);
        CoverFragment.keepLoadedChapters(createMedia(1), reloaded);

        assertNull(reloaded.getChapters());
    }

    private static FeedMedia createMedia(long id) {
        FeedItem item = new FeedItem();
        item.setId(id);
        FeedMedia media = new FeedMedia(item, "https://example.com/episode.mp3", 0, "audio/mpeg");
        media.setId(id);
        item.setMedia(media);
        return media;
    }
}
