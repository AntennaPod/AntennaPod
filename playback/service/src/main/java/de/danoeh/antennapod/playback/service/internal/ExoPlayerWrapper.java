package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.util.Consumer;

import androidx.media3.common.C;
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
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;

import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.mp3.Mp3Extractor;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;

import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.net.common.HttpCredentialEncoder;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@OptIn(markerClass = UnstableApi.class)
public class ExoPlayerWrapper {
    public static final int BUFFERING_STARTED = -1;
    public static final int BUFFERING_ENDED = -2;
    static final float[] EQUALIZER_BAND_CUTOFFS = {100, 230, 430, 775, 1750, 4250, 8500, 13000, 15000, 20000};
    static final int EQUALIZER_BAND_COUNT = EQUALIZER_BAND_CUTOFFS.length;
    private static final String TAG = "ExoPlayerWrapper";

    private final Context context;
    private final Disposable bufferingUpdateDisposable;
    private ExoPlayer exoPlayer;
    private MediaSource mediaSource;
    private Runnable audioSeekCompleteListener;
    private Runnable audioCompletionListener;
    private Consumer<String> audioErrorListener;
    private Consumer<Integer> bufferingUpdateListener;
    private PlaybackParameters playbackParameters;
    private DefaultTrackSelector trackSelector;
    private SimpleCache simpleCache;
    @Nullable
    private LoudnessEnhancer loudnessEnhancer = null;
    @Nullable
    private DynamicsProcessing dynamicsProcessing = null;

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
        loadControl.setBufferDurationsMs((int) TimeUnit.HOURS.toMillis(1), (int) TimeUnit.HOURS.toMillis(3),
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        loadControl.setBackBuffer((int) TimeUnit.MINUTES.toMillis(5), true);
        trackSelector = new DefaultTrackSelector(context);
        exoPlayer = new ExoPlayer.Builder(context, new DefaultRenderersFactory(context))
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
            public void onPlayerError(@NonNull PlaybackException error) {
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
                        if (cause != null && cause.getMessage() != null) {
                            audioErrorListener.accept(cause.getMessage());
                        } else if (error.getMessage() != null && cause != null) {
                            audioErrorListener.accept(error.getMessage() + ": " + cause.getClass().getSimpleName());
                        } else {
                            audioErrorListener.accept(null);
                        }
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
                initDynamicsProcessing(audioSessionId);
            }
        });
        simpleCache = new SimpleCache(new File(context.getCacheDir(), "streaming"),
                new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), new StandaloneDatabaseProvider(context));
        initLoudnessEnhancer(exoPlayer.getAudioSessionId());
        initDynamicsProcessing(exoPlayer.getAudioSessionId());
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public float getCurrentSpeedMultiplier() {
        return playbackParameters.speed;
    }

    public boolean getCurrentSkipSilence() {
        return exoPlayer.getSkipSilenceEnabled();
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
        if (simpleCache != null) {
            simpleCache.release();
            simpleCache = null;
        }
        audioSeekCompleteListener = null;
        audioCompletionListener = null;
        audioErrorListener = null;
        bufferingUpdateListener = null;
    }

    public void reset() {
        exoPlayer.release();
        if (simpleCache != null) {
            simpleCache.release();
            simpleCache = null;
        }
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
        final DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();
        httpDataSourceFactory.setUserAgent(UserAgentInterceptor.USER_AGENT);
        httpDataSourceFactory.setAllowCrossProtocolRedirects(true);
        httpDataSourceFactory.setKeepPostFor302Redirects(true);

        if (!TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)) {
            final HashMap<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Authorization", HttpCredentialEncoder.encode(user, password, "ISO-8859-1"));
            httpDataSourceFactory.setDefaultRequestProperties(requestProperties);
        }
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);
        if (s.startsWith("http")) {
            dataSourceFactory = new CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory);
        }
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
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(true);
                    loudnessEnhancer.setTargetGain((int) (1000 * (v - 1)));
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } else {
            exoPlayer.setVolume(v);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.setEnabled(false);
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
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

    public synchronized void changeCompressor(
            boolean enabled, float threshold,
            float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain)  {
        setAndApplyMbcBandParameters(this.dynamicsProcessing, enabled,
                threshold, ratio, attackTime, releaseTime, noiseGateThreshold, postGain);
    }

    public synchronized void changeEqualizer(
            boolean enabled, float[] gains) {
        setAndApplyPostEqualizerParameters(this.dynamicsProcessing, enabled, gains);
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
        DefaultTrackSelector.Parameters params = trackSelector.buildUponParameters()
                .setSelectionOverride(getAudioRendererIndex(), trackGroups, override).build();
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
        if (!VolumeAdaptionSetting.isBoostSupported()) {
            return;
        }

        LoudnessEnhancer newEnhancer = new LoudnessEnhancer(audioStreamId);
        LoudnessEnhancer oldEnhancer = this.loudnessEnhancer;
        if (oldEnhancer != null) {
            try {
                newEnhancer.setEnabled(oldEnhancer.getEnabled());
                if (oldEnhancer.getEnabled()) {
                    newEnhancer.setTargetGain((int) oldEnhancer.getTargetGain());
                }
                oldEnhancer.release();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        }

        this.loudnessEnhancer = newEnhancer;
    }

    private boolean shallDynamicsProcessingBeEnabled() {
        return shallDynamicsProcessingBeEnabled(
                UserPreferences.isCompressorEnabled(),
                UserPreferences.isEqualizerEnabled());
    }

    private boolean shallDynamicsProcessingBeEnabled(boolean isCompressorEnabled, boolean isEqualizerEnabled) {
        return isCompressorEnabled || isEqualizerEnabled;
    }

    private void initDynamicsProcessing(int audioSessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        // Prepare dynamics processing build/config object
        DynamicsProcessing.Config config = buildDynamicsProcessingConfig();
        if (config == null) {
            return;
        }

        // Set up MBC (multi band compressor) initial config
        DynamicsProcessing.Mbc mbc = config.getMbcByChannelIndex(0);
        config.setMbcAllChannelsTo(mbc);
        DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);
        setMbcBandParameters(
                mbcBand,
                UserPreferences.isCompressorEnabled(),
                UserPreferences.getCompressorThreshold(),
                UserPreferences.getCompressorRatio(),
                UserPreferences.getCompressorAttackTime(),
                UserPreferences.getCompressorReleaseTime(),
                UserPreferences.getCompressorNoiseGateThreshold(),
                UserPreferences.getCompressorPostGain());
        applyMbcBand(config, mbcBand);

        // Set up post equalizer initial config
        boolean isPostEqEnabled = UserPreferences.isEqualizerEnabled();
        DynamicsProcessing.Eq postEq = config.getPostEqByChannelIndex(0);
        config.setPostEqAllChannelsTo(postEq);
        float[] userPreferencesGains = UserPreferences.getEqualizerGains();
        if (userPreferencesGains.length != EQUALIZER_BAND_COUNT) {
            Log.e(TAG, "Invalid equalizer preferences band count: " + EQUALIZER_BAND_COUNT + " bands exist, but "
                    + userPreferencesGains.length + "are set in preferences!");
            isPostEqEnabled = false;
        } else if (postEq.getBandCount() != EQUALIZER_BAND_COUNT) {
            Log.e(TAG, "Invalid post equalizer band count: " + EQUALIZER_BAND_COUNT + " requested, but "
                    + postEq.getBandCount() + "are available!");
            isPostEqEnabled = false;
        }
        setPostEqualizerParameters(postEq, isPostEqEnabled, userPreferencesGains);
        applyPostEqualizer(config, postEq);

        // Create DynamicsProcessing object and subobjects and attach to the supplied audioSessionId
        int priority = 0;
        boolean enableDynProc = shallDynamicsProcessingBeEnabled();
        DynamicsProcessing oldDynamicsProcessing = this.dynamicsProcessing;
        synchronized (this) {
            this.dynamicsProcessing = new DynamicsProcessing(priority, audioSessionId, config);
            this.dynamicsProcessing.setEnabled(enableDynProc);
        }
        Log.i(TAG, "DynamicsProcessing Enabled=" + this.dynamicsProcessing.getEnabled());

        if (oldDynamicsProcessing != null) {
            oldDynamicsProcessing.release();
        }
    }

    @Nullable
    private static DynamicsProcessing.Config buildDynamicsProcessingConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }

        int channelCount = 1;
        boolean preEqInUse = false;
        boolean mbcInUse = true;
        int mbcBandCount = 1;
        boolean postEqInUse = true;
        boolean limiterInUse = false;
        DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channelCount,
                preEqInUse, EQUALIZER_BAND_COUNT,
                mbcInUse, mbcBandCount,
                postEqInUse, EQUALIZER_BAND_COUNT,
                limiterInUse);
        builder.setPreferredFrameDuration(25);
        return builder.build();
    }

    private void setMbcBandParameters(
            DynamicsProcessing.MbcBand mbcBand, boolean enabled,
            float threshold, float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        if (enabled) {
            mbcBand.setPreGain(0);
            mbcBand.setExpanderRatio(20);
            mbcBand.setCutoffFrequency(20000);

            mbcBand.setThreshold(threshold);
            mbcBand.setRatio(ratio);
            mbcBand.setAttackTime(attackTime);
            mbcBand.setReleaseTime(releaseTime);
            mbcBand.setNoiseGateThreshold(noiseGateThreshold);
            mbcBand.setPostGain(postGain);

            mbcBand.setEnabled(true);
        } else {
            mbcBand.setEnabled(false);

            mbcBand.setThreshold(-45);
            mbcBand.setRatio(1);
            mbcBand.setAttackTime(3.0f);
            mbcBand.setReleaseTime(80.0f);
            mbcBand.setNoiseGateThreshold(-90);
            mbcBand.setPostGain(0);
        }
    }

    private void applyMbcBand(DynamicsProcessing.Config config, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            logMbcBandParameters("applyMbcBand(config, mbcBand)", mbcBand);
            config.setMbcBandAllChannelsTo(0, mbcBand);
        }
    }

    private void applyMbcBand(DynamicsProcessing dynProc, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            logMbcBandParameters("applyMbcBand(dynProc, mbcBand)", mbcBand);
            dynProc.setMbcBandAllChannelsTo(0, mbcBand);
        }
    }

    private void logMbcBandParameters(String funcName, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, funcName + " Enabled=" + mbcBand.isEnabled()
                    + " AttackTime=" + mbcBand.getAttackTime() + " ReleaseTime=" + mbcBand.getReleaseTime()
                    + " PreGain=" + mbcBand.getPreGain() + " PostGain=" + mbcBand.getPostGain()
                    + " Threshold=" + mbcBand.getThreshold() + " Ratio=" + mbcBand.getRatio()
                    + " KneeWidth=" + mbcBand.getKneeWidth() + " CutoffFrequency=" + mbcBand.getCutoffFrequency()
                    + " NoiseGateThreshold=" + mbcBand.getNoiseGateThreshold()
                    + " ExpanderRatio=" + mbcBand.getExpanderRatio());
        }
    }

    private synchronized void setAndApplyMbcBandParameters(
            DynamicsProcessing dynProc, boolean enabled,
            float threshold, float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        if (dynProc.getChannelCount() < 1) {
            Log.e(TAG, "No channel found to apply MbcBandParameters! "
                    + "(getChannelCount()=" + dynProc.getChannelCount() + ")");
            return;
        }
        DynamicsProcessing.Mbc mbc = dynProc.getMbcByChannelIndex(0);
        if (mbc.getBandCount() < 1) {
            Log.e(TAG, "No band found to apply MbcBandParameters! (getBandCount()=" + mbc.getBandCount() + ")");
            return;
        }
        DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);

        boolean enableDynProc = shallDynamicsProcessingBeEnabled(enabled, UserPreferences.isEqualizerEnabled());
        if (!enableDynProc) {
            dynProc.setEnabled(false);
        }
        setMbcBandParameters(mbcBand, enabled, threshold, ratio, attackTime, releaseTime, noiseGateThreshold, postGain);
        applyMbcBand(dynProc, mbcBand);
        if (enableDynProc) {
            dynProc.setEnabled(true);
            Log.i(TAG, "Dynamics Processing enabled=" + dynProc.getEnabled());
        }
    }

    private void setPostEqualizerParameters(DynamicsProcessing.Eq postEq, boolean enabled, float[] gains) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        for (int i = 0; i < EQUALIZER_BAND_COUNT; i++) {
            DynamicsProcessing.EqBand band = postEq.getBand(i);
            band.setEnabled(enabled);
            band.setGain(gains[i]);
            band.setCutoffFrequency(EQUALIZER_BAND_CUTOFFS[i]);
        }
    }

    private void applyPostEqualizer(DynamicsProcessing.Config config, DynamicsProcessing.Eq postEq) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            config.setPostEqAllChannelsTo(postEq);
        }
    }

    private void applyPostEqualizer(DynamicsProcessing dynProc, DynamicsProcessing.Eq postEq) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynProc.setPostEqAllChannelsTo(postEq);
        }
    }

    private synchronized void setAndApplyPostEqualizerParameters(
            DynamicsProcessing dynProc, boolean enabled, float[] gains
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        if (dynProc.getChannelCount() < 1) {
            return;
        }
        DynamicsProcessing.Eq postEq = dynProc.getPostEqByChannelIndex(0);
        if (postEq.getBandCount() != EQUALIZER_BAND_COUNT) {
            return;
        }

        boolean enableDynProc = shallDynamicsProcessingBeEnabled(UserPreferences.isCompressorEnabled(), enabled);
        if (!enableDynProc) {
            dynProc.setEnabled(false);
        }
        setPostEqualizerParameters(postEq, enabled, gains);
        applyPostEqualizer(dynProc, postEq);
        if (enableDynProc) {
            dynProc.setEnabled(true);
        }
    }
}
