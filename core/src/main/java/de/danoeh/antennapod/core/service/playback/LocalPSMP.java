package de.danoeh.antennapod.core.service.playback;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.base.RewindAfterPauseUtils;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the MediaPlayer object of the PlaybackService.
 */
public class LocalPSMP extends PlaybackServiceMediaPlayer {
    private static final String TAG = "LclPlaybackSvcMPlayer";

    private final AudioManager audioManager;

    private volatile PlayerStatus statusBeforeSeeking;
    private volatile ExoPlayerWrapper mediaPlayer;
    private volatile Playable media;

    private volatile boolean stream;
    private volatile MediaType mediaType;
    private final AtomicBoolean startWhenPrepared;
    private volatile boolean pausedBecauseOfTransientAudiofocusLoss;
    private volatile Pair<Integer, Integer> videoSize;
    private final AudioFocusRequestCompat audioFocusRequest;
    private final Handler audioFocusCanceller;
    private boolean isShutDown = false;
    private CountDownLatch seekLatch;

    public LocalPSMP(@NonNull Context context,
                     @NonNull PlaybackServiceMediaPlayer.PSMPCallback callback) {
        super(context, callback);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.startWhenPrepared = new AtomicBoolean(false);
        audioFocusCanceller = new Handler(Looper.getMainLooper());
        mediaPlayer = null;
        statusBeforeSeeking = null;
        pausedBecauseOfTransientAudiofocusLoss = false;
        mediaType = MediaType.UNKNOWN;
        videoSize = null;

        AudioAttributesCompat audioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
                .build();
        audioFocusRequest = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(true)
                .build();
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
    @Override
    public void playMediaObject(@NonNull final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        Log.d(TAG, "playMediaObject(...)");
        try {
            playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     * <p/>
     * This method requires the playerLock and is executed on the caller's thread.
     *
     * @see #playMediaObject(Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(@NonNull final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
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
                    callback.onPlaybackPause(media, getPosition());
                }

                if (!media.getIdentifier().equals(playable.getIdentifier())) {
                    final Playable oldMedia = media;
                    callback.onPostPlayback(oldMedia, false, false, true);
                }

                setPlayerStatus(PlayerStatus.INDETERMINATE, null);
            }
        }

