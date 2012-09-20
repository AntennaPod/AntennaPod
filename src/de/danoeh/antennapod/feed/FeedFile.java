package de.danoeh.antennapod.feed;

/** Represents a component of a Feed that has to be downloaded */
public abstract class FeedFile extends FeedComponent {
	protected String file_url;
	protected String download_url;
	protected boolean downloaded;

	public FeedFile(String file_url, String download_url, boolean downloaded) {
		super();
		this.file_url = file_url;
		this.download_url = download_url;
		this.downloaded = downloaded;
	}

	public FeedFile() {
		this(null, null, false);
	}

	/**
	 * Should return a non-null, human-readable String so that the item can be
	 * identified by the user. Can be title, download-url, etc.
	 */
	public abstract String getHumanReadableIdentifier();

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

	public boolean isDownloaded() {
		return downloaded;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}
}
