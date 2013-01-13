package de.danoeh.antennapod.service.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;

public class HttpDownloader extends Downloader {
	private static final String TAG = "HttpDownloader";

	private static final int MAX_REDIRECTS = 5;

	private static final int BUFFER_SIZE = 8 * 1024;
	private static final int CONNECTION_TIMEOUT = 5000;

	public HttpDownloader(DownloaderCallback downloaderCallback,
			DownloadStatus status) {
		super(downloaderCallback, status);
	}

	private AndroidHttpClient createHttpClient() {
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
		HttpParams params = httpClient.getParams();
		params.setIntParameter("http.protocol.max-redirects", MAX_REDIRECTS);
		params.setBooleanParameter("http.protocol.reject-relative-redirect",
				false);
		params.setIntParameter("http.socket.timeout", CONNECTION_TIMEOUT);
		HttpClientParams.setRedirecting(params, true);
		return httpClient;
	}

	@Override
	protected void download() {
		AndroidHttpClient httpClient = null;
		OutputStream out = null;
		InputStream connection = null;
		try {
			HttpGet httpGet = new HttpGet(status.getFeedFile()
					.getDownload_url());
			httpClient = createHttpClient();
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity httpEntity = response.getEntity();
			int responseCode = response.getStatusLine().getStatusCode();
			if (AppConfig.DEBUG)
				Log.d(TAG, "Response code is " + responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK && httpEntity != null) {
				if (StorageUtils.storageAvailable(PodcastApp.getInstance())) {
					File destination = new File(status.getFeedFile()
							.getFile_url());
					if (!destination.exists()) {
						connection = AndroidHttpClient.getUngzippedContent(httpEntity);
						InputStream in = new BufferedInputStream(connection);
						out = new BufferedOutputStream(new FileOutputStream(
								destination));
						byte[] buffer = new byte[BUFFER_SIZE];
						int count = 0;
						status.setStatusMsg(R.string.download_running);
						if (AppConfig.DEBUG)
							Log.d(TAG, "Getting size of download");
						status.setSize(httpEntity.getContentLength());
						if (AppConfig.DEBUG)
							Log.d(TAG, "Size is " + status.getSize());
						if (status.getSize() < 0) {
							status.setSize(DownloadStatus.SIZE_UNKNOWN);
						}

						long freeSpace = StorageUtils.getFreeSpaceAvailable();
						if (AppConfig.DEBUG)
							Log.d(TAG, "Free space is " + freeSpace);
						if (status.getSize() == DownloadStatus.SIZE_UNKNOWN
								|| status.getSize() <= freeSpace) {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Starting download");
							while (!cancelled
									&& (count = in.read(buffer)) != -1) {
								out.write(buffer, 0, count);
								status.setSoFar(status.getSoFar() + count);
								status.setProgressPercent((int) (((double) status
										.getSoFar() / (double) status.getSize()) * 100));
							}
							if (cancelled) {
								onCancelled();
							} else {
								onSuccess();
							}
						} else {
							onFail(DownloadError.ERROR_NOT_ENOUGH_SPACE, null);
						}
					} else {
						Log.w(TAG, "File already exists");
						onFail(DownloadError.ERROR_FILE_EXISTS, null);
					}
				} else {
					onFail(DownloadError.ERROR_DEVICE_NOT_FOUND, null);
				}
			} else {
				onFail(DownloadError.ERROR_HTTP_DATA_ERROR,
						String.valueOf(responseCode));
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			onFail(DownloadError.ERROR_MALFORMED_URL, e.getMessage());
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			onFail(DownloadError.ERROR_CONNECTION_ERROR, e.getMessage());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			onFail(DownloadError.ERROR_UNKNOWN_HOST, e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			onFail(DownloadError.ERROR_IO_ERROR, e.getMessage());
		} catch (NullPointerException e) {
			// might be thrown by connection.getInputStream()
			e.printStackTrace();
			onFail(DownloadError.ERROR_CONNECTION_ERROR, status.getFeedFile()
					.getDownload_url());
		} finally {
			IOUtils.closeQuietly(connection);
			IOUtils.closeQuietly(out);
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}

	private void onSuccess() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Download was successful");
		status.setSuccessful(true);
		status.setDone(true);
	}

	private void onFail(int reason, String reasonDetailed) {
		if (AppConfig.DEBUG) {
			Log.d(TAG, "Download failed");
		}
		status.setReason(reason);
		status.setReasonDetailed(reasonDetailed);
		status.setDone(true);
		status.setSuccessful(false);
	}

	private void onCancelled() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Download was cancelled");
		status.setReason(DownloadError.ERROR_DOWNLOAD_CANCELLED);
		status.setDone(true);
		status.setSuccessful(false);
		status.setCancelled(true);
	}

}
