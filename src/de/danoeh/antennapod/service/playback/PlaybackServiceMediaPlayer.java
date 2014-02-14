package de.danoeh.antennapod.service.playback;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.util.playback.AudioPlayer;
import de.danoeh.antennapod.util.playback.IPlayer;
import de.danoeh.antennapod.util.playback.Playable;
import de.danoeh.antennapod.util.playback.VideoPlayer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the MediaPlayer object of the PlaybackService.
 */
public class PlaybackServiceMediaPlayer {
    public static final String TAG = "PlaybackServiceMediaPlayer";

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

    private final PSMPCallback callback;
    private final Context context;

    private final ExecutorService executor;

    public PlaybackServiceMediaPlayer(Context context, PSMPCallback callback) {
        if (context == null)
            throw new IllegalArgumentException("context = null");
        if (callback == null)
            throw new IllegalArgumentException("callback = null");

        this.context = context;
        this.callback = callback;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.playerLock = new ReentrantLock();
        this.startWhenPrepared = new AtomicBoolean(false);
        executor = Executors.newSingleThreadExecutor();
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
    public void playMediaObject(final Playable playable, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (playable == null)
            throw new IllegalArgumentException("playable = null");
        if (AppConfig.DEBUG) Log.d(TAG, "Play media object.");
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    playMediaObject(playable, false, stream, startWhenPrepared, prepareImmediately);
                } catch (RuntimeException e) {
                    throw e;
                } finally {
                    playerLock.unlock();
                }
            }
        });
    }

    /**
     * Internal implementation of playMediaObject. This method has an additional parameter that allows the caller to force a media player reset even if
     * the given playable parameter is the same object as the currently playing media.
     * <p/>
     * This method requires the playerLock and is executed on the caller's thread.
     *
     * @see #playMediaObject(de.danoeh.antennapod.util.playback.Playable, boolean, boolean, boolean)
     */
    private void playMediaObject(final Playable playable, final boolean forceReset, final boolean stream, final boolean startWhenPrepared, final boolean prepareImmediately) {
        if (playable == null)
            throw new IllegalArgumentException("playable = null");
        if (!playerLock.isHeldByCurrentThread())
            throw new IllegalStateException("method requires playerLock");


        if (media != null) {
            if (!forceReset && media.getIdentifier().equals(playable.getIdentifier())) {
                // episode is already playing -> ignore method call
                return;
            } else {
                // stop playback of this episode
                if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PLAYING || playerStatus == PlayerStatus.PREPARED) {
                    mediaPlayer.stop();
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
            if (stream) {
                mediaPlayer.setDataSource(media.getStreamUrl());
            } else {
                mediaPlayer.setDataSource(media.getLocalMediaUrl());
            }
            setPlayerStatus(PlayerStatus.INITIALIZED, media);

            if (mediaType == MediaType.VIDEO) {
                VideoPlayer vp = (VideoPlayer) mediaPlayer;
                //  vp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }

            if (prepareImmediately) {
                setPlayerStatus(PlayerStatus.PREPARING, media);
                mediaPlayer.prepare();
                onPrepared(startWhenPrepared);
            }

        } catch (Playable.PlayableException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        } catch (IOException e) {
            e.printStackTrace();
            setPlayerStatus(PlayerStatus.ERROR, null);
        } catch (IllegalStateException e) {
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
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                resumeSync();
                playerLock.unlock();
            }
        });
    }

    private void resumeSync() {
        if (playerStatus == PlayerStatus.PAUSED || playerStatus == PlayerStatus.PREPARED) {
            int focusGained = audioManager.requestAudioFocus(
                    audioFocusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (focusGained == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                setSpeed(Float.parseFloat(UserPreferences.getPlaybackSpeed()));
                mediaPlayer.start();
                if (playerStatus == PlayerStatus.PREPARED && media.getPosition() > 0) {
                    mediaPlayer.seekTo(media.getPosition());
                }

                setPlayerStatus(PlayerStatus.PLAYING, media);
                pausedBecauseOfTransientAudiofocusLoss = false;
                if (android.os.Build.VERSION.SDK_INT >= 14) {
                    RemoteControlClient remoteControlClient = callback.getRemoteControlClient();
                    if (remoteControlClient != null) {
                        audioManager
                                .registerRemoteControlClient(remoteControlClient);
                    }
                }
                audioManager
                        .registerMediaButtonEventReceiver(new ComponentName(context.getPackageName(),
                                MediaButtonReceiver.class.getName()));
                media.onPlaybackStart();

            } else {
                if (AppConfig.DEBUG) Log.e(TAG, "Failed to request audio focus");
            }
        } else {
            if (AppConfig.DEBUG)
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
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();

                if (playerStatus == PlayerStatus.PLAYING) {
                    if (AppConfig.DEBUG)
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
                    if (AppConfig.DEBUG) Log.d(TAG, "Ignoring call to pause: Player is in " + playerStatus + " state");
                }

                playerLock.unlock();
            }
        });
    }

    /**
     * Prepared media player for playback if the service is in the INITALIZED
     * state.
     * <p/>
     * This method is executed on an internal executor service.
     */
    public void prepare() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();

                if (playerStatus == PlayerStatus.INITIALIZED) {
                    if (AppConfig.DEBUG)
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

            }
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

        if (AppConfig.DEBUG)
            Log.d(TAG, "Resource prepared");

        if (mediaType == MediaType.VIDEO) {
            VideoPlayer vp = (VideoPlayer) mediaPlayer;
            videoSize = new Pair<Integer, Integer>(vp.getVideoWidth(), vp.getVideoHeight());
        }

        if (media.getPosition() > 0) {
            mediaPlayer.seekTo(media.getPosition());
        }

        if (media.getDuration() == 0) {
            if (AppConfig.DEBUG)
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
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();

                if (media != null) {
                    playMediaObject(media, true, stream, startWhenPrepared.get(), false);
                } else if (mediaPlayer != null) {
                    mediaPlayer.reset();
                } else {
                    if (AppConfig.DEBUG) Log.d(TAG, "Call to reinit was ignored: media and mediaPlayer were null");
                }
                playerLock.unlock();
            }
        });
    }


    /**
     * Seeks to the specified position. If the PSMP object is in an invalid state, this method will do nothing.
     * Invalid time values (< 0) will be ignored.
     * <p/>
     * This method is executed on the caller's thread.
     */
    private void seekToSync(int t) {
        if (t < 0) {
            if (AppConfig.DEBUG) Log.d(TAG, "Received invalid value for t");
            return;
        }
        playerLock.lock();

        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            if (stream) {
                //    statusBeforeSeeking = playerStatus;
                //    setPlayerStatus(PlayerStatus.SEEKING, media);
            }
            mediaPlayer.seekTo(t);

        } else if (playerStatus == PlayerStatus.INITIALIZED) {
            media.setPosition(t);
            startWhenPrepared.set(true);
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
        executor.submit(new Runnable() {
            @Override
            public void run() {
                seekToSync(t);
            }
        });
    }

    /**
     * Seek a specific position from the current position
     *
     * @param d offset from current position (positive or negative)
     */
    public void seekDelta(final int d) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                int currentPosition = getPosition();
                if (currentPosition != INVALID_TIME) {
                    seekToSync(currentPosition + d);
                } else {
                    Log.e(TAG, "getPosition() returned INVALID_TIME in seekDelta");
                }

                playerLock.unlock();
            }
        });
    }

    /**
     * Seek to the start of the specified chapter.
     */
    public void seekToChapter(Chapter c) {
        if (c == null)
            throw new IllegalArgumentException("c = null");
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
        if (!playerLock.tryLock()) {
            return INVALID_TIME;
        }

        int retVal = INVALID_TIME;
        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            retVal = mediaPlayer.getCurrentPosition();
        } else if (media != null && media.getPosition() > 0) {
            retVal = media.getPosition();
        }

        playerLock.unlock();
        return retVal;
    }

    public boolean isStartWhenPrepared() {
        return startWhenPrepared.get();
    }

    public void setStartWhenPrepared(boolean startWhenPrepared) {
        this.startWhenPrepared.set(startWhenPrepared);
    }

    /**
     * Returns true if the playback speed can be adjusted. This method can also return false if the PSMP object's
     * internal MediaPlayer cannot be accessed at the moment.
     */
    public boolean canSetSpeed() {
        if (!playerLock.tryLock()) {
            return false;
        }
        boolean retVal = false;
        if (mediaPlayer != null && media != null && media.getMediaType() == MediaType.AUDIO) {
            retVal = (mediaPlayer).canSetSpeed();
        }

        playerLock.unlock();
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
                mediaPlayer.setPlaybackSpeed((float) speed);
                if (AppConfig.DEBUG)
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
        executor.submit(new Runnable() {
            @Override
            public void run() {
                setSpeedSync(speed);
            }
        });
    }

    /**
     * Returns the current playback speed. If the playback speed could not be retrieved, 1 is returned.
     */
    public float getPlaybackSpeed() {
        if (!playerLock.tryLock()) {
            return 1;
        }

        int retVal = 1;
        if (playerStatus == PlayerStatus.PLAYING
                || playerStatus == PlayerStatus.PAUSED
                || playerStatus == PlayerStatus.PREPARED) {
            retVal = mediaPlayer.getCurrentPosition();
        } else if (media != null && media.getPosition() > 0) {
            retVal = media.getPosition();
        }

        playerLock.unlock();
        return retVal;
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
    }

    public void setVideoSurface(final SurfaceHolder surface) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                if (mediaPlayer != null) {
                    mediaPlayer.setDisplay(surface);
                }
                playerLock.unlock();
            }
        });
    }

    public void resetVideoSurface() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Resetting video surface");
                mediaPlayer.setDisplay(null);
                reinit();
                playerLock.unlock();
            }
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
            videoSize = new Pair<Integer, Integer>(vp.getVideoWidth(), vp.getVideoHeight());
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
     * Sets the player status of the PSMP object. PlayerStatus and media attributes have to be set at the same time
     * so that getPSMPInfo can't return an invalid state (e.g. status is PLAYING, but media is null).
     * <p/>
     * This method will notify the callback about the change of the player status (even if the new status is the same
     * as the old one).
     *
     * @param newStatus The new PlayerStatus. This must not be null.
     * @param newMedia  The new playable object of the PSMP object. This can be null.
     */
    private synchronized void setPlayerStatus(PlayerStatus newStatus, Playable newMedia) {
        if (newStatus == null)
            throw new IllegalArgumentException("newStatus = null");
        if (AppConfig.DEBUG) Log.d(TAG, "Setting player status to " + newStatus);

        this.playerStatus = newStatus;
        this.media = newMedia;
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
        return setMediaPlayerListeners(mediaPlayer);
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(final int focusChange) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    playerLock.lock();

                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Lost audio focus");
                            pause(true, false);
                            callback.shouldStop();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Gained audio focus");
                            if (pausedBecauseOfTransientAudiofocusLoss) // we paused => play now
                                resume();
                            else // we ducked => raise audio level back
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                        AudioManager.ADJUST_RAISE, 0);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (playerStatus == PlayerStatus.PLAYING) {
                                if (!UserPreferences.shouldPauseForFocusLoss()) {
                                    if (AppConfig.DEBUG)
                                        Log.d(TAG, "Lost audio focus temporarily. Ducking...");
                                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                            AudioManager.ADJUST_LOWER, 0);
                                    pausedBecauseOfTransientAudiofocusLoss = false;
                                } else {
                                    if (AppConfig.DEBUG)
                                        Log.d(TAG, "Lost audio focus temporarily. Could duck, but won't, pausing...");
                                    pause(false, false);
                                    pausedBecauseOfTransientAudiofocusLoss = true;
                                }
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (playerStatus == PlayerStatus.PLAYING) {
                                if (AppConfig.DEBUG)
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

    public void endPlayback() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();

                if (playerStatus != PlayerStatus.INDETERMINATE) {
                    setPlayerStatus(PlayerStatus.INDETERMINATE, media);
                }
                if (mediaPlayer != null) {
                    mediaPlayer.reset();

                }
                callback.endPlayback(true);

                playerLock.unlock();
            }
        });
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

    public static interface PSMPCallback {
        public void statusChanged(PSMPInfo newInfo);

        public void shouldStop();

        public void playbackSpeedChanged(float s);

        public void onBufferingUpdate(int percent);

        public boolean onMediaPlayerInfo(int code);

        public boolean onMediaPlayerError(Object inObj, int what, int extra);

        public boolean endPlayback(boolean playNextEpisode);

        public RemoteControlClient getRemoteControlClient();
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

    private final com.aocate.media.MediaPlayer.OnCompletionListener audioCompletionListener = new com.aocate.media.MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(com.aocate.media.MediaPlayer mp) {
            genericOnCompletion();
        }
    };

    private final android.media.MediaPlayer.OnCompletionListener videoCompletionListener = new android.media.MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(android.media.MediaPlayer mp) {
            genericOnCompletion();
        }
    };

    private void genericOnCompletion() {
        endPlayback();
    }

    private final com.aocate.media.MediaPlayer.OnBufferingUpdateListener audioBufferingUpdateListener = new com.aocate.media.MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(com.aocate.media.MediaPlayer mp,
                                      int percent) {
            genericOnBufferingUpdate(percent);
        }
    };

    private final android.media.MediaPlayer.OnBufferingUpdateListener videoBufferingUpdateListener = new android.media.MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
            genericOnBufferingUpdate(percent);
        }
    };

    private void genericOnBufferingUpdate(int percent) {
        callback.onBufferingUpdate(percent);
    }

    private final com.aocate.media.MediaPlayer.OnInfoListener audioInfoListener = new com.aocate.media.MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(com.aocate.media.MediaPlayer mp, int what,
                              int extra) {
            return genericInfoListener(what);
        }
    };

    private final android.media.MediaPlayer.OnInfoListener videoInfoListener = new android.media.MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(android.media.MediaPlayer mp, int what, int extra) {
            return genericInfoListener(what);
        }
    };

    private boolean genericInfoListener(int what) {
        return callback.onMediaPlayerInfo(what);
    }

    private final com.aocate.media.MediaPlayer.OnErrorListener audioErrorListener = new com.aocate.media.MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(com.aocate.media.MediaPlayer mp, int what,
                               int extra) {
            return genericOnError(mp, what, extra);
        }
    };

    private final android.media.MediaPlayer.OnErrorListener videoErrorListener = new android.media.MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
            return genericOnError(mp, what, extra);
        }
    };

    private boolean genericOnError(Object inObj, int what, int extra) {
        return callback.onMediaPlayerError(inObj, what, extra);
    }

    private final com.aocate.media.MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener = new com.aocate.media.MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(com.aocate.media.MediaPlayer mp) {
            genericSeekCompleteListener();
        }
    };

    private final android.media.MediaPlayer.OnSeekCompleteListener videoSeekCompleteListener = new android.media.MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(android.media.MediaPlayer mp) {
            genericSeekCompleteListener();
        }
    };

    private final void genericSeekCompleteListener() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                if (playerStatus == PlayerStatus.SEEKING) {
                    setPlayerStatus(statusBeforeSeeking, media);
                }
                playerLock.unlock();
            }
        });
    }
}
