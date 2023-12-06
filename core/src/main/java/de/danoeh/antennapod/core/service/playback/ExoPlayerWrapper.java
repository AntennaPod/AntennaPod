package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.ui.TrackNameProvider;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import com.google.android.exoplayer2.upstream.HttpDataSource;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.service.download.HttpCredentialEncoder;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.model.playback.Playable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExoPlayerWrapper {
    public static final int BUFFERING_STARTED = -1;
    public static final int BUFFERING_ENDED = -2;
    private static final String TAG = "ExoPlayerWrapper";
    public static final int ERROR_CODE_OFFSET = 1000;

    private final Context context;
    private final Disposable bufferingUpdateDisposable;
    private SimpleExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private Runnable audioSeekCompleteListener;
    private Runnable audioCompletionListener;
    private Consumer<String> audioErrorListener;
    private Consumer<Integer> bufferingUpdateListener;
    private PlaybackParameters playbackParameters;
    private DefaultTrackSelector trackSelector;

    private LoudnessEnhancer loudnessEnhancer;

    ExoPlayerWrapper(Context context) {
        this.context = context;
        createPlayer();
        playbackParameters = exoPlayer.getPlaybackParameters();
        bufferingUpdateDisposable = Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tickNumber -> {
                    if (bufferingUpdateListener != null) {
                        bufferingUpdateListener.accept(exoPlayer.getBufferedPercentage());
                    }
                });
    }

    private void createPlayer() {
        DefaultLoadControl.Builder loadControl = new DefaultLoadControl.Builder();
        loadControl.setBufferDurationsMs(30000, 120000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        loadControl.setBackBuffer(UserPreferences.getRewindSecs() * 1000 + 500, true);
        trackSelector = new DefaultTrackSelector(context);
        exoPlayer = new SimpleExoPlayer.Builder(context, new DefaultRenderersFactory(context))
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl.build())
                .build();
        exoPlayer.setSeekParameters(SeekParameters.EXACT);
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int playbackState) {
                if (audioCompletionListener != null && playbackState == Player.STATE_ENDED) {
                    audioCompletionListener.run();
                } else if (bufferingUpdateListener != null && playbackState == Player.STATE_BUFFERING) {
                    bufferingUpdateListener.accept(BUFFERING_STARTED);
                } else if (bufferingUpdateListener != null) {
                    bufferingUpdateListener.accept(BUFFERING_ENDED);
                }
            }

            @Override
            public void onPlayerError(@NonNull ExoPlaybackException error) {
                if (audioErrorListener != null) {
                    if (NetworkUtils.wasDownloadBlocked(error)) {
                        audioErrorListener.accept(context.getString(R.string.download_error_blocked));
                    } else {
                        Throwable cause = error.getCause();
                        if (cause instanceof HttpDataSource.HttpDataSourceException) {
                            if (cause.getCause() != null) {
                                cause = cause.getCause();
                            }
                        }
                        if (cause != null && "Source error".equals(cause.getMessage())) {
                            cause = cause.getCause();
                        }
                        audioErrorListener.accept(cause != null ? cause.getMessage() : error.getMessage());
                    }
                }
            }

            @Override
            public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                                @NonNull Player.PositionInfo newPosition,
                                                @Player.DiscontinuityReason int reason) {
                if (audioSeekCompleteListener != null && reason == Player.DISCONTINUITY_REASON_SEEK) {
                    audioSeekCompleteListener.run();
                }
            }

            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                initLoudnessEnhancer(audioSessionId);
            }
        });

        initLoudnessEnhancer(exoPlayer.getAudioSessionId());
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public float getCurrentSpeedMultiplier() {
        return playbackParameters.speed;
    }

    public int getDuration() {
        if (exoPlayer.getDuration() == C.TIME_UNSET) {
            return Playable.INVALID_TIME;
        }
        return (int) exoPlayer.getDuration();
    }

    public boolean isPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    public void pause() {
        exoPlayer.pause();
    }

    public void prepare() throws IllegalStateException {
        exoPlayer.setMediaSource(mediaSource, false);
        exoPlayer.prepare();
    }

    public void release() {
        bufferingUpdateDisposable.dispose();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        audioSeekCompleteListener = null;
        audioCompletionListener = null;
        audioErrorListener = null;
        bufferingUpdateListener = null;
    }

    public void reset() {
        exoPlayer.release();
        createPlayer();
    }

    public void seekTo(int i) throws IllegalStateException {
        exoPlayer.seekTo(i);
        if (audioSeekCompleteListener != null) {
            audioSeekCompleteListener.run();
        }
    }

    public void setAudioStreamType(int i) {
        AudioAttributes a = exoPlayer.getAudioAttributes();
        AudioAttributes.Builder b = new AudioAttributes.Builder();
        b.setContentType(i);
        b.setFlags(a.flags);
        b.setUsage(a.usage);
        exoPlayer.setAudioAttributes(b.build(), false);
    }

    public void setDataSource(String s, String user, String password)
            throws IllegalArgumentException, IllegalStateException {
        Log.d(TAG, "setDataSource: " + s);
        final OkHttpDataSource.Factory httpDataSourceFactory =
                new OkHttpDataSource.Factory(AntennapodHttpClient.getHttpClient())
                        .setUserAgent(ClientConfig.USER_AGENT);

        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            final HashMap<String, String> requestProperties = new HashMap<>();
            requestProperties.put(
                    "Authorization",
                    HttpCredentialEncoder.encode(user, password, "ISO-8859-1")
            );
            httpDataSourceFactory.setDefaultRequestProperties(requestProperties);
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, null, httpDataSourceFactory);
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        extractorsFactory.setConstantBitrateSeekingEnabled(true);
        extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_DISABLE_ID3_METADATA);
        ProgressiveMediaSource.Factory f = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory);
        final MediaItem mediaItem = MediaItem.fromUri(Uri.parse(s));
        mediaSource = f.createMediaSource(mediaItem);
    }

    public void setDataSource(String s) throws IllegalArgumentException, IllegalStateException {
        setDataSource(s, null, null);
    }

    public void setDisplay(SurfaceHolder sh) {
        exoPlayer.setVideoSurfaceHolder(sh);
    }

    public void setPlaybackParams(float speed, boolean skipSilence) {
        playbackParameters = new PlaybackParameters(speed, playbackParameters.pitch);
        exoPlayer.setSkipSilenceEnabled(skipSilence);
        exoPlayer.setPlaybackParameters(playbackParameters);
    }

    public void setVolume(float v, float v1) {
        if (v > 1) {
            exoPlayer.setVolume(1f);
            loudnessEnhancer.setEnabled(true);
            loudnessEnhancer.setTargetGain((int) (1000 * (v - 1)));
        } else {
            exoPlayer.setVolume(v);
            loudnessEnhancer.setEnabled(false);
        }
    }

    public void start() {
        exoPlayer.play();
        // Can't set params when paused - so always set it on start in case they changed
        exoPlayer.setPlaybackParameters(playbackParameters);
    }

    public void stop() {
        exoPlayer.stop();
    }

    public List<String> getAudioTracks() {
        List<String> trackNames = new ArrayList<>();
        TrackNameProvider trackNameProvider = new DefaultTrackNameProvider(context.getResources());
        for (Format format : getFormats()) {
            trackNames.add(trackNameProvider.getTrackName(format));
        }
        return trackNames;
    }

    private List<Format> getFormats() {
        List<Format> formats = new ArrayList<>();
        MappingTrackSelector.MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (trackInfo == null) {
            return Collections.emptyList();
        }
        TrackGroupArray trackGroups = trackInfo.getTrackGroups(getAudioRendererIndex());
        for (int i = 0; i < trackGroups.length; i++) {
            formats.add(trackGroups.get(i).getFormat(0));
        }
        return formats;
    }

    public void setAudioTrack(int track) {
        MappingTrackSelector.MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (trackInfo == null) {
            return;
        }
        TrackGroupArray trackGroups = trackInfo.getTrackGroups(getAudioRendererIndex());
        DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(track, 0);
        DefaultTrackSelector.ParametersBuilder params = trackSelector.buildUponParameters()
                .setSelectionOverride(getAudioRendererIndex(), trackGroups, override);
        trackSelector.setParameters(params);
    }

    private int getAudioRendererIndex() {
        for (int i = 0; i < exoPlayer.getRendererCount(); i++) {
            if (exoPlayer.getRendererType(i) == C.TRACK_TYPE_AUDIO) {
                return i;
            }
        }
        return -1;
    }

    public int getSelectedAudioTrack() {
        TrackSelectionArray trackSelections = exoPlayer.getCurrentTrackSelections();
        List<Format> availableFormats = getFormats();
        for (int i = 0; i < trackSelections.length; i++) {
            ExoTrackSelection track = (ExoTrackSelection) trackSelections.get(i);
            if (track == null) {
                continue;
            }
            if (availableFormats.contains(track.getSelectedFormat())) {
                return availableFormats.indexOf(track.getSelectedFormat());
            }
        }
        return -1;
    }

    void setOnCompletionListener(Runnable audioCompletionListener) {
        this.audioCompletionListener = audioCompletionListener;
    }

    void setOnSeekCompleteListener(Runnable audioSeekCompleteListener) {
        this.audioSeekCompleteListener = audioSeekCompleteListener;
    }

    void setOnErrorListener(Consumer<String> audioErrorListener) {
        this.audioErrorListener = audioErrorListener;
    }

    int getVideoWidth() {
        if (exoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return exoPlayer.getVideoFormat().width;
    }

    int getVideoHeight() {
        if (exoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return exoPlayer.getVideoFormat().height;
    }

    void setOnBufferingUpdateListener(Consumer<Integer> bufferingUpdateListener) {
        this.bufferingUpdateListener = bufferingUpdateListener;
    }

    private void initLoudnessEnhancer(int audioStreamId) {
        LoudnessEnhancer newEnhancer = new LoudnessEnhancer(audioStreamId);
        LoudnessEnhancer oldEnhancer = this.loudnessEnhancer;
        if (oldEnhancer != null) {
            newEnhancer.setEnabled(oldEnhancer.getEnabled());
            if (oldEnhancer.getEnabled()) {
                newEnhancer.setTargetGain((int) oldEnhancer.getTargetGain());
            }
            oldEnhancer.release();
        }

        this.loudnessEnhancer = newEnhancer;
    }
}
