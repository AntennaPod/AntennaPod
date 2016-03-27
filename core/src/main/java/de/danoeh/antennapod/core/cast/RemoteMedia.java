package de.danoeh.antennapod.core.cast;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Playable implementation for media on a Cast Device for which a local version of
 * {@link de.danoeh.antennapod.core.feed.FeedMedia} could not be found.
 */
public class RemoteMedia implements Playable {

    public static final int PLAYABLE_TYPE_REMOTE_MEDIA = 3;

    private String downloadUrl;
    private String itemIdentifier;
    private String feedUrl;
    private String feedTitle;
    private String episodeTitle;
    private String episodeLink;
    private String feedAuthor;
    private String imageUrl;
    private String mime_type;
    private Date pubDate;
    private String notes;
    private List<Chapter> chapters;
    private int duration;
    private int position;
    private long lastPlayedTime;

    public RemoteMedia(String downloadUrl, String itemId, String feedUrl, String feedTitle,
                       String episodeTitle, String episodeLink, String feedAuthor,
                       String imageUrl, String mime_type, Date pubDate) {
        this.downloadUrl = downloadUrl;
        this.itemIdentifier = itemId;
        this.feedUrl = feedUrl;
        this.feedTitle = feedTitle;
        this.episodeTitle = episodeTitle;
        this.episodeLink = episodeLink;
        this.feedAuthor = feedAuthor;
        this.imageUrl = imageUrl;
        this.mime_type = mime_type;
        this.pubDate = pubDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public void writeToPreferences(SharedPreferences.Editor prefEditor) {
        //it seems pointless to do it, since the session should be kept by the remote device.
    }

    @Override
    public void loadMetadata() throws PlayableException {
        //Already loaded
    }

    @Override
    public void loadChapterMarks() {
        ChapterUtils.loadChaptersFromStreamUrl(this);
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
    public String getPaymentLink() {
        return null;
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
        if (TextUtils.isEmpty(mime_type)) {
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
    public boolean streamAvailable() {
        return true;
    }

    @Override
    public void saveCurrentPosition(SharedPreferences pref, int newPosition, long timestamp) {
        //we're not saving playback information for this kind of items on preferences
        setPosition(newPosition);
        setLastPlayedTime(timestamp);
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
    public void onPlaybackCompleted() {
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
    public Uri getImageUri() {
        if (imageUrl != null) {
            return Uri.parse(imageUrl);
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public Callable<String> loadShownotes() {
        return () -> (notes != null) ? notes : "";
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
        dest.writeString(mime_type);
        dest.writeLong(pubDate.getTime());
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
                    in.readString(), new Date(in.readLong()));
            result.setNotes(in.readString());
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
}
