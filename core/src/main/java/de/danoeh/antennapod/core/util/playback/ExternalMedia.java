package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaMetadataRetriever;
import android.os.Parcel;
import android.os.Parcelable;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.ChapterUtils;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.io.FilenameUtils;

/** Represents a media file that is stored on the local storage device. */
public class ExternalMedia implements Playable {
    public static final int PLAYABLE_TYPE_EXTERNAL_MEDIA = 2;
    public static final String PREF_SOURCE_URL = "ExternalMedia.PrefSourceUrl";
    public static final String PREF_POSITION = "ExternalMedia.PrefPosition";
    public static final String PREF_MEDIA_TYPE = "ExternalMedia.PrefMediaType";
    public static final String PREF_LAST_PLAYED_TIME = "ExternalMedia.PrefLastPlayedTime";

    private final String source;
    private String episodeTitle;
    private String feedTitle;
    private MediaType mediaType;
    private List<Chapter> chapters;
    private int duration;
    private int position;
    private long lastPlayedTime;

    /**
     * Creates a new playable for files on the sd card.
     * @param source File path of the file
     * @param mediaType Type of the file
     */
    public ExternalMedia(String source, MediaType mediaType) {
        super();
        this.source = source;
        this.mediaType = mediaType;
    }

    /**
     * Creates a new playable for files on the sd card.
     * @param source File path of the file
     * @param mediaType Type of the file
     * @param position Position to start from
     * @param lastPlayedTime Timestamp when it was played last
     */
    public ExternalMedia(String source, MediaType mediaType, int position, long lastPlayedTime) {
        this(source, mediaType);
        this.position = position;
        this.lastPlayedTime = lastPlayedTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(source);
        dest.writeString(mediaType.toString());
        dest.writeInt(position);
        dest.writeLong(lastPlayedTime);
    }

    @Override
    public void writeToPreferences(Editor prefEditor) {
        prefEditor.putString(PREF_SOURCE_URL, source);
        prefEditor.putString(PREF_MEDIA_TYPE, mediaType.toString());
        prefEditor.putInt(PREF_POSITION, position);
        prefEditor.putLong(PREF_LAST_PLAYED_TIME, lastPlayedTime);
    }

    @Override
    public void loadMetadata() throws PlayableException {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(source);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new PlayableException("IllegalArgumentException when setting up MediaMetadataReceiver");
        } catch (RuntimeException e) {
            // http://code.google.com/p/android/issues/detail?id=39770
            e.printStackTrace();
            throw new PlayableException("RuntimeException when setting up MediaMetadataRetriever");
        }
        episodeTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (episodeTitle == null) {
            episodeTitle = FilenameUtils.getName(source);
        }
        feedTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        try {
            duration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new PlayableException("NumberFormatException when reading duration of media file");
        }
        setChapters(ChapterUtils.loadChaptersFromFileUrl(this));
    }

    @Override
    public void loadChapterMarks(Context context) {

    }

    @Override
    public String getEpisodeTitle() {
        return episodeTitle;
    }

    @Override
    public Callable<String> loadShownotes() {
        return () -> "";
    }

    @Override
    public List<Chapter> getChapters() {
        return chapters;
    }

    @Override
    public String getWebsiteLink() {
        return null;
    }

    @Override
    public String getPaymentLink() {
        return null;
    }

    @Override
    public String getFeedTitle() {
        return feedTitle;
    }

    @Override
    public Object getIdentifier() {
        return source;
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
        return mediaType;
    }

    @Override
    public String getLocalMediaUrl() {
        return source;
    }

    @Override
    public String getStreamUrl() {
        return null;
    }

    @Override
    public boolean localFileAvailable() {
        return true;
    }

    @Override
    public boolean streamAvailable() {
        return false;
    }

    @Override
    public void saveCurrentPosition(SharedPreferences pref, int newPosition, long timestamp) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(PREF_POSITION, newPosition);
        editor.putLong(PREF_LAST_PLAYED_TIME, timestamp);
        position = newPosition;
        lastPlayedTime = timestamp;
        editor.apply();
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
    public void setLastPlayedTime(long lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    @Override
    public void onPlaybackStart() {

    }

    @Override
    public void onPlaybackPause(Context context) {

    }

    @Override
    public void onPlaybackCompleted(Context context) {

    }

    @Override
    public int getPlayableType() {
        return PLAYABLE_TYPE_EXTERNAL_MEDIA;
    }

    @Override
    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public static final Parcelable.Creator<ExternalMedia> CREATOR = new Parcelable.Creator<ExternalMedia>() {
        public ExternalMedia createFromParcel(Parcel in) {
            String source = in.readString();
            MediaType type = MediaType.valueOf(in.readString());
            int position = 0;
            if (in.dataAvail() > 0) {
                position = in.readInt();
            }
            long lastPlayedTime = 0;
            if (in.dataAvail() > 0) {
                lastPlayedTime = in.readLong();
            }

            return new ExternalMedia(source, type, position, lastPlayedTime);
        }

        public ExternalMedia[] newArray(int size) {
            return new ExternalMedia[size];
        }
    };

    @Override
    public String getImageLocation() {
        if (localFileAvailable()) {
            return getLocalMediaUrl();
        } else {
            return null;
        }
    }
}
