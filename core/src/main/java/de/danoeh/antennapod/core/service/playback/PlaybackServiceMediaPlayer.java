package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

import org.antennapod.audio.MediaPlayer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.RewindAfterPauseUtils;
import de.danoeh.antennapod.core.util.playback.AudioPlayer;
import de.danoeh.antennapod.core.util.playback.IPlayer;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.VideoPlayer;

/**
 * Manages the MediaPlayer object of the PlaybackService.
 */
public class PlaybackServiceMediaPlayer {
    public static final String TAG = "PlaybackSvcMediaPlayer";

    /**
     * Return value of some PSMP methods if the method call failed.
     */
    public static final int INVALID_TIME = -1;

    private final AudioManager audioManager;

    private volatile PlayerStatus playerStatus;
    private volatile PlayerStatus statusBeforeSeeking;
    private volatile IPlayer mediaPlayer;
    private volatile Playable media;

    private volatile boolean stream;
    private volatile MediaType mediaType;
    private volatile AtomicBoolean startWhenPrepared;
    private volatile boolean pausedBecauseOfTransientAudiofocusLoss;
    private volatile Pair<Integer, Integer> videoSize;

    /**
     * Some asynchronous calls might change the state of the MediaPlayer object. Therefore calls in other threads
     * have to wait until these operations have finished.
     */
    private final ReentrantLock playerLock;
    private CountDownLatch seekLatch;

    private final PSMPCallback callback;
    private final Context context;

    private final ThreadPoolExecutor executor;

    /**
     * A wifi-lock that is acquired if the media file is being streamed.
     */
    private WifiManager.WifiLock wifiLock;

