package de.podfetcher.asynctask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import de.podfetcher.R;
import de.podfetcher.feed.FeedFile;
import de.podfetcher.storage.DownloadRequester;

/** Observes the status of a specific Download */
public class DownloadObserver extends AsyncTask<Void, Void, Void> {
	private static final String TAG = "DownloadObserver";

	/** Types of downloads to observe. */
	public static final int TYPE_FEED = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_MEDIA = 2;

	/** Error codes */
	public static final int ALREADY_DOWNLOADED = 1;
	public static final int NO_DOWNLOAD_FOUND = 2;

	private final long DEFAULT_WAITING_INTERVALL = 1000L;

	private DownloadRequester requester;
	private Context context;
	private ArrayList<DownloadStatus> statusList;
	private List<DownloadObserver.Callback> observer;

	public DownloadObserver(Context context) {
		super();
		this.context = context;
		requester = DownloadRequester.getInstance();
		statusList = new ArrayList<DownloadStatus>();
		observer = Collections
				.synchronizedList(new ArrayList<DownloadObserver.Callback>());
	}

	@Override
	protected void onCancelled() {
		Log.d(TAG, "Task was cancelled.");
	}

	protected Void doInBackground(Void... params) {
		Log.d(TAG, "Background Task started.");
		while (downloadsLeft() && !isCancelled()) {
			refreshStatuslist();
			publishProgress();
			try {
				Thread.sleep(DEFAULT_WAITING_INTERVALL);
			} catch (InterruptedException e) {
				Log.w(TAG, "Thread was interrupted while waiting.");
			}
		}
		Log.d(TAG, "Background Task finished.");
		return null;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		for (DownloadObserver.Callback callback : observer) {
			callback.onProgressUpdate();
		}
	}

	private void refreshStatuslist() {
		Cursor cursor = getDownloadCursor();
		if (cursor.moveToFirst()) {
			do {
				long downloadId = getDownloadStatus(cursor,
						DownloadManager.COLUMN_ID);
				FeedFile feedFile = requester.getFeedFile(downloadId);
				DownloadStatus status = findDownloadStatus(feedFile);
				if (status == null) {
					status = new DownloadStatus(feedFile);
					statusList.add(status);
				}

				// refresh status
				int statusId = getDownloadStatus(cursor,
						DownloadManager.COLUMN_STATUS);
				getDownloadProgress(cursor, status);
				switch (statusId) {
				case DownloadManager.STATUS_SUCCESSFUL:
					status.statusMsg = R.string.download_successful;
					status.successful = true;
					status.done = true;
				case DownloadManager.STATUS_RUNNING:
					status.statusMsg = R.string.download_running;
					break;
				case DownloadManager.STATUS_FAILED:
					status.statusMsg = R.string.download_failed;
					requester.notifyDownloadService(context);
					status.successful = false;
					status.done = true;
					status.reason = getDownloadStatus(cursor,
							DownloadManager.COLUMN_REASON);
				case DownloadManager.STATUS_PENDING:
					status.statusMsg = R.string.download_pending;
					break;
				default:
					status.done = true;
					status.successful = false;
					status.statusMsg = R.string.download_cancelled_msg;
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
	}

	/** Request a cursor with all running Feedfile downloads */
	private Cursor getDownloadCursor() {
		// Collect download ids
		int numDownloads = requester.getNumberOfDownloads();
		long ids[] = new long[numDownloads];
		for (int i = 0; i < numDownloads; i++) {
			ids[i] = requester.downloads.get(i).getDownloadId();
		}
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(ids);
		DownloadManager manager = (DownloadManager) context
				.getSystemService(Context.DOWNLOAD_SERVICE);

		Cursor result = manager.query(query);
		return result;
	}

	/** Return value of a specific column */
	private int getDownloadStatus(Cursor c, String column) {
		int status = c.getInt(c.getColumnIndex(column));
		return status;
	}

	private void getDownloadProgress(Cursor c, DownloadStatus status) {
		status.size = c.getLong(c
				.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
		status.soFar = c
				.getLong(c
						.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
		status.progressPercent = (int) (((double) status.soFar / (double) status.size) * 100);
		Log.d(TAG, "Setting progress to " + status.progressPercent);
	}

	public Context getContext() {
		return context;
	}

	/** Find a DownloadStatus entry by its FeedFile */
	public DownloadStatus findDownloadStatus(FeedFile f) {
		for (DownloadStatus status : statusList) {
			if (status.feedfile == f) {
				return status;
			}
		}
		return null;
	}

	public ArrayList<DownloadStatus> getStatusList() {
		return statusList;
	}

	private boolean downloadsLeft() {
		for (DownloadStatus status : statusList) {
			if (status.done == false) {
				return true;
			}
		}
		return false;
	}

	public void registerCallback(DownloadObserver.Callback callback) {
		observer.add(callback);
	}

	public void unregisterCallback(DownloadObserver.Callback callback) {
		observer.remove(callback);
	}

	public interface Callback {
		public void onProgressUpdate();
	}

}
