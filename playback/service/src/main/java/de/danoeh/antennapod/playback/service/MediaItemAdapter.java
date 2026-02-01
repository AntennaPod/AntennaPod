package de.danoeh.antennapod.playback.service;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;

public class MediaItemAdapter {
    public static MediaItem fromPlayable(Playable playable) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setTitle(playable.getEpisodeTitle());
        metadataBuilder.setIsPlayable(true);
        metadataBuilder.setIsBrowsable(false);
        metadataBuilder.setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE);
        String mediaId = "0";
        if (playable instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) playable;
            mediaId = String.valueOf(feedMedia.getId());
            metadataBuilder.setSubtitle(feedMedia.getFeedTitle());
            metadataBuilder.setArtworkUri(Uri.parse(feedMedia.getImageLocation()));
        }
        String uriString = playable.localFileAvailable() ? playable.getLocalFileUrl() : playable.getStreamUrl();
        return new MediaItem.Builder()
                .setUri(uriString != null ? Uri.parse(uriString) : null)
                .setMediaId(mediaId)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }
}
