package de.test.antennapod.ui;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.test.antennapod.util.service.download.HTTPBin;
import de.test.antennapod.util.syndication.feedgenerator.Rss2Generator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.junit.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for UI tests.
 * Starts a web server that hosts feeds, episodes and images.
 */
public class UITestUtils {

    private static final String TAG = UITestUtils.class.getSimpleName();

    private static final int NUM_FEEDS = 5;
    private static final int NUM_ITEMS_PER_FEED = 10;

    private String testFileName = "3sec.mp3";
    private boolean hostTextOnlyFeeds = false;
    private final Context context;
    private final HTTPBin server = new HTTPBin();
    private File destDir;
    private File hostedFeedDir;
    private File hostedMediaDir;

    public final List<Feed> hostedFeeds = new ArrayList<>();

    public UITestUtils(Context context) {
        this.context = context;
    }


    public void setup() throws IOException {
        destDir = new File(context.getFilesDir(), "test/UITestUtils");
        destDir.mkdirs();
        hostedFeedDir = new File(destDir, "hostedFeeds");
        hostedFeedDir.mkdir();
        hostedMediaDir = new File(destDir, "hostedMediaDir");
        hostedMediaDir.mkdir();
        Assert.assertTrue(destDir.exists());
        Assert.assertTrue(hostedFeedDir.exists());
        Assert.assertTrue(hostedMediaDir.exists());
        server.start();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(destDir);
        FileUtils.deleteDirectory(hostedMediaDir);
        FileUtils.deleteDirectory(hostedFeedDir);
        server.stop();

        if (localFeedDataAdded) {
            PodDBAdapter.deleteDatabase();
        }
    }

    private String hostFeed(Feed feed) throws IOException {
        File feedFile = new File(hostedFeedDir, feed.getTitle());
        FileOutputStream out = new FileOutputStream(feedFile);
        Rss2Generator generator = new Rss2Generator();
        generator.writeFeed(feed, out, "UTF-8", 0);
        out.close();
        int id = server.serveFile(feedFile);
        Assert.assertTrue(id != -1);
        return String.format(Locale.US, "%s/files/%d", server.getBaseUrl(), id);
    }

    private String hostFile(File file) {
        int id = server.serveFile(file);
        Assert.assertTrue(id != -1);
        return String.format(Locale.US, "%s/files/%d", server.getBaseUrl(), id);
    }

    private File newMediaFile(String name) throws IOException {
        File mediaFile = new File(hostedMediaDir, name);
        if (mediaFile.exists()) {
            mediaFile.delete();
        }
        Assert.assertFalse(mediaFile.exists());

        InputStream in = context.getAssets().open(testFileName);
        Assert.assertNotNull(in);

        FileOutputStream out = new FileOutputStream(mediaFile);
        IOUtils.copy(in, out);
        out.close();

        return mediaFile;
    }

    private boolean feedDataHosted = false;

    /**
     * Adds feeds, images and episodes to the webserver for testing purposes.
     */
    public void addHostedFeedData() throws IOException {
        if (feedDataHosted) throw new IllegalStateException("addHostedFeedData was called twice on the same instance");
        for (int i = 0; i < NUM_FEEDS; i++) {
            Feed feed = new Feed(0, null, "Title " + i, "http://example.com/" + i, "Description of feed " + i,
                    "http://example.com/pay/feed" + i, "author " + i, "en", Feed.TYPE_RSS2, "feed" + i, null, null,
                    "http://example.com/feed/src/" + i, false);

            // create items
            List<FeedItem> items = new ArrayList<>();
            for (int j = 0; j < NUM_ITEMS_PER_FEED; j++) {
                FeedItem item = new FeedItem(j, "Feed " + (i+1) + ": Item " + (j+1), "item" + j,
                        "http://example.com/feed" + i + "/item/" + j, new Date(), FeedItem.UNPLAYED, feed);
                items.add(item);

                if (!hostTextOnlyFeeds) {
                    File mediaFile = newMediaFile("feed-" + i + "-episode-" + j + ".mp3");
                    item.setMedia(new FeedMedia(j, item, 0, 0, mediaFile.length(), "audio/mp3", null, hostFile(mediaFile), false, null, 0, 0));
                }
            }
            feed.setItems(items);
            feed.setDownload_url(hostFeed(feed));
            hostedFeeds.add(feed);
        }
        feedDataHosted = true;
    }


    private boolean localFeedDataAdded = false;

    /**
     * Adds feeds, images and episodes to the local database. This method will also call addHostedFeedData if it has not
     * been called yet.
     *
     * Adds one item of each feed to the queue and to the playback history.
     *
     * This method should NOT be called if the testing class wants to download the hosted feed data.
     *
     * @param downloadEpisodes true if episodes should also be marked as downloaded.
     */
    public void addLocalFeedData(boolean downloadEpisodes) throws Exception {
        if (localFeedDataAdded) {
            Log.w(TAG, "addLocalFeedData was called twice on the same instance");
            // might be a flaky test, this is actually not that severe
            return;
        }
        if (!feedDataHosted) {
            addHostedFeedData();
        }

        List<FeedItem> queue = new ArrayList<>();
        for (Feed feed : hostedFeeds) {
            feed.setDownloaded(true);
            if (downloadEpisodes) {
                for (FeedItem item : feed.getItems()) {
                    if (item.hasMedia()) {
                        FeedMedia media = item.getMedia();
                        int fileId = Integer.parseInt(StringUtils.substringAfter(media.getDownload_url(), "files/"));
                        media.setFile_url(server.accessFile(fileId).getAbsolutePath());
                        media.setDownloaded(true);
                    }
                }
            }

            queue.add(feed.getItems().get(0));
            if (feed.getItems().get(1).hasMedia()) {
                feed.getItems().get(1).getMedia().setPlaybackCompletionDate(new Date());
            }
        }
        localFeedDataAdded = true;

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(hostedFeeds.toArray(new Feed[0]));
        adapter.setQueue(queue);
        adapter.close();
        EventBus.getDefault().post(new FeedListUpdateEvent(hostedFeeds));
        EventBus.getDefault().post(QueueEvent.setQueue(queue));
    }

    public void setMediaFileName(String filename) {
        testFileName = filename;
    }

    public void setHostTextOnlyFeeds(boolean hostTextOnlyFeeds) {
        this.hostTextOnlyFeeds = hostTextOnlyFeeds;
    }
}
