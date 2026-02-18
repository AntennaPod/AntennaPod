package de.danoeh.antennapod.playback.cast;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.MediaItemConverter;
import androidx.media3.cast.RemoteCastPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;

public class CastPlayerWrapper {
    private static final String KEY_MEDIA_ID = "media_id";
    private static final String KEY_LOCAL_FILE_URL = "local_file_url";

    @OptIn(markerClass = UnstableApi.class)
    public static Player wrap(Player player, Context context) {
        RemoteCastPlayer remotePlayer = new RemoteCastPlayer.Builder(context)
                .setMediaItemConverter(new ApMediaItemConverter())
                .build();
        return new CastPlayer.Builder(context)
                .setLocalPlayer(player)
                .setRemotePlayer(remotePlayer)
                .build();
    }

    /**
     * Alias name to avoid specifying the full package import due to the MediaMetadata name collision
     */
    private static class CastMediaMetadata extends com.google.android.gms.cast.MediaMetadata {
        public CastMediaMetadata(int type) {
            super(type);
        }
    }

    @UnstableApi
    public static class ApMediaItemConverter implements MediaItemConverter {
        @Override
        @NonNull
        public MediaQueueItem toMediaQueueItem(@NonNull MediaItem mediaItem) {
            CastMediaMetadata metadata = new CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_GENERIC);
            if (mediaItem.mediaMetadata.title != null) {
                metadata.putString(CastMediaMetadata.KEY_TITLE, mediaItem.mediaMetadata.title.toString());
            }
            if (mediaItem.mediaMetadata.subtitle != null) {
                metadata.putString(CastMediaMetadata.KEY_SUBTITLE, mediaItem.mediaMetadata.subtitle.toString());
            }
            if (mediaItem.mediaMetadata.artworkUri != null) {
                metadata.addImage(new WebImage(mediaItem.mediaMetadata.artworkUri));
            }
            if (!mediaItem.mediaId.isEmpty()) {
                metadata.putString(KEY_MEDIA_ID, mediaItem.mediaId);
            }
            if (mediaItem.localConfiguration != null) {
                metadata.putString(KEY_LOCAL_FILE_URL, mediaItem.localConfiguration.uri.toString());
            }
            String streamUrl = null;
            if (mediaItem.mediaMetadata.extras != null) {
                streamUrl = mediaItem.mediaMetadata.extras.getString(MediaItemAdapter.KEY_STREAM_URL);
            }
            MediaInfo mediaInfo = new MediaInfo.Builder(streamUrl != null ? streamUrl : "")
                    .setMetadata(metadata)
                    .build();
            return new MediaQueueItem.Builder(mediaInfo).build();
        }

        @Override
        @NonNull
        public MediaItem toMediaItem(@NonNull MediaQueueItem mediaQueueItem) {
            MediaInfo mediaInfo = mediaQueueItem.getMedia();
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
            MediaItem.Builder builder = new MediaItem.Builder();

            if (mediaInfo == null) {
                return builder.build();
            }
            if (mediaInfo.getContentUrl() != null) {
                builder.setUri(mediaInfo.getContentUrl());
                Bundle extras = new Bundle();
                extras.putString(MediaItemAdapter.KEY_STREAM_URL, mediaInfo.getContentUrl());
                metadataBuilder.setExtras(extras);
            }
            com.google.android.gms.cast.MediaMetadata castMetadata = mediaInfo.getMetadata();
            if (castMetadata == null) {
                return builder.build();
            }
            if (castMetadata.containsKey(CastMediaMetadata.KEY_TITLE)) {
                metadataBuilder.setTitle(castMetadata.getString(CastMediaMetadata.KEY_TITLE));
            }
            if (castMetadata.containsKey(CastMediaMetadata.KEY_SUBTITLE)) {
                metadataBuilder.setSubtitle(castMetadata.getString(CastMediaMetadata.KEY_SUBTITLE));
            }
            if (!castMetadata.getImages().isEmpty()) {
                metadataBuilder.setArtworkUri(castMetadata.getImages().get(0).getUrl());
            }
            if (castMetadata.containsKey(KEY_MEDIA_ID)) {
                builder.setMediaId(castMetadata.getString(KEY_MEDIA_ID));
            }
            if (castMetadata.containsKey(KEY_LOCAL_FILE_URL)) {
                builder.setUri(castMetadata.getString(KEY_LOCAL_FILE_URL));
            }
            builder.setMediaMetadata(metadataBuilder.build());
            return builder.build();
        }
    }
}
