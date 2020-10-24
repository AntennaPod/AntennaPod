package de.danoeh.antennapod.core.feed;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.AssetsDocumentFile;
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

import java.io.File;
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
    private static final String LOCAL_FEED_DIR1 = "local-feed1";
    private static final String LOCAL_FEED_DIR2 = "local-feed2";

    private Context context;

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

        // fill ShadowMediaMetadataRetriever with dummy duration and title
        File localFeedBaseDir = new File("src/test/assets");
        for (File folder : localFeedBaseDir.listFiles()) {
            for (File file : folder.listFiles()) {
                String path = folder.getName() + '/' + file.getName();
                ShadowMediaMetadataRetriever.addMetadata(path,
                        MediaMetadataRetriever.METADATA_KEY_DURATION, "10");
                ShadowMediaMetadataRetriever.addMetadata(path,
                        MediaMetadataRetriever.METADATA_KEY_TITLE, file.getName());
            }
        }
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    /**
     * Test adding a new local feed.
     */
    @Test
    public void testUpdateFeed_AddNewFeed() {
        // check for empty database
        List<Feed> feedListBefore = DBReader.getFeedList();
        assertTrue(feedListBefore.isEmpty());

        callUpdateFeed(LOCAL_FEED_DIR2);

        // verify new feed in database
        verifySingleFeedInDatabaseAndItemCount(2);
        Feed feedAfter = verifySingleFeedInDatabase();
        assertEquals(FEED_URL, feedAfter.getDownload_url());
    }

    /**
     * Test adding further items to an existing local feed.
     */
    @Test
    public void testUpdateFeed_AddMoreItems() {
        // add local feed with 1 item (localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR1);

        // now add another item (by changing to local feed folder localFeedDir2)
        callUpdateFeed(LOCAL_FEED_DIR2);

        verifySingleFeedInDatabaseAndItemCount(2);
    }

    /**
     * Test removing items from an existing local feed without a corresponding media file.
     */
    @Test
    public void testUpdateFeed_RemoveItems() {
        // add local feed with 2 items (localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR2);

        // now remove an item (by changing to local feed folder localFeedDir1)
        callUpdateFeed(LOCAL_FEED_DIR1);

        verifySingleFeedInDatabaseAndItemCount(1);
    }

    /**
     * Test feed icon defined in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconFromFolder() {
        callUpdateFeed(LOCAL_FEED_DIR2);

        Feed feedAfter = verifySingleFeedInDatabase();
        assertTrue(feedAfter.getImageUrl().contains("local-feed2/folder.png"));
    }

    /**
     * Test default feed icon if there is no matching file in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconDefault() {
        callUpdateFeed(LOCAL_FEED_DIR1);

        Feed feedAfter = verifySingleFeedInDatabase();
        String resourceEntryName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertTrue(feedAfter.getImageUrl().contains(resourceEntryName));
    }

    /**
     * Calls the method {@link LocalFeedUpdater#updateFeed(Feed, Context)} with
     * the given local feed folder.
     *
     * @param localFeedDir local feed folder with media files
     */
    private void callUpdateFeed(@NonNull String localFeedDir) {
        DocumentFile documentFile = new AssetsDocumentFile(localFeedDir, context.getAssets());
        try (MockedStatic<DocumentFile> dfMock = Mockito.mockStatic(DocumentFile.class)) {
            // mock external storage
            dfMock.when(() -> DocumentFile.fromTreeUri(any(), any())).thenReturn(documentFile);

            // call method to test
            Feed feed = new Feed(FEED_URL, null);
            LocalFeedUpdater.updateFeed(feed, context);
        }
    }

    /**
     * Verify that the database contains exactly one feed and return that feed.
     */
    @NonNull
    private static Feed verifySingleFeedInDatabase() {
        List<Feed> feedListAfter = DBReader.getFeedList();
        assertEquals(1, feedListAfter.size());
        return feedListAfter.get(0);
    }

    /**
     * Verify that the database contains exactly one feed and the number of
     * items in the feed.
     *
     * @param expectedItemCount expected number of items in the feed
     */
    private static void verifySingleFeedInDatabaseAndItemCount(int expectedItemCount) {
        Feed feed = verifySingleFeedInDatabase();
        List<FeedItem> feedItems = DBReader.getFeedItemList(feed);
        assertEquals(expectedItemCount, feedItems.size());
    }
}
