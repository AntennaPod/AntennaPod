package de.danoeh.antennapod.net.download.serviceinterface;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.common.UrlChecker;
import de.danoeh.antennapod.model.feed.FeedMedia;

public class DownloadRequest implements Parcelable {
    public static final String REQUEST_ARG_PAGE_NR = "page";

    private final String destination;
    private final String source;
    private final String title;
    private String username;
    private String password;
    private String lastModified;
    private final long feedfileId;
    private final int feedfileType;
    private final Bundle arguments;

    private int progressPercent;
    private long soFar;
    private long size;
    private int statusMsg;
    private boolean mediaEnqueued;
    private boolean initiatedByUser;

    public DownloadRequest(@NonNull String destination, @NonNull String source, @NonNull String title, long feedfileId,
                           int feedfileType, String username, String password,
                           Bundle arguments, boolean initiatedByUser) {
        this(destination, source, title, feedfileId, feedfileType, null, username, password, false,
                arguments, initiatedByUser);
    }

    private DownloadRequest(Builder builder) {
        this(builder.destination, builder.source, builder.title, builder.feedfileId, builder.feedfileType,
                builder.lastModified, builder.username, builder.password, false,
                builder.arguments, builder.initiatedByUser);
    }

    private DownloadRequest(Parcel in) {
        this(in.readString(), in.readString(), in.readString(), in.readLong(), in.readInt(), in.readString(),
                nullIfEmpty(in.readString()), nullIfEmpty(in.readString()), in.readByte() > 0,
                in.readBundle(), in.readByte() > 0);
    }

    private DownloadRequest(String destination, String source, String title, long feedfileId, int feedfileType,
                            String lastModified, String username, String password,
                            boolean mediaEnqueued, Bundle arguments, boolean initiatedByUser) {
        this.destination = destination;
        this.source = source;
        this.title = title;
        this.feedfileId = feedfileId;
        this.feedfileType = feedfileType;
        this.lastModified = lastModified;
        this.username = username;
        this.password = password;
        this.mediaEnqueued = mediaEnqueued;
        this.arguments = arguments;
        this.initiatedByUser = initiatedByUser;
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
        // in case of null username/password, still write an empty string
        // (rather than skipping it). Otherwise, unmarshalling  a collection
        // of them from a Parcel (from an Intent extra to submit a request to DownloadService) will fail.
        //
        // see: https://stackoverflow.com/a/22926342
        dest.writeString(nonNullString(username));
        dest.writeString(nonNullString(password));
        dest.writeByte((mediaEnqueued) ? (byte) 1 : 0);
        dest.writeBundle(arguments);
        dest.writeByte(initiatedByUser ? (byte) 1 : 0);
    }

    private static String nonNullString(String str) {
        return str != null ? str : "";
    }

    private static String nullIfEmpty(String str) {
        return TextUtils.isEmpty(str) ? null : str;
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
        if (!(o instanceof DownloadRequest)) return false;

        DownloadRequest that = (DownloadRequest) o;

        if (lastModified != null ? !lastModified.equals(that.lastModified) : that.lastModified != null)
            return false;
        if (feedfileId != that.feedfileId) return false;
        if (feedfileType != that.feedfileType) return false;
        if (progressPercent != that.progressPercent) return false;
        if (size != that.size) return false;
        if (soFar != that.soFar) return false;
        if (statusMsg != that.statusMsg) return false;
        if (!destination.equals(that.destination)) return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (!source.equals(that.source)) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null)
            return false;
        if (mediaEnqueued != that.mediaEnqueued) return false;
        if (initiatedByUser != that.initiatedByUser) return false;
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
        result = 31 * result + (int) (feedfileId ^ (feedfileId >>> 32));
        result = 31 * result + feedfileType;
        result = 31 * result + arguments.hashCode();
        result = 31 * result + progressPercent;
        result = 31 * result + (int) (soFar ^ (soFar >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + statusMsg;
        result = 31 * result + (mediaEnqueued ? 1 : 0);
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

    public Bundle getArguments() {
        return arguments;
    }

    public static class Builder {
        private final String destination;
        private String source;
        private final String title;
        private String username;
        private String password;
        private String lastModified;
        private final long feedfileId;
        private final int feedfileType;
        private final Bundle arguments = new Bundle();
        private boolean initiatedByUser = true;

        public Builder(@NonNull String destination, @NonNull FeedMedia media) {
            this.destination = destination;
            this.source = UrlChecker.prepareUrl(media.getDownload_url());
            this.title = media.getHumanReadableIdentifier();
            this.feedfileId = media.getId();
            this.feedfileType = media.getTypeAsInt();
        }

        public Builder(@NonNull String destination, @NonNull Feed feed) {
            this.destination = destination;
            this.source = feed.isLocalFeed() ? feed.getDownload_url() : UrlChecker.prepareUrl(feed.getDownload_url());
            this.title = feed.getHumanReadableIdentifier();
            this.feedfileId = feed.getId();
            this.feedfileType = feed.getTypeAsInt();
            arguments.putInt(REQUEST_ARG_PAGE_NR, feed.getPageNr());
        }

        public Builder withInitiatedByUser(boolean initiatedByUser) {
            this.initiatedByUser = initiatedByUser;
            return this;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setForce(boolean force) {
            if (force) {
                lastModified = null;
            }
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
    }
}
