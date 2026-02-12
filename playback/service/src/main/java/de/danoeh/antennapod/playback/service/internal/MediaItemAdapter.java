package de.danoeh.antennapod.playback.service.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import com.google.common.collect.ImmutableList;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;

import java.util.List;

public class MediaItemAdapter {
    public static final String MEDIA_ID_FEED_PREFIX = "FeedId:";

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
            if (feedMedia.getImageLocation() != null && feedMedia.getImageLocation().startsWith("http")) {
                metadataBuilder.setArtworkUri(Uri.parse(feedMedia.getImageLocation()));
            }
        }
        String uriString = playable.localFileAvailable() ? playable.getLocalFileUrl() : playable.getStreamUrl();
        return new MediaItem.Builder()
                .setUri(uriString != null ? Uri.parse(uriString) : null)
                .setMediaId(mediaId)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }


    public static MediaItem fromFeed(Feed feed) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setTitle(feed.getTitle());
        if (feed.getImageUrl() != null && feed.getImageUrl().startsWith("http")) {
            metadataBuilder.setArtworkUri(Uri.parse(feed.getImageUrl()));
        }
        metadataBuilder.setSubtitle(feed.getAuthor());
        metadataBuilder.setIsBrowsable(true);
        metadataBuilder.setIsPlayable(false);
        return new MediaItem.Builder()
                .setMediaId(MEDIA_ID_FEED_PREFIX + feed.getId())
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    public static MediaItem from(Context context, String id, String title,
                                      @DrawableRes int iconResId, @Nullable String subtitle) {
        Uri iconUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getResources().getResourcePackageName(iconResId))
                .appendPath(context.getResources().getResourceTypeName(iconResId))
                .appendPath(context.getResources().getResourceEntryName(iconResId))
                .build();

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setTitle(title);
        metadataBuilder.setArtworkUri(iconUri);
        if (subtitle != null) {
            metadataBuilder.setSubtitle(subtitle);
        }
        metadataBuilder.setIsBrowsable(true);
        metadataBuilder.setIsPlayable(false);
        return new MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    public static ImmutableList<MediaItem> fromItemList(List<FeedItem> feedItems) {
        ImmutableList.Builder<MediaItem> itemsBuilder = ImmutableList.builder();
        for (FeedItem item : feedItems) {
            if (item.getMedia() != null) {
                itemsBuilder.add(MediaItemAdapter.fromPlayable(item.getMedia()));
            }
        }
        return itemsBuilder.build();
    }
}
