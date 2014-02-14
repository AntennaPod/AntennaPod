package de.danoeh.antennapod.service.playback;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.util.BitmapDecoder;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.playback.Playable;
import de.danoeh.antennapod.util.playback.PlaybackController;

import java.util.List;

/**
 * Controls the MediaPlayer that plays a FeedMedia-file
 */
public class PlaybackService extends Service {
    /**
     * Logging tag
     */
    private static final String TAG = "PlaybackService";

    /**
     * Parcelable of type Playable.
     */
    public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
    /**
     * True if media should be streamed.
     */
    public static final String EXTRA_SHOULD_STREAM = "extra.de.danoeh.antennapod.service.shouldStream";
    /**
     * True if playback should be started immediately after media has been
     * prepared.
     */
    public static final String EXTRA_START_WHEN_PREPARED = "extra.de.danoeh.antennapod.service.startWhenPrepared";

    public static final String EXTRA_PREPARE_IMMEDIATELY = "extra.de.danoeh.antennapod.service.prepareImmediately";

    public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.service.playerStatusChanged";
    private static final String AVRCP_ACTION_PLAYER_STATUS_CHANGED = "com.android.music.playstatechanged";

    public static final String ACTION_PLAYER_NOTIFICATION = "action.de.danoeh.antennapod.service.playerNotification";
    public static final String EXTRA_NOTIFICATION_CODE = "extra.de.danoeh.antennapod.service.notificationCode";
    public static final String EXTRA_NOTIFICATION_TYPE = "extra.de.danoeh.antennapod.service.notificationType";

    /**
     * If the PlaybackService receives this action, it will stop playback and
     * try to shutdown.
     */
    public static final String ACTION_SHUTDOWN_PLAYBACK_SERVICE = "action.de.danoeh.antennapod.service.actionShutdownPlaybackService";

    /**
     * If the PlaybackService receives this action, it will end playback of the
     * current episode and load the next episode if there is one available.
     */
    public static final String ACTION_SKIP_CURRENT_EPISODE = "action.de.danoeh.antennapod.service.skipCurrentEpisode";

    /**
     * Used in NOTIFICATION_TYPE_RELOAD.
     */
    public static final int EXTRA_CODE_AUDIO = 1;
    public static final int EXTRA_CODE_VIDEO = 2;

    public static final int NOTIFICATION_TYPE_ERROR = 0;
    public static final int NOTIFICATION_TYPE_INFO = 1;
    public static final int NOTIFICATION_TYPE_BUFFER_UPDATE = 2;

    /**
     * Receivers of this intent should update their information about the curently playing media
     */
    public static final int NOTIFICATION_TYPE_RELOAD = 3;
    /**
     * The state of the sleeptimer changed.
     */
    public static final int NOTIFICATION_TYPE_SLEEPTIMER_UPDATE = 4;
    public static final int NOTIFICATION_TYPE_BUFFER_START = 5;
    public static final int NOTIFICATION_TYPE_BUFFER_END = 6;
    /**
     * No more episodes are going to be played.
     */
    public static final int NOTIFICATION_TYPE_PLAYBACK_END = 7;

    /**
     * Playback speed has changed
     */
    public static final int NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE = 8;

    /**
     * Returned by getPositionSafe() or getDurationSafe() if the playbackService
     * is in an invalid state.
     */
    public static final int INVALID_TIME = -1;

    /**
     * Is true if service is running.
     */
    public static boolean isRunning = false;

    private static final int NOTIFICATION_ID = 1;

    private RemoteControlClient remoteControlClient;
    private PlaybackServiceMediaPlayer mediaPlayer;
    private PlaybackServiceTaskManager taskManager;

