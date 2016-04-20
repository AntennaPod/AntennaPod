package de.danoeh.antennapod.core.cast;

import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;

public class DefaultCastConsumer extends VideoCastConsumerImpl implements CastConsumer {
    @Override
    public void onStreamVolumeChanged(double value, boolean isMute) {
        // no-op
    }
}
