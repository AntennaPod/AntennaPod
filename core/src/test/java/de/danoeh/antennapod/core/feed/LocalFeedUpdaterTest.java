package de.danoeh.antennapod.core.feed;

import android.app.Application;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.webkit.MimeTypeMap;

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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.danoeh.antennapod.core.ApplicationCallbacks;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
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
    private static final String LOCAL_FEED_DIR1 = "local-feed1";
    private static final String LOCAL_FEED_DIR2 = "local-feed2";

    private Context context;

    @Before
    public void setUp() throws Exception {
        // Initialize environment
        context = InstrumentationRegistry.getInstrumentation().getContext();
        UserPreferences.init(context);

        Application app = (Application) context;
        ClientConfig.applicationCallbacks = mock(ApplicationCallbacks.class);
        when(ClientConfig.applicationCallbacks.getApplicationInstance()).thenReturn(app);

        // Initialize database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        mapDummyMetadata(LOCAL_FEED_DIR1);
        mapDummyMetadata(LOCAL_FEED_DIR2);
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("mp3", "audio/mp3");
    }

    @After
    public void tearDown() {
        DBWriter.tearDownTests();
        PodDBAdapter.tearDownTests();
    }

    /**
     * Test adding a new local feed.
     */
    @Test
    public void testUpdateFeed_AddNewFeed() {
        // check for empty database
        List<Feed> feedListBefore = DBReader.getFeedList();
        assertThat(feedListBefore, is(empty()));

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
        assertThat(feedAfter.getImageUrl(), endsWith("local-feed2/folder.png"));
    }

    /**
     * Test default feed icon if there is no matching file in the local feed media folder.
     */
    @Test
    public void testUpdateFeed_FeedIconDefault() {
        callUpdateFeed(LOCAL_FEED_DIR1);

        Feed feedAfter = verifySingleFeedInDatabase();
        String resourceEntryName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertThat(feedAfter.getImageUrl(), endsWith(resourceEntryName));
    }

    /**
     * Test default feed metadata.
     *
     * @see #mapDummyMetadata Title and PubDate are dummy values.
     */
    @Test
    public void testUpdateFeed_FeedMetadata() {
        callUpdateFeed(LOCAL_FEED_DIR1);

        Feed feed = verifySingleFeedInDatabase();
        List<FeedItem> feedItems = DBReader.getFeedItemList(feed);
        FeedItem feedItem = feedItems.get(0);

        assertEquals("track1.mp3", feedItem.getTitle());

        Date pubDate = feedItem.getPubDate();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(pubDate);
        assertEquals(2020, calendar.get(Calendar.YEAR));
        assertEquals(6 - 1, calendar.get(Calendar.MONTH));
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(23, calendar.get(Calendar.MINUTE));
        assertEquals(24, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testGetImageUrl_EmptyFolder() {
        DocumentFile documentFolder = mockDocumentFolder();
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        String defaultImageName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertThat(imageUrl, endsWith(defaultImageName));
    }

    @Test
    public void testGetImageUrl_NoImageButAudioFiles() {
        DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        String defaultImageName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertThat(imageUrl, endsWith(defaultImageName));
    }

    @Test
    public void testGetImageUrl_PreferredImagesFilenames() {
        for (String filename : LocalFeedUpdater.PREFERRED_FEED_IMAGE_FILENAMES) {
            DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"),
                    mockDocumentFile(filename, "image/jpeg")); // image MIME type doesn't matter
            String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
            assertThat(imageUrl, endsWith(filename));
        }
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameJpg() {
        DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.jpg", "image/jpeg"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        assertThat(imageUrl, endsWith("my-image.jpg"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameJpeg() {
        DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.jpeg", "image/jpeg"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        assertThat(imageUrl, endsWith("my-image.jpeg"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenamePng() {
        DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.png", "image/png"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        assertThat(imageUrl, endsWith("my-image.png"));
    }

    @Test
    public void testGetImageUrl_OtherImageFilenameUnsupportedMimeType() {
        DocumentFile documentFolder = mockDocumentFolder(mockDocumentFile("audio.mp3", "audio/mp3"),
                mockDocumentFile("my-image.svg", "image/svg+xml"));
        String imageUrl = LocalFeedUpdater.getImageUrl(context, documentFolder);
        String defaultImageName = context.getResources().getResourceEntryName(R.raw.local_feed_default_icon);
        assertThat(imageUrl, endsWith(defaultImageName));
    }

    /**
     * Fill ShadowMediaMetadataRetriever with dummy duration and title.
     *
     * @param localFeedDir assets local feed folder with media files
     */
    private void mapDummyMetadata(@NonNull String localFeedDir) throws IOException {
        String[] fileNames = context.getAssets().list(localFeedDir);
        for (String fileName : fileNames) {
            String path = localFeedDir + '/' + fileName;
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_DURATION, "10");
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_TITLE, fileName);
            ShadowMediaMetadataRetriever.addMetadata(path,
                    MediaMetadataRetriever.METADATA_KEY_DATE, "20200601T222324");
        }

    }

    /**
     * Calls the method {@link LocalFeedUpdater#updateFeed(Feed, Context)} with
     * the given local feed folder.
     *
     * @param localFeedDir assets local feed folder with media files
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

    /**
     * Create a DocumentFile mock object.
     */
    @NonNull
    private static DocumentFile mockDocumentFile(@NonNull String fileName, @NonNull String mimeType) {
        DocumentFile file = mock(DocumentFile.class);
        when(file.getName()).thenReturn(fileName);
        when(file.getUri()).thenReturn(Uri.parse("file:///path/" + fileName));
        when(file.getType()).thenReturn(mimeType);
        return file;
    }

    /**
     *  Create a DocumentFile folder mock object with a list of files.
     */
    @NonNull
    private static DocumentFile mockDocumentFolder(DocumentFile... files) {
        DocumentFile documentFolder = mock(DocumentFile.class);
        when(documentFolder.listFiles()).thenReturn(files);
        return documentFolder;
    }
}
