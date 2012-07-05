package de.podfetcher.feed;


public class FeedImage extends FeedFile {
	protected String title;

	public FeedImage(String download_url, String title) {
		super(null, download_url, false);
		this.download_url = download_url;
		this.title = title;
	}

	public FeedImage(long id, String title, String file_url,
			String download_url, boolean downloaded) {
		super(file_url, download_url, downloaded);
		this.id = id;
		this.title = title;
	}

	public FeedImage() {
		super();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
