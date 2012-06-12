package de.podfetcher.feed;

public class FeedMedia extends FeedFile{
	private int duration;
	private int position;	// Current position in file
	private long size;	// File size in Byte
	private String mime_type;
	private FeedItem item;

	public FeedMedia(FeedItem i, String download_url, long size, String mime_type) {
		super();
		this.item = i;
		this.download_url = download_url;
		this.size = size;
		this.mime_type = mime_type;
	}

	public FeedMedia(long id, FeedItem item, int duration, int position, long size, String mime_type,
			String file_url, String download_url) {
		super();
		this.id = id;
		this.item = item;
		this.duration = duration;
		this.position = position;
		this.size = size;
		this.mime_type = mime_type;
		this.file_url = file_url;
		this.download_url = download_url;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getMime_type() {
		return mime_type;
	}

	public void setMime_type(String mime_type) {
		this.mime_type = mime_type;
	}

	public FeedItem getItem() {
		return item;
	}

	public void setItem(FeedItem item) {
		this.item = item;
	}
	
	
	
	
	
	
}
