package de.danoeh.antennapod.service.download;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.DownloadStatus;

/** Downloads files */
public abstract class Downloader extends Thread {
	private static final String TAG = "Downloader";
	private DownloaderCallback downloaderCallback;

	protected boolean finished;

	protected volatile boolean cancelled;

	protected volatile DownloadStatus status;

	public Downloader(DownloaderCallback downloaderCallback,
			DownloadStatus status) {
		super();
		this.downloaderCallback = downloaderCallback;
		this.status = status;
		this.status.setStatusMsg(R.string.download_pending);
		this.cancelled = false;
	}

	/**
	 * This method must be called when the download was completed, failed, or
	 * was cancelled
	 */
	protected void finish() {
		if (!finished) {
			finished = true;
			downloaderCallback.onDownloadCompleted(this);
		}
	}

	protected abstract void download();

	@Override
	public final void run() {
		download();
		finish();
	}

	public DownloadStatus getStatus() {
		return status;
	}

	public void cancel() {
		cancelled = true;
	}

}