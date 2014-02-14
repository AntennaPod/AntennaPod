package instrumentationTest.de.test.antennapod.service.download;

import java.io.File;

import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.service.download.*;

import android.test.AndroidTestCase;
import android.util.Log;

public class HttpDownloaderTest extends InstrumentationTestCase {
    private static final String TAG = "HttpDownloaderTest";
    private static final String DOWNLOAD_DIR = "testdownloads";

    private static boolean successful = true;

    private File destDir;

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
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        destDir = getInstrumentation().getTargetContext().getExternalFilesDir(DOWNLOAD_DIR);
        assertNotNull(destDir);
        assertTrue(destDir.exists());
    }

    private FeedFileImpl setupFeedFile(String downloadUrl, String title) {
        FeedFileImpl feedfile = new FeedFileImpl(downloadUrl);
        String fileUrl = new File(destDir, title).getAbsolutePath();
        File file = new File(fileUrl);
        Log.d(TAG, "Deleting file: " + file.delete());
        feedfile.setFile_url(fileUrl);
        return feedfile;
    }

    private void download(String url, String title, boolean expectedResult) {
        FeedFile feedFile = setupFeedFile(url, title);
        DownloadRequest request = new DownloadRequest(feedFile.getFile_url(), url, title, 0, feedFile.getTypeAsInt());
        Downloader downloader = new HttpDownloader(request);
        downloader.call();
        DownloadStatus status = downloader.getResult();
        assertNotNull(status);
        assertTrue(status.isSuccessful() == expectedResult);
        assertTrue(status.isDone());
        // the file should not exist if the download has failed
        assertTrue(new File(feedFile.getFile_url()).exists() == expectedResult);
    }

    public void testPassingHttp() {
        download("http://httpbin.org/status/200", "test200", true);
    }

    public void testPassingHttps() {
        download("https://httpbin.org/status/200", "test200", true);
    }

    public void testRedirect() {
        download("http://httpbin.org/redirect/4", "testRedirect", true);
    }

    public void testRelativeRedirect() {
        download("http://httpbin.org/relative-redirect/4", "testRelativeRedirect", true);
    }

    public void testGzip() {
        download("http://httpbin.org/gzip", "testGzip", true);
    }

    public void test404() {
        download("http://httpbin.org/status/404", "test404", false);
    }

    public void testCancel() {
        final String url = "http://httpbin.org/delay/3";
        FeedFileImpl feedFile = setupFeedFile(url, "delay");
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
