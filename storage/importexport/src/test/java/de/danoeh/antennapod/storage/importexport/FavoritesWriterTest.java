package de.danoeh.antennapod.storage.importexport;

import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FavoritesWriterTest {

    @Test
    public void testFeedWithoutLinkAndImage() throws Exception {
        Feed feed = new Feed(1, null, "Feed One", null, null, null, null, null, null, null, null, null,
                "https://example.com/feed.xml", 0);
        FeedItem item = new FeedItem(1, "Episode One", "id1", null, new Date(), FeedItem.PLAYED, feed);
        item.setFeedId(1);
        StringWriter writer = new StringWriter();
        FavoritesWriter.writeDocument(Collections.singletonList(item), writer,
                ApplicationProvider.getApplicationContext());

        String result = writer.toString();
        assertTrue(result.contains("Feed One"));
        assertTrue(result.contains("Episode One"));
        assertTrue(result.contains("https://example.com/feed.xml"));
    }
}
