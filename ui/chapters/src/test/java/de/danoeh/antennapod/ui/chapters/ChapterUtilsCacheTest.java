package de.danoeh.antennapod.ui.chapters;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ChapterUtilsCacheTest {
    private static final String STREAM_URL = "content://de.danoeh.antennapod.test/episode";
    private static final AtomicInteger nextMediaId = new AtomicInteger(1000);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Context context;
    private File fixture;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        fixture = new File(getClass().getClassLoader().getResource("auphonic.mp3").toURI());
    }

    @Test
    public void repeatedLoadsOpenStreamOnce() {
        long id = nextMediaId.getAndIncrement();
        AtomicInteger opens = registerStream(id);

        for (int i = 0; i < 5; i++) {
            FeedMedia media = createMedia(id, null, STREAM_URL + id, 0);
            ChapterUtils.loadChapters(media, context, false);
            assertEquals(4, media.getChapters().size());
        }
        assertEquals(1, opens.get());
    }

    @Test
    public void cachedChaptersAreOnlyReturnedAfterLoading() {
        long id = nextMediaId.getAndIncrement();
        AtomicInteger opens = registerStream(id);
        assertNull(ChapterUtils.getCachedChapters(createMedia(id, null, STREAM_URL + id, 0)));

        ChapterUtils.loadChapters(createMedia(id, null, STREAM_URL + id, 0), context, false);

        assertEquals(4, ChapterUtils.getCachedChapters(createMedia(id, null, STREAM_URL + id, 0)).size());
        assertNull(ChapterUtils.getCachedChapters(createMedia(id, fixture.getAbsolutePath(), STREAM_URL + id, 1)));
        assertEquals(1, opens.get());
    }

    @Test
    public void forceRefreshOpensStreamAgain() {
        long id = nextMediaId.getAndIncrement();
        AtomicInteger opens = registerStream(id);

        ChapterUtils.loadChapters(createMedia(id, null, STREAM_URL + id, 0), context, false);
        FeedMedia refreshed = createMedia(id, null, STREAM_URL + id, 0);
        ChapterUtils.loadChapters(refreshed, context, true);
        assertEquals(4, refreshed.getChapters().size());
        assertEquals(2, opens.get());

        ChapterUtils.loadChapters(createMedia(id, null, STREAM_URL + id, 0), context, false);
        assertEquals(2, opens.get());
    }

    @Test
    public void differentEpisodeIsLoaded() {
        long id1 = nextMediaId.getAndIncrement();
        long id2 = nextMediaId.getAndIncrement();
        AtomicInteger opens1 = registerStream(id1);
        AtomicInteger opens2 = registerStream(id2);

        ChapterUtils.loadChapters(createMedia(id1, null, STREAM_URL + id1, 0), context, false);
        FeedMedia other = createMedia(id2, null, STREAM_URL + id2, 0);
        ChapterUtils.loadChapters(other, context, false);

        assertEquals(4, other.getChapters().size());
        assertEquals(1, opens1.get());
        assertEquals(1, opens2.get());
    }

    @Test
    public void failedLoadIsNotCached() throws Exception {
        long id = nextMediaId.getAndIncrement();
        File localFile = new File(temporaryFolder.getRoot(), "episode.mp3");

        FeedMedia failed = createMedia(id, localFile.getAbsolutePath(), STREAM_URL + id, 1);
        ChapterUtils.loadChapters(failed, context, false);
        assertEquals(0, failed.getChapters().size());

        FileUtils.copyFile(fixture, localFile);
        FeedMedia retried = createMedia(id, localFile.getAbsolutePath(), STREAM_URL + id, 1);
        ChapterUtils.loadChapters(retried, context, false);
        assertEquals(4, retried.getChapters().size());
    }

    private AtomicInteger registerStream(long id) {
        AtomicInteger opens = new AtomicInteger();
        shadowOf(context.getContentResolver()).registerInputStreamSupplier(Uri.parse(STREAM_URL + id), () -> {
            opens.incrementAndGet();
            try {
                return new FileInputStream(fixture);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        });
        return opens;
    }

    private static FeedMedia createMedia(long id, String localFileUrl, String downloadUrl, long downloadDate) {
        FeedItem item = new FeedItem();
        item.setId(id);
        FeedMedia media = new FeedMedia(id, item, 0, 0, 0, "audio/mpeg",
                localFileUrl, downloadUrl, downloadDate, null, 0, 0);
        item.setMedia(media);
        return media;
    }
}
