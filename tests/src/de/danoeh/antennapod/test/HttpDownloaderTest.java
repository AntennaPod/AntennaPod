package de.danoeh.antennapod.test;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.service.download.DownloaderCallback;
import de.danoeh.antennapod.service.download.HttpDownloader;

import android.test.AndroidTestCase;
import android.util.Log;

public class HttpDownloaderTest extends AndroidTestCase {
	private static final String TAG = "HttpDownloaderTest";
	private static final String DOWNLOAD_DIR = "testdownloads";
	
	private static boolean successful = true;
	private static ExecutorService es;
	
	private static DownloaderCallback downloaderCallback = new DownloaderCallback() {
		
		@Override
		public void onDownloadCompleted(Downloader downloader) {
			DownloadStatus status = downloader.getStatus();
			if (status != null) {
				final String downloadUrl = status.getFeedFile().getDownload_url();
				final String fileUrl = status.getFeedFile().getFile_url();
				new File(fileUrl).delete();
				if (status.isSuccessful()) {
					Log.i(TAG, "Download successful: " + downloadUrl);
				} else {
					Log.e(TAG, "Download not successful: " + status.toString());
					successful = false;
				}
			} else {
				Log.wtf(TAG, "Status was null");
				successful = false;
			}
			if (successful == false) {
				es.shutdownNow();
			}
		}
	};
	
	public void testDownload() throws InterruptedException {
		es = Executors.newFixedThreadPool(5);
		int i = 0;
		for (String url : TestDownloads.urls) {
			Feed feed = new Feed(url, new Date());
			String fileUrl = new File(getContext().getExternalFilesDir(DOWNLOAD_DIR).getAbsolutePath(), Integer.toString(i)).getAbsolutePath();
			File file = new File(fileUrl);
			Log.d(TAG, "Deleting file: " + file.delete());
			feed.setFile_url(fileUrl);
			DownloadStatus status = new DownloadStatus(feed, Integer.toString(i));
			Downloader downloader = new HttpDownloader(downloaderCallback, status);
			es.submit(downloader);
			i++;
		}
		Log.i(TAG, "Awaiting termination");
		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		assertTrue(successful);
	}
	
}
