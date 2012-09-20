package de.danoeh.antennapod.feed;

public class FeedImage extends FeedFile {
	protected String title;
	protected Feed feed;

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

	@Override
	public String getHumanReadableIdentifier() {
		if (feed != null && feed.getTitle() != null) {
			return feed.getTitle();
		} else {
			return download_url;
		}
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

	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}

}
