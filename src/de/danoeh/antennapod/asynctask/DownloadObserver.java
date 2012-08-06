package de.danoeh.antennapod.asynctask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import de.danoeh.antennapod.feed.FeedFile;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;

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
	private List<DownloadStatus> statusList;
	private List<DownloadObserver.Callback> observer;
	private Handler contentChanger;

	public DownloadObserver(Context context) {
		super();
		this.context = context;
		requester = DownloadRequester.getInstance();
		statusList = new CopyOnWriteArrayList<DownloadStatus>();
		observer = Collections
				.synchronizedList(new ArrayList<DownloadObserver.Callback>());
		contentChanger = new Handler();
	}

	@Override
	protected void onCancelled() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Task was cancelled.");
		statusList.clear();
		for (DownloadObserver.Callback callback : observer) {
			callback.onFinish();
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Background task has finished");
		statusList.clear();
		for (DownloadObserver.Callback callback : observer) {
			callback.onFinish();
		}
	}

	protected Void doInBackground(Void... params) {
		if (AppConfig.DEBUG)
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
		if (AppConfig.DEBUG)
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
		final ArrayList<DownloadStatus> unhandledItems = new ArrayList<DownloadStatus>(
				statusList);

		Cursor cursor = getDownloadCursor();
		if (cursor.moveToFirst()) {
			do {
				long downloadId = getDownloadStatus(cursor,
						DownloadManager.COLUMN_ID);
				FeedFile feedFile = requester.getFeedFile(downloadId);
				if (feedFile != null) {
					DownloadStatus status = findDownloadStatus(feedFile);

					if (status == null) {
						status = new DownloadStatus(feedFile);
						final DownloadStatus statusToAdd = status;
						contentChanger.post(new Runnable() {
							@Override
							public void run() {
								statusList.add(statusToAdd);
								publishProgress();
							}
						});
					} else {
						unhandledItems.remove(status);
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
						break;
					case DownloadManager.STATUS_PENDING:
						status.statusMsg = R.string.download_pending;
						break;
					default:
						status.done = true;
						status.successful = false;
						status.statusMsg = R.string.download_cancelled_msg;
						requester.notifyDownloadService(context);
					}
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		contentChanger.post(new Runnable() {

			@Override
			public void run() {
				// remove unhandled items from statuslist
				for (DownloadStatus status : unhandledItems) {
					statusList.remove(status);
				}
				publishProgress();
			}
		});

	}

	/** Request a cursor with all running Feedfile downloads */
	private Cursor getDownloadCursor() {
		// Collect download ids

		ArrayList<Long> ids = new ArrayList<Long>();
		for (FeedFile download : requester.getDownloads()) {
			ids.add(download.getDownloadId());
		}
		DownloadManager.Query query = new DownloadManager.Query();
		long[] pIds = new long[ids.size()];
		for (int x = 0; x < ids.size(); x++) {
			pIds[x] = ids.get(x);
		}
		query.setFilterById(pIds);
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

	public List<DownloadStatus> getStatusList() {
		return statusList;
	}

	private boolean downloadsLeft() {
		return !requester.hasNoDownloads();
	}

	public void registerCallback(DownloadObserver.Callback callback) {
		observer.add(callback);
	}

	public void unregisterCallback(DownloadObserver.Callback callback) {
		observer.remove(callback);
	}

	public interface Callback {
		public void onProgressUpdate();

		public void onFinish();
	}

}
