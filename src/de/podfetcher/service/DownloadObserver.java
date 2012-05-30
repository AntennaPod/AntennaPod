package de.podfetcher.service;

import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.feed.*;
import de.podfetcher.R;
import android.content.Context;
import android.app.DownloadManager;
import android.util.Log;
import android.database.Cursor;
import java.util.concurrent.Callable;
import android.os.AsyncTask;

/** Observes the status of a specific Download */
public class DownloadObserver extends AsyncTask<FeedFile, Integer, Boolean> {
	private static final String TAG = "DownloadObserver";

	/** Types of downloads to observe. */
	public static final int TYPE_FEED = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_MEDIA = 2;

	/** Error codes */
	public static final int ALREADY_DOWNLOADED = 1;
	public static final int NO_DOWNLOAD_FOUND = 2;

	private final long DEFAULT_WAITING_INTERVALL = 1000L;

	private int progressPercent;
	private int statusMsg;

	private int reason;

	private DownloadRequester requester;
	private FeedFile feedfile;
	private Context context;

	public DownloadObserver(Context context) {
		super();
		this.context = context;
	}


	protected Boolean doInBackground(FeedFile... files) {
		Log.d(TAG, "Background Task started.");

		feedfile = files[0];
		if (feedfile.getFile_url() == null) {
			reason = NO_DOWNLOAD_FOUND;
			return Boolean.valueOf(false);
		}

		if (feedfile.isDownloaded()) {
			reason = ALREADY_DOWNLOADED;
			return Boolean.valueOf(false);
		}

		while(true) {
			Cursor cursor = getDownloadCursor();
			int status = getDownloadStatus(cursor, DownloadManager.COLUMN_STATUS);
			int progressPercent = getDownloadProgress(cursor);
			switch(status) {
				case DownloadManager.STATUS_SUCCESSFUL:
					statusMsg = R.string.download_successful;
					return Boolean.valueOf(true);
				case DownloadManager.STATUS_RUNNING:
					statusMsg = R.string.download_running;
					break;
				case DownloadManager.STATUS_FAILED:
					statusMsg = R.string.download_failed;
					requester.notifyDownloadService(context);
					return Boolean.valueOf(false);
				case DownloadManager.STATUS_PENDING:
					statusMsg = R.string.download_pending;
					break;

			}

			publishProgress(progressPercent);

			try {
				Thread.sleep(DEFAULT_WAITING_INTERVALL);
			} catch (InterruptedException e) {
				Log.w(TAG, "Thread was interrupted while waiting.");
			}
		}
	}

	public Cursor getDownloadCursor() {
		DownloadManager.Query query = buildQuery(feedfile.getDownloadId());
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

		Cursor result = manager.query(query);
		return result;
	}
	public int getDownloadStatus(Cursor c, String column) {
		if(c.moveToFirst()) {
			int status = c.getInt(c.getColumnIndex(column));
			return status;	
		} else {
			return -1;
		}
	}

	private int getDownloadProgress(Cursor c) {
		if (c.moveToFirst()) {
			long size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
			long soFar = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
			int progress = (int) (((double) soFar / (double) size) * 100);
			Log.d(TAG, "Setting progress to " + progress);
			return progress;
		} else {
			return -1;
		}
	}

	private DownloadManager.Query buildQuery(long id) {
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(id);
		return query;
	}

	public int getProgressPercent() {
		return progressPercent;
	}

	public int getStatusMsg() {
		return statusMsg;
	}

	public Context getContext() {
		return context;
	}
}
