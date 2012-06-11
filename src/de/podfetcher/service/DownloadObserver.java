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
public class DownloadObserver extends AsyncTask<FeedFile, DownloadObserver.DownloadStatus, Boolean> {
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
    private DownloadStatus[] statusList;

	public DownloadObserver(Context context) {
		super();
		this.context = context;
		requester = DownloadRequester.getInstance();
	}

    @Override
    protected void onCancelled(Boolean result) {
        Log.d(TAG, "Task was cancelled.");
    }

	protected Boolean doInBackground(FeedFile... files) {
		Log.d(TAG, "Background Task started.");
        statusList = new DownloadStatus[files.length];
        for (int i = 0; i < files.length; i++) {
            FeedFile feedfile = files[i];
            statusList[i] = new DownloadStatus(feedfile);

            if (feedfile.getFile_url() == null) {
                statusList[i].reason = NO_DOWNLOAD_FOUND;
                statusList[i].successful = false;
                statusList[i].done = true;
            }

            if (feedfile.isDownloaded()) {
                statusList[i].reason = ALREADY_DOWNLOADED;
                statusList[i].successful = false;
                statusList[i].done = true;
            }
        }


		while(downloadsLeft() && !isCancelled()) {
            for (DownloadStatus status : statusList) {
                if (status.done == false) {
                    Cursor cursor = getDownloadCursor(status.feedfile);
                    int statusId = getDownloadStatus(cursor, DownloadManager.COLUMN_STATUS);
                    getDownloadProgress(cursor, status);
                    switch(statusId) {
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
                            status.successful = Boolean.valueOf(false);
                            status.done = true;
                        case DownloadManager.STATUS_PENDING:
                            status.statusMsg = R.string.download_pending;
                            break;
                    }
                }
            }

			publishProgress(statusList);

			try {
				Thread.sleep(DEFAULT_WAITING_INTERVALL);
			} catch (InterruptedException e) {
				Log.w(TAG, "Thread was interrupted while waiting.");
			}
		}
        Log.d(TAG, "Background Task finished.");
        return Boolean.valueOf(true);
	}

	public Cursor getDownloadCursor(FeedFile feedfile) {
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

	private void getDownloadProgress(Cursor c, DownloadStatus status) {
        if (c.moveToFirst()) {
            status.size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            status.soFar = c.getLong(c.getColumnIndex(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            status.progressPercent = (int) ((
                        (double) status.soFar / (double) status.size) * 100);
            Log.d(TAG, "Setting progress to " + status.progressPercent);
        }
    }

	private DownloadManager.Query buildQuery(long id) {
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(id);
		return query;
	}

	public Context getContext() {
		return context;
	}

    public DownloadStatus[] getStatusList() {
        return statusList;
    }

    private boolean downloadsLeft() {
        boolean result = false;
        for (int i = 0; i < statusList.length; i++) {
            if (statusList[i].done == false) {
                return true;
            }
        }
        return result;
    }

    /** Contains status attributes for one download*/
    public class DownloadStatus {

        protected FeedFile feedfile;
        protected int progressPercent;
        protected long soFar;
        protected long size;
        protected int statusMsg;
        protected int reason;
        protected boolean successful;
        protected boolean done;

        public DownloadStatus(FeedFile feedfile) {
            this.feedfile = feedfile;
        }

        public FeedFile getFeedFile() {
            return feedfile;
        }

        public int getProgressPercent() {
            return progressPercent;
        }

        public long getSoFar() {
            return soFar;
        }

        public long getSize() {
            return size;
        }

        public int getStatusMsg() {
            return statusMsg;
        }

        public int getReason() {
            return reason;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }
}
