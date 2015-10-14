package de.danoeh.antennapod.core.feed;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

public class FeedMedia extends FeedFile implements Playable {
    private static final String TAG = "FeedMedia";

    public static final int FEEDFILETYPE_FEEDMEDIA = 2;
    public static final int PLAYABLE_TYPE_FEEDMEDIA = 1;

    public static final String PREF_MEDIA_ID = "FeedMedia.PrefMediaId";
    public static final String PREF_FEED_ID = "FeedMedia.PrefFeedId";

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
    private int played_duration; // How many ms of this file have been played (for autoflattring)
    private long size; // File size in Byte
    private String mime_type;
    private volatile FeedItem item;
    private Date playbackCompletionDate;

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

    public static FeedMedia fromCursor(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexPlaybackCompletionDate = cursor.getColumnIndex(PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE);
        int indexDuration = cursor.getColumnIndex(PodDBAdapter.KEY_DURATION);
        int indexPosition = cursor.getColumnIndex(PodDBAdapter.KEY_POSITION);
        int indexSize = cursor.getColumnIndex(PodDBAdapter.KEY_SIZE);
        int indexMimeType = cursor.getColumnIndex(PodDBAdapter.KEY_MIME_TYPE);
        int indexFileUrl = cursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED);
        int indexPlayedDuration = cursor.getColumnIndex(PodDBAdapter.KEY_PLAYED_DURATION);
        int indexLastPlayedTime = cursor.getColumnIndex(PodDBAdapter.KEY_LAST_PLAYED_TIME);

        long mediaId = cursor.getLong(indexId);
        Date playbackCompletionDate = null;
        long playbackCompletionTime = cursor.getLong(indexPlaybackCompletionDate);
        if (playbackCompletionTime > 0) {
            playbackCompletionDate = new Date(playbackCompletionTime);
        }

        Boolean hasEmbeddedPicture;
        switch(cursor.getInt(cursor.getColumnIndex(PodDBAdapter.KEY_HAS_EMBEDDED_PICTURE))) {
            case 1:
                hasEmbeddedPicture = Boolean.TRUE;
                break;
            case 0:
                hasEmbeddedPicture = Boolean.FALSE;
                break;
            default:
                hasEmbeddedPicture = null;
                break;
        }

        return new FeedMedia(
                mediaId,
                null,
                cursor.getInt(indexDuration),
                cursor.getInt(indexPosition),
                cursor.getLong(indexSize),
                cursor.getString(indexMimeType),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                playbackCompletionDate,
                cursor.getInt(indexPlayedDuration),
                hasEmbeddedPicture,
                cursor.getLong(indexLastPlayedTime)
        );
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
     * Uses mimetype to determine the type of media.
     */
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

    /**
     * Reads playback preferences to determine whether this FeedMedia object is
     * currently being played.
     */
    public boolean isPlaying() {
        return PlaybackPreferences.getCurrentlyPlayingMedia() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA
                && PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == id;
    }

    /**
     * Reads playback preferences to determine whether this FeedMedia object is
     * currently being played and the current player status is playing.
     */
    public boolean isCurrentlyPlaying() {
        return isPlaying() &&
                ((PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING));
    }

    /**
     * Reads playback preferences to determine whether this FeedMedia object is
     * currently being played and the current player status is paused.
     */
    public boolean isCurrentlyPaused() {
        return isPlaying() &&
                ((PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PAUSED));
    }


    public boolean hasAlmostEnded() {
        int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
        return this.position >= this.duration - smartMarkAsPlayedSecs * 1000;
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
        if(position > 0 && item.isNew()) {
            this.item.setPlayed(false);
        }
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

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
        return false;
        // TODO: reenable!
        //if(hasEmbeddedPicture == null) {
        //    checkEmbeddedPicture();
        //}
        //return hasEmbeddedPicture;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(item.getId());

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
        prefEditor.putLong(PREF_FEED_ID, item.getFeed().getId());
        prefEditor.putLong(PREF_MEDIA_ID, id);
    }

    @Override
    public void loadMetadata() throws PlayableException {
        if (item == null && itemID != 0) {
            item = DBReader.getFeedItem(itemID);
        }
    }

    @Override
    public void loadChapterMarks() {
        if (item == null && itemID != 0) {
            item = DBReader.getFeedItem(itemID);
        }
        // check if chapters are stored in db and not loaded yet.
        if (item != null && item.hasChapters() && item.getChapters() == null) {
            DBReader.loadChaptersOfFeedItem(item);
        } else if (item != null && item.getChapters() == null && !localFileAvailable()) {
            ChapterUtils.loadChaptersFromStreamUrl(this);
            if (getChapters() != null && item != null) {
                DBWriter.setFeedItem(
                        item);
            }
        }
    }

    @Override
    public String getEpisodeTitle() {
        if (item == null) {
            return null;
        }
        if (getItem().getTitle() != null) {
            return getItem().getTitle();
        } else {
            return getItem().getIdentifyingValue();
        }
    }

    @Override
    public List<Chapter> getChapters() {
        if (item == null) {
            return null;
        }
        return getItem().getChapters();
    }

    @Override
    public String getWebsiteLink() {
        if (item == null) {
            return null;
        }
        return getItem().getLink();
    }

    @Override
    public String getFeedTitle() {
        if (item == null) {
            return null;
        }
        return getItem().getFeed().getTitle();
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

    @Override
    public String getPaymentLink() {
        if (item == null) {
            return null;
        }
        return getItem().getPaymentLink();
    }

    @Override
    public boolean localFileAvailable() {
        return isDownloaded() && file_url != null;
    }

    @Override
    public boolean streamAvailable() {
        return download_url != null;
    }

    @Override
    public void saveCurrentPosition(SharedPreferences pref, int newPosition, long timeStamp) {
        if(item.isNew()) {
            DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());
        }
        setPosition(newPosition);
        setLastPlayedTime(timeStamp);
        DBWriter.setFeedMediaPlaybackInformation(this);
    }

    @Override
    public void onPlaybackStart() {
    }
    @Override
    public void onPlaybackCompleted() {

    }

    @Override
    public int getPlayableType() {
        return PLAYABLE_TYPE_FEEDMEDIA;
    }

    @Override
    public void setChapters(List<Chapter> chapters) {
        getItem().setChapters(chapters);
    }

    @Override
    public Callable<String> loadShownotes() {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (item == null) {
                    item = DBReader.getFeedItem(
                            itemID);
                }
                if (item.getContentEncoded() == null || item.getDescription() == null) {
                    DBReader.loadExtraInformationOfFeedItem(
                            item);

                }
                return (item.getContentEncoded() != null) ? item.getContentEncoded() : item.getDescription();
            }
        };
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
    public Uri getImageUri() {
        if (hasEmbeddedPicture()) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(SCHEME_MEDIA).encodedPath(getLocalMediaUrl());

            if (item != null && item.getFeed() != null) {
                final Uri feedImgUri = item.getFeed().getImageUri();
                if (feedImgUri != null) {
                    builder.appendQueryParameter(PARAM_FALLBACK, feedImgUri.toString());
                }
            }
            return builder.build();
        } else {
            return item.getImageUri();
        }
    }

    public void setHasEmbeddedPicture(Boolean hasEmbeddedPicture) {
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    @Override
    public void setDownloaded(boolean downloaded) {
        super.setDownloaded(downloaded);
        if(downloaded) {
            item.setPlayed(false);
        }
    }

    @Override
    public void setFile_url(String file_url) {
        super.setFile_url(file_url);
    }

    private void checkEmbeddedPicture() {
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
}
