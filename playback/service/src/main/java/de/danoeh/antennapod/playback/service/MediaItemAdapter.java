package de.danoeh.antennapod.playback.service;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;

public class MediaItemAdapter {
    public static MediaItem fromPlayable(Playable playable) {
        String uriString =  playable.getStreamUrl() != null ? playable.getStreamUrl() : playable.getLocalFileUrl();
        String mediaId = "0";
        if (playable instanceof FeedMedia) {
            mediaId = String.valueOf(((FeedMedia) playable).getId());
        }
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(playable.getEpisodeTitle())
                .build();
        return new MediaItem.Builder()
                .setUri(uriString != null ? Uri.parse(uriString) : null)
                .setMediaId(mediaId)
                .setMediaMetadata(metadata)
                .build();
    }
}
