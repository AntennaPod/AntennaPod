package de.podfetcher.feed;

/** Represents a component of a Feed that has to be downloaded*/
public abstract class FeedFile extends FeedComponent {
	protected String file_url;
	protected String download_url;
	protected long downloadId;		// temporary id given by the Android DownloadManager
	
	public String getFile_url() {
		return file_url;
	}
	public void setFile_url(String file_url) {
		this.file_url = file_url;
	}
	public String getDownload_url() {
		return download_url;
	}
	public void setDownload_url(String download_url) {
		this.download_url = download_url;
	}

	public long getDownloadId() {
		return downloadId;
	}

	public void setDownloadId(long downloadId) {
		this.downloadId = downloadId;
	}
}
