package de.danoeh.antennapod.playback.cast;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;

/**
 * Stub implementation of CastPsmp for Free build flavour
 */
public class CastPsmp {
    @Nullable
    public static PlaybackServiceMediaPlayer getInstanceIfConnected(@NonNull Context context,
                                        @NonNull PlaybackServiceMediaPlayer.PSMPCallback callback) {
        return null;
    }
}
