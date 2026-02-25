package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class ExoPlayerUtils {
    @OptIn(markerClass = UnstableApi.class)
    public static ExoPlayer buildPlayer(Context context) {
        return new ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(UserPreferences.getRewindSecs() * 1000L)
                .setSeekForwardIncrementMs(UserPreferences.getFastForwardSecs() * 1000L)
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs((int) (UserPreferences.getFastForwardSecs() * 1000L),
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
}
