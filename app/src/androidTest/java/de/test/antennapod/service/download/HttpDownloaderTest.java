package de.test.antennapod.service.download;

import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import de.danoeh.antennapod.core.feed.FeedFile;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.util.DownloadError;
import de.test.antennapod.util.service.download.HTTPBin;

public class HttpDownloaderTest extends InstrumentationTestCase {
    private static final String TAG = "HttpDownloaderTest";
    private static final String DOWNLOAD_DIR = "testdownloads";

    private static boolean successful = true;

    private File destDir;

    private HTTPBin httpServer;

    public HttpDownloaderTest() {
        super();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        File[] contents = destDir.listFiles();
        for (File f : contents) {
            assertTrue(f.delete());
        }

        httpServer.stop();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        UserPreferences.init(getInstrumentation().getTargetContext());
        destDir = getInstrumentation().getTargetContext().getExternalFilesDir(DOWNLOAD_DIR);
        assertNotNull(destDir);
        assertTrue(destDir.exists());
        httpServer = new HTTPBin();
        httpServer.start();
    }

    private FeedFileImpl setupFeedFile(String downloadUrl, String title, boolean deleteExisting) {
        FeedFileImpl feedfile = new FeedFileImpl(downloadUrl);
        String fileUrl = new File(destDir, title).getAbsolutePath();
        File file = new File(fileUrl);
        if (deleteExisting) {
            Log.d(TAG, "Deleting file: " + file.delete());
        }
        feedfile.setFile_url(fileUrl);
        return feedfile;
    }

    private Downloader download(String url, String title, boolean expectedResult) {
        return download(url, title, expectedResult, true, null, null, true);
    }

    private Downloader download(String url, String title, boolean expectedResult, boolean deleteExisting, String username, String password, boolean deleteOnFail) {
        FeedFile feedFile = setupFeedFile(url, title, deleteExisting);
        DownloadRequest request = new DownloadRequest(feedFile.getFile_url(), url, title, 0, feedFile.getTypeAsInt(), username, password, deleteOnFail, null);
        Downloader downloader = new HttpDownloader(request);
        downloader.call();
        DownloadStatus status = downloader.getResult();
        assertNotNull(status);
        assertTrue(status.isSuccessful() == expectedResult);
        assertTrue(status.isDone());
        // the file should not exist if the download has failed and deleteExisting was true
        assertTrue(!deleteExisting || new File(feedFile.getFile_url()).exists() == expectedResult);
        return downloader;
    }


    private static final String URL_404 = HTTPBin.BASE_URL + "/status/404";
    private static final String URL_AUTH = HTTPBin.BASE_URL + "/basic-auth/user/passwd";

    public void testPassingHttp() {
        download(HTTPBin.BASE_URL + "/status/200", "test200", true);
    }

    public void testRedirect() {
        download(HTTPBin.BASE_URL + "/redirect/4", "testRedirect", true);
    }

    public void testGzip() {
        download(HTTPBin.BASE_URL + "/gzip/100", "testGzip", true);
    }

    public void test404() {
        download(URL_404, "test404", false);
    }

    public void testCancel() {
        final String url = HTTPBin.BASE_URL + "/delay/3";
        FeedFileImpl feedFile = setupFeedFile(url, "delay", true);
        final Downloader downloader = new HttpDownloader(new DownloadRequest(feedFile.getFile_url(), url, "delay", 0, feedFile.getTypeAsInt()));
        Thread t = new Thread() {
            @Override
            public void run() {
                downloader.call();
            }
        };
        t.start();
        downloader.cancel();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DownloadStatus result = downloader.getResult();
        assertTrue(result.isDone());
        assertFalse(result.isSuccessful());
        assertTrue(result.isCancelled());
        assertFalse(new File(feedFile.getFile_url()).exists());
    }

    public void testDeleteOnFailShouldDelete() {
        Downloader downloader = download(URL_404, "testDeleteOnFailShouldDelete", false, true, null, null, true);
        assertFalse(new File(downloader.getDownloadRequest().getDestination()).exists());
    }

    public void testDeleteOnFailShouldNotDelete() throws IOException {
        String filename = "testDeleteOnFailShouldDelete";
        File dest = new File(destDir, filename);
        dest.delete();
        assertTrue(dest.createNewFile());
        Downloader downloader = download(URL_404, filename, false, false, null, null, false);
        assertTrue(new File(downloader.getDownloadRequest().getDestination()).exists());
    }

    public void testAuthenticationShouldSucceed() throws InterruptedException {
        download(URL_AUTH, "testAuthSuccess", true, true, "user", "passwd", true);
    }

    public void testAuthenticationShouldFail() {
        Downloader downloader = download(URL_AUTH, "testAuthSuccess", false, true, "user", "Wrong passwd", true);
        assertEquals(DownloadError.ERROR_UNAUTHORIZED, downloader.getResult().getReason());
    }

    /* TODO: replace with smaller test file
    public void testUrlWithSpaces() {
        download("http://acedl.noxsolutions.com/ace/Don't Call Salman Rushdie Sneezy in Finland.mp3", "testUrlWithSpaces", true);
    }
    */

    private static class FeedFileImpl extends FeedFile {
        public FeedFileImpl(String download_url) {
            super(null, download_url, false);
        }


        @Override
        public String getHumanReadableIdentifier() {
            return download_url;
        }

        @Override
        public int getTypeAsInt() {
            return 0;
        }
    }

}
