package de.danoeh.antennapod.feed;

import java.util.Date;

public class FeedMedia extends FeedFile {

	public static final int FEEDFILETYPE_FEEDMEDIA = 2;

	private int duration;
	private int position; // Current position in file
	private long size; // File size in Byte
	private String mime_type;
	private FeedItem item;
	private Date playbackCompletionDate;

	public FeedMedia(FeedItem i, String download_url, long size,
			String mime_type) {
		super(null, download_url, false);
		this.item = i;
		this.size = size;
		this.mime_type = mime_type;
	}

	public FeedMedia(long id, FeedItem item, int duration, int position,
			long size, String mime_type, String file_url, String download_url,
			boolean downloaded, Date playbackCompletionDate) {
		super(file_url, download_url, downloaded);
		this.id = id;
		this.item = item;
		this.duration = duration;
		this.position = position;
		this.size = size;
		this.mime_type = mime_type;
		this.playbackCompletionDate = playbackCompletionDate;
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

	/** Uses mimetype to determine the type of media. */
	public MediaType getMediaType() {
		if (mime_type == null || mime_type.isEmpty()) {
			return MediaType.UNKNOWN;
		} else {
			if (mime_type.startsWith("audio")) {
				return MediaType.AUDIO;
			} else if (mime_type.startsWith("video")) {
				return MediaType.VIDEO;
			} else if (mime_type.equals("application/ogg")) {
				return MediaType.AUDIO;
			}
		}
		return MediaType.UNKNOWN;
	}

	public void updateFromOther(FeedMedia other) {
		super.updateFromOther(other);
		if (other.size > 0) {
			size = other.size;
		}
		if (other.mime_type != null) {
			mime_type = other.mime_type;
		}
	}

	public boolean compareWithOther(FeedMedia other) {
		if (super.compareWithOther(other)) {
			return true;
		}
		if (other.mime_type != null) {
			if (mime_type == null || !mime_type.equals(other.mime_type)) {
				return true;
			}
		}
		if (other.size > 0 && other.size != size) {
			return true;
		}
		return false;
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

	public Date getPlaybackCompletionDate() {
		return playbackCompletionDate;
	}

	public void setPlaybackCompletionDate(Date playbackCompletionDate) {
		this.playbackCompletionDate = playbackCompletionDate;
	}

	public boolean isInProgress() {
		return (this.position > 0);
	}
	
	public FeedImage getImage() {
		if (item != null && item.getFeed() != null) {
			return item.getFeed().getImage();
		}
		return null;
	}

}
