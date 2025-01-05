package de.danoeh.antennapod.playback.service;

import static de.danoeh.antennapod.model.feed.FeedPreferences.SPEED_USE_GLOBAL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.service.quicksettings.TileService;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.ViewConfiguration;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.app.connection.CarConnection;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.media.MediaBrowserServiceCompat;

import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.service.internal.LocalPSMP;
import de.danoeh.antennapod.playback.service.internal.PlayableUtils;
import de.danoeh.antennapod.playback.service.internal.PlaybackServiceNotificationBuilder;
import de.danoeh.antennapod.playback.service.internal.PlaybackServiceStateManager;
import de.danoeh.antennapod.playback.service.internal.PlaybackServiceTaskManager;
import de.danoeh.antennapod.playback.service.internal.PlaybackVolumeUpdater;
import de.danoeh.antennapod.playback.service.internal.WearMediaSession;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import de.danoeh.antennapod.ui.widget.WidgetUpdater;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.playback.service.internal.PlaybackServiceTaskManager.SleepTimer;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.PlaybackServiceEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.event.settings.SkipIntroEndingChangedEvent;
import de.danoeh.antennapod.event.settings.SpeedPresetChangedEvent;
import de.danoeh.antennapod.event.settings.VolumeAdaptionChangedEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.cast.CastPsmp;
import de.danoeh.antennapod.playback.cast.CastStateListener;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.VideoPlayerActivityStarter;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Controls the MediaPlayer that plays a FeedMedia-file
 */
public class PlaybackService extends MediaBrowserServiceCompat {
    /**
     * Logging tag
     */
    private static final String TAG = "PlaybackService";

    public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.core.service.playerStatusChanged";
    private static final String AVRCP_ACTION_PLAYER_STATUS_CHANGED = "com.android.music.playstatechanged";
    private static final String AVRCP_ACTION_META_CHANGED = "com.android.music.metachanged";

    /**
     * Custom actions used by Android Wear, Android Auto, and Android (API 33+ only)
     */
    private static final String CUSTOM_ACTION_SKIP_TO_NEXT = "action.de.danoeh.antennapod.core.service.skipToNext";
    private static final String CUSTOM_ACTION_FAST_FORWARD = "action.de.danoeh.antennapod.core.service.fastForward";
    private static final String CUSTOM_ACTION_REWIND = "action.de.danoeh.antennapod.core.service.rewind";
    private static final String CUSTOM_ACTION_CHANGE_PLAYBACK_SPEED =
            "action.de.danoeh.antennapod.core.service.changePlaybackSpeed";
    private static final String CUSTOM_ACTION_TOGGLE_SLEEP_TIMER =
            "action.de.danoeh.antennapod.core.service.toggleSleepTimer";
    public static final String CUSTOM_ACTION_NEXT_CHAPTER = "action.de.danoeh.antennapod.core.service.next_chapter";

    /**
     * Set a max number of episodes to load for Android Auto, otherwise there could be performance issues
     */
    public static final int MAX_ANDROID_AUTO_EPISODES_PER_FEED = 100;

    /**
     * Is true if service is running.
     */
    public static boolean isRunning = false;
    /**
     * Is true if the service was running, but paused due to headphone disconnect
     */
    private static boolean transientPause = false;
    /**
     * Is true if a Cast Device is connected to the service.
     */
    private static volatile boolean isCasting = false;

    private PlaybackServiceMediaPlayer mediaPlayer;
    private PlaybackServiceTaskManager taskManager;
    private PlaybackServiceStateManager stateManager;
    private Disposable positionEventTimer;
    private PlaybackServiceNotificationBuilder notificationBuilder;
    private CastStateListener castStateListener;

    private String autoSkippedFeedMediaId = null;
    private String positionJustResetAfterPlayback = null;
    private int clickCount = 0;
    private final Handler clickHandler = new Handler(Looper.getMainLooper());

    /**
     * Used for Lollipop notifications, Android Wear, and Android Auto.
     */
    private MediaSessionCompat mediaSession;

