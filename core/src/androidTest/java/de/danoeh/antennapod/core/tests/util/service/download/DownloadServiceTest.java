package de.danoeh.antennapod.core.tests.util.service.download;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.service.download.DownloadService;

public class DownloadServiceTest extends AndroidTestCase {

    public void testRemoveDuplicateImages() {
        List<FeedItem> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            FeedItem item = new FeedItem();
            String url = (i % 5 == 0) ? "dupe_url" : String.format("url_%d", i);
            item.setImage(new FeedImage(null, url, ""));
            items.add(item);
        }
        Feed feed = new Feed();
        feed.setItems(items);

        DownloadService.removeDuplicateImages(feed);

        assertEquals(50, items.size());
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            String want = (i == 0) ? "dupe_url" : (i % 5 == 0) ? null : String.format("url_%d", i);
            assertEquals(want, item.getImageLocation());
        }
    }
}
