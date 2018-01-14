package de.test.antennapod.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.fragment.ExternalPlayerFragment;
import de.greenrobot.event.EventBus;
import de.test.antennapod.util.service.download.HTTPBin;
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator;

/**
 * Utility methods for UI tests.
 * Starts a web server that hosts feeds, episodes and images.
 */
class UITestUtils {

    private static final String TAG = UITestUtils.class.getSimpleName();

    private static final String DATA_FOLDER = "test/UITestUtils";

    private static final int NUM_FEEDS = 5;
    private static final int NUM_ITEMS_PER_FEED = 10;

    private static final String TEST_FILE_NAME = "3sec.mp3";


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
        destDir = context.getExternalFilesDir(DATA_FOLDER);
        destDir.mkdir();
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
        RSS2Generator generator = new RSS2Generator();
        generator.writeFeed(feed, out, "UTF-8", 0);
        out.close();
        int id = server.serveFile(feedFile);
        Assert.assertTrue(id != -1);
        return String.format("%s/files/%d", HTTPBin.BASE_URL, id);
    }

    private String hostFile(File file) {
        int id = server.serveFile(file);
        Assert.assertTrue(id != -1);
        return String.format("%s/files/%d", HTTPBin.BASE_URL, id);
    }

    private File newBitmapFile(String name) throws IOException {
        File imgFile = new File(destDir, name);
        Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        FileOutputStream out = new FileOutputStream(imgFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 1, out);
        out.close();
        return imgFile;
    }

    private File newMediaFile(String name) throws IOException {
        File mediaFile = new File(hostedMediaDir, name);
        if(mediaFile.exists()) {
            mediaFile.delete();
        }
        Assert.assertFalse(mediaFile.exists());

        InputStream in = context.getAssets().open(TEST_FILE_NAME);
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
            File bitmapFile = newBitmapFile("image" + i);
            FeedImage image = new FeedImage(0, "image " + i, null, hostFile(bitmapFile), false);
            Feed feed = new Feed(0, null, "Title " + i, "http://example.com/" + i, "Description of feed " + i,
                    "http://example.com/pay/feed" + i, "author " + i, "en", Feed.TYPE_RSS2, "feed" + i, image, null,
                    "http://example.com/feed/src/" + i, false);
            image.setOwner(feed);

            // create items
            List<FeedItem> items = new ArrayList<>();
            for (int j = 0; j < NUM_ITEMS_PER_FEED; j++) {
                FeedItem item = new FeedItem(j, "Feed " + (i+1) + ": Item " + (j+1), "item" + j,
                        "http://example.com/feed" + i + "/item/" + j, new Date(), FeedItem.UNPLAYED, feed);
                items.add(item);

                File mediaFile = newMediaFile("feed-" + i + "-episode-" + j + ".mp3");
                item.setMedia(new FeedMedia(j, item, 0, 0, mediaFile.length(), "audio/mp3", null, hostFile(mediaFile), false, null, 0, 0));

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
            if (feed.getImage() != null) {
                FeedImage image = feed.getImage();
                int fileId = Integer.parseInt(StringUtils.substringAfter(image.getDownload_url(), "files/"));
                image.setFile_url(server.accessFile(fileId).getAbsolutePath());
                image.setDownloaded(true);
            }
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
            feed.getItems().get(1).getMedia().setPlaybackCompletionDate(new Date());
        }
        localFeedDataAdded = true;

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(hostedFeeds.toArray(new Feed[hostedFeeds.size()]));
        adapter.setQueue(queue);
        adapter.close();
        EventDistributor.getInstance().sendFeedUpdateBroadcast();
        EventBus.getDefault().post(QueueEvent.setQueue(queue));
    }

    public PlaybackController getPlaybackController(MainActivity mainActivity) {
        ExternalPlayerFragment fragment = (ExternalPlayerFragment)mainActivity.getSupportFragmentManager().findFragmentByTag(ExternalPlayerFragment.TAG);
        return fragment.getPlaybackControllerTestingOnly();
    }

    public FeedMedia getCurrentMedia(MainActivity mainActivity) {
        return (FeedMedia)getPlaybackController(mainActivity).getMedia();
    }
}
