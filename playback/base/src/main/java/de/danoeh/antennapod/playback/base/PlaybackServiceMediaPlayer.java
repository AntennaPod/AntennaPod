package de.danoeh.antennapod.playback.base;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import java.util.List;

import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.feed.FeedPreferences;


/*
 * An inconvenience of an implementation like this is that some members and methods that once were
 * private are now protected, allowing for access from classes of the same package, namely
 * PlaybackService. A workaround would be to move this to a dedicated package.
 */
/**
 * Abstract class that allows for different implementations of the PlaybackServiceMediaPlayer for local
 * and remote (cast devices) playback.
 */
public abstract class PlaybackServiceMediaPlayer {
    private static final String TAG = "PlaybackSvcMediaPlayer";

    private volatile PlayerStatus oldPlayerStatus;
    protected volatile PlayerStatus playerStatus;

    /**
     * A wifi-lock that is acquired if the media file is being streamed.
     */
    private WifiManager.WifiLock wifiLock;

    protected final PSMPCallback callback;
    protected final Context context;

    protected PlaybackServiceMediaPlayer(@NonNull Context context,
                               @NonNull PSMPCallback callback){
        this.context = context;
        this.callback = callback;

        playerStatus = PlayerStatus.STOPPED;
    }

    /**
     * Starts or prepares playback of the specified Playable object. If another Playable object is already being played, the currently playing
     * episode will be stopped and replaced with the new Playable object. If the Playable object is already being played, the method will
     * not do anything.
     * Whether playback starts immediately depends on the given parameters. See below for more details.
     * <p/>
     * States:
     * During execution of the method, the object will be in the INITIALIZING state. The end state depends on the given parameters.
     * <p/>
     * If 'prepareImmediately' is set to true, the method will go into PREPARING state and after that into PREPARED state. If
     * 'startWhenPrepared' is set to true, the method will additionally go into PLAYING state.
     * <p/>
     * If an unexpected error occurs while loading the Playable's metadata or while setting the MediaPlayers data source, the object
     * will enter the ERROR state.
     * <p/>
     * This method is executed on an internal executor service.
     *
     * @param playable           The Playable object that is supposed to be played. This parameter must not be null.
     * @param stream             The type of playback. If false, the Playable object MUST provide access to a locally available file via
     *                           getLocalMediaUrl. If true, the Playable object MUST provide access to a resource that can be streamed by
     *                           the Android MediaPlayer via getStreamUrl.
     * @param startWhenPrepared  Sets the 'startWhenPrepared' flag. This flag determines whether playback will start immediately after the
     *                           episode has been prepared for playback. Setting this flag to true does NOT mean that the episode will be prepared
     *                           for playback immediately (see 'prepareImmediately' parameter for more details)
     * @param prepareImmediately Set to true if the method should also prepare the episode for playback.
     */
    public abstract void playMediaObject(@NonNull Playable playable, boolean stream, boolean startWhenPrepared, boolean prepareImmediately);

    /**
     * Resumes playback if the PSMP object is in PREPARED or PAUSED state. If the PSMP object is in an invalid state.
     * nothing will happen.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void resume();

    /**
     * Saves the current position and pauses playback. Note that, if audiofocus
     * is abandoned, the lockscreen controls will also disapear.
     * <p/>
     * This method is executed on an internal executor service.
     *
     * @param abandonFocus is true if the service should release audio focus
     * @param reinit       is true if service should reinit after pausing if the media
     *                     file is being streamed
     */
    public abstract void pause(boolean abandonFocus, boolean reinit);

    /**
     * Prepared media player for playback if the service is in the INITALIZED
     * state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void prepare();

    /**
     * Resets the media player and moves it into INITIALIZED state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void reinit();

    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     * Invalid time values (< 0) will be ignored.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public abstract void seekTo(int t);

    /**
     * Seek a specific position from the current position
     *
     * @param d offset from current position (positive or negative)
     */
    public abstract void seekDelta(int d);

    /**
     * Returns the duration of the current media object or INVALID_TIME if the duration could not be retrieved.
     */
    public abstract int getDuration();

