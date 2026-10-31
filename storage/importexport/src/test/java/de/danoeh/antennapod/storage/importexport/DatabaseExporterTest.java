package de.danoeh.antennapod.storage.importexport;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class DatabaseExporterTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testCompressedRoundtrip() throws Exception {
        testRoundtrip(true);
    }

    @Test
    public void testUncompressedRoundtrip() throws Exception {
        testRoundtrip(false);
    }

    private void testRoundtrip(boolean compress) throws Exception {
        Feed feed = new Feed(0, null, "Backup Feed", "link", "d", null, null, null, "rss", "Backup Feed",
                null, "", "", 0);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        File backupFile = new File(context.getCacheDir(), "backup");
        Uri backupUri = Uri.fromFile(backupFile);
        DatabaseExporter.exportToDocument(backupUri, context, compress);
        try (InputStream inputStream = new FileInputStream(backupFile)) {
            int magic = inputStream.read() | (inputStream.read() << 8);
            assertEquals(compress, magic == GZIPInputStream.GZIP_MAGIC);
        }

        PodDBAdapter.deleteDatabase();
        assertEquals(0, DBReader.getFeedList().size());

        DatabaseExporter.importBackup(backupUri, context);
        PodDBAdapter.tearDownTests();
        List<Feed> feeds = DBReader.getFeedList();
        assertEquals(1, feeds.size());
        assertEquals("Backup Feed", feeds.get(0).getTitle());
    }
}
