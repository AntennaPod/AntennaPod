package de.danoeh.antennapod.service.download;

import de.danoeh.antennapod.R;

/** Downloads files */
public abstract class Downloader extends Thread {
	private static final String TAG = "Downloader";
	private DownloaderCallback downloaderCallback;

	protected boolean finished;

	protected volatile boolean cancelled;

	protected volatile DownloadRequest request;
	protected volatile DownloadStatus result;

	public Downloader(DownloaderCallback downloaderCallback,
			DownloadRequest request) {
		super();
		this.downloaderCallback = downloaderCallback;
		this.request = request;
		this.request.setStatusMsg(R.string.download_pending);
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
		if (result == null) {
			throw new IllegalStateException(
					"Downloader hasn't created DownloadStatus object");
		}
		finish();
	}

	public DownloadRequest getDownloadRequest() {
		return request;
	}

	public DownloadStatus getResult() {
		return result;
	}

	public boolean isFinished() {
		return finished;
	}

	public void cancel() {
		cancelled = true;
	}

}