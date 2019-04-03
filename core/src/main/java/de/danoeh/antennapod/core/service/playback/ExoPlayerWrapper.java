package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.view.SurfaceHolder;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
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
import de.danoeh.antennapod.core.util.playback.IPlayer;
import org.antennapod.audio.MediaPlayer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ExoPlayerWrapper implements IPlayer {
    private final Context mContext;
    private SimpleExoPlayer mExoPlayer;
    private MediaSource mediaSource;
    private MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener;
    private MediaPlayer.OnCompletionListener audioCompletionListener;
    private MediaPlayer.OnErrorListener audioErrorListener;
    private static ExecutorService executor;

    ExoPlayerWrapper(Context context) {
        mContext = context;
        runOnExoPlayerThread(() -> mExoPlayer = createPlayer());
    }

    private void prepareExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
            runOnExoPlayerThread(Looper::prepare);
        }
    }

    private synchronized <T> T runOnExoPlayerThread(Callable<T> callable, T fallback) {
        prepareExecutor();
        try {
            return executor.submit(callable).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return fallback;
        }
    }

    private synchronized void runOnExoPlayerThread(Runnable runnable) {
        prepareExecutor();
        try {
            executor.submit(runnable).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private SimpleExoPlayer createPlayer() {
        SimpleExoPlayer p = ExoPlayerFactory.newSimpleInstance(mContext, new DefaultRenderersFactory(mContext),
                new DefaultTrackSelector(), new DefaultLoadControl(), null, Looper.myLooper());
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
        return runOnExoPlayerThread(() -> (int) mExoPlayer.getCurrentPosition(), PlaybackServiceMediaPlayer.INVALID_TIME);
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return runOnExoPlayerThread(() -> mExoPlayer.getPlaybackParameters().speed, 1f);
    }

    @Override
    public int getDuration() {
        long duration = runOnExoPlayerThread(() -> mExoPlayer.getDuration(), C.TIME_UNSET);
        if (duration == C.TIME_UNSET) {
            return PlaybackServiceMediaPlayer.INVALID_TIME;
        }
        return (int) duration;
    }

    @Override
    public boolean isPlaying() {
        return runOnExoPlayerThread(() -> mExoPlayer.getPlayWhenReady(), false);
    }

    @Override
    public void pause() {
        runOnExoPlayerThread(() -> mExoPlayer.setPlayWhenReady(false));
    }

    @Override
    public void prepare() throws IllegalStateException {
        runOnExoPlayerThread(() -> mExoPlayer.prepare(mediaSource));
    }

    @Override
    public void release() {
        if (mExoPlayer != null) {
            runOnExoPlayerThread(() -> mExoPlayer.release());
        }
        audioSeekCompleteListener = null;
        audioCompletionListener = null;
        audioErrorListener = null;
    }

    @Override
    public void reset() {
        runOnExoPlayerThread(() -> {
            mExoPlayer.release();
            mExoPlayer = createPlayer();
        });
    }

    @Override
    public void seekTo(int i) throws IllegalStateException {
        runOnExoPlayerThread(() -> mExoPlayer.seekTo(i));
    }

    @Override
    public void setAudioStreamType(int i) {
        runOnExoPlayerThread(() -> {
            AudioAttributes a = mExoPlayer.getAudioAttributes();
            AudioAttributes.Builder b = new AudioAttributes.Builder();
            b.setContentType(i);
            b.setFlags(a.flags);
            b.setUsage(a.usage);
            mExoPlayer.setAudioAttributes(b.build());
        });
    }

    @Override
    public void setDataSource(String s) throws IllegalArgumentException, IllegalStateException {
        runOnExoPlayerThread(() -> {
            DataSource.Factory dataSourceFactory =
                    new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, mContext.getPackageName()), null);
            ExtractorMediaSource.Factory f = new ExtractorMediaSource.Factory(dataSourceFactory);
            mediaSource = f.createMediaSource(Uri.parse(s));
        });
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        runOnExoPlayerThread(() -> mExoPlayer.setVideoSurfaceHolder(sh));
    }

    @Override
    public void setPlaybackParams(float speed, boolean skipSilence) {
        runOnExoPlayerThread(() -> {
            PlaybackParameters params = mExoPlayer.getPlaybackParameters();
            mExoPlayer.setPlaybackParameters(new PlaybackParameters(speed, params.pitch, skipSilence));
        });
    }

    @Override
    public void setDownmix(boolean b) {

    }

    @Override
    public void setVolume(float v, float v1) {
        runOnExoPlayerThread(() -> mExoPlayer.setVolume(v));
    }

    @Override
    public void setWakeMode(Context context, int i) {

    }

    @Override
    public void start() {
        runOnExoPlayerThread(() -> mExoPlayer.setPlayWhenReady(true));
    }

    @Override
    public void stop() {
        runOnExoPlayerThread(() -> mExoPlayer.stop());
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
        Format format = runOnExoPlayerThread(() -> mExoPlayer.getVideoFormat(), null);
        if (format == null) {
            return 0;
        }
        return format.width;
    }

    int getVideoHeight() {
        Format format = runOnExoPlayerThread(() -> mExoPlayer.getVideoFormat(), null);
        if (format == null) {
            return 0;
        }
        return format.height;
    }
}
