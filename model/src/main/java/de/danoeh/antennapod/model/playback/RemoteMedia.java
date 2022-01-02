package de.danoeh.antennapod.model.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Playable implementation for media for which a local version of
 * {@link FeedMedia} hasn't been found.
 * Used for Casting and for previewing unsubscribed feeds.
 */
public class RemoteMedia implements Playable {
    public static final String TAG = "RemoteMedia";

    public static final int PLAYABLE_TYPE_REMOTE_MEDIA = 3;

    private final String downloadUrl;
    private final String itemIdentifier;
    private final String feedUrl;
    private final String feedTitle;
    private final String episodeTitle;
    private final String episodeLink;
    private final String feedAuthor;
    private final String imageUrl;
    private final String feedLink;
    private final String mimeType;
    private final Date pubDate;
    private final String notes;
    private List<Chapter> chapters;
    private int duration;
    private int position;
    private long lastPlayedTime;

    public RemoteMedia(String downloadUrl, String itemId, String feedUrl, String feedTitle,
                       String episodeTitle, String episodeLink, String feedAuthor,
                       String imageUrl, String feedLink, String mimeType, Date pubDate,
                       String notes) {
        this.downloadUrl = downloadUrl;
        this.itemIdentifier = itemId;
        this.feedUrl = feedUrl;
        this.feedTitle = feedTitle;
        this.episodeTitle = episodeTitle;
        this.episodeLink = episodeLink;
        this.feedAuthor = feedAuthor;
        this.imageUrl = imageUrl;
        this.feedLink = feedLink;
        this.mimeType = mimeType;
        this.pubDate = pubDate;
        this.notes = notes;
    }

    public RemoteMedia(FeedItem item) {
        this.downloadUrl = item.getMedia().getDownload_url();
        this.itemIdentifier = item.getItemIdentifier();
        this.feedUrl = item.getFeed().getDownload_url();
        this.feedTitle = item.getFeed().getTitle();
        this.episodeTitle = item.getTitle();
        this.episodeLink = item.getLink();
        this.feedAuthor = item.getFeed().getAuthor();
        if (!TextUtils.isEmpty(item.getImageUrl())) {
            this.imageUrl = item.getImageUrl();
        } else {
            this.imageUrl = item.getFeed().getImageUrl();
        }
        this.feedLink = item.getFeed().getLink();
        this.mimeType = item.getMedia().getMime_type();
        this.pubDate = item.getPubDate();
        this.notes = item.getDescription();
    }

    public String getEpisodeIdentifier() {
        return itemIdentifier;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getEpisodeLink() {
        return episodeLink;
    }

    public String getFeedAuthor() {
        return feedAuthor;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFeedLink() {
        return feedLink;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public void writeToPreferences(SharedPreferences.Editor prefEditor) {
        //it seems pointless to do it, since the session should be kept by the remote device.
    }

    @Override
    public String getEpisodeTitle() {
        return episodeTitle;
    }

    @Override
    public List<Chapter> getChapters() {
        return chapters;
    }

    @Override
    public String getWebsiteLink() {
        if (episodeLink != null) {
            return episodeLink;
        } else {
            return feedUrl;
        }
    }

    @Override
    public String getFeedTitle() {
        return feedTitle;
    }

    @Override
    public Object getIdentifier() {
        return itemIdentifier + "@" + feedUrl;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public long getLastPlayedTime() {
        return lastPlayedTime;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.fromMimeType(mimeType);
    }

    @Override
    public String getLocalMediaUrl() {
        return null;
    }

    @Override
    public String getStreamUrl() {
        return downloadUrl;
    }

    @Override
    public boolean localFileAvailable() {
        return false;
    }

    @Override
    public void setPosition(int newPosition) {
        position = newPosition;
    }

    @Override
    public void setDuration(int newDuration) {
        duration = newDuration;
    }

    @Override
    public void setLastPlayedTime(long lastPlayedTimestamp) {
        lastPlayedTime = lastPlayedTimestamp;
    }

    @Override
    public void onPlaybackStart() {
        // no-op
    }

    @Override
    public void onPlaybackPause(Context context) {
        // no-op
    }

    @Override
    public void onPlaybackCompleted(Context context) {
        // no-op
    }

    @Override
    public int getPlayableType() {
        return PLAYABLE_TYPE_REMOTE_MEDIA;
    }

    @Override
    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    @Nullable
    public String getImageLocation() {
        return imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDescription() {
        return notes;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(downloadUrl);
        dest.writeString(itemIdentifier);
        dest.writeString(feedUrl);
        dest.writeString(feedTitle);
        dest.writeString(episodeTitle);
        dest.writeString(episodeLink);
        dest.writeString(feedAuthor);
        dest.writeString(imageUrl);
        dest.writeString(feedLink);
        dest.writeString(mimeType);
        dest.writeLong((pubDate != null) ? pubDate.getTime() : 0);
        dest.writeString(notes);
        dest.writeInt(duration);
        dest.writeInt(position);
        dest.writeLong(lastPlayedTime);
    }

    public static final Parcelable.Creator<RemoteMedia> CREATOR = new Parcelable.Creator<RemoteMedia>() {
        @Override
        public RemoteMedia createFromParcel(Parcel in) {
            RemoteMedia result = new RemoteMedia(in.readString(), in.readString(), in.readString(),
                    in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
                    in.readString(), in.readString(), new Date(in.readLong()), in.readString());
            result.setDuration(in.readInt());
            result.setPosition(in.readInt());
            result.setLastPlayedTime(in.readLong());
            return result;
        }

        @Override
        public RemoteMedia[] newArray(int size) {
            return new RemoteMedia[size];
        }
    };

    @Override
    public boolean equals(Object other) {
        if (other instanceof RemoteMedia) {
            RemoteMedia rm = (RemoteMedia) other;
            return TextUtils.equals(downloadUrl, rm.downloadUrl)
                    && TextUtils.equals(feedUrl, rm.feedUrl)
                    && TextUtils.equals(itemIdentifier, rm.itemIdentifier);
        }
        if (other instanceof FeedMedia) {
            FeedMedia fm = (FeedMedia) other;
            if (!TextUtils.equals(downloadUrl, fm.getStreamUrl())) {
                return false;
            }
            FeedItem fi = fm.getItem();
            if (fi == null || !TextUtils.equals(itemIdentifier, fi.getItemIdentifier())) {
                return false;
            }
            Feed feed = fi.getFeed();
            return feed != null && TextUtils.equals(feedUrl, feed.getDownload_url());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(downloadUrl)
                .append(feedUrl)
                .append(itemIdentifier)
                .toHashCode();
    }
}
