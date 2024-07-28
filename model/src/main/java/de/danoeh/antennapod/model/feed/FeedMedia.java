package de.danoeh.antennapod.model.feed;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import de.danoeh.antennapod.model.MediaMetadataRetrieverCompat;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Date;
import java.util.List;

public class FeedMedia implements Playable {
    public static final int FEEDFILETYPE_FEEDMEDIA = 2;
    public static final int PLAYABLE_TYPE_FEEDMEDIA = 1;
    public static final String FILENAME_PREFIX_EMBEDDED_COVER = "metadata-retriever:";

    /**
     * Indicates we've checked on the size of the item via the network
     * and got an invalid response. Using Integer.MIN_VALUE because
     * 1) we'll still check on it in case it gets downloaded (it's <= 0)
     * 2) By default all FeedMedia have a size of 0 if we don't know it,
     *    so this won't conflict with existing practice.
     */
    private static final int CHECKED_ON_SIZE_BUT_UNKNOWN = Integer.MIN_VALUE;

    private long id;
    private String localFileUrl;
    private String downloadUrl;
    private long downloadDate;
    private int duration;
    private int position; // Current position in file
    private long lastPlayedTime; // Last time this media was played (in ms)
    private int playedDuration; // How many ms of this file have been played
    private long size; // File size in Byte
    private String mimeType;
    @Nullable private volatile FeedItem item;
    private Date playbackCompletionDate;
    private int startPosition = -1;
    private int playedDurationWhenStarted;

    // if null: unknown, will be checked
    private Boolean hasEmbeddedPicture;

    /* Used for loading item when restoring from parcel. */
    private long itemID;

