package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.SurfaceHolder;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Implementation of PlaybackServiceMediaPlayer suitable for remote playback on Cast Devices.
 */
public class RemotePSMP extends PlaybackServiceMediaPlayer {


    public RemotePSMP(@NonNull Context context, @NonNull PSMPCallback callback) {
        super(context, callback);
        //TODO
    }

    @Override
    public void playMediaObject(@NonNull Playable playable, boolean stream, boolean startWhenPrepared, boolean prepareImmediately) {
        //TODO
    }

    @Override
    public void resume() {
        //TODO
    }

    @Override
    public void pause(boolean abandonFocus, boolean reinit) {
        //TODO
    }

    @Override
    public void prepare() {
        //TODO
    }

    @Override
    public void reinit() {
        //TODO
    }

    @Override
    public void seekTo(int t) {
        //TODO
    }

    @Override
    public void seekDelta(int d) {
        //TODO
    }

    @Override
    public void seekToChapter(@NonNull Chapter c) {
        //TODO
    }

    @Override
    public int getDuration() {
        //TODO
        return 0;
    }

    @Override
    public int getPosition() {
        //TODO
        return 0;
    }

    @Override
    public boolean isStartWhenPrepared() {
        //TODO
        return false;
    }

    @Override
    public void setStartWhenPrepared(boolean startWhenPrepared) {
        //TODO
    }

    @Override
    public boolean canSetSpeed() {
        //TODO
        return false;
    }

    @Override
    public void setSpeed(float speed) {
        //TODO
    }

    @Override
    public float getPlaybackSpeed() {
        //TODO
        return 0;
    }

    @Override
    public void setVolume(float volumeLeft, float volumeRight) {
        //TODO
    }

    @Override
    public boolean canDownmix() {
        //TODO
        return false;
    }

    @Override
    public void setDownmix(boolean enable) {
        //TODO
    }

    @Override
    public MediaType getCurrentMediaType() {
        //TODO
        return null;
    }

    @Override
    public boolean isStreaming() {
        return true;
    }

    @Override
    public void shutdown() {
        //TODO
    }

    @Override
    public void shutdownAsync() {
        //TODO
        this.shutdown();
    }

    @Override
    public void setVideoSurface(SurfaceHolder surface) {
        //TODO
    }

    @Override
    public void resetVideoSurface() {
        //TODO
    }

    @Override
    public Pair<Integer, Integer> getVideoSize() {
        //TODO
        return null;
    }

    @Override
    public Playable getPlayable() {
        //TODO
        return null;
    }

    @Override
    protected void setPlayable(Playable playable) {
        //TODO
    }

    @Override
    public void endPlayback(boolean wasSkipped, boolean switchingPlayers) {
        //TODO
    }

    @Override
    public void stop() {
        //TODO
    }
}
