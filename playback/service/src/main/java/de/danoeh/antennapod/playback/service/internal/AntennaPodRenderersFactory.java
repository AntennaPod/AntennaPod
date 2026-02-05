package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.SonicAudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

import java.util.Objects;

@OptIn(markerClass = UnstableApi.class)
final class AntennaPodRenderersFactory extends DefaultRenderersFactory {
    private final SilenceSkippingAudioProcessor silenceSkippingAudioProcessor;

    AntennaPodRenderersFactory(Context context, SilenceSkippingAudioProcessor silenceSkippingAudioProcessor) {
        super(context);
        this.silenceSkippingAudioProcessor = Objects.requireNonNull(
                silenceSkippingAudioProcessor, "silenceSkippingAudioProcessor must not be null");
    }

    @Override
    protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput,
                                       boolean enableAudioTrackPlaybackParams,
                                       boolean enableOffload) {
        DefaultAudioSink.DefaultAudioProcessorChain audioProcessorChain =
                new DefaultAudioSink.DefaultAudioProcessorChain(
                        new AudioProcessor[0],
                        silenceSkippingAudioProcessor,
                        new SonicAudioProcessor());
        return new DefaultAudioSink.Builder(context)
                .setAudioProcessorChain(audioProcessorChain)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setOffloadMode(
                        enableOffload
                                ? DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED
                                : DefaultAudioSink.OFFLOAD_MODE_DISABLED)
                .build();
    }
}
