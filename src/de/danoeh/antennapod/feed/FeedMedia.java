package de.danoeh.antennapod.feed;

public class FeedMedia extends FeedFile {

	public static final int FEEDFILETYPE_FEEDMEDIA = 2;

	private int duration;
	private int position; // Current position in file
	private long size; // File size in Byte
	private String mime_type;
	private FeedItem item;

	public FeedMedia(FeedItem i, String download_url, long size,
			String mime_type) {
		super(null, download_url, false);
		this.item = i;
		this.size = size;
		this.mime_type = mime_type;
	}

	public FeedMedia(long id, FeedItem item, int duration, int position,
			long size, String mime_type, String file_url, String download_url,
			boolean downloaded) {
		super(file_url, download_url, downloaded);
		this.id = id;
		this.item = item;
		this.duration = duration;
		this.position = position;
		this.size = size;
		this.mime_type = mime_type;
	}

	public FeedMedia(long id, FeedItem item) {
		super();
		this.id = id;
		this.item = item;
	}

	@Override
	public String getHumanReadableIdentifier() {
		if (item != null && item.getTitle() != null) {
			return item.getTitle();
		} else {
			return download_url;
		}
	}

	@Override
	public int getTypeAsInt() {
		return FEEDFILETYPE_FEEDMEDIA;
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
