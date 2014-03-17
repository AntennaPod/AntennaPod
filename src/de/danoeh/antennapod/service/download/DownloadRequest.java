package de.danoeh.antennapod.service.download;

import android.os.Parcel;
import android.os.Parcelable;

public class DownloadRequest implements Parcelable {

	private final String destination;
	private final String source;
	private final String title;
    private final String username;
    private final String password;
	private final long feedfileId;
	private final int feedfileType;

	protected int progressPercent;
	protected long soFar;
	protected long size;
	protected int statusMsg;

	public DownloadRequest(String destination, String source, String title,
			long feedfileId, int feedfileType, String username, String password) {
		if (destination == null) {
			throw new IllegalArgumentException("Destination must not be null");
		}
		if (source == null) {
			throw new IllegalArgumentException("Source must not be null");
		}
		if (title == null) {
			throw new IllegalArgumentException("Title must not be null");
		}

		this.destination = destination;
		this.source = source;
		this.title = title;
		this.feedfileId = feedfileId;
		this.feedfileType = feedfileType;
        this.username = username;
        this.password = password;
	}

    public DownloadRequest(String destination, String source, String title,
                           long feedfileId, int feedfileType) {
        this(destination, source, title, feedfileId, feedfileType, null, null);
    }

	private DownloadRequest(Parcel in) {
		destination = in.readString();
		source = in.readString();
		title = in.readString();
		feedfileId = in.readLong();
		feedfileType = in.readInt();
        if (in.dataAvail() > 0) {
            username = in.readString();
            password = in.readString();
        } else {
            username = null;
            password = null;
        }
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(destination);
		dest.writeString(source);
		dest.writeString(title);
		dest.writeLong(feedfileId);
		dest.writeInt(feedfileType);
	}

	public static final Parcelable.Creator<DownloadRequest> CREATOR = new Parcelable.Creator<DownloadRequest>() {
		public DownloadRequest createFromParcel(Parcel in) {
			return new DownloadRequest(in);
		}

		public DownloadRequest[] newArray(int size) {
			return new DownloadRequest[size];
		}
	};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + (int) (feedfileId ^ (feedfileId >>> 32));
		result = prime * result + feedfileType;
		result = prime * result + progressPercent;
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + (int) (soFar ^ (soFar >>> 32));
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + statusMsg;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DownloadRequest other = (DownloadRequest) obj;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (feedfileId != other.feedfileId)
			return false;
		if (feedfileType != other.feedfileType)
			return false;
		if (progressPercent != other.progressPercent)
			return false;
		if (size != other.size)
			return false;
		if (soFar != other.soFar)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (statusMsg != other.statusMsg)
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	public String getDestination() {
		return destination;
	}

	public String getSource() {
		return source;
	}

	public String getTitle() {
		return title;
	}

	public long getFeedfileId() {
		return feedfileId;
	}

	public int getFeedfileType() {
		return feedfileType;
	}

	public int getProgressPercent() {
		return progressPercent;
	}

	public void setProgressPercent(int progressPercent) {
		this.progressPercent = progressPercent;
	}

	public long getSoFar() {
		return soFar;
	}

	public void setSoFar(long soFar) {
		this.soFar = soFar;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getStatusMsg() {
		return statusMsg;
	}

	public void setStatusMsg(int statusMsg) {
		this.statusMsg = statusMsg;
	}

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
