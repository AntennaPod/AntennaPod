package de.danoeh.antennapod.core.cast;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Playable implementation for media on a Cast Device for which a local version of
 * {@link de.danoeh.antennapod.core.feed.FeedMedia} hasn't been found.
 */
public class RemoteMedia implements Playable {
    public static final String TAG = "RemoteMedia";

    public static final int PLAYABLE_TYPE_REMOTE_MEDIA = 3;

    private String downloadUrl;
    private String itemIdentifier;
    private String feedUrl;
    private String feedTitle;
    private String episodeTitle;
    private String episodeLink;
    private String feedAuthor;
    private String imageUrl;
    private String feedLink;
    private String mime_type;
    private Date pubDate;
    private String notes;
    private List<Chapter> chapters;
    private int duration;
    private int position;
    private long lastPlayedTime;

    public RemoteMedia(String downloadUrl, String itemId, String feedUrl, String feedTitle,
                       String episodeTitle, String episodeLink, String feedAuthor,
                       String imageUrl, String feedLink, String mime_type, Date pubDate) {
        this.downloadUrl = downloadUrl;
        this.itemIdentifier = itemId;
        this.feedUrl = feedUrl;
        this.feedTitle = feedTitle;
        this.episodeTitle = episodeTitle;
        this.episodeLink = episodeLink;
        this.feedAuthor = feedAuthor;
        this.imageUrl = imageUrl;
        this.feedLink = feedLink;
        this.mime_type = mime_type;
        this.pubDate = pubDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public MediaInfo extractMediaInfo() {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);

        metadata.putString(MediaMetadata.KEY_TITLE, episodeTitle);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, feedTitle);
        if (!TextUtils.isEmpty(imageUrl)) {
            metadata.addImage(new WebImage(Uri.parse(imageUrl)));
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(pubDate);
        metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);
        if (!TextUtils.isEmpty(feedAuthor)) {
            metadata.putString(MediaMetadata.KEY_ARTIST, feedAuthor);
        }
        if (!TextUtils.isEmpty(feedUrl)) {
            metadata.putString(CastUtils.KEY_FEED_URL, feedUrl);
        }
        if (!TextUtils.isEmpty(feedLink)) {
            metadata.putString(CastUtils.KEY_FEED_WEBSITE, feedLink);
        }
        if (!TextUtils.isEmpty(itemIdentifier)) {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, itemIdentifier);
        } else {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, downloadUrl);
        }
        if (!TextUtils.isEmpty(episodeLink)) {
            metadata.putString(CastUtils.KEY_EPISODE_LINK, episodeLink);
        }
        String notes = this.notes;
        if (notes != null) {
            if (notes.length() > CastUtils.EPISODE_NOTES_MAX_LENGTH) {
                notes = notes.substring(0, CastUtils.EPISODE_NOTES_MAX_LENGTH);
            }
            metadata.putString(CastUtils.KEY_EPISODE_NOTES, notes);
        }
        // Default id value
        metadata.putInt(CastUtils.KEY_MEDIA_ID, 0);
        metadata.putInt(CastUtils.KEY_FORMAT_VERSION, CastUtils.FORMAT_VERSION_VALUE);

        MediaInfo.Builder builder = new MediaInfo.Builder(downloadUrl)
                .setContentType(mime_type)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata);
        if (duration > 0) {
            builder.setStreamDuration(duration);
        }
        return builder.build();
    }

    public String getEpisodeIdentifier() {
        return itemIdentifier;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public FeedMedia lookForFeedMedia() {
        FeedItem feedItem = DBReader.getFeedItem(feedUrl, itemIdentifier);
        if (feedItem == null) {
            return null;
        }
        return feedItem.getMedia();
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
        return MediaType.fromMimeType(mime_type);
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
        dest.writeString(feedLink);
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
                    in.readString(), in.readString(), new Date(in.readLong()));
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof RemoteMedia) {
            RemoteMedia rm = (RemoteMedia) other;
            return TextUtils.equals(downloadUrl, rm.downloadUrl) &&
                    TextUtils.equals(feedUrl, rm.feedUrl) &&
                    TextUtils.equals(itemIdentifier, rm.itemIdentifier);
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