    /**
     * Returns the position of the current media object or INVALID_TIME if the position could not be retrieved.
     */
    public abstract int getPosition();

    public abstract boolean isStartWhenPrepared();

    public abstract void setStartWhenPrepared(boolean startWhenPrepared);

    /**
     * Sets the playback parameters.
     * - Speed
     * - SkipSilence (ExoPlayer only)
     * This method is executed on an internal executor service.
     */
    public abstract void  setPlaybackParams(final float speed, final FeedPreferences.SkipSilence skipSilence);

    /**
     * Returns the current playback speed. If the playback speed could not be retrieved, 1 is returned.
     */
    public abstract float getPlaybackSpeed();

    public abstract FeedPreferences.SkipSilence getSkipSilence();

    /**
     * Sets the playback volume.
     * This method is executed on an internal executor service.
     */
    public abstract void setVolume(float volumeLeft, float volumeRight);

    public abstract MediaType getCurrentMediaType();

    public abstract boolean isStreaming();

    /**
     * Releases internally used resources. This method should only be called when the object is not used anymore.
     */
    public abstract void shutdown();

    public abstract void setVideoSurface(SurfaceHolder surface);

    public abstract void resetVideoSurface();

    /**
     * Return width and height of the currently playing video as a pair.
     *
     * @return Width and height as a Pair or null if the video size could not be determined. The method might still
     * return an invalid non-null value if the getVideoWidth() and getVideoHeight() methods of the media player return
     * invalid values.
     */
    public abstract Pair<Integer, Integer> getVideoSize();

    /**
     * Returns a PSMInfo object that contains information about the current state of the PSMP object.
     *
     * @return The PSMPInfo object.
     */
    public final synchronized PSMPInfo getPSMPInfo() {
        return new PSMPInfo(oldPlayerStatus, playerStatus, getPlayable());
    }

    /**
     * Returns the current status, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current player status
     */
    public synchronized PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    /**
     * Returns the current media, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current media. May be null
     */
    public abstract Playable getPlayable();

    protected abstract void setPlayable(Playable playable);

    public abstract List<String> getAudioTracks();

    public abstract void setAudioTrack(int track);

    public abstract int getSelectedAudioTrack();

    public void skip() {
        if (getPosition() < 1000) {
            Log.d(TAG, "Ignoring skip, is in first second of playback");
            return;
        }
        endPlayback(false, true, true, true);
    }

    /**
     * Ends playback of current media (if any) and moves into INDETERMINATE state, unless
     * {@param toStoppedState} is set to true, in which case it moves into STOPPED state.
     *
     * @see #endPlayback(boolean, boolean, boolean, boolean)
     */
    public void stopPlayback(boolean toStoppedState) {
        endPlayback(false, false, false, toStoppedState);
    }

    /**
     * Internal method that handles end of playback.
     *
     * Currently, it has 5 use cases:
     * <ul>
     * <li>Media playback has completed: call with (true, false, true, true)</li>
     * <li>User asks to skip to next episode: call with (false, true, true, true)</li>
     * <li>Skipping to next episode due to playback error: call with (false, false, true, true)</li>
     * <li>Stopping the media player: call with (false, false, false, true)</li>
     * <li>We want to change the media player implementation: call with (false, false, false, false)</li>
     * </ul>
     *
     * @param hasEnded         If true, we assume the current media's playback has ended, for
     *                         purposes of post playback processing.
     * @param wasSkipped       Whether the user chose to skip the episode (by pressing the skip
     *                         button).
     * @param shouldContinue   If true, the media player should try to load, and possibly play,
     *                         the next item, based on the user preferences and whether such item
     *                         exists.
     * @param toStoppedState   If true, the playback state gets set to STOPPED if the media player
     *                         is not loading/playing after this call, and the UI will reflect that.
     *                         Only relevant if {@param shouldContinue} is set to false, otherwise
     *                         this method's behavior defaults as if this parameter was true.
     *
     * @return a Future, just for the purpose of tracking its execution.
     */
    protected abstract void endPlayback(boolean hasEnded, boolean wasSkipped,
                                             boolean shouldContinue, boolean toStoppedState);

