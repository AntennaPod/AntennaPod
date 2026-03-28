package de.danoeh.antennapod.playback.base;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import com.bumptech.glide.Glide;
import com.google.common.collect.ImmutableList;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.system.utils.ThreadUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MediaItemAdapter {
    private static final String TAG = "MediaItemAdapter";
    public static final String MEDIA_ID_FEED_PREFIX = "FeedId:";
    public static final String MEDIA_ID_CONFIRM_STREAMING = "confirm_streaming";
    public static final String KEY_STREAM_URL = "stream_url";

    /**
     * Create a basic media item without attached metadata.
     * Should be used when initiating playback from outside the service.
     */
    public static MediaItem fromPlayableStub(Playable playable) {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.setIsPlayable(true);
        metadataBuilder.setIsBrowsable(false);
        String mediaId = "0";
        if (playable instanceof FeedMedia) {
            mediaId = String.valueOf(((FeedMedia) playable).getId());
        }
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    /**
     * Create a media item and load all its metadata, including cover art using Glide.
     * Do NOT use this method on the main thread.
     */
    public static MediaItem fromPlayable(Context context, Playable playable) {
        ThreadUtils.assertNotMainThread();
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
        }
        int iconSize = (int) (128 * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = loadArtworkBitmap(context, playable, iconSize);
        if (bitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            metadataBuilder.setArtworkData(bos.toByteArray(), MediaMetadata.PICTURE_TYPE_FRONT_COVER);
        } else if (playable.getImageLocation() != null && playable.getImageLocation().startsWith("http")) {
            metadataBuilder.setArtworkUri(Uri.parse(playable.getImageLocation()));
        }
        Bundle extras = new Bundle();
        extras.putString(KEY_STREAM_URL, playable.getStreamUrl());
        metadataBuilder.setExtras(extras);
        String localPlaybackUri = playable.localFileAvailable() ? playable.getLocalFileUrl() : playable.getStreamUrl();
        return new MediaItem.Builder()
                .setUri(localPlaybackUri != null ? Uri.parse(localPlaybackUri) : null)
                .setMediaId(mediaId)
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }

    private static Bitmap loadArtworkBitmap(Context context, Playable playable, int iconSize) {
        try {
            return Glide.with(context).asBitmap().load(playable.getImageLocation())
                    .submit(iconSize, iconSize).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception tr1) {
            // fall through to try feed image
        }
        if (!(playable instanceof FeedMedia)) {
            return null;
        }
        FeedMedia feedMedia = (FeedMedia) playable;
        if (feedMedia.getItem() == null || feedMedia.getItem().getFeed() == null) {
            return null;
        }
        String fallback = feedMedia.getItem().getFeed().getImageUrl();
        if (fallback == null) {
            return null;
        }
        try {
            return Glide.with(context).asBitmap().load(fallback)
                    .submit(iconSize, iconSize).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception tr2) {
            Log.e(TAG, "Error loading artwork bitmap", tr2);
        }
        return null;
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

    public static MediaItem buildStreamingConfirmationItem(Context context,
                                                              @RawRes int audioResId,
                                                              String title, String description) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getResources().getResourcePackageName(audioResId))
                .appendPath(context.getResources().getResourceTypeName(audioResId))
                .appendPath(context.getResources().getResourceEntryName(audioResId))
                .build();
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(title)
                .setDescription(description)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build();
        return new MediaItem.Builder()
                .setMediaId(MEDIA_ID_CONFIRM_STREAMING)
                .setUri(uri)
                .setMediaMetadata(metadata)
                .build();
    }

    public static ImmutableList<MediaItem> fromItemList(Context context, List<FeedItem> feedItems) {
        ImmutableList.Builder<MediaItem> itemsBuilder = ImmutableList.builder();
        for (FeedItem item : feedItems) {
            if (item.getMedia() != null) {
                itemsBuilder.add(MediaItemAdapter.fromPlayable(context, item.getMedia()));
            }
        }
        return itemsBuilder.build();
    }
}
