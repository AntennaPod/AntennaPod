package de.podfetcher.feed;

public class FeedMedia extends FeedFile{
	private long length;
	private long position;
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

	public FeedMedia(long id, FeedItem item, long length, long position, long size, String mime_type,
			String file_url, String download_url) {
		super();
		this.id = id;
		this.item = item;
		this.length = length;
		this.position = position;
		this.size = size;
		this.mime_type = mime_type;
		this.file_url = file_url;
		this.download_url = download_url;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
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
