package de.danoeh.antennapod.core.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.core.app.SafeJobIntentService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Updates the state of the player widget
 */
public class PlayerWidgetJobService extends SafeJobIntentService {

    private static final String TAG = "PlayerWidgetJobService";

    private PlaybackService playbackService;
    private final Object waitForService = new Object();
    private final Object waitUsingService = new Object();

    private static final int JOB_ID = -17001;

    public static void updateWidget(Context context) {
        enqueueWork(context, PlayerWidgetJobService.class, JOB_ID, new Intent(context, PlayerWidgetJobService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (!PlayerWidget.isEnabled(getApplicationContext())) {
            return;
        }

        synchronized (waitForService) {
            if (PlaybackService.isRunning && playbackService == null) {
                bindService(new Intent(this, PlaybackService.class), mConnection, 0);
                while (playbackService == null) {
                    try {
                        waitForService.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }

        synchronized (waitUsingService) {
            updateViews();
        }

        if (playbackService != null) {
            try {
                unbindService(mConnection);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "IllegalArgumentException when trying to unbind service");
            }
        }
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

    private void updateViews() {

        ComponentName playerWidget = new ComponentName(this, PlayerWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] widgetIds = manager.getAppWidgetIds(playerWidget);
        final PendingIntent startMediaPlayer = PendingIntent.getActivity(this, R.id.pending_intent_player_activity,
                PlaybackService.getPlayerActivityIntent(this), PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews views;
        views = new RemoteViews(getPackageName(), R.layout.player_widget);

        Playable media;
        PlayerStatus status;
        if (playbackService != null) {
            media = playbackService.getPlayable();
            status = playbackService.getStatus();
        } else {
            media = Playable.PlayableUtils.createInstanceFromPreferences(getApplicationContext());
            status = PlayerStatus.STOPPED;
        }

        if (media != null) {
            Bitmap icon;
            int iconSize = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
            views.setOnClickPendingIntent(R.id.layout_left, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.imgvCover, startMediaPlayer);

            try {
                icon = Glide.with(PlayerWidgetJobService.this)
                        .asBitmap()
                        .load(ImageResourceUtils.getImageLocation(media))
                        .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                        .submit(iconSize, iconSize)
                        .get(500, TimeUnit.MILLISECONDS);
                views.setImageViewBitmap(R.id.imgvCover, icon);
            } catch (Throwable tr1) {
                try {
                    icon = Glide.with(PlayerWidgetJobService.this)
                            .asBitmap()
                            .load(ImageResourceUtils.getFallbackImageLocation(media))
                            .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                            .submit(iconSize, iconSize)
                            .get(500, TimeUnit.MILLISECONDS);
                    views.setImageViewBitmap(R.id.imgvCover, icon);
                } catch (Throwable tr2) {
                    Log.e(TAG, "Error loading the media icon for the widget", tr2);
                    views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_round);
                }
            }

            views.setTextViewText(R.id.txtvTitle, media.getEpisodeTitle());
            views.setViewVisibility(R.id.txtvTitle, View.VISIBLE);
            views.setViewVisibility(R.id.txtNoPlaying, View.GONE);

            String progressString;
            if (playbackService != null) {
                progressString = getProgressString(playbackService.getCurrentPosition(),
                        playbackService.getDuration(), playbackService.getCurrentPlaybackSpeed());
            } else {
                progressString = getProgressString(media.getPosition(), media.getDuration(), PlaybackSpeedUtils.getCurrentPlaybackSpeed(media));
            }

            if (progressString != null) {
                views.setViewVisibility(R.id.txtvProgress, View.VISIBLE);
                views.setTextViewText(R.id.txtvProgress, progressString);
            }

            if (status == PlayerStatus.PLAYING) {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_av_pause_white_48dp);
                views.setContentDescription(R.id.butPlay, getString(R.string.pause_label));
                views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_av_pause_white_48dp);
                views.setContentDescription(R.id.butPlayExtended, getString(R.string.pause_label));
            } else {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_av_play_white_48dp);
                views.setContentDescription(R.id.butPlay, getString(R.string.play_label));
                views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_av_play_white_48dp);
                views.setContentDescription(R.id.butPlayExtended, getString(R.string.play_label));
            }
            views.setOnClickPendingIntent(R.id.butPlay,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setOnClickPendingIntent(R.id.butPlayExtended,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setOnClickPendingIntent(R.id.butRew,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_REWIND));
            views.setOnClickPendingIntent(R.id.butFastForward,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));
            views.setOnClickPendingIntent(R.id.butSkip,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_NEXT));
        } else {
            // start the app if they click anything
            views.setOnClickPendingIntent(R.id.layout_left, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.butPlay, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.butPlayExtended,
                    createMediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setViewVisibility(R.id.txtvProgress, View.GONE);
            views.setViewVisibility(R.id.txtvTitle, View.GONE);
            views.setViewVisibility(R.id.txtNoPlaying, View.VISIBLE);
            views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_round);
            views.setImageViewResource(R.id.butPlay, R.drawable.ic_av_play_white_48dp);
            views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_av_play_white_48dp);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            for (int id : widgetIds) {
                Bundle options = manager.getAppWidgetOptions(id);
                SharedPreferences prefs = getSharedPreferences(PlayerWidget.PREFS_NAME, Context.MODE_PRIVATE);
                int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                int columns = getCellsForSize(minWidth);
                if (columns < 3) {
                    views.setViewVisibility(R.id.layout_center, View.INVISIBLE);
                } else {
                    views.setViewVisibility(R.id.layout_center, View.VISIBLE);
                }
                boolean showRewind = prefs.getBoolean(PlayerWidget.KEY_WIDGET_REWIND + id, false);
                boolean showFastForward = prefs.getBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + id, false);
                boolean showSkip = prefs.getBoolean(PlayerWidget.KEY_WIDGET_SKIP + id, false);

                if (showRewind || showSkip || showFastForward) {
                    views.setInt(R.id.extendedButtonsContainer, "setVisibility", View.VISIBLE);
                    views.setInt(R.id.butPlay, "setVisibility", View.GONE);
                    views.setInt(R.id.butRew, "setVisibility", showRewind ? View.VISIBLE : View.GONE);
                    views.setInt(R.id.butFastForward, "setVisibility", showFastForward ? View.VISIBLE : View.GONE);
                    views.setInt(R.id.butSkip, "setVisibility", showSkip ? View.VISIBLE : View.GONE);
                }

                int backgroundColor = prefs.getInt(PlayerWidget.KEY_WIDGET_COLOR + id, PlayerWidget.DEFAULT_COLOR);
                views.setInt(R.id.widgetLayout, "setBackgroundColor", backgroundColor);

                manager.updateAppWidget(id, views);
            }
        } else {
            manager.updateAppWidget(playerWidget, views);
        }
    }

    /**
     * Creates an intent which fakes a mediabutton press
     */
    private PendingIntent createMediaButtonIntent(int eventCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
        Intent startingIntent = new Intent(getBaseContext(), MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(this, eventCode, startingIntent, 0);
    }

    private String getProgressString(int position, int duration, float speed) {
        if (position >= 0 && duration > 0) {
            TimeSpeedConverter converter = new TimeSpeedConverter(speed);
            position = converter.convert(position);
            duration = converter.convert(duration);
            return Converter.getDurationStringLong(position) + " / "
                    + Converter.getDurationStringLong(duration);
        } else {
            return null;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connection to service established");
            if (service instanceof PlaybackService.LocalBinder) {
                synchronized (waitForService) {
                    playbackService = ((PlaybackService.LocalBinder) service).getService();
                    waitForService.notifyAll();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (waitUsingService) {
                playbackService = null;
            }
            Log.d(TAG, "Disconnected from service");
        }
    };
}
