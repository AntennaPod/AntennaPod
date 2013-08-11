package instrumentationTest.de.test.antennapod.service.download;

import java.io.File;

import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.service.download.*;

import android.test.AndroidTestCase;
import android.util.Log;

public class HttpDownloaderTest extends AndroidTestCase {
    private static final String TAG = "HttpDownloaderTest";
    private static final String DOWNLOAD_DIR = "testdownloads";

    private static boolean successful = true;

    public HttpDownloaderTest() {
        super();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        File externalDir = getContext().getExternalFilesDir(DOWNLOAD_DIR);
        assertNotNull(externalDir);
        File[] contents = externalDir.listFiles();
        for (File f : contents) {
            assertTrue(f.delete());
        }
    }

    private FeedFileImpl setupFeedFile(String downloadUrl, String title) {
        FeedFileImpl feedfile = new FeedFileImpl(downloadUrl);
        String fileUrl = new File(getContext().getExternalFilesDir(DOWNLOAD_DIR).getAbsolutePath(), title).getAbsolutePath();
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
        assertTrue(new File(feedFile.getFile_url()).exists());
    }

    public void testRandomUrls() {
        final String[] urls = {
                "http://radiobox.omroep.nl/programme/read_programme_podcast/9168/read.rss",
                "http://content.zdf.de/podcast/zdf_heute/heute_a.xml",
                "http://rss.sciam.com/sciam/60secsciencepodcast",
                "http://rss.sciam.com/sciam/60-second-mind",
                "http://rss.sciam.com/sciam/60-second-space",
                "http://rss.sciam.com/sciam/60-second-health",
                "http://rss.sciam.com/sciam/60-second-tech",
                "http://risky.biz/feeds/risky-business",
                "http://risky.biz/feeds/rb2",
                "http://podcast.hr-online.de/lateline/podcast.xml",
                "http://bitlove.org/nsemak/mikrodilettanten/feed",
                "http://bitlove.org/moepmoeporg/riotburnz/feed",
                "http://bitlove.org/moepmoeporg/schachcast/feed",
                "http://bitlove.org/moepmoeporg/sundaymoaning/feed",
                "http://bitlove.org/motofunk/anekdotkast/feed",
                "http://bitlove.org/motofunk/motofunk/feed",
                "http://bitlove.org/nerdinand/zch/feed",
                "http://podcast.homerj.de/podcasts.xml",
                "http://www.dradio.de/rss/podcast/sendungen/wissenschaftundbildung/",
                "http://www.dradio.de/rss/podcast/sendungen/wirtschaftundverbraucher/",
                "http://www.dradio.de/rss/podcast/sendungen/literatur/",
                "http://www.dradio.de/rss/podcast/sendungen/sport/",
                "http://www.dradio.de/rss/podcast/sendungen/wirtschaftundgesellschaft/",
                "http://www.dradio.de/rss/podcast/sendungen/filmederwoche/",
                "http://www.blacksweetstories.com/feed/podcast/",
                "http://feeds.5by5.tv/buildanalyze",
                "http://bitlove.org/ranzzeit/ranz/feed"
        };
        for (int i = 0; i < urls.length; i++) {
            download(urls[i], Integer.toString(i), true);
        }
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
