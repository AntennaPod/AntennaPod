package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.mp3.Mp3Extractor;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.io.File;
import java.util.Collections;

public class ExoPlayerUtils {
    @OptIn(markerClass = UnstableApi.class)
    public static ExoPlayer buildPlayer(Context context) {
        return new ExoPlayer.Builder(context)
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                (int) (UserPreferences.getFastForwardSecs() * 1000L),
                                Math.max(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                                        (int) (UserPreferences.getFastForwardSecs() * 1000L)),
                                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                        .setBackBuffer((int) (3 * UserPreferences.getRewindSecs() * 1000L), true)
                        .build())
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(), true)
                .setMediaSourceFactory(new ApMediaSourceFactory(context))
                .setSeekParameters(SeekParameters.EXACT)
                .build();
    }

    public static String translateErrorReason(@NonNull PlaybackException error, Context context) {
        if (NetworkUtils.wasDownloadBlocked(error)) {
            return context.getString(R.string.download_error_blocked);
        }

        Throwable cause = error.getCause();
        if (cause instanceof HttpDataSource.HttpDataSourceException) {
            if (cause.getCause() != null) {
                cause = cause.getCause();
            }
        }
        if (cause != null && "Source error".equals(cause.getMessage())) {
            cause = cause.getCause();
        }
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        } else if (error.getMessage() != null && cause != null) {
            return error.getMessage() + ": " + cause.getClass().getSimpleName();
        } else {
            return "Unknown error";
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static class ApMediaSourceFactory implements MediaSource.Factory {
        /**
         * Process-wide SimpleCache used to insulate streamed playback from podcast servers
         * that perform dynamic ad insertion. Without this, ExoPlayer's range requests for
         * the same byte offsets can return different payloads on each request, causing the
         * player to loop or end an episode early (AntennaPod issues #7409, #7523).
         * <p>
         * Static so it survives ExoPlayer lifecycle resets. Same approach as Pocket Casts.
         */
        private static final long STREAM_CACHE_BYTES = 100L * 1024 * 1024;
        private static volatile SimpleCache streamCache;

        private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
        private DrmSessionManagerProvider drmSessionManagerProvider;
        private final DefaultExtractorsFactory extractorsFactory;
        private final DefaultMediaSourceFactory defaultFactory;
        private final Context context;

        public ApMediaSourceFactory(Context context) {
            super();
            this.context = context.getApplicationContext();
            this.extractorsFactory = new DefaultExtractorsFactory();
            this.extractorsFactory.setConstantBitrateSeekingEnabled(true);
            this.extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA);
            this.defaultFactory = new DefaultMediaSourceFactory(this.context, extractorsFactory);
        }

        private synchronized SimpleCache getStreamCache() {
            if (streamCache == null) {
                File cacheDir = new File(context.getCacheDir(), "stream-stable");
                streamCache = new SimpleCache(cacheDir,
                        new LeastRecentlyUsedCacheEvictor(STREAM_CACHE_BYTES),
                        new StandaloneDatabaseProvider(context));
            }
            return streamCache;
        }

        @Override
        public MediaSource.Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider) {
            this.drmSessionManagerProvider = drmSessionManagerProvider;
            return this;
        }

        @Override
        public MediaSource.Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy policy) {
            this.loadErrorHandlingPolicy = policy;
            return this;
        }

        @Override
        public MediaSource createMediaSource(MediaItem mediaItem) {
            DefaultMediaSourceFactory factory = new DefaultMediaSourceFactory(
                    buildDataSourceFactory(mediaItem), extractorsFactory);
            if (loadErrorHandlingPolicy != null) {
                factory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
            }
            if (drmSessionManagerProvider != null) {
                factory.setDrmSessionManagerProvider(drmSessionManagerProvider);
            }
            return factory.createMediaSource(mediaItem);
        }

        private DataSource.Factory buildDataSourceFactory(MediaItem mediaItem) {
            DefaultHttpDataSource.Factory httpDataSourceFactory =
                    new DefaultHttpDataSource.Factory();
            httpDataSourceFactory.setUserAgent(UserAgentInterceptor.USER_AGENT);
            httpDataSourceFactory.setAllowCrossProtocolRedirects(true);
            httpDataSourceFactory.setKeepPostFor302Redirects(true);
            String authHeader = mediaItem.requestMetadata.extras != null
                    ? mediaItem.requestMetadata.extras.getString(
                            MediaItemAdapter.KEY_AUTHORIZATION_HEADER)
                    : null;
            if (authHeader != null) {
                httpDataSourceFactory.setDefaultRequestProperties(
                        Collections.singletonMap("Authorization", authHeader));
            }

            DataSource.Factory upstream = new DefaultDataSource.Factory(context, httpDataSourceFactory);
            return new CacheDataSource.Factory()
                    .setCache(getStreamCache())
                    .setUpstreamDataSourceFactory(upstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        }

        @Override
        public int[] getSupportedTypes() {
            return defaultFactory.getSupportedTypes();
        }
    }
}
