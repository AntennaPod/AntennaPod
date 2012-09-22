package de.danoeh.antennapod.service.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;
import de.danoeh.antennapod.util.DownloadError;
import de.danoeh.antennapod.util.StorageUtils;

public class HttpDownloader extends Downloader {
	private static final String TAG = "HttpDownloader";

	private static final int BUFFER_SIZE = 8 * 1024;
	private static final int CONNECTION_TIMEOUT = 5000;

	public HttpDownloader(DownloadService downloadService, DownloadStatus status) {
		super(downloadService, status);
	}

	@Override
	protected void download() {
		HttpURLConnection connection = null;
		OutputStream out = null;
		try {
			status.setStatusMsg(R.string.download_pending);
			URL url = new URL(status.getFeedFile().getDownload_url());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(CONNECTION_TIMEOUT);
			if (AppConfig.DEBUG) {
				Log.d(TAG, "Connected to resource");
			}
			if (StorageUtils.externalStorageMounted()) {
				File destination = new File(status.getFeedFile().getFile_url());
				if (!destination.exists()) {
					InputStream in = new BufferedInputStream(
							connection.getInputStream());
					out = new BufferedOutputStream(new FileOutputStream(
							destination));
					byte[] buffer = new byte[BUFFER_SIZE];
					int count = 0;
					status.setStatusMsg(R.string.download_running);
					if (AppConfig.DEBUG)
						Log.d(TAG, "Getting size of download");
					status.setSize(connection.getContentLength());
					if (AppConfig.DEBUG)
						Log.d(TAG, "Size is " + status.getSize());
					if (status.getSize() == -1) {
						status.setSize(DownloadStatus.SIZE_UNKNOWN);
					}

					long freeSpace = StorageUtils.getFreeSpaceAvailable();
					if (AppConfig.DEBUG)
						Log.d(TAG, "Free space is " + freeSpace);
					if (status.getSize() == DownloadStatus.SIZE_UNKNOWN
							|| status.getSize() <= freeSpace) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Starting download");
						while (!cancelled && (count = in.read(buffer)) != -1) {
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
					onFail(DownloadError.ERROR_FILE_EXISTS, null);
				}
			} else {
				onFail(DownloadError.ERROR_DEVICE_NOT_FOUND, null);
			}
		} catch (MalformedURLException e) {
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
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
