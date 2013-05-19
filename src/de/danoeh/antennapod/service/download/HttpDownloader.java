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
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;

public class HttpDownloader extends Downloader {
	private static final String TAG = "HttpDownloader";

	private static final int MAX_REDIRECTS = 5;

	private static final int BUFFER_SIZE = 8 * 1024;
	private static final int CONNECTION_TIMEOUT = 30000;
	private static final int SOCKET_TIMEOUT = 30000;

	public HttpDownloader(DownloaderCallback downloaderCallback,
			DownloadRequest request) {
		super(downloaderCallback, request);
	}

	private DefaultHttpClient createHttpClient() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		params.setIntParameter("http.protocol.max-redirects", MAX_REDIRECTS);
		params.setBooleanParameter("http.protocol.reject-relative-redirect",
				false);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpClientParams.setRedirecting(params, true);

		// Workaround for broken URLs in redirection
		((AbstractHttpClient) httpClient)
				.setRedirectHandler(new APRedirectHandler());
		return httpClient;
	}

	@Override
	protected void download() {
		DefaultHttpClient httpClient = null;
		OutputStream out = null;
		InputStream connection = null;
		try {
			HttpGet httpGet = new HttpGet(request.getSource());
			httpClient = createHttpClient();
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity httpEntity = response.getEntity();
			int responseCode = response.getStatusLine().getStatusCode();
			if (AppConfig.DEBUG)
				Log.d(TAG, "Response code is " + responseCode);
			if (responseCode == HttpURLConnection.HTTP_OK && httpEntity != null) {
				if (StorageUtils.storageAvailable(PodcastApp.getInstance())) {
					File destination = new File(request.getDestination());
					if (!destination.exists()) {
						connection = AndroidHttpClient
								.getUngzippedContent(httpEntity);
						InputStream in = new BufferedInputStream(connection);
						out = new BufferedOutputStream(new FileOutputStream(
								destination));
						byte[] buffer = new byte[BUFFER_SIZE];
						int count = 0;
						request.setStatusMsg(R.string.download_running);
						if (AppConfig.DEBUG)
							Log.d(TAG, "Getting size of download");
						request.setSize(httpEntity.getContentLength());
						if (AppConfig.DEBUG)
							Log.d(TAG, "Size is " + request.getSize());
						if (request.getSize() < 0) {
							request.setSize(DownloadStatus.SIZE_UNKNOWN);
						}

						long freeSpace = StorageUtils.getFreeSpaceAvailable();
						if (AppConfig.DEBUG)
							Log.d(TAG, "Free space is " + freeSpace);
						if (request.getSize() == DownloadStatus.SIZE_UNKNOWN
								|| request.getSize() <= freeSpace) {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Starting download");
							while (!cancelled
									&& (count = in.read(buffer)) != -1) {
								out.write(buffer, 0, count);
								request.setSoFar(request.getSoFar() + count);
								request.setProgressPercent((int) (((double) request
										.getSoFar() / (double) request
										.getSize()) * 100));
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
			onFail(DownloadError.ERROR_CONNECTION_ERROR, request.getSource());
		} finally {
			IOUtils.closeQuietly(connection);
			IOUtils.closeQuietly(out);
			if (httpClient != null) {
				httpClient.getConnectionManager().shutdown();
			}
		}
	}

	private void onSuccess() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Download was successful");
		result = new DownloadStatus(request, 0, true, false, null);
	}

	private void onFail(int reason, String reasonDetailed) {
		if (AppConfig.DEBUG) {
			Log.d(TAG, "Download failed");
		}
		result = new DownloadStatus(request, reason, false, false,
				reasonDetailed);
		cleanup();
	}

	private void onCancelled() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Download was cancelled");
		result = new DownloadStatus(request,
				DownloadError.ERROR_DOWNLOAD_CANCELLED, false, true, null);
		cleanup();
	}

	/** Deletes unfinished downloads. */
	private void cleanup() {
		if (request.getDestination() != null) {
			File dest = new File(request.getDestination());
			if (dest.exists()) {
				boolean rc = dest.delete();
				if (AppConfig.DEBUG)
					Log.d(TAG, "Deleted file " + dest.getName() + "; Result: "
							+ rc);
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "cleanup() didn't delete file: does not exist.");
			}
		}
	}

}