        this.media = playable;
        this.stream = stream;
        this.mediaType = media.getMediaType();
        this.videoSize = null;
        createMediaPlayer();
        LocalPSMP.this.startWhenPrepared.set(startWhenPrepared);
        setPlayerStatus(PlayerStatus.INITIALIZING, media);
        try {
            callback.ensureMediaInfoLoaded(media);
            callback.onMediaChanged(false);
            setPlaybackParams(PlaybackSpeedUtils.getCurrentPlaybackSpeed(media), UserPreferences.isSkipSilence());
            if (stream) {
                if (playable instanceof FeedMedia) {
                    FeedMedia feedMedia = (FeedMedia) playable;
                    FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
                    mediaPlayer.setDataSource(
                            media.getStreamUrl(),
                            preferences.getUsername(),
                            preferences.getPassword());
                } else {
                    mediaPlayer.setDataSource(media.getStreamUrl());
                }
            } else if (media.getLocalMediaUrl() != null && new File(media.getLocalMediaUrl()).canRead()) {
                mediaPlayer.setDataSource(media.getLocalMediaUrl());
            } else {
                throw new IOException("Unable to read local file " + media.getLocalMediaUrl());
            }
            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            if (uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR) {
                setPlayerStatus(PlayerStatus.INITIALIZED, media);
            }

            if (prepareImmediately) {
                setPlayerStatus(PlayerStatus.PREPARING, media);
                mediaPlayer.prepare();
                onPrepared(startWhenPrepared);
            }

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
            EventBus.getDefault().postSticky(new PlayerErrorEvent(e.getLocalizedMessage()));
        }
    }

    /**
     * Resumes playback if the PSMP object is in PREPARED or PAUSED state. If the PSMP object is in an invalid state.
     * nothing will happen.
     * <p/>
     * This method is executed on an internal executor service.
     */
    @Override
    public void resume() {
        if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PREPARED) {
            int focusGained = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest);

            if (focusGained == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audiofocus successfully requested");
                Log.d(TAG, "Resuming/Starting playback");
                acquireWifiLockIfNecessary();

                setPlaybackParams(PlaybackSpeedUtils.getCurrentPlaybackSpeed(media), UserPreferences.isSkipSilence());
                setVolume(1.0f, 1.0f);

                if (playerStatus == PlayerStatus.PREPARED && media.getPosition() > 0) {
                    int newPosition = RewindAfterPauseUtils.calculatePositionWithRewind(
                        media.getPosition(),
                        media.getLastPlayedTime());
                    seekTo(newPosition);
                }
                mediaPlayer.start();

                setPlayerStatus(PlayerStatus.PLAYING, media);
                pausedBecauseOfTransientAudiofocusLoss = false;
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
    @Override
    public void pause(final boolean abandonFocus, final boolean reinit) {
        releaseWifiLockIfNecessary();
        if (playerStatus == PlayerStatus.PLAYING) {
            Log.d(TAG, "Pausing playback.");
            mediaPlayer.pause();
            setPlayerStatus(PlayerStatus.PAUSED, media, getPosition());

            if (abandonFocus) {
                abandonAudioFocus();
                pausedBecauseOfTransientAudiofocusLoss = false;
            }
            if (stream && reinit) {
                reinit();
            }
        } else {
            Log.d(TAG, "Ignoring call to pause: Player is in " + playerStatus + " state");
        }
    }

    private void abandonAudioFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest);
    }

    /**
     * Prepares media player for playback if the service is in the INITALIZED
     * state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    @Override
    public void prepare() {
        if (playerStatus == PlayerStatus.INITIALIZED) {
            Log.d(TAG, "Preparing media player");
            setPlayerStatus(PlayerStatus.PREPARING, media);
            mediaPlayer.prepare();
            onPrepared(startWhenPrepared.get());
        }
    }

    /**
     * Called after media player has been prepared. This method is executed on the caller's thread.
     */
    private void onPrepared(final boolean startWhenPrepared) {
        if (playerStatus != PlayerStatus.PREPARING) {
            throw new IllegalStateException("Player is not in PREPARING state");
        }
        Log.d(TAG, "Resource prepared");

        if (mediaType == MediaType.VIDEO) {
            videoSize = new Pair<>(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
        }

        // TODO this call has no effect!
        if (media.getPosition() > 0) {
            seekTo(media.getPosition());
        }

        if (media.getDuration() <= 0) {
            Log.d(TAG, "Setting duration of media");
            media.setDuration(mediaPlayer.getDuration());
        }
        setPlayerStatus(PlayerStatus.PREPARED, media);

        if (startWhenPrepared) {
            resume();
        }
    }

    /**
     * Resets the media player and moves it into INITIALIZED state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    @Override
    public void reinit() {
        Log.d(TAG, "reinit()");
        releaseWifiLockIfNecessary();
        if (media != null) {
            playMediaObject(media, true, stream, startWhenPrepared.get(), false);
        } else if (mediaPlayer != null) {
            mediaPlayer.reset();
        } else {
            Log.d(TAG, "Call to reinit was ignored: media and mediaPlayer were null");
        }
    }

    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     * Invalid time values (< 0) will be ignored.
     * <p/>
     * This method is executed on an internal executor service.
     */
    @Override
    public void seekTo(int t) {
        if (t < 0) {
            t = 0;
        }

        if (t >= getDuration()) {
            Log.d(TAG, "Seek reached end of file, skipping to next episode");
            skip();
            return;
        }

        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            if(seekLatch != null && seekLatch.getCount() > 0) {
                try {
                    seekLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            seekLatch = new CountDownLatch(1);
            statusBeforeSeeking = playerStatus;
            setPlayerStatus(PlayerStatus.SEEKING, media, getPosition());
            mediaPlayer.seekTo(t);
            if (statusBeforeSeeking == PlayerStatus.PREPARED) {
                media.setPosition(t);
            }
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
    }

    /**
     * Seek a specific position from the current position
     *
     * @param d offset from current position (positive or negative)
     */
    @Override
    public void seekDelta(final int d) {
        int currentPosition = getPosition();
        if (currentPosition != Playable.INVALID_TIME) {
            seekTo(currentPosition + d);
        } else {
            Log.e(TAG, "getPosition() returned INVALID_TIME in seekDelta");
        }
    }

    /**
     * Returns the duration of the current media object or INVALID_TIME if the duration could not be retrieved.
     */
    @Override
    public int getDuration() {
        int retVal = Playable.INVALID_TIME;
        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            retVal = mediaPlayer.getDuration();
        }
        if (retVal <= 0 && media != null && media.getDuration() > 0) {
            retVal = media.getDuration();
        }
        return retVal;
    }

    /**
     * Returns the position of the current media object or INVALID_TIME if the position could not be retrieved.
     */
    @Override
    public int getPosition() {
        int retVal = Playable.INVALID_TIME;
        if (playerStatus.isAtLeast(PlayerStatus.PREPARED)) {
            retVal = mediaPlayer.getCurrentPosition();
        }
        if (retVal <= 0 && media != null && media.getPosition() >= 0) {
            retVal = media.getPosition();
        }
        return retVal;
    }

    @Override
    public boolean isStartWhenPrepared() {
        return startWhenPrepared.get();
    }

    @Override
    public void setStartWhenPrepared(boolean startWhenPrepared) {
        this.startWhenPrepared.set(startWhenPrepared);
    }

    /**
     * Sets the playback speed.
     * This method is executed on an internal executor service.
     */
    @Override
    public void setPlaybackParams(final float speed, final boolean skipSilence) {
        Log.d(TAG, "Playback speed was set to " + speed);
        EventBus.getDefault().post(new SpeedChangedEvent(speed));
        mediaPlayer.setPlaybackParams(speed, skipSilence);
    }

    /**
     * Returns the current playback speed. If the playback speed could not be retrieved, 1 is returned.
     */
    @Override
    public float getPlaybackSpeed() {
        float retVal = 1;
        if ((playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.INITIALIZED
                || playerStatus == PlayerStatus.PREPARED)) {
            retVal = mediaPlayer.getCurrentSpeedMultiplier();
        }
        return retVal;
    }

    /**
     * Sets the playback volume.
     * This method is executed on an internal executor service.
     */
    @Override
    public void setVolume(float volumeLeft, float volumeRight) {
        Playable playable = getPlayable();
        if (playable instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) playable;
            FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
            VolumeAdaptionSetting volumeAdaptionSetting = preferences.getVolumeAdaptionSetting();
            float adaptionFactor = volumeAdaptionSetting.getAdaptionFactor();
            volumeLeft *= adaptionFactor;
            volumeRight *= adaptionFactor;
        }
        mediaPlayer.setVolume(volumeLeft, volumeRight);
        Log.d(TAG, "Media player volume was set to " + volumeLeft + " " + volumeRight);
    }

    @Override
    public MediaType getCurrentMediaType() {
        return mediaType;
    }

    @Override
    public boolean isStreaming() {
        return stream;
    }

    /**
     * Releases internally used resources. This method should only be called when the object is not used anymore.
     */
    @Override
    public void shutdown() {
        if (mediaPlayer != null) {
            try {
                clearMediaPlayerListeners();
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            playerStatus = PlayerStatus.STOPPED;
        }
        isShutDown = true;
        abandonAudioFocus();
        releaseWifiLockIfNecessary();
    }

    @Override
    public void setVideoSurface(final SurfaceHolder surface) {
        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(surface);
        }
    }

    @Override
    public void resetVideoSurface() {
        if (mediaType == MediaType.VIDEO) {
            Log.d(TAG, "Resetting video surface");
            mediaPlayer.setDisplay(null);
            reinit();
        } else {
            Log.e(TAG, "Resetting video surface for media of Audio type");
        }
    }

    /**
     * Return width and height of the currently playing video as a pair.
     *
     * @return Width and height as a Pair or null if the video size could not be determined. The method might still
     * return an invalid non-null value if the getVideoWidth() and getVideoHeight() methods of the media player return
     * invalid values.
     */
    @Override
    public Pair<Integer, Integer> getVideoSize() {
        if (mediaPlayer != null && playerStatus != PlayerStatus.ERROR && mediaType == MediaType.VIDEO) {
            videoSize = new Pair<>(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
        }
        return videoSize;
    }

    /**
     * Returns the current media, if you need the media and the player status together, you should
     * use getPSMPInfo() to make sure they're properly synchronized. Otherwise a race condition
     * could result in nonsensical results (like a status of PLAYING, but a null playable)
     * @return the current media. May be null
     */
    @Override
    public Playable getPlayable() {
        return media;
    }

    @Override
    protected void setPlayable(Playable playable) {
        media = playable;
    }

    public List<String> getAudioTracks() {
        return mediaPlayer.getAudioTracks();
    }

    public void setAudioTrack(int track) {
        mediaPlayer.setAudioTrack(track);
    }

    public int getSelectedAudioTrack() {
        return mediaPlayer.getSelectedAudioTrack();
    }

    private void createMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (media == null) {
            mediaPlayer = null;
            playerStatus = PlayerStatus.STOPPED;
            return;
        }

        mediaPlayer = new ExoPlayerWrapper(context);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        setMediaPlayerListeners(mediaPlayer);
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(final int focusChange) {
            if (isShutDown) {
                return;
            }
            if (!PlaybackService.isRunning) {
                abandonAudioFocus();
                Log.d(TAG, "onAudioFocusChange: PlaybackService is no longer running");
                return;
            }

            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.d(TAG, "Lost audio focus");
                pause(true, false);
                callback.shouldStop();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                    && !UserPreferences.shouldPauseForFocusLoss()) {
                if (playerStatus == PlayerStatus.PLAYING) {
                    Log.d(TAG, "Lost audio focus temporarily. Ducking...");
                    setVolume(0.25f, 0.25f);
                    pausedBecauseOfTransientAudiofocusLoss = false;
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (playerStatus == PlayerStatus.PLAYING) {
                    Log.d(TAG, "Lost audio focus temporarily. Pausing...");
                    mediaPlayer.pause(); // Pause without telling the PlaybackService
                    pausedBecauseOfTransientAudiofocusLoss = true;

                    audioFocusCanceller.removeCallbacksAndMessages(null);
                    audioFocusCanceller.postDelayed(() -> {
                        if (pausedBecauseOfTransientAudiofocusLoss) {
                            // Still did not get back the audio focus. Now actually pause.
                            pause(true, false);
                        }
                    }, 30000);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.d(TAG, "Gained audio focus");
                audioFocusCanceller.removeCallbacksAndMessages(null);
                if (pausedBecauseOfTransientAudiofocusLoss) { // we paused => play now
                    mediaPlayer.start();
                } else { // we ducked => raise audio level back
                    setVolume(1.0f, 1.0f);
                }
                pausedBecauseOfTransientAudiofocusLoss = false;
            }
        }
    };


    @Override
    protected void endPlayback(final boolean hasEnded, final boolean wasSkipped,
                                    final boolean shouldContinue, final boolean toStoppedState) {
        releaseWifiLockIfNecessary();

        boolean isPlaying = playerStatus == PlayerStatus.PLAYING;

        // we're relying on the position stored in the Playable object for post-playback processing
        if (media != null) {
            int position = getPosition();
            if (position >= 0) {
                media.setPosition(position);
            }
        }

        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }

        abandonAudioFocus();

        final Playable currentMedia = media;
        Playable nextMedia = null;

        if (shouldContinue) {
            // Load next episode if previous episode was in the queue and if there
            // is an episode in the queue left.
            // Start playback immediately if continuous playback is enabled
            nextMedia = callback.getNextInQueue(currentMedia);
            if (nextMedia != null) {
                callback.onPlaybackEnded(nextMedia.getMediaType(), false);
                // setting media to null signals to playMediaObject() that
                // we're taking care of post-playback processing
                media = null;
                playMediaObject(nextMedia, false, !nextMedia.localFileAvailable(), isPlaying, isPlaying);
            }
        }
        if (shouldContinue || toStoppedState) {
            if (nextMedia == null) {
                callback.onPlaybackEnded(null, true);
                stop();
            }
            final boolean hasNext = nextMedia != null;

            callback.onPostPlayback(currentMedia, hasEnded, wasSkipped, hasNext);
        } else if (isPlaying) {
            callback.onPlaybackPause(currentMedia, currentMedia.getPosition());
        }
    }

    /**
     * Moves the LocalPSMP into STOPPED state. This call is only valid if the player is currently in
     * INDETERMINATE state, for example after a call to endPlayback.
     * This method will only take care of changing the PlayerStatus of this object! Other tasks like
     * abandoning audio focus have to be done with other methods.
     */
    private void stop() {
        releaseWifiLockIfNecessary();

        if (playerStatus == PlayerStatus.INDETERMINATE) {
            setPlayerStatus(PlayerStatus.STOPPED, null);
        } else {
            Log.d(TAG, "Ignored call to stop: Current player state is: " + playerStatus);
        }
    }

    @Override
    protected boolean shouldLockWifi() {
        return stream;
    }

    private void setMediaPlayerListeners(ExoPlayerWrapper mp) {
        if (mp == null || media == null) {
            return;
        }
        mp.setOnCompletionListener(() -> endPlayback(true, false, true, true));
        mp.setOnSeekCompleteListener(this::genericSeekCompleteListener);
        mp.setOnBufferingUpdateListener(percent -> {
            if (percent == ExoPlayerWrapper.BUFFERING_STARTED) {
                EventBus.getDefault().post(BufferUpdateEvent.started());
            } else if (percent == ExoPlayerWrapper.BUFFERING_ENDED) {
                EventBus.getDefault().post(BufferUpdateEvent.ended());
            } else {
                EventBus.getDefault().post(BufferUpdateEvent.progressUpdate(0.01f * percent));
            }
        });
        mp.setOnErrorListener(message -> EventBus.getDefault().postSticky(new PlayerErrorEvent(message)));
    }

    private void clearMediaPlayerListeners() {
        mediaPlayer.setOnCompletionListener(() -> { });
        mediaPlayer.setOnSeekCompleteListener(() -> { });
        mediaPlayer.setOnBufferingUpdateListener(percent -> { });
        mediaPlayer.setOnErrorListener(x -> { });
    }

    private void genericSeekCompleteListener() {
        Log.d(TAG, "genericSeekCompleteListener");
        if (seekLatch != null) {
            seekLatch.countDown();
        }
        if (playerStatus == PlayerStatus.PLAYING) {
            callback.onPlaybackStart(media, getPosition());
        }
        if (playerStatus == PlayerStatus.SEEKING) {
            setPlayerStatus(statusBeforeSeeking, media, getPosition());
        }
    }

    @Override
    public boolean isCasting() {
        return false;
    }
}
