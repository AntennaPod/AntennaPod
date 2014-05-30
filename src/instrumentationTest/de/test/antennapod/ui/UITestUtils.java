package instrumentationTest.de.test.antennapod.ui;

import android.content.Context;
import android.graphics.Bitmap;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import instrumentationTest.de.test.antennapod.util.service.download.HTTPBin;
import instrumentationTest.de.test.antennapod.util.syndication.feedgenerator.RSS2Generator;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility methods for UI tests
 */
public class UITestUtils {

    private static final String DATA_FOLDER = "test/UITestUtils";

    public static final int NUM_FEEDS = 10;
    public static final int NUM_ITEMS_PER_FEED = 20;


    private Context context;
    private HTTPBin server;
    private File destDir;
    private File hostedFeedDir;

    public UITestUtils(Context context) {
        this.context = context;
    }


    public void setup() throws IOException {
        destDir = context.getExternalFilesDir(DATA_FOLDER);
        destDir.mkdir();
        hostedFeedDir = new File(destDir, "hostedFeeds");
        hostedFeedDir.mkdir();
        Assert.assertTrue(destDir.exists());
        Assert.assertTrue(hostedFeedDir.exists());
        server.start();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(destDir);
        server.stop();
    }

    private String hostFeed(Feed feed) throws IOException {
        File feedFile = new File(hostedFeedDir, feed.getTitle());
        FileOutputStream out = new FileOutputStream(feedFile);
        RSS2Generator generator = new RSS2Generator();
        generator.writeFeed(feed, out, "UTF-8", 0);
        out.close();
        int id = server.serveFile(feedFile);
        Assert.assertTrue(id != -1);
        return String.format("http://127.0.0.1/files/%d", id);
    }

    private File newBitmapFile(String name) throws IOException {
        File imgFile = new File(destDir, name);
        Bitmap bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        FileOutputStream out = new FileOutputStream(imgFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 1, out);
        out.close();
        return imgFile;
    }

    public void addFeedData() throws IOException {
        for (int i = 0; i < NUM_FEEDS; i++) {
            FeedImage image = new FeedImage(0, "image " + i, newBitmapFile("image" + i).getAbsolutePath(), "http://example.com/feed" + i + "/image", true);
            Feed feed = new Feed(0, new Date(), "Title " + i, "http://example.com/" + i, "Description of feed " + i,
                    "http://example.com/pay/feed" + i, "author " + i, "en", Feed.TYPE_RSS2, "feed" + i, image, null,
                    "http://example.com/feed/src/" + i, false);
            feed.setDownload_url(hostFeed(feed));

            // create items
            List<FeedItem> items = new ArrayList<FeedItem>();
            for (int j = 0; j < NUM_ITEMS_PER_FEED; j++) {
                FeedItem item = new FeedItem(0, "item" + j, "item" + j, "http://example.com/feed" + i + "/item/" + j, new Date(), true, feed);
                items.add(item);
            }
        }
    }
}
