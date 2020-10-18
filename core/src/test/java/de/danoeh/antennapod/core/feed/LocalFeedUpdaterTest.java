package de.danoeh.antennapod.core.feed;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowMediaMetadataRetriever;
import org.robolectric.shadows.util.DataSource;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.ApplicationCallbacks;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test local feeds handling in class LocalFeedUpdater.
 */
@RunWith(RobolectricTestRunner.class)
public class LocalFeedUpdaterTest {

    /**
     * URL to locate the local feed media files on the external storage (SD card).
     * The exact URL doesn't matter here as access to external storage is mocked
     * (seems not to be supported by Robolectric).
     */
    private static final String FEED_URL =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2Flocal-feed";

    private Context context;
    private File localFeedDir1;
    private File localFeedDir2;

    @Before
    public void setUp() {

        // Initialize environment
        context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);

        // Initialize database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        // Emulate turned off gpodder.net support in SyncService
        SharedPreferences spref = mock(SharedPreferences.class);
        when(spref.getString(eq("prefGpodnetHostname"), anyString())).thenReturn("gpodder.net");
        Application app = mock(Application.class);
        when(app.getSharedPreferences(anyString(), anyInt())).thenReturn(spref);
        ClientConfig.applicationCallbacks = mock(ApplicationCallbacks.class);
        when(ClientConfig.applicationCallbacks.getApplicationInstance()).thenReturn(app);

        localFeedDir1 = new File("sampledata/local-feed1");
        localFeedDir2 = new File("sampledata/local-feed2");
        List<File> files = new ArrayList<>();
        Collections.addAll(files, localFeedDir1.listFiles());
        Collections.addAll(files, localFeedDir2.listFiles());
        for (File file : files) {
            DataSource ds = DataSource.toDataSource(context, Uri.fromFile(file));
            ShadowMediaMetadataRetriever.addMetadata(ds, MediaMetadataRetriever.METADATA_KEY_DURATION, "10");
            ShadowMediaMetadataRetriever.addMetadata(ds, MediaMetadataRetriever.METADATA_KEY_TITLE, file.getName());
        }
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("mp3", "audio/mp3");
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @After
    public void tearDown() throws Exception {
        // Workaround for Robolectric issue in ShadowSQLiteConnection: IllegalStateException: Illegal connection pointer
        Field field = PodDBAdapter.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(null, null);

        for (Class<?> innerClass : PodDBAdapter.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("SingletonHolder")) {
                Field dbHelperField = innerClass.getDeclaredField("dbHelper");
                dbHelperField.setAccessible(true);
                SQLiteOpenHelper dbHelper = (SQLiteOpenHelper) dbHelperField.get(null);
                Field databaseField = SQLiteOpenHelper.class.getDeclaredField("mDatabase");
                databaseField.setAccessible(true);
                databaseField.set(dbHelper, null);
            }
        }
    }

    /**
     * Test adding a new local feed.
     */
    @Test
    public void testUpdateFeed_AddNewFeed() {
        // verify empty database
        List<Feed> feedListBefore = DBReader.getFeedList();
        assertTrue(feedListBefore.isEmpty());

        DocumentFile documentFile = DocumentFile.fromFile(localFeedDir2);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // verify new feed in database
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        Feed feedAfter = feedListAfter.get(0);
        assertEquals(FEED_URL, feedAfter.getDownload_url());
        List<FeedItem> feedItems = DBReader.getFeedItemList(feedAfter);
        assertEquals(2, feedItems.size());
    }

    /**
     * Test adding further items to an existing local feed.
     */
    @Test
    public void testUpdateFeed_AddMoreItems() {
        // add local feed with 1 item (localFeedDir1)
        DocumentFile documentFile1 = DocumentFile.fromFile(localFeedDir1);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile1);
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // now add another item (by changing to local feed folder localFeedDir2)
        DocumentFile documentFile2 = DocumentFile.fromFile(localFeedDir2);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile2);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // verify new feed in database
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        Feed feedAfter = feedListAfter.get(0);
        List<FeedItem> feedItems = DBReader.getFeedItemList(feedAfter);
        assertEquals(2, feedItems.size());
    }

    /**
     * Test removing items from an existing local feed without a corresponding media file.
     */
    @Test
    public void testUpdateFeed_RemoveItems() {
        // add local feed with 2 items (localFeedDir1)
        DocumentFile documentFile1 = DocumentFile.fromFile(localFeedDir2);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile1);
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // now remove an item (by changing to local feed folder localFeedDir1)
        DocumentFile documentFile2 = DocumentFile.fromFile(localFeedDir1);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile2);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // verify new feed in database
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        Feed feedAfter = feedListAfter.get(0);
        List<FeedItem> feedItems = DBReader.getFeedItemList(feedAfter);
        assertEquals(1, feedItems.size());
    }

    /**
     * Test feed icon defined in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconFromFolder() {
        DocumentFile documentFile2 = DocumentFile.fromFile(localFeedDir2);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile2);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // verify new feed in database
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        Feed feedAfter = feedListAfter.get(0);
        assertTrue(feedAfter.getImageUrl().contains("local-feed2/folder.png"));
    }

    /**
     * Test default feed icon if there is no matching file in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconDefault() {
        DocumentFile documentFile2 = DocumentFile.fromFile(localFeedDir1);
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile2);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }

        // verify new feed in database
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        Feed feedAfter = feedListAfter.get(0);
        String resourceEntryName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertTrue(feedAfter.getImageUrl().contains(resourceEntryName));
    }
}
