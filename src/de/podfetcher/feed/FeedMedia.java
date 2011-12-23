package de.podfetcher.feed;

public class FeedMedia extends FeedFile{
	public long length;
	public long position;
	public long size;	// File size in Byte
	public String mime_type;
	
	public FeedItem item; // TODO remove

	public FeedMedia(FeedItem i, String download_url, long size, String mime_type) {
		this.item = i;
		this.download_url = download_url;
		this.size = size;
		this.mime_type = mime_type;
	}

	public FeedMedia(long id, long length, long position, long size, String mime_type,
			String file_url, String download_url) {
		super();
		this.id = id;
		this.length = length;
		this.position = position;
		this.size = size;
		this.mime_type = mime_type;
		this.file_url = file_url;
		this.download_url = download_url;
	}
	
	
	
	
}