    private static volatile MediaType currentMediaType = MediaType.UNKNOWN;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Received onUnbind event");
        return super.onUnbind(intent);
    }

    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played. If the playbackservice is not
     * running, the type of the last played media will be looked up.
     */
    public static Intent getPlayerActivityIntent(Context context) {
        if (isRunning) {
            if (currentMediaType == MediaType.VIDEO) {
                return new Intent(context, VideoplayerActivity.class);
            } else {
                return new Intent(context, AudioplayerActivity.class);
            }
        } else {
            if (PlaybackPreferences.getCurrentEpisodeIsVideo()) {
                return new Intent(context, VideoplayerActivity.class);
            } else {
                return new Intent(context, AudioplayerActivity.class);
            }
        }
    }

    /**
     * Same as getPlayerActivityIntent(context), but here the type of activity
     * depends on the FeedMedia that is provided as an argument.
     */
    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        MediaType mt = media.getMediaType();
        if (mt == MediaType.VIDEO) {
            return new Intent(context, VideoplayerActivity.class);
        } else {
            return new Intent(context, AudioplayerActivity.class);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Service created.");
        isRunning = true;

        registerReceiver(headsetDisconnected, new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
        registerReceiver(shutdownReceiver, new IntentFilter(
                ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        registerReceiver(audioBecomingNoisy, new IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(skipCurrentEpisodeReceiver, new IntentFilter(
                ACTION_SKIP_CURRENT_EPISODE));
        remoteControlClient = setupRemoteControlClient();
        taskManager = new PlaybackServiceTaskManager(this, taskManagerCallback);
        mediaPlayer = new PlaybackServiceMediaPlayer(this, mediaPlayerCallback);

    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Service is about to be destroyed");
        isRunning = false;
        currentMediaType = MediaType.UNKNOWN;

        unregisterReceiver(headsetDisconnected);
        unregisterReceiver(shutdownReceiver);
        unregisterReceiver(audioBecomingNoisy);
        unregisterReceiver(skipCurrentEpisodeReceiver);
        mediaPlayer.shutdown();
        taskManager.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Received onBind event");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (AppConfig.DEBUG)
            Log.d(TAG, "OnStartCommand called");
        final int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
        final Playable playable = intent.getParcelableExtra(EXTRA_PLAYABLE);
        if (keycode == -1 && playable == null) {
            Log.e(TAG, "PlaybackService was started with no arguments");
            stopSelf();
        }

        if ((flags & Service.START_FLAG_REDELIVERY) != 0) {
            if (AppConfig.DEBUG) Log.d(TAG, "onStartCommand is a redelivered intent, calling stopForeground now.");
            stopForeground(true);
        }

        if (keycode != -1) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Received media button event");
            handleKeycode(keycode);
        } else {
            boolean stream = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
                    true);
            boolean startWhenPrepared = intent.getBooleanExtra(EXTRA_START_WHEN_PREPARED, false);
            boolean prepareImmediately = intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false);
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
            mediaPlayer.playMediaObject(playable, stream, startWhenPrepared, prepareImmediately);
        }

        return Service.START_REDELIVER_INTENT;
    }

    /**
     * Handles media button events
     */
    private void handleKeycode(int keycode) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Handling keycode: " + keycode);

        final PlayerStatus status = mediaPlayer.getPSMPInfo().playerStatus;
        switch (keycode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(true, true);
                } else if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.PREPARING) {
                    mediaPlayer.setStartWhenPrepared(!mediaPlayer.isStartWhenPrepared());
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(true, true);
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
                mediaPlayer.seekDelta(PlaybackController.DEFAULT_SEEK_DELTA);
                break;
            }
            case KeyEvent.KEYCODE_MEDIA_REWIND: {
                mediaPlayer.seekDelta(-PlaybackController.DEFAULT_SEEK_DELTA);
                break;
            }
        }
    }

    /**
     * Called by a mediaplayer Activity as soon as it has prepared its
     * mediaplayer.
     */
    public void setVideoSurface(SurfaceHolder sh) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Setting display");
        mediaPlayer.setVideoSurface(sh);
    }

    /**
     * Called when the surface holder of the mediaplayer has to be changed.
     */
    private void resetVideoSurface() {
        taskManager.cancelPositionSaver();
        mediaPlayer.resetVideoSurface();
    }

    public void notifyVideoSurfaceAbandoned() {
        stopForeground(true);
        mediaPlayer.resetVideoSurface();
    }

    private final PlaybackServiceTaskManager.PSTMCallback taskManagerCallback = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {
            saveCurrentPosition();
        }

        @Override
        public void onSleepTimerExpired() {
            mediaPlayer.pause(true, true);
            sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
        }


        @Override
        public void onWidgetUpdaterTick() {
            updateWidget();
        }

        @Override
        public void onChapterLoaded(Playable media) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
        }
    };

    private final PlaybackServiceMediaPlayer.PSMPCallback mediaPlayerCallback = new PlaybackServiceMediaPlayer.PSMPCallback() {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {
            currentMediaType = mediaPlayer.getCurrentMediaType();
            switch (newInfo.playerStatus) {
                case INITIALIZED:
                    writePlaybackPreferences();
                    break;

                case PREPARED:
                    taskManager.startChapterLoader(newInfo.playable);
                    break;

                case PAUSED:
                    taskManager.cancelPositionSaver();
                    saveCurrentPosition();
                    taskManager.cancelWidgetUpdater();
                    stopForeground(true);
                    break;

                case STOPPED:
                    //setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
                    //stopSelf();
                    break;

                case PLAYING:
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Audiofocus successfully requested");
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Resuming/Starting playback");

                    taskManager.startPositionSaver();
                    taskManager.startWidgetUpdater();
                    setupNotification(newInfo);
                    break;

            }

            sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
            updateWidget();
            refreshRemoteControlClientState(newInfo);
            bluetoothNotifyChange(newInfo);
        }

        @Override
        public void shouldStop() {
            stopSelf();
        }

        @Override
        public void playbackSpeedChanged(float s) {
            sendNotificationBroadcast(
                    NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE, 0);
        }

        @Override
        public void onBufferingUpdate(int percent) {
            sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_UPDATE, percent);
        }

        @Override
        public boolean onMediaPlayerInfo(int code) {
            switch (code) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_START, 0);
                    return true;
                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_END, 0);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onMediaPlayerError(Object inObj, int what, int extra) {
            final String TAG = "PlaybackService.onErrorListener";
            Log.w(TAG, "An error has occured: " + what + " " + extra);
            if (mediaPlayer.getPSMPInfo().playerStatus == PlayerStatus.PLAYING) {
                mediaPlayer.pause(true, false);
            }
            sendNotificationBroadcast(NOTIFICATION_TYPE_ERROR, what);
            setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
            stopSelf();
            return true;
        }

        @Override
        public boolean endPlayback(boolean playNextEpisode) {
            PlaybackService.this.endPlayback(true);
            return true;
        }

        @Override
        public RemoteControlClient getRemoteControlClient() {
            return remoteControlClient;
        }
    };

    private void endPlayback(boolean playNextEpisode) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Playback ended");

        final Playable media = mediaPlayer.getPSMPInfo().playable;
        if (media == null) {
            Log.e(TAG, "Cannot end playback: media was null");
            return;
        }

        taskManager.cancelPositionSaver();

        boolean isInQueue = false;
        FeedItem nextItem = null;

        if (media instanceof FeedMedia) {
            FeedItem item = ((FeedMedia) media).getItem();
            DBWriter.markItemRead(PlaybackService.this, item, true, true);

            try {
                final List<FeedItem> queue = taskManager.getQueue();
                isInQueue = QueueAccess.ItemListAccess(queue).contains(((FeedMedia) media).getItem().getId());
                nextItem = DBTasks.getQueueSuccessorOfItem(this, item.getId(), queue);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // isInQueue remains false
            }
            if (isInQueue) {
                DBWriter.removeQueueItem(PlaybackService.this, item.getId(), true);
            }
            DBWriter.addItemToPlaybackHistory(PlaybackService.this, (FeedMedia) media);
        }

        // Load next episode if previous episode was in the queue and if there
        // is an episode in the queue left.
        // Start playback immediately if continuous playback is enabled
        Playable nextMedia = null;
        boolean loadNextItem = isInQueue && nextItem != null;
        playNextEpisode = playNextEpisode && loadNextItem
                && UserPreferences.isFollowQueue();
        if (loadNextItem) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Loading next item in queue");
            nextMedia = nextItem.getMedia();
        }
        final boolean prepareImmediately;
        final boolean startWhenPrepared;
        final boolean stream;

        if (playNextEpisode) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Playback of next episode will start immediately.");
            prepareImmediately = startWhenPrepared = true;
        } else {
            if (AppConfig.DEBUG)
                Log.d(TAG, "No more episodes available to play");

            prepareImmediately = startWhenPrepared = false;
            stopForeground(true);
            stopWidgetUpdater();
        }

        writePlaybackPreferences();
        if (nextMedia != null) {
            stream = !media.localFileAvailable();
            mediaPlayer.playMediaObject(nextMedia, stream, startWhenPrepared, prepareImmediately);
            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                    (nextMedia.getMediaType() == MediaType.VIDEO) ? EXTRA_CODE_VIDEO : EXTRA_CODE_AUDIO);
        } else {
            sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_END, 0);
            //stopSelf();
        }
    }

    public void setSleepTimer(long waitingTime) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Setting sleep timer to " + Long.toString(waitingTime)
                    + " milliseconds");
        taskManager.setSleepTimer(waitingTime);
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
    }

    public void disableSleepTimer() {
        taskManager.disableSleepTimer();
        sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
    }


    private void writePlaybackPreferences() {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Writing playback preferences");

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
        MediaType mediaType = mediaPlayer.getCurrentMediaType();
        boolean stream = mediaPlayer.isStreaming();

        if (info.playable != null) {
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
                    info.playable.getPlayableType());
            editor.putBoolean(
                    PlaybackPreferences.PREF_CURRENT_EPISODE_IS_STREAM,
                    stream);
            editor.putBoolean(
                    PlaybackPreferences.PREF_CURRENT_EPISODE_IS_VIDEO,
                    mediaType == MediaType.VIDEO);
            if (info.playable instanceof FeedMedia) {
                FeedMedia fMedia = (FeedMedia) info.playable;
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                        fMedia.getItem().getFeed().getId());
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                        fMedia.getId());
            } else {
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                        PlaybackPreferences.NO_MEDIA_PLAYING);
                editor.putLong(
                        PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                        PlaybackPreferences.NO_MEDIA_PLAYING);
            }
            info.playable.writeToPreferences(editor);
        } else {
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
            editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
            editor.putLong(
                    PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
                    PlaybackPreferences.NO_MEDIA_PLAYING);
        }

        editor.commit();
    }

    /**
     * Send ACTION_PLAYER_STATUS_CHANGED without changing the status attribute.
     */
    private void postStatusUpdateIntent() {
        sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
    }

    private void sendNotificationBroadcast(int type, int code) {
        Intent intent = new Intent(ACTION_PLAYER_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
        intent.putExtra(EXTRA_NOTIFICATION_CODE, code);
        sendBroadcast(intent);
    }

    /**
     * Used by setupNotification to load notification data in another thread.
     */
    private AsyncTask<Void, Void, Void> notificationSetupTask;

    /**
     * Prepares notification and starts the service in the foreground.
     */
    @SuppressLint("NewApi")
    private void setupNotification(final PlaybackServiceMediaPlayer.PSMPInfo info) {
        final PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (notificationSetupTask != null) {
            notificationSetupTask.cancel(true);
        }
        notificationSetupTask = new AsyncTask<Void, Void, Void>() {
            Bitmap icon = null;

            @Override
            protected Void doInBackground(Void... params) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Starting background work");
                if (android.os.Build.VERSION.SDK_INT >= 11) {
                    if (info.playable != null) {
                        int iconSize = getResources().getDimensionPixelSize(
                                android.R.dimen.notification_large_icon_width);
                        icon = BitmapDecoder
                                .decodeBitmapFromWorkerTaskResource(iconSize,
                                        info.playable);
                    }

                }
                if (icon == null) {
                    icon = BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_stat_antenna);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (!isCancelled() && info.playerStatus == PlayerStatus.PLAYING
                        && info.playable != null) {
                    String contentText = info.playable.getFeedTitle();
                    String contentTitle = info.playable.getEpisodeTitle();
                    Notification notification = null;
                    if (android.os.Build.VERSION.SDK_INT >= 16) {
                        Intent pauseButtonIntent = new Intent(
                                PlaybackService.this, PlaybackService.class);
                        pauseButtonIntent.putExtra(
                                MediaButtonReceiver.EXTRA_KEYCODE,
                                KeyEvent.KEYCODE_MEDIA_PAUSE);
                        PendingIntent pauseButtonPendingIntent = PendingIntent
                                .getService(PlaybackService.this, 0,
                                        pauseButtonIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification.Builder notificationBuilder = new Notification.Builder(
                                PlaybackService.this)
                                .setContentTitle(contentTitle)
                                .setContentText(contentText)
                                .setOngoing(true)
                                .setContentIntent(pIntent)
                                .setLargeIcon(icon)
                                .setSmallIcon(R.drawable.ic_stat_antenna)
                                .addAction(android.R.drawable.ic_media_pause,
                                        getString(R.string.pause_label),
                                        pauseButtonPendingIntent);
                        notification = notificationBuilder.build();
                    } else {
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                                PlaybackService.this)
                                .setContentTitle(contentTitle)
                                .setContentText(contentText).setOngoing(true)
                                .setContentIntent(pIntent).setLargeIcon(icon)
                                .setSmallIcon(R.drawable.ic_stat_antenna);
                        notification = notificationBuilder.getNotification();
                    }
                    startForeground(NOTIFICATION_ID, notification);
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Notification set up");
                }
            }

        };
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            notificationSetupTask
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            notificationSetupTask.execute();
        }

    }

    /**
     * Saves the current position of the media file to the DB
     */
    private synchronized void saveCurrentPosition() {
        int position = getCurrentPosition();
        final Playable playable = mediaPlayer.getPSMPInfo().playable;
        if (position != INVALID_TIME && playable != null) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Saving current position to " + position);
            playable.saveCurrentPosition(PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()),
                    position);
        }
    }

    private void stopWidgetUpdater() {
        taskManager.cancelWidgetUpdater();
        sendBroadcast(new Intent(PlayerWidget.STOP_WIDGET_UPDATE));
    }

    private void updateWidget() {
        PlaybackService.this.sendBroadcast(new Intent(
                PlayerWidget.FORCE_WIDGET_UPDATE));
    }

    public boolean sleepTimerActive() {
        return taskManager.isSleepTimerActive();
    }

    public long getSleepTimerTimeLeft() {
        return taskManager.getSleepTimerTimeLeft();
    }

    @SuppressLint("NewApi")
    private RemoteControlClient setupRemoteControlClient() {
        if (Build.VERSION.SDK_INT < 14) {
            return null;
        }

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(new ComponentName(getPackageName(),
                MediaButtonReceiver.class.getName()));
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, mediaButtonIntent, 0);
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        int controlFlags;
        if (android.os.Build.VERSION.SDK_INT < 16) {
            controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_NEXT;
        } else {
            controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE;
        }
        remoteControlClient.setTransportControlFlags(controlFlags);
        return remoteControlClient;
    }

    /**
     * Refresh player status and metadata.
     */
    @SuppressLint("NewApi")
    private void refreshRemoteControlClientState(PlaybackServiceMediaPlayer.PSMPInfo info) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            if (remoteControlClient != null) {
                switch (info.playerStatus) {
                    case PLAYING:
                        remoteControlClient
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                        break;
                    case PAUSED:
                    case INITIALIZED:
                        remoteControlClient
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                        break;
                    case STOPPED:
                        remoteControlClient
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                        break;
                    case ERROR:
                        remoteControlClient
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_ERROR);
                        break;
                    default:
                        remoteControlClient
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_BUFFERING);
                }
                if (info.playable != null) {
                    MetadataEditor editor = remoteControlClient
                            .editMetadata(false);
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                            info.playable.getEpisodeTitle());

                    editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                            info.playable.getFeedTitle());

                    editor.apply();
                }
                if (AppConfig.DEBUG)
                    Log.d(TAG, "RemoteControlClient state was refreshed");
            }
        }
    }

    private void bluetoothNotifyChange(PlaybackServiceMediaPlayer.PSMPInfo info) {
        boolean isPlaying = false;

        if (info.playerStatus == PlayerStatus.PLAYING) {
            isPlaying = true;
        }

        if (info.playable != null) {
            Intent i = new Intent(AVRCP_ACTION_PLAYER_STATUS_CHANGED);
            i.putExtra("id", 1);
            i.putExtra("artist", "");
            i.putExtra("album", info.playable.getFeedTitle());
            i.putExtra("track", info.playable.getEpisodeTitle());
            i.putExtra("playing", isPlaying);
            final List<FeedItem> queue = taskManager.getQueueIfLoaded();
            if (queue != null) {
                i.putExtra("ListSize", queue.size());
            }
            i.putExtra("duration", info.playable.getDuration());
            i.putExtra("position", info.playable.getPosition());
            sendBroadcast(i);
        }
    }

    /**
     * Pauses playback when the headset is disconnected and the preference is
     * set
     */
    private BroadcastReceiver headsetDisconnected = new BroadcastReceiver() {
        private static final String TAG = "headsetDisconnected";
        private static final int UNPLUGGED = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if (state != -1) {
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Headset plug event. State is " + state);
                    if (state == UNPLUGGED) {
                        if (AppConfig.DEBUG)
                            Log.d(TAG, "Headset was unplugged during playback.");
                        pauseIfPauseOnDisconnect();
                    }
                } else {
                    Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
                }
            }
        }
    };

    private BroadcastReceiver audioBecomingNoisy = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // sound is about to change, eg. bluetooth -> speaker
            if (AppConfig.DEBUG)
                Log.d(TAG, "Pausing playback because audio is becoming noisy");
            pauseIfPauseOnDisconnect();
        }
        // android.media.AUDIO_BECOMING_NOISY
    };

    /**
     * Pauses playback if PREF_PAUSE_ON_HEADSET_DISCONNECT was set to true.
     */
    private void pauseIfPauseOnDisconnect() {
        if (UserPreferences.isPauseOnHeadsetDisconnect()) {
            mediaPlayer.pause(true, true);
        }
    }

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().equals(ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
                stopSelf();
            }
        }

    };

    private BroadcastReceiver skipCurrentEpisodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().equals(ACTION_SKIP_CURRENT_EPISODE)) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Received SKIP_CURRENT_EPISODE intent");
                mediaPlayer.endPlayback();
            }
        }
    };

    public static MediaType getCurrentMediaType() {
        return currentMediaType;
    }

    public void resume() {
        mediaPlayer.resume();
    }

    public void prepare() {
        mediaPlayer.prepare();
    }

    public void pause(boolean abandonAudioFocus, boolean reinit) {
        mediaPlayer.pause(abandonAudioFocus, reinit);
    }

    public void reinit() {
        mediaPlayer.reinit();
    }

    public PlaybackServiceMediaPlayer.PSMPInfo getPSMPInfo() {
        return mediaPlayer.getPSMPInfo();
    }

    public PlayerStatus getStatus() {
        return mediaPlayer.getPSMPInfo().playerStatus;
    }

    public Playable getPlayable() {
        return mediaPlayer.getPSMPInfo().playable;
    }

    public void setSpeed(float speed) {
        mediaPlayer.setSpeed(speed);
    }

    public boolean canSetSpeed() {
        return mediaPlayer.canSetSpeed();
    }

    public float getCurrentPlaybackSpeed() {
        return mediaPlayer.getPlaybackSpeed();
    }

    public boolean isStartWhenPrepared() {
        return mediaPlayer.isStartWhenPrepared();
    }

    public void setStartWhenPrepared(boolean s) {
        mediaPlayer.setStartWhenPrepared(s);
    }


    public void seekTo(final int t) {
        mediaPlayer.seekTo(t);
    }


    public void seekDelta(final int d) {
        mediaPlayer.seekDelta(d);
    }

    /**
     * @see de.danoeh.antennapod.service.playback.PlaybackServiceMediaPlayer#seekToChapter(de.danoeh.antennapod.feed.Chapter)
     */
    public void seekToChapter(Chapter c) {
        mediaPlayer.seekToChapter(c);
    }

    /**
     * call getDuration() on mediaplayer or return INVALID_TIME if player is in
     * an invalid state.
     */
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    /**
     * call getCurrentPosition() on mediaplayer or return INVALID_TIME if player
     * is in an invalid state.
     */
    public int getCurrentPosition() {
        return mediaPlayer.getPosition();
    }

    public boolean isStreaming() {
        return mediaPlayer.isStreaming();
    }

    public Pair<Integer, Integer> getVideoSize() {
        return mediaPlayer.getVideoSize();
    }

    private void setCurrentlyPlayingMedia(long id) {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA, id);
        editor.commit();
    }
}
