package de.test.antennapod.storage.benchmarks;

import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

@RunWith(AndroidJUnit4.class)
class FeedFilterBenchMark {
   @Rule
   public BenchmarkRule benchMarkRule = new BenchmarkRule();


   Feed feed = new Feed("url", null, "Test Feed title 1 " + System.currentTimeMillis());


   private List<FeedItem> createFeedItems(
           Feed feed,
           Integer startId,
           int feedItemCount,
           int state,
           Date playbackCompletionDate,
           boolean isDownloaded,
           boolean isInQeue,
           boolean isFavorite) throws Exception {
      // To avoid complication in case of test failures, leaving behind orphaned
      // media files: add a timestamp so that each test run will have its own directory for media files.
      List<FeedItem> items = new ArrayList<>();
      feed.setItems(items);
      for (int itemIndex = 0; itemIndex < feedItemCount; ++feedItemCount) {
         FeedItem item1 = new FeedItem(startId++, "Item " + startId, "Item " + startId, "url", new Date(), state, feed);
         items.add(item1);
         FeedMedia media1 = new FeedMedia(startId, item1, 123, 1, 1, "audio/mp3", null, "http://example.com/episode.mp3", isDownloaded, playbackCompletionDate, 0, 0);
         item1.setMedia(media1);

         DBWriter.setFeedItem(item1).get();
      }

      return items;
   }
}

