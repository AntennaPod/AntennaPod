package de.danoeh.antennapod.service.download;

import android.os.Handler;

/** Downloads files */
public abstract class Downloader extends Thread {
	private static final String TAG = "Downloader";
	private Handler handler;
	private DownloadService downloadService;
	
	protected boolean finished;
	
	protected String destination;
	protected String source;
	
	

	public Downloader(DownloadService downloadService, String destination, String source) {
		super();
		this.downloadService = downloadService;
		this.destination = destination;
		this.source = source;
		handler = new Handler();
	}

	/**
	 * This method must be called when the download was completed, failed,
	 * or was cancelled
	 */
	protected void finish() {
		if (!finished) {
			finished = true;
			handler.post(new Runnable() {

				@Override
				public void run() {
					downloadService.queryDownloads();
				}

			});
		}
	}

	protected abstract void download();
	
	@Override
	public final void run() {
		download();
	}

}