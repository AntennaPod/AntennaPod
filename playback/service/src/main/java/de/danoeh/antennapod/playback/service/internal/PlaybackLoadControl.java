package de.danoeh.antennapod.playback.service.internal;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;

import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public final class PlaybackLoadControl {
    static final int MIN_BUFFER_MS = (int) TimeUnit.MINUTES.toMillis(2);
    static final int MAX_BUFFER_MS = (int) TimeUnit.MINUTES.toMillis(10);
    static final int BACK_BUFFER_MS = (int) TimeUnit.SECONDS.toMillis(30);

    private PlaybackLoadControl() {
    }

    public static LoadControl create() {
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        MIN_BUFFER_MS,
                        MAX_BUFFER_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .setBackBuffer(BACK_BUFFER_MS, false)
                .build();
    }
}
