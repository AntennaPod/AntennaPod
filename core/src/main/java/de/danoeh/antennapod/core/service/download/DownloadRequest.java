package de.danoeh.antennapod.core.service.download;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.lang3.Validate;

public class DownloadRequest implements Parcelable {

    private final String destination;
    private final String source;
    private final String title;
    private String username;
    private String password;
    private boolean deleteOnFailure;
    private final long feedfileId;
    private final int feedfileType;
    private final Bundle arguments;

    protected int progressPercent;
    protected long soFar;
    protected long size;
    protected int statusMsg;

    public DownloadRequest(String destination, String source, String title,
                           long feedfileId, int feedfileType, String username, String password, boolean deleteOnFailure, Bundle arguments) {
        Validate.notNull(destination);
        Validate.notNull(source);
        Validate.notNull(title);

        this.destination = destination;
        this.source = source;
        this.title = title;
        this.feedfileId = feedfileId;
        this.feedfileType = feedfileType;
        this.username = username;
        this.password = password;
        this.deleteOnFailure = deleteOnFailure;
        this.arguments = (arguments != null) ? arguments : new Bundle();
    }

    public DownloadRequest(String destination, String source, String title,
                           long feedfileId, int feedfileType) {
        this(destination, source, title, feedfileId, feedfileType, null, null, true, null);
    }

    private DownloadRequest(Parcel in) {
        destination = in.readString();
        source = in.readString();
        title = in.readString();
        feedfileId = in.readLong();
        feedfileType = in.readInt();
        deleteOnFailure = (in.readByte() > 0);
        arguments = in.readBundle();
        if (in.dataAvail() > 0) {
            username = in.readString();
        } else {
            username = null;
        }
        if (in.dataAvail() > 0) {
            password = in.readString();
        } else {
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
        dest.writeByte((deleteOnFailure) ? (byte) 1 : 0);
        dest.writeBundle(arguments);
        if (username != null) {
            dest.writeString(username);
        }
        if (password != null) {
            dest.writeString(password);
        }
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadRequest that = (DownloadRequest) o;

        if (deleteOnFailure != that.deleteOnFailure) return false;
        if (feedfileId != that.feedfileId) return false;
        if (feedfileType != that.feedfileType) return false;
        if (progressPercent != that.progressPercent) return false;
        if (size != that.size) return false;
        if (soFar != that.soFar) return false;
        if (statusMsg != that.statusMsg) return false;
        if (!arguments.equals(that.arguments)) return false;
        if (!destination.equals(that.destination)) return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (!source.equals(that.source)) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = destination.hashCode();
        result = 31 * result + source.hashCode();
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (deleteOnFailure ? 1 : 0);
        result = 31 * result + (int) (feedfileId ^ (feedfileId >>> 32));
        result = 31 * result + feedfileType;
        result = 31 * result + arguments.hashCode();
        result = 31 * result + progressPercent;
        result = 31 * result + (int) (soFar ^ (soFar >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + statusMsg;
        return result;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDeleteOnFailure() {
        return deleteOnFailure;
    }

    public Bundle getArguments() {
        return arguments;
    }
}