    public PlaybackServiceMediaPlayer(@NonNull Context context,
                                      @NonNull PSMPCallback callback) {
        this.context = context;
        this.callback = callback;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.playerLock = new ReentrantLock();
        this.startWhenPrepared = new AtomicBoolean(false);
        executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        Log.d(TAG, "Rejected execution of runnable");
                    }
                }
        );

        mediaPlayer = null;
        statusBeforeSeeking = null;
        pausedBecauseOfTransientAudiofocusLoss = false;
        mediaType = MediaType.UNKNOWN;
        playerStatus = PlayerStatus.STOPPED;
        videoSize = null;
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
    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject(...)");
        executor.submit(() -> {
            playerLock.lock();
            try {
                playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } finally {
                playerLock.unlock();
            }
        });
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     * <p/>
     * This method requires the playerLock and is executed on the caller's thread.
     *
     * @see #playMediaObject(de.danoeh.antennapod.core.util.playback.Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (!playerLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("method requires playerLock");
        }


        if (media != null) {
            if (!forceReset && media.getIdentifier().equals(playable.getIdentifier())
                    && playerStatus == PlayerStatus.PLAYING) {
                // episode is already playing -> ignore method call
                Log.d(TAG, "Method call to playMediaObject was ignored: media file already playing.");
                return;
            } else {
                // stop playback of this episode
                if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.PREPARED) {
                    mediaPlayer.stop();
                }
                // set temporarily to pause in order to update list with current position
                if (playerStatus == PlayerStatus.PLAYING) {
                    setPlayerStatus(PlayerStatus.PAUSED, media);
                }

                // smart mark as played
                if(media != null && media instanceof FeedMedia) {
                    FeedMedia oldMedia = (FeedMedia) media;
                    if(oldMedia.hasAlmostEnded()) {
                        Log.d(TAG, "smart mark as read");
                        FeedItem item = oldMedia.getItem();
                        DBWriter.markItemPlayed(item, FeedItem.PLAYED, false);
                        DBWriter.removeQueueItem(context, item, false);
                        DBWriter.addItemToPlaybackHistory(oldMedia);
                        if (item.getFeed().getPreferences().getCurrentAutoDelete()) {
                            Log.d(TAG, "Delete " + oldMedia.toString());
                            DBWriter.deleteFeedMediaOfItem(context, oldMedia.getId());
                        }
                    }
                }

                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        this.media = playable;
        this.stream = stream;
        this.mediaType = media.getMediaType();
        this.videoSize = null;
        createMediaPlayer();
        PlaybackServiceMediaPlayer.this.startWhenPrepared.set(startWhenPrepared);
        setPlayerStatus(PlayerStatus.INITIALIZING, media);
        try {
            media.loadMetadata();
            executor.submit(() -> callback.updateMediaSessionMetadata(media));
            if (stream) {
                mediaPlayer.setDataSource(media.getStreamUrl());
            } else {
                mediaPlayer.setDataSource(media.getLocalMediaUrl());
            }
            setPlayerStatus(PlayerStatus.INITIALIZED, media);

            if (prepareImmediately) {
                setPlayerStatus(PlayerStatus.PREPARING, media);
                mediaPlayer.prepare();
                onPrepared(startWhenPrepared);
            }

        } catch (Playable.PlayableException | IOException | IllegalStateException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        }
    }

    /**
     * Resumes playback if the PSMP object is in PREPARED or PAUSED state. If the PSMP object is in an invalid state.
     * nothing will happen.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public void resume() {
        executor.submit(() -> {
            playerLock.lock();
            resumeSync();
            playerLock.unlock();
        });
    }

    private void resumeSync() {
        if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PREPARED) {
            int focusGained = audioManager.requestAudioFocus(
                    audioFocusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (focusGained == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                acquireWifiLockIfNecessary();
                float speed = 1.0f;
                try {
                    speed = Float.parseFloat(UserPreferences.getPlaybackSpeed());
                } catch(NumberFormatException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    UserPreferences.setPlaybackSpeed(String.valueOf(speed));
                }
                setSpeed(speed);
                setVolume(UserPreferences.getLeftVolume(), UserPreferences.getRightVolume());

                if (playerStatus == PlayerStatus.PREPARED && media.getPosition() > 0) {
                    int newPosition = RewindAfterPauseUtils.calculatePositionWithRewind(
                        media.getPosition(),
                        media.getLastPlayedTime());
                    seekToSync(newPosition);
                }
                mediaPlayer.start();

                setPlayerStatus(PlayerStatus.PLAYING, media);
                pausedBecauseOfTransientAudiofocusLoss = false;
                media.onPlaybackStart();

            } else {
                Log.e(TAG, "Failed to request audio focus");
            }
        } else {
            Log.d(TAG, "Call to resume() was ignored because current state of PSMP object is " + playerStatus);
        }
    }


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
    public void pause(final boolean abandonFocus, final boolean reinit) {
        executor.submit(() -> {
            playerLock.lock();
            releaseWifiLockIfNecessary();
            if (playerStatus == PlayerStatus.PLAYING) {
                Log.d(TAG, "Pausing playback.");
                mediaPlayer.pause();
                setPlayerStatus(PlayerStatus.PAUSED, media);

                if (abandonFocus) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    pausedBecauseOfTransientAudiofocusLoss = false;
                }
                if (stream && reinit) {
                    reinit();
                }
            } else {
                Log.d(TAG, "Ignoring call to pause: Player is in " + playerStatus + " state");
            }

            playerLock.unlock();
        });
    }

    /**
     * Prepares media player for playback if the service is in the INITALIZED
     * state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public void prepare() {
        executor.submit(() -> {
            playerLock.lock();

            if (playerStatus == PlayerStatus.INITIALIZED) {
                Log.d(TAG, "Preparing media player");
                setPlayerStatus(PlayerStatus.PREPARING, media);
                try {
                    mediaPlayer.prepare();
                    onPrepared(startWhenPrepared.get());
                } catch (IOException e) {
                    e.printStackTrace();
                    setPlayerStatus(PlayerStatus.ERROR, null);
                }
            }
            playerLock.unlock();

        });
    }

    /**
     * Called after media player has been prepared. This method is executed on the caller's thread.
     */
    void onPrepared(final boolean startWhenPrepared) {
        playerLock.lock();

        if (playerStatus != PlayerStatus.PREPARING) {
            playerLock.unlock();
            throw new IllegalStateException("Player is not in PREPARING state");
        }

        Log.d(TAG, "Resource prepared");

        if (mediaType == MediaType.VIDEO) {
            VideoPlayer vp = (VideoPlayer) mediaPlayer;
            videoSize = new Pair<>(vp.getVideoWidth(), vp.getVideoHeight());
        }

        if (media.getPosition() > 0) {
            seekToSync(media.getPosition());
        }

        if (media.getDuration() == 0) {
            Log.d(TAG, "Setting duration of media");
            media.setDuration(mediaPlayer.getDuration());
        }
        setPlayerStatus(PlayerStatus.PREPARED, media);

        if (startWhenPrepared) {
            resumeSync();
        }

        playerLock.unlock();
    }

    /**
     * Resets the media player and moves it into INITIALIZED state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public void reinit() {
        executor.submit(() -> {
            playerLock.lock();
            releaseWifiLockIfNecessary();
            if (media != null) {
                playMediaObject(media, true, stream, startWhenPrepared.get(), false);
            } else if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                Log.d(TAG, "Call to reinit was ignored: media and mediaPlayer were null");
            }
            playerLock.unlock();
        });
    }


    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     *
     * @param t The position to seek to in milliseconds. t < 0 will be interpreted as t = 0
     *          <p/>
     *          This method is executed on the caller's thread.
     */
    private void seekToSync(int t) {
        if (t < 0) {
            t = 0;
        }
        playerLock.lock();

        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            if (!stream) {
                statusBeforeSeeking = playerStatus;
                setPlayerStatus(PlayerStatus.SEEKING, media);
            }
            if(seekLatch != null && seekLatch.getCount() > 0) {
                try {
                    seekLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            seekLatch = new CountDownLatch(1);
            mediaPlayer.seekTo(t);
            try {
                seekLatch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        } else if (playerStatus == PlayerStatus.INITIALIZED) {
            media.setPosition(t);
            startWhenPrepared.set(false);
            prepare();
        }
        playerLock.unlock();
    }

    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     * Invalid time values (< 0) will be ignored.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public void seekTo(final int t) {
        executor.submit(() -> seekToSync(t));
    }

    /**
     * Seek a specific position from the current position
     *
     * @param d offset from current position (positive or negative)
     */
    public void seekDelta(final int d) {
        executor.submit(() -> {
            playerLock.lock();
            int currentPosition = getPosition();
            if (currentPosition != INVALID_TIME) {
                seekToSync(currentPosition + d);
            } else {
                Log.e(TAG, "getPosition() returned INVALID_TIME in seekDelta");
            }

            playerLock.unlock();
        });
    }

    /**
     * Seek to the start of the specified chapter.
     */
    public void seekToChapter(@NonNull Chapter c) {
        seekTo((int) c.getStart());
    }

    /**
     * Returns the duration of the current media object or INVALID_TIME if the duration could not be retrieved.
     */
    public int getDuration() {
        if (!playerLock.tryLock()) {
            return INVALID_TIME;
        }

        int retVal = INVALID_TIME;
        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            retVal = mediaPlayer.getDuration();
        } else if (media != null && media.getDuration() > 0) {
            retVal = media.getDuration();
        }

        playerLock.unlock();
        return retVal;
    }

    /**
     * Returns the position of the current media object or INVALID_TIME if the position could not be retrieved.
     */
    public int getPosition() {
        try {
            if (!playerLock.tryLock(50, TimeUnit.MILLISECONDS)) {
                return INVALID_TIME;
            }
        } catch (InterruptedException e) {
            return INVALID_TIME;
        }

        int retVal = INVALID_TIME;
        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED
                || playerStatus == PlayerStatus.SEEKING) {
            retVal = mediaPlayer.getCurrentPosition();
        }
        if (retVal <= 0 && media != null && media.getPosition() >= 0) {
            retVal = media.getPosition();
        }

        playerLock.unlock();
        Log.d(TAG, "getPosition() -> " + retVal);
        return retVal;
    }

    public boolean isStartWhenPrepared() {
        return startWhenPrepared.get();
    }

    public void setStartWhenPrepared(boolean startWhenPrepared) {
        this.startWhenPrepared.set(startWhenPrepared);
    }

    /**
     * Returns true if the playback speed can be adjusted.
     */
    public boolean canSetSpeed() {
        boolean retVal = false;
        if (mediaPlayer != null && media != null && media.getMediaType() == MediaType.AUDIO) {
            retVal = (mediaPlayer).canSetSpeed();
        }
        return retVal;
    }

    /**
     * Sets the playback speed.
     * This method is executed on the caller's thread.
     */
    private void setSpeedSync(float speed) {
        playerLock.lock();
        if (media != null && media.getMediaType() == MediaType.AUDIO) {
            if (mediaPlayer.canSetSpeed()) {
                mediaPlayer.setPlaybackSpeed(speed);
                Log.d(TAG, "Playback speed was set to " + speed);
                callback.playbackSpeedChanged(speed);
            }
        }
        playerLock.unlock();
    }

    /**
     * Sets the playback speed.
     * This method is executed on an internal executor service.
     */
    public void setSpeed(final float speed) {
        executor.submit(() -> setSpeedSync(speed));
    }

    /**
     * Returns the current playback speed. If the playback speed could not be retrieved, 1 is returned.
     */
    public float getPlaybackSpeed() {
        if (!playerLock.tryLock()) {
            return 1;
        }

        float retVal = 1;
        if ((playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) && mediaPlayer.canSetSpeed()) {
            retVal = mediaPlayer.getCurrentSpeedMultiplier();
        }
        playerLock.unlock();
        return retVal;
    }

    /**
     * Sets the playback volume.
     * This method is executed on an internal executor service.
     */
    public void setVolume(final float volumeLeft, float volumeRight) {
        executor.submit(() -> setVolumeSync(volumeLeft, volumeRight));
    }

    /**
     * Sets the playback volume.
     * This method is executed on the caller's thread.
     */
    private void setVolumeSync(float volumeLeft, float volumeRight) {
        playerLock.lock();
        if (media != null && media.getMediaType() == MediaType.AUDIO) {
            mediaPlayer.setVolume(volumeLeft, volumeRight);
            Log.d(TAG, "Media player volume was set to " + volumeLeft + " " + volumeRight);
        }
        playerLock.unlock();
    }

    /**
     * Returns true if the mediaplayer can mix stereo down to mono
     */
    public boolean canDownmix() {
        boolean retVal = false;
        if (mediaPlayer != null && media != null && media.getMediaType() == MediaType.AUDIO) {
            retVal = mediaPlayer.canDownmix();
        }
        return retVal;
    }

    public void setDownmix(boolean enable) {
        playerLock.lock();
        if (media != null && media.getMediaType() == MediaType.AUDIO) {
            mediaPlayer.setDownmix(enable);
            Log.d(TAG, "Media player downmix was set to " + enable);
        }
        playerLock.unlock();
    }

    public MediaType getCurrentMediaType() {
        return mediaType;
    }

    public boolean isStreaming() {
        return stream;
    }


    /**
     * Releases internally used resources. This method should only be called when the object is not used anymore.
     */
    public void shutdown() {
        executor.shutdown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        releaseWifiLockIfNecessary();
    }

    public void setVideoSurface(final SurfaceHolder surface) {
        executor.submit(() -> {
            playerLock.lock();
            if (mediaPlayer != null) {
                mediaPlayer.setDisplay(surface);
            }
            playerLock.unlock();
        });
    }

    public void resetVideoSurface() {
        executor.submit(() -> {
            playerLock.lock();
            Log.d(TAG, "Resetting video surface");
            mediaPlayer.setDisplay(null);
            reinit();
            playerLock.unlock();
        });
    }

    /**
     * Return width and height of the currently playing video as a pair.
     *
     * @return Width and height as a Pair or null if the video size could not be determined. The method might still
     * return an invalid non-null value if the getVideoWidth() and getVideoHeight() methods of the media player return
     * invalid values.
     */
    public Pair<Integer, Integer> getVideoSize() {
        if (!playerLock.tryLock()) {
            // use cached value if lock can't be aquired
            return videoSize;
        }
        Pair<Integer, Integer> res;
        if (mediaPlayer == null || playerStatus == PlayerStatus.ERROR || mediaType != MediaType.VIDEO) {
            res = null;
        } else {
            VideoPlayer vp = (VideoPlayer) mediaPlayer;
            videoSize = new Pair<>(vp.getVideoWidth(), vp.getVideoHeight());
            res = videoSize;
        }
        playerLock.unlock();
        return res;
    }

    /**
     * Returns a PSMInfo object that contains information about the current state of the PSMP object.
     *
     * @return The PSMPInfo object.
     */
    public synchronized PSMPInfo getPSMPInfo() {
        return new PSMPInfo(playerStatus, media);
    }

    /**
     * Returns the current status, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current player status
     */
    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }

    /**
     * Returns the current media, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current media. May be null
     */
    public Playable getPlayable() {
        return media;
    }

    /**
     * Sets the player status of the PSMP object. PlayerStatus and media attributes have to be set at the same time
     * so that getPSMPInfo can't return an invalid state (e.g. status is PLAYING, but media is null).
     * <p/>
     * This method will notify the callback about the change of the player status (even if the new status is the same
     * as the old one).
     *
     * @param newStatus The new PlayerStatus. This must not be null.
     * @param newMedia  The new playable object of the PSMP object. This can be null.
     */
    private synchronized void setPlayerStatus(@NonNull PlayerStatus newStatus, Playable newMedia) {
        Log.d(TAG, "Setting player status to " + newStatus);

        this.playerStatus = newStatus;
        this.media = newMedia;
        if (playerStatus != null) {
            Log.d(TAG, "playerStatus: " + playerStatus.toString());
        }

        callback.statusChanged(new PSMPInfo(playerStatus, media));
    }

    private IPlayer createMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (media == null || media.getMediaType() == MediaType.VIDEO) {
            mediaPlayer = new VideoPlayer();
        } else {
            mediaPlayer = new AudioPlayer(context);
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        return setMediaPlayerListeners(mediaPlayer);
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(final int focusChange) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    playerLock.lock();

                    // If there is an incoming call, playback should be paused permanently
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    final int callState = (tm != null) ? tm.getCallState() : 0;
                    Log.i(TAG, "Call state:" + callState);

                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                            (!UserPreferences.shouldResumeAfterCall() && callState != TelephonyManager.CALL_STATE_IDLE)) {
                        Log.d(TAG, "Lost audio focus");
                        pause(true, false);
                        callback.shouldStop();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        Log.d(TAG, "Gained audio focus");
                        if (pausedBecauseOfTransientAudiofocusLoss) { // we paused => play now
                            resume();
                        } else { // we ducked => raise audio level back
                            setVolumeSync(UserPreferences.getLeftVolume(),
                                    UserPreferences.getRightVolume());
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        if (playerStatus == PlayerStatus.PLAYING) {
                            if (!UserPreferences.shouldPauseForFocusLoss()) {
                                Log.d(TAG, "Lost audio focus temporarily. Ducking...");
                                final float DUCK_FACTOR = 0.25f;
                                setVolumeSync(DUCK_FACTOR * UserPreferences.getLeftVolume(),
                                        DUCK_FACTOR * UserPreferences.getRightVolume());
                                pausedBecauseOfTransientAudiofocusLoss = false;
                            } else {
                                Log.d(TAG, "Lost audio focus temporarily. Could duck, but won't, pausing...");
                                pause(false, false);
                                pausedBecauseOfTransientAudiofocusLoss = true;
                            }
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if (playerStatus == PlayerStatus.PLAYING) {
                            Log.d(TAG, "Lost audio focus temporarily. Pausing...");
                            pause(false, false);
                            pausedBecauseOfTransientAudiofocusLoss = true;
                        }
                    }
                    playerLock.unlock();
                }
            });
        }
    };


    public void endPlayback(final boolean wasSkipped) {
        executor.submit(() -> {
            playerLock.lock();
            releaseWifiLockIfNecessary();

            boolean isPlaying = playerStatus == PlayerStatus.PLAYING;

            if (playerStatus != PlayerStatus.INDETERMINATE) {
                setPlayerStatus(PlayerStatus.INDETERMINATE, media);
            }
            if (mediaPlayer != null) {
                mediaPlayer.reset();

            }
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            callback.endPlayback(isPlaying, wasSkipped);

            playerLock.unlock();
        });
    }

    /**
     * Moves the PlaybackServiceMediaPlayer into STOPPED state. This call is only valid if the player is currently in
     * INDETERMINATE state, for example after a call to endPlayback.
     * This method will only take care of changing the PlayerStatus of this object! Other tasks like
     * abandoning audio focus have to be done with other methods.
     */
    public void stop() {
        executor.submit(() -> {
            playerLock.lock();
            releaseWifiLockIfNecessary();

            if (playerStatus == PlayerStatus.INDETERMINATE) {
                setPlayerStatus(PlayerStatus.STOPPED, null);
            } else {
                Log.d(TAG, "Ignored call to stop: Current player state is: " + playerStatus);
            }
            playerLock.unlock();

        });
    }

    private synchronized void acquireWifiLockIfNecessary() {
        if (stream) {
            if (wifiLock == null) {
                wifiLock = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
                wifiLock.setReferenceCounted(false);
            }
            wifiLock.acquire();
        }
    }

    private synchronized void releaseWifiLockIfNecessary() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Holds information about a PSMP object.
     */
    public class PSMPInfo {
        public PlayerStatus playerStatus;
        public Playable playable;

        public PSMPInfo(PlayerStatus playerStatus, Playable playable) {
            this.playerStatus = playerStatus;
            this.playable = playable;
        }
    }

    public interface PSMPCallback {
        void statusChanged(PSMPInfo newInfo);

        void shouldStop();

        void playbackSpeedChanged(float s);

        void setSpeedAbilityChanged();

        void onBufferingUpdate(int percent);

        void updateMediaSessionMetadata(Playable p);

        boolean onMediaPlayerInfo(int code);

        boolean onMediaPlayerError(Object inObj, int what, int extra);

        boolean endPlayback(boolean playNextEpisode, boolean wasSkipped);
    }

    private IPlayer setMediaPlayerListeners(IPlayer mp) {
        if (mp != null && media != null) {
            if (media.getMediaType() == MediaType.AUDIO) {
                ((AudioPlayer) mp)
                        .setOnCompletionListener(audioCompletionListener);
                ((AudioPlayer) mp)
                        .setOnSeekCompleteListener(audioSeekCompleteListener);
                ((AudioPlayer) mp).setOnErrorListener(audioErrorListener);
                ((AudioPlayer) mp)
                        .setOnBufferingUpdateListener(audioBufferingUpdateListener);
                ((AudioPlayer) mp).setOnInfoListener(audioInfoListener);
                ((AudioPlayer) mp).setOnSpeedAdjustmentAvailableChangedListener(audioSetSpeedAbilityListener);
            } else {
                ((VideoPlayer) mp)
                        .setOnCompletionListener(videoCompletionListener);
                ((VideoPlayer) mp)
                        .setOnSeekCompleteListener(videoSeekCompleteListener);
                ((VideoPlayer) mp).setOnErrorListener(videoErrorListener);
                ((VideoPlayer) mp)
                        .setOnBufferingUpdateListener(videoBufferingUpdateListener);
                ((VideoPlayer) mp).setOnInfoListener(videoInfoListener);
            }
        }
        return mp;
    }

    private final MediaPlayer.OnCompletionListener audioCompletionListener =
            mp -> genericOnCompletion();

    private final android.media.MediaPlayer.OnCompletionListener videoCompletionListener =
            mp -> genericOnCompletion();

    private void genericOnCompletion() {
        endPlayback(false);
    }

    private final MediaPlayer.OnBufferingUpdateListener audioBufferingUpdateListener =
            (mp, percent) -> genericOnBufferingUpdate(percent);

    private final android.media.MediaPlayer.OnBufferingUpdateListener videoBufferingUpdateListener =
            (mp, percent) -> genericOnBufferingUpdate(percent);

    private void genericOnBufferingUpdate(int percent) {
        callback.onBufferingUpdate(percent);
    }

    private final MediaPlayer.OnInfoListener audioInfoListener =
            (mp, what, extra) -> genericInfoListener(what);

    private final android.media.MediaPlayer.OnInfoListener videoInfoListener =
            (mp, what, extra) -> genericInfoListener(what);

    private boolean genericInfoListener(int what) {
        return callback.onMediaPlayerInfo(what);
    }

    private final MediaPlayer.OnSpeedAdjustmentAvailableChangedListener
            audioSetSpeedAbilityListener = new MediaPlayer.OnSpeedAdjustmentAvailableChangedListener() {
        @Override
        public void onSpeedAdjustmentAvailableChanged(MediaPlayer arg0, boolean speedAdjustmentAvailable) {
            callback.setSpeedAbilityChanged();
        }
    };


    private final MediaPlayer.OnErrorListener audioErrorListener =
            (mp, what, extra) -> {
                if(mp.canFallback()) {
                    mp.fallback();
                    return true;
                } else {
                    return genericOnError(mp, what, extra);
                }
            };

    private final android.media.MediaPlayer.OnErrorListener videoErrorListener = this::genericOnError;

    private boolean genericOnError(Object inObj, int what, int extra) {
        return callback.onMediaPlayerError(inObj, what, extra);
    }

    private final MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener =
            mp -> genericSeekCompleteListener();

    private final android.media.MediaPlayer.OnSeekCompleteListener videoSeekCompleteListener =
            mp -> genericSeekCompleteListener();

    private void genericSeekCompleteListener() {
        Thread t = new Thread(() -> {
            Log.d(TAG, "genericSeekCompleteListener");
            if(seekLatch != null) {
                seekLatch.countDown();
            }
            playerLock.lock();
            if (playerStatus == PlayerStatus.SEEKING) {
                setPlayerStatus(statusBeforeSeeking, media);
            }
            playerLock.unlock();
        });
        t.start();
    }
}
