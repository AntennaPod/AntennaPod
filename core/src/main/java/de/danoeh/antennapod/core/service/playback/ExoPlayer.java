package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import de.danoeh.antennapod.core.util.playback.IPlayer;
import org.antennapod.audio.MediaPlayer;

import java.io.File;
import java.io.IOException;

/**
 * @author Hans-Peter Lehmann
 * @version 1.0
 */
public class ExoPlayer implements IPlayer {
    private final Context mContext;
    private SimpleExoPlayer mExoPlayer;
    private MediaSource mediaSource;
    private MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener;

    public ExoPlayer(Context context) {
        mContext = context;
        mExoPlayer = createPlayer();
    }

    private SimpleExoPlayer createPlayer() {
        SimpleExoPlayer p = ExoPlayerFactory.newSimpleInstance(
                mContext, new DefaultTrackSelector(), new DefaultLoadControl());
        p.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
        return p;
    }

    @Override
    public boolean canSetPitch() {
        return true;
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
    public float getCurrentPitchStepsAdjustment() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return (int) mExoPlayer.getCurrentPosition();
    }

    @Override
    public float getCurrentSpeedMultiplier() {
        return 0;
    }

    @Override
    public int getDuration() {
        return (int) mExoPlayer.getDuration();
    }

    @Override
    public float getMaxSpeedMultiplier() {
        return 0;
    }

    @Override
    public float getMinSpeedMultiplier() {
        return 0;
    }

    @Override
    public boolean isLooping() {
        return mExoPlayer.getRepeatMode() == Player.REPEAT_MODE_ONE;
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
    public void prepareAsync() {
        mExoPlayer.prepare(mediaSource);
    }

    @Override
    public void release() {
        if (mExoPlayer != null) {
            mExoPlayer.release();
        }
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
        mExoPlayer.setAudioStreamType(i);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {

    }

    @Override
    public void setDataSource(String s) throws IllegalArgumentException, IllegalStateException, IOException {
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(
                        mContext, Util.getUserAgent(mContext, "uamp"), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        mediaSource = new ExtractorMediaSource(
                Uri.parse(s), dataSourceFactory, extractorsFactory, null, null);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        mExoPlayer.setVideoSurfaceHolder(sh);
    }

    @Override
    public void setEnableSpeedAdjustment(boolean b) {

    }

    @Override
    public void setLooping(boolean b) {
        mExoPlayer.setRepeatMode(b ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    @Override
    public void setPitchStepsAdjustment(float v) {

    }

    @Override
    public void setPlaybackPitch(float v) {
        PlaybackParameters params = mExoPlayer.getPlaybackParameters();
        mExoPlayer.setPlaybackParameters(new PlaybackParameters(params.speed, v));
    }

    @Override
    public void setPlaybackSpeed(float v) {
        PlaybackParameters params = mExoPlayer.getPlaybackParameters();
        mExoPlayer.setPlaybackParameters(new PlaybackParameters(v, params.pitch));
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

    @Override
    public void setVideoScalingMode(int mode) {
        mExoPlayer.setVideoScalingMode(mode);
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener audioCompletionListener) {
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener) {
        this.audioSeekCompleteListener = audioSeekCompleteListener;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener audioErrorListener) {
    }

    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener audioBufferingUpdateListener) {
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener audioInfoListener) {
    }

    public void setOnSpeedAdjustmentAvailableChangedListener(MediaPlayer.OnSpeedAdjustmentAvailableChangedListener audioSetSpeedAbilityListener) {
    }

    public int getVideoWidth() {
        if (mExoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return mExoPlayer.getVideoFormat().width;
    }

    public int getVideoHeight() {
        if (mExoPlayer.getVideoFormat() == null) {
            return 0;
        }
        return mExoPlayer.getVideoFormat().height;
    }
}