    /**
     * @return {@code true} if the WifiLock feature should be used, {@code false} otherwise.
     */
    protected abstract boolean shouldLockWifi();

    public abstract boolean isCasting();

    protected final synchronized void acquireWifiLockIfNecessary() {
        if (shouldLockWifi()) {
            if (wifiLock == null) {
                wifiLock = ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
                wifiLock.setReferenceCounted(false);
            }
            wifiLock.acquire();
        }
    }

    protected final synchronized void releaseWifiLockIfNecessary() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Sets the player status of the PSMP object. PlayerStatus and media attributes have to be set at the same time
     * so that getPSMPInfo can't return an invalid state (e.g. status is PLAYING, but media is null).
     * <p/>
     * This method will notify the callback about the change of the player status (even if the new status is the same
     * as the old one).
     * <p/>
     * It will also call {@link PSMPCallback#onPlaybackPause(Playable, int)} or {@link PSMPCallback#onPlaybackStart(Playable, int)}
     * depending on the status change.
     *
     * @param newStatus The new PlayerStatus. This must not be null.
     * @param newMedia  The new playable object of the PSMP object. This can be null.
     * @param position  The position to be set to the current Playable object in case playback started or paused.
     *                  Will be ignored if given the value of {@link Playable#INVALID_TIME}.
     */
    protected final synchronized void setPlayerStatus(@NonNull PlayerStatus newStatus,
                                                      Playable newMedia, int position) {
        Log.d(TAG, this.getClass().getSimpleName() + ": Setting player status to " + newStatus);

        this.oldPlayerStatus = playerStatus;
        this.playerStatus = newStatus;
        setPlayable(newMedia);

        if (newMedia != null && newStatus != PlayerStatus.INDETERMINATE) {
            if (oldPlayerStatus == PlayerStatus.PLAYING && newStatus != PlayerStatus.PLAYING) {
                callback.onPlaybackPause(newMedia, position);
            } else if (oldPlayerStatus != PlayerStatus.PLAYING && newStatus == PlayerStatus.PLAYING) {
                callback.onPlaybackStart(newMedia, position);
            }
        }

        callback.statusChanged(new PSMPInfo(oldPlayerStatus, playerStatus, getPlayable()));
    }

    public boolean isAudioChannelInUse() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return (audioManager.getMode() != AudioManager.MODE_NORMAL || audioManager.isMusicActive());
    }

    /**
     * @see #setPlayerStatus(PlayerStatus, Playable, int)
     */
    protected final void setPlayerStatus(@NonNull PlayerStatus newStatus, Playable newMedia) {
        setPlayerStatus(newStatus, newMedia, Playable.INVALID_TIME);
    }

    public interface PSMPCallback {
        void statusChanged(PSMPInfo newInfo);

        void shouldStop();

        void onMediaChanged(boolean reloadUI);

        void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped, boolean playingNext);

        void onPlaybackStart(@NonNull Playable playable, int position);

        void onPlaybackPause(Playable playable, int position);

        Playable getNextInQueue(Playable currentMedia);

        @Nullable
        Playable findMedia(@NonNull String url);

        void onPlaybackEnded(MediaType mediaType, boolean stopPlaying);

        void ensureMediaInfoLoaded(@NonNull Playable media);
    }

    /**
     * Holds information about a PSMP object.
     */
    public static class PSMPInfo {
        private final PlayerStatus oldPlayerStatus;
        private final PlayerStatus playerStatus;
        private Playable playable;

        public PSMPInfo(PlayerStatus oldPlayerStatus, PlayerStatus playerStatus, Playable playable) {
            this.oldPlayerStatus = oldPlayerStatus;
            this.playerStatus = playerStatus;
            this.playable = playable;
        }

        public PlayerStatus getOldPlayerStatus() {
            return oldPlayerStatus;
        }

        public PlayerStatus getPlayerStatus() {
            return playerStatus;
        }

        public Playable getPlayable() {
            return playable;
        }

        public void setPlayable(final Playable newPlayable) {
            playable = newPlayable;
        }
    }
}