    public FeedMedia(FeedItem i, String downloadUrl, long size,
                     String mimeType) {
        this.localFileUrl = null;
        this.downloadUrl = downloadUrl;
        this.downloadDate = 0;
        this.item = i;
        this.itemID = i != null ? i.getId() : 0;
        this.size = size;
        this.mimeType = mimeType;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                     long size, String mimeType, String localFileUrl, String downloadUrl,
                     long downloadDate, Date playbackCompletionDate, int playedDuration,
                     long lastPlayedTime) {
        this.localFileUrl = localFileUrl;
        this.downloadUrl = downloadUrl;
        this.downloadDate = downloadDate;
        this.id = id;
        this.item = item;
        this.itemID = item != null ? item.getId() : 0;
        this.duration = duration;
        this.position = position;
        this.playedDuration = playedDuration;
        this.playedDurationWhenStarted = playedDuration;
        this.size = size;
        this.mimeType = mimeType;
        this.playbackCompletionDate = playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
        this.lastPlayedTime = lastPlayedTime;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                     long size, String mimeType, String localFileUrl, String downloadUrl,
                     long downloadDate, Date playbackCompletionDate, int playedDuration,
                     Boolean hasEmbeddedPicture, long lastPlayedTime) {
        this(id, item, duration, position, size, mimeType, localFileUrl, downloadUrl, downloadDate,
                playbackCompletionDate, playedDuration, lastPlayedTime);
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    public String getHumanReadableIdentifier() {
        if (item != null && item.getTitle() != null) {
            return item.getTitle();
        } else {
            return downloadUrl;
        }
    }

    /**
     * Returns a MediaItem representing the FeedMedia object.
     * This is used by the MediaBrowserService
     */
    public MediaBrowserCompat.MediaItem getMediaItem() {
        Playable p = this;
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(String.valueOf(id))
                .setTitle(p.getEpisodeTitle())
                .setDescription(p.getFeedTitle())
                .setSubtitle(p.getFeedTitle());
        if (item != null) {
            // getImageLocation() also loads embedded images, which we can not send to external devices
            if (item.getImageUrl() != null) {
                builder.setIconUri(Uri.parse(item.getImageUrl()));
            } else if (item.getFeed() != null && item.getFeed().getImageUrl() != null) {
                builder.setIconUri(Uri.parse(item.getFeed().getImageUrl()));
            }
        }
        return new MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    /**
     * Uses mimetype to determine the type of media.
     */
    public MediaType getMediaType() {
        return MediaType.fromMimeType(mimeType);
    }

    public void updateFromOther(FeedMedia other) {
        this.downloadUrl = other.downloadUrl;
        if (other.size > 0) {
            size = other.size;
        }
        if (other.duration > 0 && duration <= 0) { // Do not overwrite duration that we measured after downloading
            duration = other.duration;
        }
        if (other.mimeType != null) {
            mimeType = other.mimeType;
        }
    }

    /**
     * Compare's this FeedFile's attribute values with another FeedFile's
     * attribute values. This method will only compare attributes which were
     * read from the feed.
     *
     * @return true if attribute values are different, false otherwise
     */
    public boolean compareWithOther(FeedMedia other) {
        if (!StringUtils.equals(downloadUrl, other.downloadUrl)) {
            return true;
        }
        if (other.mimeType != null) {
            if (mimeType == null || !mimeType.equals(other.mimeType)) {
                return true;
            }
        }
        if (other.size > 0 && other.size != size) {
            return true;
        }
        if (other.duration > 0 && duration <= 0) {
            return true;
        }
        return false;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public void setLastPlayedTime(long lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public int getPlayedDuration() {
        return playedDuration;
    }

    public int getPlayedDurationWhenStarted() {
        return playedDurationWhenStarted;
    }

    public void setPlayedDuration(int played_duration) {
        this.playedDuration = played_duration;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public long getLastPlayedTime() {
        return lastPlayedTime;
    }

    public void setPosition(int position) {
        this.position = position;
        if(position > 0 && item != null && item.isNew()) {
            this.item.setPlayed(false);
        }
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String getDescription() {
        if (item != null) {
            return item.getDescription();
        }
        return null;
    }

    /**
     * Indicates we asked the service what the size was, but didn't
     * get a valid answer and we shoudln't check using the network again.
     */
    public void setCheckedOnSizeButUnknown() {
        this.size = CHECKED_ON_SIZE_BUT_UNKNOWN;
    }

    public boolean checkedOnSizeButUnknown() {
        return (CHECKED_ON_SIZE_BUT_UNKNOWN == this.size);
    }

    public String getMimeType() {
        return mimeType;
    }

    @Nullable
    public FeedItem getItem() {
        return item;
    }

    /**
     * Sets the item object of this FeedMedia. If the given
     * FeedItem object is not null, it's 'media'-attribute value
     * will also be set to this media object.
     */
    public void setItem(FeedItem item) {
        this.item = item;
        this.itemID = item != null ? item.getId() : 0;
        if (item != null && item.getMedia() != this) {
            item.setMedia(this);
        }
    }

    public Date getPlaybackCompletionDate() {
        return playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
    }

    public void setPlaybackCompletionDate(Date playbackCompletionDate) {
        this.playbackCompletionDate = playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
    }

    public boolean isInProgress() {
        return (this.position > 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean hasEmbeddedPicture() {
        if(hasEmbeddedPicture == null) {
            checkEmbeddedPicture();
        }
        return hasEmbeddedPicture;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(item != null ? item.getId() : 0L);

        dest.writeInt(duration);
        dest.writeInt(position);
        dest.writeLong(size);
        dest.writeString(mimeType);
        dest.writeString(localFileUrl);
        dest.writeString(downloadUrl);
        dest.writeLong(downloadDate);
        dest.writeLong((playbackCompletionDate != null) ? playbackCompletionDate.getTime() : 0);
        dest.writeInt(playedDuration);
        dest.writeLong(lastPlayedTime);
    }

    @Override
    public String getEpisodeTitle() {
        if (item == null) {
            return null;
        }
        if (item.getTitle() != null) {
            return item.getTitle();
        } else {
            return item.getIdentifyingValue();
        }
    }

    @Override
    public List<Chapter> getChapters() {
        if (item == null) {
            return null;
        }
        return item.getChapters();
    }

    @Override
    public String getWebsiteLink() {
        if (item == null) {
            return null;
        }
        return item.getLink();
    }

    @Override
    public String getFeedTitle() {
        if (item == null || item.getFeed() == null) {
            return null;
        }
        return item.getFeed().getTitle();
    }

    @Override
    public Object getIdentifier() {
        return id;
    }

    @Override
    public String getLocalFileUrl() {
        return localFileUrl;
    }

    @Override
    public String getStreamUrl() {
        return downloadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public int getStartPosition() {
        return startPosition;
    }

    @Override
    public Date getPubDate() {
        if (item == null) {
            return null;
        }
        if (item.getPubDate() != null) {
            return item.getPubDate();
        } else {
            return null;
        }
    }

    @Override
    public boolean localFileAvailable() {
        return isDownloaded() && localFileUrl != null;
    }

    public boolean fileExists() {
        if (localFileUrl == null) {
            return false;
        } else {
            File f = new File(localFileUrl);
            return f.exists();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDownloaded() {
        return downloadDate > 0;
    }

    public long getItemId() {
        return itemID;
    }

    public void setItemId(long id) {
        itemID = id;
    }

    @Override
    public void onPlaybackStart() {
        startPosition = Math.max(position, 0);
        playedDurationWhenStarted = playedDuration;
    }

    @Override
    public void onPlaybackPause(Context context) {
        if (position > startPosition) {
            playedDuration = playedDurationWhenStarted + position - startPosition;
            playedDurationWhenStarted = playedDuration;
        }
        startPosition = position;
    }

    @Override
    public void onPlaybackCompleted(Context context) {
        startPosition = -1;
    }

    @Override
    public int getPlayableType() {
        return PLAYABLE_TYPE_FEEDMEDIA;
    }

    @Override
    public void setChapters(List<Chapter> chapters) {
        if (item != null) {
            item.setChapters(chapters);
        }
    }

    public static final Parcelable.Creator<FeedMedia> CREATOR = new Parcelable.Creator<FeedMedia>() {
        public FeedMedia createFromParcel(Parcel in) {
            final long id = in.readLong();
            final long itemID = in.readLong();
            FeedMedia result = new FeedMedia(id, null, in.readInt(), in.readInt(), in.readLong(), in.readString(), in.readString(),
                    in.readString(), in.readLong(), new Date(in.readLong()), in.readInt(), in.readLong());
            result.itemID = itemID;
            return result;
        }

        public FeedMedia[] newArray(int size) {
            return new FeedMedia[size];
        }
    };

    @Override
    public String getImageLocation() {
        if (item != null) {
            return item.getImageLocation();
        } else if (hasEmbeddedPicture()) {
            return FILENAME_PREFIX_EMBEDDED_COVER + getLocalFileUrl();
        } else {
            return null;
        }
    }

    public void setHasEmbeddedPicture(Boolean hasEmbeddedPicture) {
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    public void setDownloaded(boolean downloaded, long when) {
        this.downloadDate = downloaded ? when : 0;
        if (item != null && downloaded && item.isNew()) {
            item.setPlayed(false);
        }
    }

    public long getDownloadDate() {
        return downloadDate;
    }

    public void setLocalFileUrl(String fileUrl) {
        this.localFileUrl = fileUrl;
        if (fileUrl == null) {
            downloadDate = 0;
        }
    }

    public void checkEmbeddedPicture() {
        if (!localFileAvailable()) {
            hasEmbeddedPicture = Boolean.FALSE;
            return;
        }
        try (MediaMetadataRetrieverCompat mmr = new MediaMetadataRetrieverCompat()) {
            mmr.setDataSource(getLocalFileUrl());
            byte[] image = mmr.getEmbeddedPicture();
            if (image != null) {
                hasEmbeddedPicture = Boolean.TRUE;
            } else {
                hasEmbeddedPicture = Boolean.FALSE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasEmbeddedPicture = Boolean.FALSE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof RemoteMedia) {
            return o.equals(this);
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        FeedMedia feedMedia = (FeedMedia) o;
        return id == feedMedia.id;
    }

    public String getTranscriptFileUrl() {
        if (getLocalFileUrl() == null) {
            return null;
        }
        return getLocalFileUrl() + ".transcript";
    }

    public void setTranscript(Transcript t) {
        if (item == null)  {
            return;
        }
        item.setTranscript(t);
    }

    public Transcript getTranscript() {
        if (item == null)  {
            return null;
        }
        return item.getTranscript();
    }

    public Boolean hasTranscript() {
        if (item == null)  {
            return false;
        }
        return item.hasTranscript();
    }
}
