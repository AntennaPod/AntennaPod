package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.antennapod.audio.MediaPlayer;
import de.danoeh.antennapod.core.util.playback.IPlayer;

public class ExoPlayerWrapper implements IPlayer {
    private final Context mContext;
    private SimpleExoPlayer mExoPlayer;
    private MediaSource mediaSource;
    private MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener;
    private MediaPlayer.OnCompletionListener audioCompletionListener;
    private MediaPlayer.OnErrorListener audioErrorListener;
    private MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener;
    private boolean shouldCheckBufferingUpdates = true;


    ExoPlayerWrapper(Context context) {
        mContext = context;
        mExoPlayer = createPlayer();

        Handler handler = new Handler(); // Main thread
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bufferingUpdateListener != null) {
                    bufferingUpdateListener.onBufferingUpdate(null, mExoPlayer.getBufferedPercentage());
                }
                if (shouldCheckBufferingUpdates) {
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    private SimpleExoPlayer createPlayer() {
        DefaultLoadControl.Builder loadControl = new DefaultLoadControl.Builder();
        loadControl.setBufferDurationsMs(30000, 120000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
        SimpleExoPlayer p = ExoPlayerFactory.newSimpleInstance(mContext, new DefaultRenderersFactory(mContext),
                new DefaultTrackSelector(), loadControl.createDefaultLoadControl());
        p.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
        p.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    audioCompletionListener.onCompletion(null);
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                if (audioErrorListener != null) {
                    audioErrorListener.onError(null, 0, 0);
                }
            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {
                audioSeekCompleteListener.onSeekComplete(null);
            }
        });
        return p;
    }

    @Override
    public boolean canSetSpeed() {
        return true;
    }

    @Override
    public boolean canDownmix() {
        return false;
    }

    @Override
    public int getCurrentPosition() {
        return (int) mExoPlayer.getCurrentPosition();
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return mExoPlayer.getPlaybackParameters().speed;
    }

    @Override
    public int getDuration() {
        if (mExoPlayer.getDuration() == C.TIME_UNSET) {
            return PlaybackServiceMediaPlayer.INVALID_TIME;
        }
        return (int) mExoPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayer.getPlayWhenReady();
    }

    @Override
    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void prepare() throws IllegalStateException {
        mExoPlayer.prepare(mediaSource);
    }

    @Override
    public void release() {
        shouldCheckBufferingUpdates = false;
        if (mExoPlayer != null) {
            mExoPlayer.release();
        }
        audioSeekCompleteListener = null;
        audioCompletionListener = null;
        audioErrorListener = null;
        bufferingUpdateListener = null;
    }

    @Override
    public void reset() {
        mExoPlayer.release();
        mExoPlayer = createPlayer();
    }

    @Override
    public void seekTo(int i) throws IllegalStateException {
        mExoPlayer.seekTo(i);
        audioSeekCompleteListener.onSeekComplete(null);
    }

    @Override
    public void setAudioStreamType(int i) {
        AudioAttributes a = mExoPlayer.getAudioAttributes();
        AudioAttributes.Builder b = new AudioAttributes.Builder();
        b.setContentType(i);
        b.setFlags(a.flags);
        b.setUsage(a.usage);
        mExoPlayer.setAudioAttributes(b.build());
    }

    @Override
    public void setDataSource(String s) throws IllegalArgumentException, IllegalStateException {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, mContext.getPackageName()), null);
        ExtractorMediaSource.Factory f = new ExtractorMediaSource.Factory(dataSourceFactory);
        mediaSource = f.createMediaSource(Uri.parse(s));
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        mExoPlayer.setVideoSurfaceHolder(sh);
    }

    @Override
    public void setPlaybackParams(float speed, boolean skipSilence) {
        PlaybackParameters params = mExoPlayer.getPlaybackParameters();
        mExoPlayer.setPlaybackParameters(new PlaybackParameters(speed, params.pitch, skipSilence));
    }

    @Override
    public void setDownmix(boolean b) {

    }

    @Override
    public void setVolume(float v, float v1) {
        mExoPlayer.setVolume(v);
    }

    @Override
    public void setWakeMode(Context context, int i) {

    }

    @Override
    public void start() {
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() {
        mExoPlayer.stop();
    }

    void setOnCompletionListener(MediaPlayer.OnCompletionListener audioCompletionListener) {
        this.audioCompletionListener = audioCompletionListener;
    }

    void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener) {
        this.audioSeekCompleteListener = audioSeekCompleteListener;
    }

    void setOnErrorListener(MediaPlayer.OnErrorListener audioErrorListener) {
        this.audioErrorListener = audioErrorListener;
    }

    int getVideoWidth() {
        if (mExoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return mExoPlayer.getVideoFormat().width;
    }

    int getVideoHeight() {
        if (mExoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return mExoPlayer.getVideoFormat().height;
    }

    void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener) {
        this.bufferingUpdateListener = bufferingUpdateListener;
    }
}
