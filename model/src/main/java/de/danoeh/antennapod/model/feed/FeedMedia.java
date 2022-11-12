package de.danoeh.antennapod.model.feed;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.playback.RemoteMedia;

import java.util.Date;
import java.util.List;

public class FeedMedia extends FeedFile implements Playable {
    public static final int FEEDFILETYPE_FEEDMEDIA = 2;
    public static final int PLAYABLE_TYPE_FEEDMEDIA = 1;
    public static final String FILENAME_PREFIX_EMBEDDED_COVER = "metadata-retriever:";

    public static final String PREF_MEDIA_ID = "FeedMedia.PrefMediaId";
    private static final String PREF_FEED_ID = "FeedMedia.PrefFeedId";

    /**
     * Indicates we've checked on the size of the item via the network
     * and got an invalid response. Using Integer.MIN_VALUE because
     * 1) we'll still check on it in case it gets downloaded (it's <= 0)
     * 2) By default all FeedMedia have a size of 0 if we don't know it,
     *    so this won't conflict with existing practice.
     */
    private static final int CHECKED_ON_SIZE_BUT_UNKNOWN = Integer.MIN_VALUE;

    private int duration;
    private int position; // Current position in file
    private long lastPlayedTime; // Last time this media was played (in ms)
    private int played_duration; // How many ms of this file have been played
    private long size; // File size in Byte
    private String mime_type;
    @Nullable private volatile FeedItem item;
    private Date playbackCompletionDate;
    private int startPosition = -1;
    private int playedDurationWhenStarted;

    // if null: unknown, will be checked
    private Boolean hasEmbeddedPicture;

    /* Used for loading item when restoring from parcel. */
    private long itemID;

    public FeedMedia(FeedItem i, String download_url, long size,
                     String mime_type) {
        super(null, download_url, false);
        this.item = i;
        this.size = size;
        this.mime_type = mime_type;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                     long size, String mime_type, String file_url, String download_url,
                     boolean downloaded, Date playbackCompletionDate, int played_duration,
                     long lastPlayedTime) {
        super(file_url, download_url, downloaded);
        this.id = id;
        this.item = item;
        this.duration = duration;
        this.position = position;
        this.played_duration = played_duration;
        this.playedDurationWhenStarted = played_duration;
        this.size = size;
        this.mime_type = mime_type;
        this.playbackCompletionDate = playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
        this.lastPlayedTime = lastPlayedTime;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                      long size, String mime_type, String file_url, String download_url,
                      boolean downloaded, Date playbackCompletionDate, int played_duration,
                      Boolean hasEmbeddedPicture, long lastPlayedTime) {
        this(id, item, duration, position, size, mime_type, file_url, download_url, downloaded,
                playbackCompletionDate, played_duration, lastPlayedTime);
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    @Override
    public String getHumanReadableIdentifier() {
        if (item != null && item.getTitle() != null) {
            return item.getTitle();
        } else {
            return download_url;
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
        return MediaType.fromMimeType(mime_type);
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

    @Override
    public void setLastPlayedTime(long lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public int getPlayedDuration() {
        return played_duration;
    }

    public int getPlayedDurationWhenStarted() {
        return playedDurationWhenStarted;
    }

    public void setPlayedDuration(int played_duration) {
        this.played_duration = played_duration;
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

    public String getMime_type() {
        return mime_type;
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
        dest.writeString(mime_type);
        dest.writeString(file_url);
        dest.writeString(download_url);
        dest.writeByte((byte) ((downloaded) ? 1 : 0));
        dest.writeLong((playbackCompletionDate != null) ? playbackCompletionDate.getTime() : 0);
        dest.writeInt(played_duration);
        dest.writeLong(lastPlayedTime);
    }

    @Override
    public void writeToPreferences(Editor prefEditor) {
        if(item != null && item.getFeed() != null) {
            prefEditor.putLong(PREF_FEED_ID, item.getFeed().getId());
        } else {
            prefEditor.putLong(PREF_FEED_ID, 0L);
        }
        prefEditor.putLong(PREF_MEDIA_ID, id);
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
    public String getLocalMediaUrl() {
        return file_url;
    }

    @Override
    public String getStreamUrl() {
        return download_url;
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
        return isDownloaded() && file_url != null;
    }

    public long getItemId() {
        return itemID;
    }

    @Override
    public void onPlaybackStart() {
        startPosition = Math.max(position, 0);
        playedDurationWhenStarted = played_duration;
    }

    @Override
    public void onPlaybackPause(Context context) {
        if (position > startPosition) {
            played_duration = playedDurationWhenStarted + position - startPosition;
            playedDurationWhenStarted = played_duration;
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
                    in.readString(), in.readByte() != 0, new Date(in.readLong()), in.readInt(), in.readLong());
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
            return FILENAME_PREFIX_EMBEDDED_COVER + getLocalMediaUrl();
        } else {
            return null;
        }
    }

    public void setHasEmbeddedPicture(Boolean hasEmbeddedPicture) {
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    @Override
    public void setDownloaded(boolean downloaded) {
        super.setDownloaded(downloaded);
        if(item != null && downloaded && item.isNew()) {
            item.setPlayed(false);
        }
    }

    @Override
    public void setFile_url(String file_url) {
        super.setFile_url(file_url);
    }

    public void checkEmbeddedPicture() {
        if (!localFileAvailable()) {
            hasEmbeddedPicture = Boolean.FALSE;
            return;
        }
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(getLocalMediaUrl());
            byte[] image = mmr.getEmbeddedPicture();
            if(image != null) {
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
        if (o == null) {
            return false;
        }
        if (o instanceof RemoteMedia) {
            return o.equals(this);
        }
        return super.equals(o);
    }
}
