package de.podfetcher.service;

import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.R;
import android.content.Context;
import android.app.DownloadManager;
import android.util.Log;
import android.database.Cursor;
import java.util.concurrent.Callable;

/** Observes the status of a specific Download */
public class DownloadObserver extends Thread {
	private static final String TAG = "DownloadObserver";
	/* Download ID*/
	long id;
	Context context;
	Callable client;
	long waiting_intervall;
	private volatile int result;
	private volatile boolean done;
	private Cursor cursor;
	private final long DEFAULT_WAITING_INTERVALL = 500L;
	private DownloadRequester requester;
	private long time_passed;
	private long timeout;
	private boolean timedOut = false;

	public DownloadObserver(long id, Context c) {
		this.id = id;
		this.context = c;
		this.client = client;
		this.waiting_intervall = DEFAULT_WAITING_INTERVALL;
		done = false;
		requester = DownloadRequester.getInstance();
	}

	public DownloadObserver(long id, Context c, long timeout) {
		this(id, c);
		this.timeout = timeout;
	}

	public void run() {
		Log.d(TAG, "Thread started.");
		while(!isInterrupted() && !timedOut) {
			cursor = getDownloadCursor();
			int status = getDownloadStatus(cursor, DownloadManager.COLUMN_STATUS);
			switch(status) {
				case DownloadManager.STATUS_SUCCESSFUL:
					Log.d(TAG, "Download was successful.");
					done = true;
					result = R.string.download_successful;
					break;
				case DownloadManager.STATUS_RUNNING:
					Log.d(TAG, "Download is running.");
					result = R.string.download_running;
					break;
				case DownloadManager.STATUS_FAILED:
					Log.d(TAG, "Download failed.");
					result = R.string.download_failed;
					done = true;
					requester.notifyDownloadService(context);
					break;
				case DownloadManager.STATUS_PENDING:
					Log.d(TAG, "Download pending.");
					result = R.string.download_pending;
					break;

			}
			try {
				client.call();
			}catch (Exception e) {
				Log.e(TAG, "Error happened when calling client: " + e.getMessage());
			}

			if(done) {
				break;
			} else {
				try {
					sleep(waiting_intervall);
					if (timeout > 0) {
						time_passed += waiting_intervall;
						if(time_passed >= timeout) {
							Log.e(TAG, "Download timed out.");
							timedOut = true;
							try {
								client.call();
							}catch (Exception e) {
								Log.e(TAG, "Error happened when calling client: " + e.getMessage());
							}
							requester.cancelDownload(context, id);
						}
					}
				}catch (InterruptedException e) {
					Log.w(TAG, "Thread was interrupted while waiting.");
				}
			}
		}
		Log.d(TAG, "Thread stopped.");
	}

	public void setClient(Callable callable) {
		this.client = callable;
	}

	public Cursor getDownloadCursor() {
		DownloadManager.Query query = buildQuery(id);
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

	private DownloadManager.Query buildQuery(long id) {
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(id);
		return query;
	}

	public int getResult() {
		return result;
	}

	public boolean getDone() {
		return done;
	}

	public boolean isTimedOut() {
		return timedOut;
	}
}
