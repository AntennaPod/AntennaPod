package de.danoeh.antennapod.service.download;

import de.danoeh.antennapod.R;

import java.util.concurrent.Callable;

/** Downloads files */
public abstract class Downloader implements Callable<Downloader> {
	private static final String TAG = "Downloader";

	protected volatile boolean finished;

	protected volatile boolean cancelled;

	protected DownloadRequest request;
	protected DownloadStatus result;

	public Downloader(DownloadRequest request) {
		super();
		this.request = request;
		this.request.setStatusMsg(R.string.download_pending);
		this.cancelled = false;
        this.result = new DownloadStatus(request, null, false, false, null);
	}

	protected abstract void download();

	public final Downloader call() {
		download();
		if (result == null) {
			throw new IllegalStateException(
					"Downloader hasn't created DownloadStatus object");
		}
        finished = true;
		return this;
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