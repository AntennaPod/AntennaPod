package de.danoeh.antennapod.core.service.download;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.danoeh.antennapod.core.feed.FeedFile;
import de.danoeh.antennapod.core.util.URLChecker;

public class DownloadRequest implements Parcelable {

    private final String destination;
    private final String source;
    private final String title;
    private String username;
    private String password;
    private String lastModified;
    private final boolean deleteOnFailure;
    private final long feedfileId;
    private final int feedfileType;
    private final Bundle arguments;

    private int progressPercent;
    private long soFar;
    private long size;
    private int statusMsg;

    public DownloadRequest(@NonNull String destination,
                           @NonNull String source,
                           @NonNull String title,
                           long feedfileId,
                           int feedfileType,
                           String username,
                           String password,
                           boolean deleteOnFailure,
                           Bundle arguments) {

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

    private DownloadRequest(Builder builder) {
        this.destination = builder.destination;
        this.source = builder.source;
        this.title = builder.title;
        this.feedfileId = builder.feedfileId;
        this.feedfileType = builder.feedfileType;
        this.username = builder.username;
        this.password = builder.password;
        this.lastModified = builder.lastModified;
        this.deleteOnFailure = builder.deleteOnFailure;
        this.arguments = (builder.arguments != null) ? builder.arguments : new Bundle();
    }

    private DownloadRequest(Parcel in) {
        destination = in.readString();
        source = in.readString();
        title = in.readString();
        feedfileId = in.readLong();
        feedfileType = in.readInt();
        lastModified = in.readString();
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
        dest.writeString(lastModified);
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

        if (lastModified != null ? !lastModified.equals(that.lastModified) : that.lastModified != null)
            return false;
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
        result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
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

    public DownloadRequest setLastModified(@Nullable String lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    @Nullable
    public String getLastModified() {
        return lastModified;
    }

    public boolean isDeleteOnFailure() {
        return deleteOnFailure;
    }

    public Bundle getArguments() {
        return arguments;
    }

    public static class Builder {
        private final String destination;
        private final String source;
        private final String title;
        private String username;
        private String password;
        private String lastModified;
        private boolean deleteOnFailure = false;
        private final long feedfileId;
        private final int feedfileType;
        private Bundle arguments;

        public Builder(@NonNull String destination, @NonNull FeedFile item) {
            this.destination = destination;
            this.source = URLChecker.prepareURL(item.getDownload_url());
            this.title = item.getHumanReadableIdentifier();
            this.feedfileId = item.getId();
            this.feedfileType = item.getTypeAsInt();
        }

        public Builder deleteOnFailure(boolean deleteOnFailure) {
            this.deleteOnFailure = deleteOnFailure;
            return this;
        }

        public Builder lastModified(String lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder withAuthentication(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(this);
        }

        public Builder withArguments(Bundle args) {
            this.arguments = args;
            return this;
        }

    }
}
