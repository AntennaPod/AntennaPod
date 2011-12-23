package de.podfetcher.feed;

public class FeedImage extends FeedFile {
	public String title;

	public FeedImage(String download_url, String title) {
		super();
		this.download_url = download_url;
		this.title = title;
	}
	
	public FeedImage(long id, String title, String file_url, String download_url) {
		this.id = id;
		this.title = title;
		this.file_url = file_url;
		this.download_url = download_url;
	}

	public FeedImage() {
		
	}
	
	
	
	
}
