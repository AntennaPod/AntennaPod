package de.danoeh.antennapod.core.cast;

import android.net.Uri;
import android.text.TextUtils;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import de.danoeh.antennapod.core.util.playback.RemoteMedia;
import java.util.Calendar;

public class MediaInfoCreator {
    public static MediaInfo from(RemoteMedia media) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC);

        metadata.putString(MediaMetadata.KEY_TITLE, media.getEpisodeTitle());
        metadata.putString(MediaMetadata.KEY_SUBTITLE, media.getFeedTitle());
        if (!TextUtils.isEmpty(media.getImageLocation())) {
            metadata.addImage(new WebImage(Uri.parse(media.getImageLocation())));
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(media.getPubDate());
        metadata.putDate(MediaMetadata.KEY_RELEASE_DATE, calendar);
        if (!TextUtils.isEmpty(media.getFeedAuthor())) {
            metadata.putString(MediaMetadata.KEY_ARTIST, media.getFeedAuthor());
        }
        if (!TextUtils.isEmpty(media.getFeedUrl())) {
            metadata.putString(CastUtils.KEY_FEED_URL, media.getFeedUrl());
        }
        if (!TextUtils.isEmpty(media.getFeedLink())) {
            metadata.putString(CastUtils.KEY_FEED_WEBSITE, media.getFeedLink());
        }
        if (!TextUtils.isEmpty(media.getEpisodeIdentifier())) {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, media.getEpisodeIdentifier());
        } else {
            metadata.putString(CastUtils.KEY_EPISODE_IDENTIFIER, media.getDownloadUrl());
        }
        if (!TextUtils.isEmpty(media.getEpisodeLink())) {
            metadata.putString(CastUtils.KEY_EPISODE_LINK, media.getEpisodeLink());
        }
        String notes = media.getNotes();
        if (notes != null) {
            if (notes.length() > CastUtils.EPISODE_NOTES_MAX_LENGTH) {
                notes = notes.substring(0, CastUtils.EPISODE_NOTES_MAX_LENGTH);
            }
            metadata.putString(CastUtils.KEY_EPISODE_NOTES, notes);
        }
        // Default id value
        metadata.putInt(CastUtils.KEY_MEDIA_ID, 0);
        metadata.putInt(CastUtils.KEY_FORMAT_VERSION, CastUtils.FORMAT_VERSION_VALUE);

        MediaInfo.Builder builder = new MediaInfo.Builder(media.getDownloadUrl())
                .setContentType(media.getMimeType())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata);
        if (media.getDuration() > 0) {
            builder.setStreamDuration(media.getDuration());
        }
        return builder.build();
    }
}