    private static volatile MediaType currentMediaType = MediaType.UNKNOWN;
    private LiveData<Integer> androidAutoConnectionState;
    private boolean androidAutoConnected = false;
    private Observer<Integer> androidAutoConnectionObserver;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Received onUnbind event");
        return super.onUnbind(intent);
    }

    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played. If the playbackservice is not
     * running, the type of the last played media will be looked up.
     */
    public static Intent getPlayerActivityIntent(Context context) {
        boolean showVideoPlayer;

        if (isRunning) {
            showVideoPlayer = currentMediaType == MediaType.VIDEO && !isCasting;
        } else {
            showVideoPlayer = PlaybackPreferences.getCurrentEpisodeIsVideo();
        }

        if (showVideoPlayer) {
            return new VideoPlayerActivityStarter(context).getIntent();
        } else {
            return new MainActivityStarter(context).withOpenPlayer().getIntent();
        }
    }

    /**
     * Same as {@link #getPlayerActivityIntent(Context)}, but here the type of activity
     * depends on the FeedMedia that is provided as an argument.
     */
    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        if (media.getMediaType() == MediaType.VIDEO && !isCasting) {
            return new VideoPlayerActivityStarter(context).getIntent();
        } else {
            return new MainActivityStarter(context).withOpenPlayer().getIntent();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created.");
        isRunning = true;

        stateManager = new PlaybackServiceStateManager(this);
        notificationBuilder = new PlaybackServiceNotificationBuilder(this);
        androidAutoConnectionState = new CarConnection(this).getType();
        androidAutoConnectionObserver = connectionState -> {
            androidAutoConnected = connectionState == CarConnection.CONNECTION_TYPE_PROJECTION;
        };
        androidAutoConnectionState.observeForever(androidAutoConnectionObserver);

        ContextCompat.registerReceiver(this, shutdownReceiver,
                new IntentFilter(PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        registerReceiver(headsetDisconnected, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(bluetoothStateUpdated, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        registerReceiver(audioBecomingNoisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        EventBus.getDefault().register(this);
        taskManager = new PlaybackServiceTaskManager(this, taskManagerCallback);

        recreateMediaSessionIfNeeded();
        castStateListener = new CastStateListener(this) {
            @Override
            public void onSessionStartedOrEnded() {
                recreateMediaPlayer();
            }
        };
        EventBus.getDefault().post(new PlaybackServiceEvent(PlaybackServiceEvent.Action.SERVICE_STARTED));
    }

    void recreateMediaSessionIfNeeded() {
        if (mediaSession != null) {
            // Media session was not destroyed, so we can re-use it.
            if (!mediaSession.isActive()) {
                mediaSession.setActive(true);
            }
            return;
        }
        ComponentName eventReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));

        mediaSession = new MediaSessionCompat(getApplicationContext(), TAG, eventReceiver, buttonReceiverIntent);
        mediaSession.setCallback(sessionCallback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        recreateMediaPlayer();
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());
    }

    void recreateMediaPlayer() {
        Playable media = null;
        boolean wasPlaying = false;
        if (mediaPlayer != null) {
            media = mediaPlayer.getPlayable();
            wasPlaying = mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING;
            mediaPlayer.pause(true, false);
            mediaPlayer.shutdown();
        }
        mediaPlayer = CastPsmp.getInstanceIfConnected(this, mediaPlayerCallback);
        if (mediaPlayer == null) {
            mediaPlayer = new LocalPSMP(this, mediaPlayerCallback); // Cast not supported or not connected
        }
        if (media != null) {
            mediaPlayer.playMediaObject(media, !media.localFileAvailable(), wasPlaying, true);
        }
        isCasting = mediaPlayer.isCasting();
        updateMediaSession(mediaPlayer.getPlayerStatus());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service is about to be destroyed");

        if (notificationBuilder.getPlayerStatus() == PlayerStatus.PLAYING) {
            notificationBuilder.setPlayerStatus(PlayerStatus.STOPPED);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(R.id.notification_playing, notificationBuilder.build());
            }
        }
        stateManager.stopForeground(!UserPreferences.isPersistNotify());
        isRunning = false;
        currentMediaType = MediaType.UNKNOWN;
        castStateListener.destroy();

        androidAutoConnectionState.removeObserver(androidAutoConnectionObserver);
        cancelPositionObserver();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        unregisterReceiver(headsetDisconnected);
        unregisterReceiver(shutdownReceiver);
        unregisterReceiver(bluetoothStateUpdated);
        unregisterReceiver(audioBecomingNoisy);
        mediaPlayer.shutdown();
        taskManager.shutdown();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName +
                "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        if (rootHints != null && rootHints.getBoolean(BrowserRoot.EXTRA_RECENT)) {
            Bundle extras = new Bundle();
            extras.putBoolean(BrowserRoot.EXTRA_RECENT, true);
            Log.d(TAG, "OnGetRoot: Returning BrowserRoot " + R.string.current_playing_episode);
            return new BrowserRoot(getResources().getString(R.string.current_playing_episode), extras);
        }

        // Name visible in Android Auto
        return new BrowserRoot(getResources().getString(R.string.app_name), null);
    }

    private void loadQueueForMediaSession() {
        Single.<List<MediaSessionCompat.QueueItem>>create(emitter -> {
            List<MediaSessionCompat.QueueItem> queueItems = new ArrayList<>();
            for (FeedItem feedItem : DBReader.getQueue()) {
                if (feedItem.getMedia() != null) {
                    MediaDescriptionCompat mediaDescription = feedItem.getMedia().getMediaItem().getDescription();
                    queueItems.add(new MediaSessionCompat.QueueItem(mediaDescription, feedItem.getId()));
                }
            }
            emitter.onSuccess(queueItems);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(queueItems -> mediaSession.setQueue(queueItems), Throwable::printStackTrace);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItem(
            @StringRes int title, @DrawableRes int icon, int numEpisodes) {
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getResources().getResourcePackageName(icon))
                .appendPath(getResources().getResourceTypeName(icon))
                .appendPath(getResources().getResourceEntryName(icon))
                .build();

        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setIconUri(uri)
                .setMediaId(getResources().getString(title))
                .setTitle(getResources().getString(title))
                .setSubtitle(getResources().getQuantityString(R.plurals.num_episodes, numEpisodes, numEpisodes))
                .build();
        return new MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForFeed(Feed feed) {
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId("FeedId:" + feed.getId())
                .setTitle(feed.getTitle())
                .setDescription(feed.getDescription())
                .setSubtitle(feed.getCustomTitle());
        if (feed.getImageUrl() != null) {
            builder.setIconUri(Uri.parse(feed.getImageUrl()));
        }
        if (feed.getLink() != null) {
            builder.setMediaUri(Uri.parse(feed.getLink()));
        }
        MediaDescriptionCompat description = builder.build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentId);
        result.detach();

        Completable.create(emitter -> {
            result.sendResult(loadChildrenSynchronous(parentId));
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {
                    }, e -> {
                        e.printStackTrace();
                        result.sendResult(null);
                    });
    }

    private List<MediaBrowserCompat.MediaItem> loadChildrenSynchronous(@NonNull String parentId) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        if (parentId.equals(getResources().getString(R.string.app_name))) {
            long currentlyPlaying = PlaybackPreferences.getCurrentPlayerStatus();
            if (currentlyPlaying == PlaybackPreferences.PLAYER_STATUS_PLAYING
                    || currentlyPlaying == PlaybackPreferences.PLAYER_STATUS_PAUSED) {
                mediaItems.add(createBrowsableMediaItem(R.string.current_playing_episode, R.drawable.ic_play_48dp, 1));
            }
            mediaItems.add(createBrowsableMediaItem(R.string.queue_label, R.drawable.ic_playlist_play_black,
                    DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.QUEUED))));
            mediaItems.add(createBrowsableMediaItem(R.string.downloads_label, R.drawable.ic_download_black,
                    DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED))));
            mediaItems.add(createBrowsableMediaItem(R.string.episodes_label, R.drawable.ic_feed_black,
                    DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.UNPLAYED))));
            List<Feed> feeds = DBReader.getFeedList();
            for (Feed feed : feeds) {
                if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                    mediaItems.add(createBrowsableMediaItemForFeed(feed));
                }
            }
            return mediaItems;
        }

        List<FeedItem> feedItems;
        if (parentId.equals(getResources().getString(R.string.queue_label))) {
            feedItems = DBReader.getQueue();
        } else if (parentId.equals(getResources().getString(R.string.downloads_label))) {
            feedItems = DBReader.getEpisodes(0, MAX_ANDROID_AUTO_EPISODES_PER_FEED,
                    new FeedItemFilter(FeedItemFilter.DOWNLOADED), UserPreferences.getDownloadsSortedOrder());
        } else if (parentId.equals(getResources().getString(R.string.episodes_label))) {
            feedItems = DBReader.getEpisodes(0, MAX_ANDROID_AUTO_EPISODES_PER_FEED,
                    new FeedItemFilter(FeedItemFilter.UNPLAYED), UserPreferences.getAllEpisodesSortOrder());
        } else if (parentId.startsWith("FeedId:")) {
            long feedId = Long.parseLong(parentId.split(":")[1]);
            feedItems = DBReader.getFeed(feedId, true, 0, MAX_ANDROID_AUTO_EPISODES_PER_FEED).getItems();
        } else if (parentId.equals(getString(R.string.current_playing_episode))) {
            FeedMedia playable = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            if (playable != null) {
                feedItems = Collections.singletonList(playable.getItem());
            } else {
                return null;
            }
        } else {
            Log.e(TAG, "Parent ID not found: " + parentId);
            return null;
        }
        int count = 0;
        for (FeedItem feedItem : feedItems) {
            if (feedItem.getMedia() != null && feedItem.getMedia().getMediaItem() != null) {
                mediaItems.add(feedItem.getMedia().getMediaItem());
                if (++count >= MAX_ANDROID_AUTO_EPISODES_PER_FEED) {
                    break;
                }
            }
        }
        return mediaItems;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Received onBind event");
        if (intent.getAction() != null && TextUtils.equals(intent.getAction(), MediaBrowserServiceCompat.SERVICE_INTERFACE)) {
            return super.onBind(intent);
        } else {
            return mBinder;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "OnStartCommand called");

        stateManager.startForeground(R.id.notification_playing, notificationBuilder.build());
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(R.id.notification_streaming_confirmation);

        final int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
        final String customAction = intent.getStringExtra(MediaButtonReceiver.EXTRA_CUSTOM_ACTION);
        final boolean hardwareButton = intent.getBooleanExtra(MediaButtonReceiver.EXTRA_HARDWAREBUTTON, false);
        Playable playable = intent.getParcelableExtra(PlaybackServiceInterface.EXTRA_PLAYABLE);
        if (keycode == -1 && playable == null && customAction == null) {
            Log.e(TAG, "PlaybackService was started with no arguments");
            stateManager.stopService();
            return Service.START_NOT_STICKY;
        }

        if ((flags & Service.START_FLAG_REDELIVERY) != 0) {
            Log.d(TAG, "onStartCommand is a redelivered intent, calling stopForeground now.");
            stateManager.stopForeground(true);
        } else {
            if (keycode != -1) {
                boolean notificationButton;
                if (hardwareButton) {
                    Log.d(TAG, "Received hardware button event");
                    notificationButton = false;
                } else {
                    Log.d(TAG, "Received media button event");
                    notificationButton = true;
                }
                boolean handled = handleKeycode(keycode, notificationButton);
                if (!handled && !stateManager.hasReceivedValidStartCommand()) {
                    stateManager.stopService();
                    return Service.START_NOT_STICKY;
                }
            } else if (playable != null) {
                stateManager.validStartCommandWasReceived();
                boolean allowStreamThisTime = intent.getBooleanExtra(
                        PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME, false);
                boolean allowStreamAlways = intent.getBooleanExtra(
                        PlaybackServiceInterface.EXTRA_ALLOW_STREAM_ALWAYS, false);
                sendNotificationBroadcast(PlaybackServiceInterface.NOTIFICATION_TYPE_RELOAD, 0);
                if (allowStreamAlways) {
                    UserPreferences.setAllowMobileStreaming(true);
                }
                Observable.fromCallable(
                        () -> {
                            if (playable instanceof FeedMedia) {
                                return DBReader.getFeedMedia(((FeedMedia) playable).getId());
                            } else {
                                return playable;
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                loadedPlayable -> startPlaying(loadedPlayable, allowStreamThisTime),
                                error -> {
                                    Log.d(TAG, "Playable was not found. Stopping service.");
                                    error.printStackTrace();
                                    stateManager.stopService();
                                });
                return Service.START_NOT_STICKY;
            } else {
                mediaSession.getController().getTransportControls().sendCustomAction(customAction, null);
            }
        }

        return Service.START_NOT_STICKY;
    }

    private void skipIntro(Playable playable) {
        if (! (playable instanceof FeedMedia)) {
            return;
        }

        FeedMedia feedMedia = (FeedMedia) playable;
        FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
        int skipIntro = preferences.getFeedSkipIntro();

        Context context = getApplicationContext();
        if (skipIntro > 0 && playable.getPosition() < skipIntro * 1000) {
            int duration = getDuration();
            if (skipIntro * 1000 < duration || duration <= 0) {
                Log.d(TAG, "skipIntro " + playable.getEpisodeTitle());
                mediaPlayer.seekTo(skipIntro * 1000);
                String skipIntroMesg = context.getString(R.string.pref_feed_skip_intro_toast,
                        skipIntro);
                Toast toast = Toast.makeText(context, skipIntroMesg,
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    @SuppressLint("LaunchActivityFromNotification")
    private void displayStreamingNotAllowedNotification(Intent originalIntent) {
        if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
            EventBus.getDefault().post(new MessageEvent(
                    getString(R.string.confirm_mobile_streaming_notification_message)));
            return;
        }

        Intent intentAllowThisTime = new Intent(originalIntent);
        intentAllowThisTime.setAction(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME);
        intentAllowThisTime.putExtra(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_THIS_TIME, true);
        PendingIntent pendingIntentAllowThisTime;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntentAllowThisTime = PendingIntent.getForegroundService(this,
                    R.id.pending_intent_allow_stream_this_time, intentAllowThisTime,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntentAllowThisTime = PendingIntent.getService(this,
                    R.id.pending_intent_allow_stream_this_time, intentAllowThisTime, PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        }

        Intent intentAlwaysAllow = new Intent(intentAllowThisTime);
        intentAlwaysAllow.setAction(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_ALWAYS);
        intentAlwaysAllow.putExtra(PlaybackServiceInterface.EXTRA_ALLOW_STREAM_ALWAYS, true);
        PendingIntent pendingIntentAlwaysAllow;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntentAlwaysAllow = PendingIntent.getForegroundService(this,
                    R.id.pending_intent_allow_stream_always, intentAlwaysAllow,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntentAlwaysAllow = PendingIntent.getService(this,
                    R.id.pending_intent_allow_stream_always, intentAlwaysAllow, PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                NotificationUtils.CHANNEL_ID_USER_ACTION)
                .setSmallIcon(R.drawable.ic_notification_stream)
                .setContentTitle(getString(R.string.confirm_mobile_streaming_notification_title))
                .setContentText(getString(R.string.confirm_mobile_streaming_notification_message))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.confirm_mobile_streaming_notification_message)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntentAllowThisTime)
                .addAction(R.drawable.ic_notification_stream,
                        getString(R.string.confirm_mobile_streaming_button_once),
                        pendingIntentAllowThisTime)
                .addAction(R.drawable.ic_notification_stream,
                        getString(R.string.confirm_mobile_streaming_button_always),
                        pendingIntentAlwaysAllow)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(R.id.notification_streaming_confirmation, builder.build());
        } else {
            Toast.makeText(getApplicationContext(),
                    R.string.confirm_mobile_streaming_notification_message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handles media button events
     * return: keycode was handled
     */
    private boolean handleKeycode(int keycode, boolean notificationButton) {
        Log.d(TAG, "Handling keycode: " + keycode);
        final PlaybackServiceMediaPlayer.PSMPInfo info = mediaPlayer.getPSMPInfo();
        final PlayerStatus status = info.getPlayerStatus();
        switch (keycode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(!UserPreferences.isPersistNotify(), false);
                } else if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.PREPARING) {
                    mediaPlayer.setStartWhenPrepared(!mediaPlayer.isStartWhenPrepared());
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                } else if (mediaPlayer.getPlayable() == null) {
                    startPlayingFromPreferences();
                } else {
                    return false;
                }
                taskManager.restartSleepTimer();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                    mediaPlayer.resume();
                } else if (status == PlayerStatus.INITIALIZED) {
                    mediaPlayer.setStartWhenPrepared(true);
                    mediaPlayer.prepare();
                } else if (mediaPlayer.getPlayable() == null) {
                    startPlayingFromPreferences();
                } else {
                    return false;
                }
                taskManager.restartSleepTimer();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(!UserPreferences.isPersistNotify(), false);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (!notificationButton) {
                    // Handle remapped button as notification button which is not remapped again.
                    return handleKeycode(UserPreferences.getHardwareForwardButton(), true);
                } else if (getStatus() == PlayerStatus.PLAYING || getStatus() == PlayerStatus.PAUSED) {
                    mediaPlayer.skip();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (getStatus() == PlayerStatus.PLAYING || getStatus() == PlayerStatus.PAUSED) {
                    mediaPlayer.seekDelta(UserPreferences.getFastForwardSecs() * 1000);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (!notificationButton) {
                    // Handle remapped button as notification button which is not remapped again.
                    return handleKeycode(UserPreferences.getHardwarePreviousButton(), true);
                } else if (getStatus() == PlayerStatus.PLAYING || getStatus() == PlayerStatus.PAUSED) {
                    mediaPlayer.seekTo(0);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (getStatus() == PlayerStatus.PLAYING || getStatus() == PlayerStatus.PAUSED) {
                    mediaPlayer.seekDelta(-UserPreferences.getRewindSecs() * 1000);
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                if (status == PlayerStatus.PLAYING) {
                    mediaPlayer.pause(true, true);
                }

                stateManager.stopForeground(true); // gets rid of persistent notification
                return true;
            default:
                Log.d(TAG, "Unhandled key code: " + keycode);
                // only notify the user about an unknown key event if it is actually doing something
                if (info.getPlayable() != null && info.getPlayerStatus() == PlayerStatus.PLAYING) {
                    String message = String.format(getResources().getString(R.string.unknown_media_key), keycode);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
        }
        return false;
    }

    private void startPlayingFromPreferences() {
        Observable.fromCallable(() -> DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        playable -> startPlaying(playable, false),
                        error -> {
                            Log.d(TAG, "Playable was not loaded from preferences. Stopping service.");
                            error.printStackTrace();
                            stateManager.stopService();
                        });
    }

    private void startPlaying(Playable playable, boolean allowStreamThisTime) {
        boolean localFeed = URLUtil.isContentUrl(playable.getStreamUrl());
        boolean stream = !playable.localFileAvailable() || localFeed;
        if (stream && !localFeed && !NetworkUtils.isStreamingAllowed() && !allowStreamThisTime) {
            displayStreamingNotAllowedNotification(
                    new PlaybackServiceStarter(this, playable)
                            .getIntent());
            PlaybackPreferences.writeNoMediaPlaying();
            stateManager.stopService();
            return;
        }

        if (!playable.getIdentifier().equals(PlaybackPreferences.getCurrentlyPlayingFeedMediaId())) {
            PlaybackPreferences.clearCurrentlyPlayingTemporaryPlaybackSettings();
        }

        mediaPlayer.playMediaObject(playable, stream, true, true);
        stateManager.validStartCommandWasReceived();
        stateManager.startForeground(R.id.notification_playing, notificationBuilder.build());
        recreateMediaSessionIfNeeded();
        updateNotificationAndMediaSession(playable);
        addPlayableToQueue(playable);
    }

    /**
     * Called by a mediaplayer Activity as soon as it has prepared its
     * mediaplayer.
     */
    public void setVideoSurface(SurfaceHolder sh) {
        Log.d(TAG, "Setting display");
        mediaPlayer.setVideoSurface(sh);
    }

    public void notifyVideoSurfaceAbandoned() {
        mediaPlayer.pause(true, false);
        mediaPlayer.resetVideoSurface();
        updateNotificationAndMediaSession(getPlayable());
        stateManager.stopForeground(!UserPreferences.isPersistNotify());
    }

    private final PlaybackServiceTaskManager.PSTMCallback taskManagerCallback = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {
            saveCurrentPosition(true, null, Playable.INVALID_TIME);
        }

        @Override
        public WidgetUpdater.WidgetState requestWidgetState() {
            return new WidgetUpdater.WidgetState(getPlayable(), getStatus(),
                    getCurrentPosition(), getDuration(), getCurrentPlaybackSpeed());
        }

        @Override
        public void onChapterLoaded(Playable media) {
            sendNotificationBroadcast(PlaybackServiceInterface.NOTIFICATION_TYPE_RELOAD, 0);
            updateMediaSession(mediaPlayer.getPlayerStatus());
        }
    };

    private final PlaybackServiceMediaPlayer.PSMPCallback mediaPlayerCallback = new PlaybackServiceMediaPlayer.PSMPCallback() {
        @Override
        public void statusChanged(PlaybackServiceMediaPlayer.PSMPInfo newInfo) {
            if (mediaPlayer != null) {
                currentMediaType = mediaPlayer.getCurrentMediaType();
            } else {
                currentMediaType = MediaType.UNKNOWN;
            }

            updateMediaSession(newInfo.getPlayerStatus());
            switch (newInfo.getPlayerStatus()) {
                case INITIALIZED:
                    if (mediaPlayer.getPSMPInfo().getPlayable() != null) {
                        PlaybackPreferences.writeMediaPlaying(mediaPlayer.getPSMPInfo().getPlayable());
                    }
                    updateNotificationAndMediaSession(newInfo.getPlayable());
                    break;
                case PREPARED:
                    if (mediaPlayer.getPSMPInfo().getPlayable() != null) {
                        PlaybackPreferences.writeMediaPlaying(mediaPlayer.getPSMPInfo().getPlayable());
                    }
                    taskManager.startChapterLoader(newInfo.getPlayable());
                    break;
                case PAUSED:
                    updateNotificationAndMediaSession(newInfo.getPlayable());
                    PlaybackPreferences.setCurrentPlayerStatus(PlaybackPreferences.PLAYER_STATUS_PAUSED);
                    if (!isCasting) {
                        stateManager.stopForeground(!UserPreferences.isPersistNotify());
                    }
                    cancelPositionObserver();
                    break;
                case STOPPED:
                    //writePlaybackPreferencesNoMediaPlaying();
                    //stopService();
                    break;
                case PLAYING:
                    PlaybackPreferences.setCurrentPlayerStatus(PlaybackPreferences.PLAYER_STATUS_PLAYING);
                    saveCurrentPosition(true, null, Playable.INVALID_TIME);
                    recreateMediaSessionIfNeeded();
                    updateNotificationAndMediaSession(newInfo.getPlayable());
                    setupPositionObserver();
                    stateManager.validStartCommandWasReceived();
                    stateManager.startForeground(R.id.notification_playing, notificationBuilder.build());
                    // set sleep timer if auto-enabled
                    boolean autoEnableByTime = true;
                    int fromSetting = SleepTimerPreferences.autoEnableFrom();
                    int toSetting = SleepTimerPreferences.autoEnableTo();
                    if (fromSetting != toSetting) {
                        Calendar now = new GregorianCalendar();
                        now.setTimeInMillis(System.currentTimeMillis());
                        int currentHour = now.get(Calendar.HOUR_OF_DAY);
                        autoEnableByTime = SleepTimerPreferences.isInTimeRange(fromSetting, toSetting, currentHour);
                    }
                    if (androidAutoConnected) {
                        Log.i(TAG, "Android Auto is connected, sleep timer will not be auto-enabled");
                        autoEnableByTime = false;
                    }

                    if (newInfo.getOldPlayerStatus() != null && newInfo.getOldPlayerStatus() != PlayerStatus.SEEKING
                            && SleepTimerPreferences.autoEnable() && autoEnableByTime && !sleepTimerActive()) {
                        setSleepTimer(SleepTimerPreferences.timerMillis());
                        EventBus.getDefault().post(new MessageEvent(getString(R.string.sleep_timer_enabled_label),
                                (ctx) -> disableSleepTimer(), getString(R.string.undo)));
                    }
                    loadQueueForMediaSession();
                    positionJustResetAfterPlayback = null;
                    break;
                case ERROR:
                    PlaybackPreferences.writeNoMediaPlaying();
                    stateManager.stopService();
                    break;
                default:
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    TileService.requestListeningState(getApplicationContext(),
                            new ComponentName(getApplicationContext(), QuickSettingsTileService.class));
                } catch (IllegalArgumentException e) {
                    Log.d(TAG, "Skipping quick settings tile setup");
                }
            }

            IntentUtils.sendLocalBroadcast(getApplicationContext(), ACTION_PLAYER_STATUS_CHANGED);
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_PLAYER_STATUS_CHANGED);
            bluetoothNotifyChange(newInfo, AVRCP_ACTION_META_CHANGED);
            taskManager.requestWidgetUpdate();
            EventBus.getDefault().post(new PlayerStatusEvent());
        }

        @Override
        public void shouldStop() {
            stateManager.stopForeground(!UserPreferences.isPersistNotify());
        }

        @Override
        public void onMediaChanged(boolean reloadUI) {
            Log.d(TAG, "reloadUI callback reached");
            if (reloadUI) {
                sendNotificationBroadcast(PlaybackServiceInterface.NOTIFICATION_TYPE_RELOAD, 0);
            }
            updateNotificationAndMediaSession(getPlayable());
        }

        @Override
        public void onPostPlayback(@NonNull Playable media, boolean ended, boolean skipped,
                                   boolean playingNext) {
            PlaybackService.this.onPostPlayback(media, ended, skipped, playingNext);
        }

        @Override
        public void onPlaybackStart(@NonNull Playable playable, int position) {
            taskManager.startWidgetUpdater();
            if (position != Playable.INVALID_TIME) {
                playable.setPosition(position);
            } else {
                skipIntro(playable);
            }
            playable.onPlaybackStart();
            taskManager.startPositionSaver();
        }

        @Override
        public void onPlaybackPause(Playable playable, int position) {
            taskManager.cancelPositionSaver();
            cancelPositionObserver();
            taskManager.cancelWidgetUpdater();
            if (playable instanceof FeedMedia) {
                FeedMedia media = (FeedMedia) playable;
                if (!media.getItem().getIdentifyingValue().equals(positionJustResetAfterPlayback)) {
                    // Don't store position after position is already reset
                    saveCurrentPosition(position == Playable.INVALID_TIME, playable, position);
                }
                SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, false);
            }
            playable.onPlaybackPause(getApplicationContext());
        }

        @Override
        public Playable getNextInQueue(Playable currentMedia) {
            return PlaybackService.this.getNextInQueue(currentMedia);
        }

        @Nullable
        @Override
        public Playable findMedia(@NonNull String url) {
            FeedItem item = DBReader.getFeedItemByGuidOrEpisodeUrl(null, url);
            return item != null ? item.getMedia() : null;
        }

        @Override
        public void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
            PlaybackService.this.onPlaybackEnded(mediaType, stopPlaying);
        }

        @Override
        public void ensureMediaInfoLoaded(@NonNull Playable media) {
            if (media instanceof FeedMedia && ((FeedMedia) media).getItem() == null) {
                ((FeedMedia) media).setItem(DBReader.getFeedItem(((FeedMedia) media).getItemId()));
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void playerError(PlayerErrorEvent event) {
        if (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING) {
            mediaPlayer.pause(true, false);
        }
        stateManager.stopService();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void bufferUpdate(BufferUpdateEvent event) {
        if (event.hasEnded()) {
            Playable playable = getPlayable();
            if (getPlayable() instanceof FeedMedia
                    && playable.getDuration() <= 0 && mediaPlayer.getDuration() > 0) {
                // Playable is being streamed and does not have a duration specified in the feed
                playable.setDuration(mediaPlayer.getDuration());
                DBWriter.setFeedMedia((FeedMedia) playable);
                updateNotificationAndMediaSession(playable);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void sleepTimerUpdate(SleepTimerUpdatedEvent event) {
        if (event.isOver()) {
            updateMediaSession(mediaPlayer.getPlayerStatus());
            mediaPlayer.pause(true, true);
            mediaPlayer.setVolume(1.0f, 1.0f);
            int newPosition = mediaPlayer.getPosition() - (int) SleepTimer.NOTIFICATION_THRESHOLD / 2;
            newPosition = Math.max(newPosition, 0);
            seekTo(newPosition);
        } else if (event.getTimeLeft() < SleepTimer.NOTIFICATION_THRESHOLD) {
            final float[] multiplicators = {0.1f, 0.2f, 0.3f, 0.3f, 0.3f, 0.4f, 0.4f, 0.4f, 0.6f, 0.8f};
            float multiplicator = multiplicators[Math.max(0, (int) event.getTimeLeft() / 1000)];
            Log.d(TAG, "onSleepTimerAlmostExpired: " + multiplicator);
            mediaPlayer.setVolume(multiplicator, multiplicator);
        } else if (event.isCancelled()) {
            updateMediaSession(mediaPlayer.getPlayerStatus());
            mediaPlayer.setVolume(1.0f, 1.0f);
        } else if (event.wasJustEnabled()) {
            updateMediaSession(mediaPlayer.getPlayerStatus());
        }
    }

    private Playable getNextInQueue(final Playable currentMedia) {
        if (!(currentMedia instanceof FeedMedia)) {
            Log.d(TAG, "getNextInQueue(), but playable not an instance of FeedMedia, so not proceeding");
            PlaybackPreferences.writeNoMediaPlaying();
            return null;
        }
        Log.d(TAG, "getNextInQueue()");
        FeedMedia media = (FeedMedia) currentMedia;
        if (media.getItem() == null) {
            media.setItem(DBReader.getFeedItem(media.getItemId()));
        }
        FeedItem item = media.getItem();
        if (item == null) {
            Log.w(TAG, "getNextInQueue() with FeedMedia object whose FeedItem is null");
            PlaybackPreferences.writeNoMediaPlaying();
            return null;
        }
        FeedItem nextItem;
        nextItem = DBReader.getNextInQueue(item);

        if (nextItem == null || nextItem.getMedia() == null) {
            PlaybackPreferences.writeNoMediaPlaying();
            return null;
        }

        if (!UserPreferences.isFollowQueue()) {
            Log.d(TAG, "getNextInQueue(), but follow queue is not enabled.");
            PlaybackPreferences.writeMediaPlaying(nextItem.getMedia());
            updateNotificationAndMediaSession(nextItem.getMedia());
            return null;
        }

        if (!nextItem.getMedia().localFileAvailable() && !NetworkUtils.isStreamingAllowed()
                && UserPreferences.isFollowQueue() && !nextItem.getFeed().isLocalFeed()) {
            displayStreamingNotAllowedNotification(
                    new PlaybackServiceStarter(this, nextItem.getMedia())
                            .getIntent());
            PlaybackPreferences.writeNoMediaPlaying();
            stateManager.stopService();
            return null;
        }
        return nextItem.getMedia();
    }

    /**
     * Set of instructions to be performed when playback ends.
     */
    private void onPlaybackEnded(MediaType mediaType, boolean stopPlaying) {
        Log.d(TAG, "Playback ended");
        PlaybackPreferences.clearCurrentlyPlayingTemporaryPlaybackSettings();
        if (stopPlaying) {
            taskManager.cancelPositionSaver();
            cancelPositionObserver();
            if (!isCasting) {
                stateManager.stopForeground(true);
                stateManager.stopService();
            }
        }
        if (mediaType == null) {
            sendNotificationBroadcast(PlaybackServiceInterface.NOTIFICATION_TYPE_PLAYBACK_END, 0);
        } else {
            sendNotificationBroadcast(PlaybackServiceInterface.NOTIFICATION_TYPE_RELOAD,
                    isCasting ? PlaybackServiceInterface.EXTRA_CODE_CAST :
                            (mediaType == MediaType.VIDEO) ? PlaybackServiceInterface.EXTRA_CODE_VIDEO :
                                    PlaybackServiceInterface.EXTRA_CODE_AUDIO);
        }
    }

    /**
     * This method processes the media object after its playback ended, either because it completed
     * or because a different media object was selected for playback.
     * <p>
     * Even though these tasks aren't supposed to be resource intensive, a good practice is to
     * usually call this method on a background thread.
     *
     * @param playable    the media object that was playing. It is assumed that its position
     *                    property was updated before this method was called.
     * @param ended       if true, it signals that {@param playable} was played until its end.
     *                    In such case, the position property of the media becomes irrelevant for
     *                    most of the tasks (although it's still a good practice to keep it
     *                    accurate).
     * @param skipped     if the user pressed a skip >| button.
     * @param playingNext if true, it means another media object is being loaded in place of this
     *                    one.
     *                    Instances when we'd set it to false would be when we're not following the
     *                    queue or when the queue has ended.
     */
    private void onPostPlayback(final Playable playable, boolean ended, boolean skipped,
                                boolean playingNext) {
        if (playable == null) {
            Log.e(TAG, "Cannot do post-playback processing: media was null");
            return;
        }
        Log.d(TAG, "onPostPlayback(): media=" + playable.getEpisodeTitle());

        if (!(playable instanceof FeedMedia)) {
            Log.d(TAG, "Not doing post-playback processing: media not of type FeedMedia");
            if (ended) {
                playable.onPlaybackCompleted(getApplicationContext());
            } else {
                playable.onPlaybackPause(getApplicationContext());
            }
            return;
        }
        FeedMedia media = (FeedMedia) playable;
        FeedItem item = media.getItem();
        int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
        boolean almostEnded = media.getDuration() > 0
                && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
        if (!ended && almostEnded) {
            Log.d(TAG, "smart mark as played");
        }

        boolean autoSkipped = false;
        if (autoSkippedFeedMediaId != null && autoSkippedFeedMediaId.equals(item.getIdentifyingValue())) {
            autoSkippedFeedMediaId = null;
            autoSkipped = true;
        }

        if (ended || almostEnded) {
            SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, true);
            media.onPlaybackCompleted(getApplicationContext());
        } else {
            SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, false);
            media.onPlaybackPause(getApplicationContext());
        }

        if (item != null) {
            if (ended || almostEnded
                    || autoSkipped
                    || (skipped && !UserPreferences.shouldSkipKeepEpisode())) {
                // only mark the item as played if we're not keeping it anyways
                positionJustResetAfterPlayback = item.getIdentifyingValue();
                DBWriter.markItemPlayed(item, FeedItem.PLAYED, ended || (skipped && almostEnded));
                // don't know if it actually matters to not autodownload when smart mark as played is triggered
                DBWriter.removeQueueItem(PlaybackService.this, ended, item);
                // Delete episode if enabled
                FeedPreferences.AutoDeleteAction action =
                        item.getFeed().getPreferences().getCurrentAutoDelete();
                boolean autoDeleteEnabledGlobally = UserPreferences.isAutoDelete()
                        && (!item.getFeed().isLocalFeed() || UserPreferences.isAutoDeleteLocal());
                boolean shouldAutoDelete = action == FeedPreferences.AutoDeleteAction.ALWAYS
                        || (action == FeedPreferences.AutoDeleteAction.GLOBAL && autoDeleteEnabledGlobally);
                if (shouldAutoDelete && (!item.isTagged(FeedItem.TAG_FAVORITE)
                        || !UserPreferences.shouldFavoriteKeepEpisode())) {
                    DBWriter.deleteFeedMediaOfItem(PlaybackService.this, media);
                    Log.d(TAG, "Episode Deleted");
                }
                notifyChildrenChanged(getString(R.string.queue_label));
            }
        }

        if (ended || skipped || playingNext) {
            DBWriter.addItemToPlaybackHistory(media);
        }
    }

    public void setSleepTimer(long waitingTime) {
        Log.d(TAG, "Setting sleep timer to " + waitingTime + " milliseconds");
        taskManager.setSleepTimer(waitingTime);
    }

    public void disableSleepTimer() {
        taskManager.disableSleepTimer();
    }

    private void sendNotificationBroadcast(int type, int code) {
        Intent intent = new Intent(PlaybackServiceInterface.ACTION_PLAYER_NOTIFICATION);
        intent.putExtra(PlaybackServiceInterface.EXTRA_NOTIFICATION_TYPE, type);
        intent.putExtra(PlaybackServiceInterface.EXTRA_NOTIFICATION_CODE, code);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void skipEndingIfNecessary() {
        Playable playable = mediaPlayer.getPlayable();
        if (! (playable instanceof FeedMedia)) {
            return;
        }

        int duration = getDuration();
        int remainingTime = duration - getCurrentPosition();

        FeedMedia feedMedia = (FeedMedia) playable;
        FeedPreferences preferences = feedMedia.getItem().getFeed().getPreferences();
        int skipEnd = preferences.getFeedSkipEnding();
        if (skipEnd > 0
                && skipEnd * 1000 < getDuration()
                && (remainingTime - (skipEnd * 1000) > 0)
                && ((remainingTime - skipEnd * 1000) < (getCurrentPlaybackSpeed() * 1000))) {
            Log.d(TAG, "skipEndingIfNecessary: Skipping the remaining " + remainingTime + " " + skipEnd * 1000 + " speed " + getCurrentPlaybackSpeed());
            Context context = getApplicationContext();
            String skipMesg = context.getString(R.string.pref_feed_skip_ending_toast, skipEnd);
            Toast toast = Toast.makeText(context, skipMesg, Toast.LENGTH_LONG);
            toast.show();

            this.autoSkippedFeedMediaId = feedMedia.getItem().getIdentifyingValue();
            mediaPlayer.skip();
        }
    }

    /**
     * Updates the Media Session for the corresponding status.
     *
     * @param playerStatus the current {@link PlayerStatus}
     */
    private void updateMediaSession(final PlayerStatus playerStatus) {
        PlaybackStateCompat.Builder sessionState = new PlaybackStateCompat.Builder();

        int state;
        if (playerStatus != null) {
            switch (playerStatus) {
                case PLAYING:
                    state = PlaybackStateCompat.STATE_PLAYING;
                    break;
                case PREPARED:
                case PAUSED:
                    state = PlaybackStateCompat.STATE_PAUSED;
                    break;
                case STOPPED:
                    state = PlaybackStateCompat.STATE_STOPPED;
                    break;
                case SEEKING:
                    state = PlaybackStateCompat.STATE_FAST_FORWARDING;
                    break;
                case PREPARING:
                case INITIALIZING:
                    state = PlaybackStateCompat.STATE_CONNECTING;
                    break;
                case ERROR:
                    state = PlaybackStateCompat.STATE_ERROR;
                    break;
                case INITIALIZED: // Deliberate fall-through
                case INDETERMINATE:
                default:
                    state = PlaybackStateCompat.STATE_NONE;
                    break;
            }
        } else {
            state = PlaybackStateCompat.STATE_NONE;
        }

        sessionState.setState(state, getCurrentPosition(), getCurrentPlaybackSpeed());
        long capabilities = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_FAST_FORWARD
                | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED;

        sessionState.setActions(capabilities);

        // On Android Auto, custom actions are added in the following order around the play button, if no default
        // actions are present: Near left, near right, far left, far right, additional actions panel
        PlaybackStateCompat.CustomAction.Builder rewindBuilder = new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_REWIND,
                getString(R.string.rewind_label),
                R.drawable.ic_notification_fast_rewind
        );
        WearMediaSession.addWearExtrasToAction(rewindBuilder);
        sessionState.addCustomAction(rewindBuilder.build());

        PlaybackStateCompat.CustomAction.Builder fastForwardBuilder = new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_FAST_FORWARD,
                getString(R.string.fast_forward_label),
                R.drawable.ic_notification_fast_forward
        );
        WearMediaSession.addWearExtrasToAction(fastForwardBuilder);
        sessionState.addCustomAction(fastForwardBuilder.build());

        if (UserPreferences.showPlaybackSpeedOnFullNotification()) {
            sessionState.addCustomAction(
                    new PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_CHANGE_PLAYBACK_SPEED,
                    getString(R.string.playback_speed),
                    R.drawable.ic_notification_playback_speed
                ).build()
            );
        }

        if (UserPreferences.showSleepTimerOnFullNotification()) {
            @DrawableRes int icon = R.drawable.ic_notification_sleep;
            if (sleepTimerActive()) {
                icon = R.drawable.ic_notification_sleep_off;
            }
            sessionState.addCustomAction(
                    new PlaybackStateCompat.CustomAction.Builder(CUSTOM_ACTION_TOGGLE_SLEEP_TIMER,
                            getString(R.string.sleep_timer_label), icon).build());
        }

        if (UserPreferences.showNextChapterOnFullNotification()) {
            if (getPlayable() != null && getPlayable().getChapters() != null) {
                sessionState.addCustomAction(
                        new PlaybackStateCompat.CustomAction.Builder(
                        CUSTOM_ACTION_NEXT_CHAPTER,
                        getString(R.string.next_chapter), R.drawable.ic_notification_next_chapter)
                        .build());
            }
        }

        if (UserPreferences.showSkipOnFullNotification()) {
            sessionState.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_SKIP_TO_NEXT,
                    getString(R.string.skip_episode_label),
                    R.drawable.ic_notification_skip
                ).build()
            );
        }

        WearMediaSession.mediaSessionSetExtraForWear(mediaSession);

        mediaSession.setPlaybackState(sessionState.build());
    }

    private void updateNotificationAndMediaSession(final Playable p) {
        setupNotification(p);
        updateMediaSessionMetadata(p);
    }

    private void updateMediaSessionMetadata(final Playable p) {
        if (p == null || mediaSession == null) {
            return;
        }

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, p.getFeedTitle());
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, p.getEpisodeTitle());
        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, p.getFeedTitle());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, p.getDuration());
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, p.getEpisodeTitle());
        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, p.getFeedTitle());


        if (notificationBuilder.isIconCached()) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, notificationBuilder.getCachedIcon());
        } else {
            String iconUri = p.getImageLocation();
            if (p instanceof FeedMedia) { // Don't use embedded cover etc, which Android can't load
                FeedMedia m = (FeedMedia) p;
                if (m.getItem() != null) {
                    FeedItem item = m.getItem();
                    if (item.getImageUrl() != null) {
                        iconUri = item.getImageUrl();
                    } else if (item.getFeed() != null) {
                        iconUri = item.getFeed().getImageUrl();
                    }
                }
            }
            if (!TextUtils.isEmpty(iconUri)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUri);
            }
        }

        if (stateManager.hasReceivedValidStartCommand()) {
            mediaSession.setSessionActivity(PendingIntent.getActivity(this, R.id.pending_intent_player_activity,
                    PlaybackService.getPlayerActivityIntent(this), PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0)));
            try {
                mediaSession.setMetadata(builder.build());
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Setting media session metadata", e);
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null);
                mediaSession.setMetadata(builder.build());
            }
        }
    }

    /**
     * Used by setupNotification to load notification data in another thread.
     */
    private Thread playableIconLoaderThread;

    /**
     * Prepares notification and starts the service in the foreground.
     */
    private synchronized void setupNotification(final Playable playable) {
        Log.d(TAG, "setupNotification");
        if (playableIconLoaderThread != null) {
            playableIconLoaderThread.interrupt();
        }
        if (playable == null || mediaPlayer == null) {
            Log.d(TAG, "setupNotification: playable=" + playable);
            Log.d(TAG, "setupNotification: mediaPlayer=" + mediaPlayer);
            if (!stateManager.hasReceivedValidStartCommand()) {
                stateManager.stopService();
            }
            return;
        }

        PlayerStatus playerStatus = mediaPlayer.getPlayerStatus();
        notificationBuilder.setPlayable(playable);
        notificationBuilder.setMediaSessionToken(mediaSession.getSessionToken());
        notificationBuilder.setPlayerStatus(playerStatus);
        notificationBuilder.updatePosition(getCurrentPosition(), getCurrentPlaybackSpeed());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(R.id.notification_playing, notificationBuilder.build());
        }

        if (!notificationBuilder.isIconCached()) {
            playableIconLoaderThread = new Thread(() -> {
                Log.d(TAG, "Loading notification icon");
                notificationBuilder.loadIcon();
                if (!Thread.currentThread().isInterrupted()) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(R.id.notification_playing, notificationBuilder.build());
                    }
                    updateMediaSessionMetadata(playable);
                }
            });
            playableIconLoaderThread.start();
        }
    }

    /**
     * Persists the current position and last played time of the media file.
     *
     * @param fromMediaPlayer if true, the information is gathered from the current Media Player
     *                        and {@param playable} and {@param position} become irrelevant.
     * @param playable        the playable for which the current position should be saved, unless
     *                        {@param fromMediaPlayer} is true.
     * @param position        the position that should be saved, unless {@param fromMediaPlayer} is true.
     */
    private synchronized void saveCurrentPosition(boolean fromMediaPlayer, Playable playable, int position) {
        int duration;
        if (fromMediaPlayer) {
            position = getCurrentPosition();
            duration = getDuration();
            playable = mediaPlayer.getPlayable();
        } else {
            duration = playable.getDuration();
        }
        if (position != Playable.INVALID_TIME && duration != Playable.INVALID_TIME && playable != null) {
            Log.d(TAG, "Saving current position to " + position);
            PlayableUtils.saveCurrentPosition(playable, position, System.currentTimeMillis());
        }
    }

    public boolean sleepTimerActive() {
        return taskManager.isSleepTimerActive();
    }

    public long getSleepTimerTimeLeft() {
        return taskManager.getSleepTimerTimeLeft();
    }

    private void bluetoothNotifyChange(PlaybackServiceMediaPlayer.PSMPInfo info, String whatChanged) {
        boolean isPlaying = false;

        if (info.getPlayerStatus() == PlayerStatus.PLAYING) {
            isPlaying = true;
        }

        if (info.getPlayable() != null) {
            Intent i = new Intent(whatChanged);
            i.putExtra("id", 1L);
            i.putExtra("artist", "");
            i.putExtra("album", info.getPlayable().getFeedTitle());
            i.putExtra("track", info.getPlayable().getEpisodeTitle());
            i.putExtra("playing", isPlaying);
            i.putExtra("duration", (long) info.getPlayable().getDuration());
            i.putExtra("position", (long) info.getPlayable().getPosition());
            sendBroadcast(i);
        }
    }

    /**
     * Pauses playback when the headset is disconnected and the preference is
     * set
     */
    private final BroadcastReceiver headsetDisconnected = new BroadcastReceiver() {
        private static final String TAG = "headsetDisconnected";
        private static final int UNPLUGGED = 0;
        private static final int PLUGGED = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInitialStickyBroadcast()) {
                // Don't pause playback after we just started, just because the receiver
                // delivers the current headset state (instead of a change)
                return;
            }

            if (TextUtils.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                Log.d(TAG, "Headset plug event. State is " + state);
                if (state != -1) {
                    if (state == UNPLUGGED) {
                        Log.d(TAG, "Headset was unplugged during playback.");
                    } else if (state == PLUGGED) {
                        Log.d(TAG, "Headset was plugged in during playback.");
                        unpauseIfPauseOnDisconnect(false);
                    }
                } else {
                    Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothStateUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    Log.d(TAG, "Received bluetooth connection intent");
                    unpauseIfPauseOnDisconnect(true);
                }
            }
        }
    };

    private final BroadcastReceiver audioBecomingNoisy = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // sound is about to change, eg. bluetooth -> speaker
            Log.d(TAG, "Pausing playback because audio is becoming noisy");
            pauseIfPauseOnDisconnect();
        }
    };

    /**
     * Pauses playback if PREF_PAUSE_ON_HEADSET_DISCONNECT was set to true.
     */
    private void pauseIfPauseOnDisconnect() {
        Log.d(TAG, "pauseIfPauseOnDisconnect()");
        transientPause = (mediaPlayer.getPlayerStatus() == PlayerStatus.PLAYING);
        if (UserPreferences.isPauseOnHeadsetDisconnect() && !isCasting()) {
            mediaPlayer.pause(!UserPreferences.isPersistNotify(), false);
        }
    }

    /**
     * @param bluetooth true if the event for unpausing came from bluetooth
     */
    private void unpauseIfPauseOnDisconnect(boolean bluetooth) {
        if (mediaPlayer.isAudioChannelInUse()) {
            Log.d(TAG, "unpauseIfPauseOnDisconnect() audio is in use");
            return;
        }
        if (transientPause) {
            transientPause = false;
            if (Build.VERSION.SDK_INT >= 31) {
                stateManager.stopService();
                return;
            }
            if (!bluetooth && UserPreferences.isUnpauseOnHeadsetReconnect()) {
                mediaPlayer.resume();
            } else if (bluetooth && UserPreferences.isUnpauseOnBluetoothReconnect()) {
                // let the user know we've started playback again...
                Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(500);
                }
                mediaPlayer.resume();
            }
        }
    }

    private final BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
                stateManager.stopService();
                PlaybackPreferences.writeNoMediaPlaying();
                EventBus.getDefault().post(new PlaybackServiceEvent(PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN));
                EventBus.getDefault().post(new PlayerStatusEvent());
            }
        }

    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void volumeAdaptionChanged(VolumeAdaptionChangedEvent event) {
        PlaybackVolumeUpdater playbackVolumeUpdater = new PlaybackVolumeUpdater();
        playbackVolumeUpdater.updateVolumeIfNecessary(mediaPlayer, event.getFeedId(), event.getVolumeAdaptionSetting());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void speedPresetChanged(SpeedPresetChangedEvent event) {
        if (getPlayable() instanceof FeedMedia) {
            FeedMedia playable = (FeedMedia) getPlayable();
            if (playable.getItem().getFeed().getId() == event.getFeedId()) {
                if (event.getSpeed() == SPEED_USE_GLOBAL) {
                    setSpeed(UserPreferences.getPlaybackSpeed());
                } else {
                    setSpeed(event.getSpeed());
                }
                if (event.getSkipSilence() == FeedPreferences.SkipSilence.GLOBAL) {
                    setSkipSilence(UserPreferences.getSkipSilence());
                } else {
                    setSkipSilence(event.getSkipSilence());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void skipIntroEndingPresetChanged(SkipIntroEndingChangedEvent event) {
        if (getPlayable() instanceof FeedMedia) {
            FeedMedia playable = (FeedMedia) getPlayable();
            if (playable.getItem().getFeed().getId() == event.getFeedId()) {
                if (event.getSkipEnding() != 0) {
                    FeedPreferences feedPreferences = playable.getItem().getFeed().getPreferences();
                    feedPreferences.setFeedSkipIntro(event.getSkipIntro());
                    feedPreferences.setFeedSkipEnding(event.getSkipEnding());
                }
            }
        }
    }

    public static MediaType getCurrentMediaType() {
        return currentMediaType;
    }

    public static boolean isCasting() {
        return isCasting;
    }

    public void resume() {
        mediaPlayer.resume();
        taskManager.restartSleepTimer();
    }

    public void prepare() {
        mediaPlayer.prepare();
        taskManager.restartSleepTimer();
    }

    public void pause(boolean abandonAudioFocus, boolean reinit) {
        mediaPlayer.pause(abandonAudioFocus, reinit);
    }

    public PlaybackServiceMediaPlayer.PSMPInfo getPSMPInfo() {
        return mediaPlayer.getPSMPInfo();
    }

    public PlayerStatus getStatus() {
        return mediaPlayer.getPlayerStatus();
    }

    public Playable getPlayable() {
        return mediaPlayer.getPlayable();
    }

    public void setSpeed(float speed) {
        PlaybackPreferences.setCurrentlyPlayingTemporaryPlaybackSpeed(speed);
        mediaPlayer.setPlaybackParams(speed, getCurrentSkipSilence());
    }

    public void setSkipSilence(FeedPreferences.SkipSilence skipSilence) {
        PlaybackPreferences.setCurrentlyPlayingTemporarySkipSilence(skipSilence);
        mediaPlayer.setPlaybackParams(getCurrentPlaybackSpeed(), skipSilence);
    }

    public float getCurrentPlaybackSpeed() {
        if (mediaPlayer == null) {
            return 1.0f;
        }
        return mediaPlayer.getPlaybackSpeed();
    }

    public FeedPreferences.SkipSilence getCurrentSkipSilence() {
        if (mediaPlayer == null) {
            return FeedPreferences.SkipSilence.OFF;
        }
        return mediaPlayer.getSkipSilence();
    }

    public boolean isStartWhenPrepared() {
        return mediaPlayer.isStartWhenPrepared();
    }

    public void setStartWhenPrepared(boolean s) {
        mediaPlayer.setStartWhenPrepared(s);
    }

    public void seekTo(final int t) {
        mediaPlayer.seekTo(t);
        EventBus.getDefault().post(new PlaybackPositionEvent(t, getDuration()));
    }

    private void seekDelta(final int d) {
        mediaPlayer.seekDelta(d);
    }

    /**
     * call getDuration() on mediaplayer or return INVALID_TIME if player is in
     * an invalid state.
     */
    public int getDuration() {
        if (mediaPlayer == null) {
            return Playable.INVALID_TIME;
        }
        return mediaPlayer.getDuration();
    }

    /**
     * call getCurrentPosition() on mediaplayer or return INVALID_TIME if player
     * is in an invalid state.
     */
    public int getCurrentPosition() {
        if (mediaPlayer == null) {
            return Playable.INVALID_TIME;
        }
        return mediaPlayer.getPosition();
    }

    public List<String> getAudioTracks() {
        if (mediaPlayer == null) {
            return Collections.emptyList();
        }
        return mediaPlayer.getAudioTracks();
    }

    public int getSelectedAudioTrack() {
        if (mediaPlayer == null) {
            return -1;
        }
        return mediaPlayer.getSelectedAudioTrack();
    }

    public void setAudioTrack(int track) {
        if (mediaPlayer != null) {
            mediaPlayer.setAudioTrack(track);
        }
    }

    public boolean isStreaming() {
        return mediaPlayer.isStreaming();
    }

    public Pair<Integer, Integer> getVideoSize() {
        return mediaPlayer.getVideoSize();
    }

    private void setupPositionObserver() {
        if (positionEventTimer != null) {
            positionEventTimer.dispose();
        }

        Log.d(TAG, "Setting up position observer");
        positionEventTimer = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(number -> {
                    EventBus.getDefault().post(new PlaybackPositionEvent(getCurrentPosition(), getDuration()));
                    if (Build.VERSION.SDK_INT < 29) {
                        notificationBuilder.updatePosition(getCurrentPosition(), getCurrentPlaybackSpeed());
                        NotificationManager notificationManager = (NotificationManager)
                                getSystemService(NOTIFICATION_SERVICE);
                        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            notificationManager.notify(R.id.notification_playing, notificationBuilder.build());
                        }
                    }
                    skipEndingIfNecessary();
                });
    }

    private void cancelPositionObserver() {
        if (positionEventTimer != null) {
            positionEventTimer.dispose();
        }
    }

    private void addPlayableToQueue(Playable playable) {
        if (playable instanceof FeedMedia) {
            long itemId = ((FeedMedia) playable).getItem().getId();
            DBWriter.addQueueItem(this, false, true, itemId);
            notifyChildrenChanged(getString(R.string.queue_label));
        }
    }

    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback() {

        private static final String TAG = "MediaSessionCompat";

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay()");
            PlayerStatus status = getStatus();
            if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
                resume();
            } else if (status == PlayerStatus.INITIALIZED) {
                setStartWhenPrepared(true);
                prepare();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: mediaId: " + mediaId + " extras: " + extras.toString());
            FeedMedia p = DBReader.getFeedMedia(Long.parseLong(mediaId));
            if (p != null) {
                startPlaying(p, false);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Log.d(TAG, "onPlayFromSearch  query=" + query + " extras=" + extras.toString());

            if (query.equals("")) {
                Log.d(TAG, "onPlayFromSearch called with empty query, resuming from the last position");
                startPlayingFromPreferences();
                return;
            }

            List<FeedItem> results = DBReader.searchFeedItems(0, query);
            if (results.size() > 0 && results.get(0).getMedia() != null) {
                FeedMedia media = results.get(0).getMedia();
                startPlaying(media, false);
                return;
            }
            onPlay();
        }

        @Override
        public void onPause() {
            Log.d(TAG, "onPause()");
            if (getStatus() == PlayerStatus.PLAYING) {
                pause(!UserPreferences.isPersistNotify(), false);
            }
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop()");
            mediaPlayer.stopPlayback(true);
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious()");
            seekDelta(-UserPreferences.getRewindSecs() * 1000);
        }

        @Override
        public void onRewind() {
            Log.d(TAG, "onRewind()");
            seekDelta(-UserPreferences.getRewindSecs() * 1000);
        }

        public void onNextChapter() {
            List<Chapter> chapters = mediaPlayer.getPlayable().getChapters();
            if (chapters == null) {
                // No chapters, just fallback to next episode
                mediaPlayer.skip();
                return;
            }

            int nextChapter = Chapter.getAfterPosition(chapters, mediaPlayer.getPosition()) + 1;

            if (chapters.size() < nextChapter + 1) {
                // We are on the last chapter, just fallback to the next episode
                mediaPlayer.skip();
                return;
            }

            mediaPlayer.seekTo((int) chapters.get(nextChapter).getStart());
        }

        @Override
        public void onFastForward() {
            Log.d(TAG, "onFastForward()");
            seekDelta(UserPreferences.getFastForwardSecs() * 1000);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext()");
            UiModeManager uiModeManager = (UiModeManager) getApplicationContext()
                    .getSystemService(Context.UI_MODE_SERVICE);
            if (UserPreferences.getHardwareForwardButton() == KeyEvent.KEYCODE_MEDIA_NEXT
                    || uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
                mediaPlayer.skip();
            } else {
                seekDelta(UserPreferences.getFastForwardSecs() * 1000);
            }
        }


        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo()");
            seekTo((int) pos);
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            Log.d(TAG, "onSetPlaybackSpeed()");
            setSpeed(speed);
        }

        @Override
        public boolean onMediaButtonEvent(final Intent mediaButton) {
            Log.d(TAG, "onMediaButtonEvent(" + mediaButton + ")");
            if (mediaButton != null) {
                KeyEvent keyEvent = mediaButton.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null &&
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getRepeatCount() == 0) {
                    int keyCode = keyEvent.getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        clickCount++;
                        clickHandler.removeCallbacksAndMessages(null);
                        clickHandler.postDelayed(() -> {
                            if (clickCount == 1) {
                                handleKeycode(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
                            } else if (clickCount == 2) {
                                onFastForward();
                            } else if (clickCount == 3) {
                                onRewind();
                            }
                            clickCount = 0;
                        }, ViewConfiguration.getDoubleTapTimeout());
                        return true;
                    } else {
                        return handleKeycode(keyCode, false);
                    }
                }
            }
            return false;
        }

        @Override
        public void onCustomAction(String action, Bundle extra) {
            Log.d(TAG, "onCustomAction(" + action + ")");
            if (CUSTOM_ACTION_FAST_FORWARD.equals(action)) {
                onFastForward();
            } else if (CUSTOM_ACTION_REWIND.equals(action)) {
                onRewind();
            } else if (CUSTOM_ACTION_SKIP_TO_NEXT.equals(action)) {
                mediaPlayer.skip();
            } else if (CUSTOM_ACTION_NEXT_CHAPTER.equals(action)) {
                onNextChapter();
            } else if (CUSTOM_ACTION_CHANGE_PLAYBACK_SPEED.equals(action)) {
                List<Float> selectedSpeeds = UserPreferences.getPlaybackSpeedArray();

                // If the list has zero or one element, there's nothing we can do to change the playback speed.
                if (selectedSpeeds.size() > 1) {
                    int speedPosition = selectedSpeeds.indexOf(mediaPlayer.getPlaybackSpeed());
                    float newSpeed;

                    if (speedPosition == selectedSpeeds.size() - 1) {
                        // This is the last element. Wrap instead of going over the size of the list.
                        newSpeed = selectedSpeeds.get(0);
                    } else {
                        // If speedPosition is still -1 (the user isn't using a preset), use the first preset in the
                        // list.
                        newSpeed = selectedSpeeds.get(speedPosition + 1);
                    }
                    onSetPlaybackSpeed(newSpeed);
                }
            } else if (CUSTOM_ACTION_TOGGLE_SLEEP_TIMER.equals(action)) {
                if (sleepTimerActive()) {
                    disableSleepTimer();
                } else {
                    setSleepTimer(SleepTimerPreferences.timerMillis());
                }
            }
        }
    };
}
