package de.danoeh.antennapod.core.cast;

import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;

public interface CastConsumer extends VideoCastConsumer{

    /**
     * Called when the stream's volume is changed.
     */
    void onStreamVolumeChanged(double value, boolean isMute);
}
