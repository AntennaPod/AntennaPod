package de.danoeh.antennapod.core.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
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
import de.danoeh.antennapod.core.preferences.PlaybackSpeedHelper;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.Converter;
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

    private static final String WIDGET_PREFS = "widget_preferences";
    private static final String WIDGET_COLOR = "widget_color";

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
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.player_widget);
        PendingIntent startMediaplayer = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this), 0);

        final PendingIntent startAppPending = PendingIntent.getActivity(this, 0,
                PlaybackService.getPlayerActivityIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT);

        boolean nothingPlaying = false;
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
            views.setOnClickPendingIntent(R.id.layout_left, startMediaplayer);

            try {
                Bitmap icon = null;
                int iconSize = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
                icon = Glide.with(PlayerWidgetJobService.this)
                        .asBitmap()
                        .load(media.getImageLocation())
                        .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                        .submit(iconSize, iconSize)
                        .get(500, TimeUnit.MILLISECONDS);
                views.setImageViewBitmap(R.id.imgvCover, icon);
            } catch (Throwable tr) {
                Log.e(TAG, "Error loading the media icon for the widget", tr);
                views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_foreground);
            }

            views.setTextViewText(R.id.txtvTitle, media.getEpisodeTitle());
            views.setViewVisibility(R.id.txtvTitle, View.VISIBLE);
            views.setViewVisibility(R.id.txtNoPlaying, View.GONE);

            String progressString;
            if (playbackService != null) {
                progressString = getProgressString(playbackService.getCurrentPosition(),
                        playbackService.getDuration(), playbackService.getCurrentPlaybackSpeed());
            } else {
                progressString = getProgressString(media.getPosition(), media.getDuration(), PlaybackSpeedHelper.getCurrentPlaybackSpeed(media));
            }

            if (progressString != null) {
                views.setViewVisibility(R.id.txtvProgress, View.VISIBLE);
                views.setTextViewText(R.id.txtvProgress, progressString);
            }

            if (status == PlayerStatus.PLAYING) {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_pause_white_24dp);
                if (Build.VERSION.SDK_INT >= 15) {
                    views.setContentDescription(R.id.butPlay, getString(R.string.pause_label));
                }
            } else {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_play_arrow_white_24dp);
                if (Build.VERSION.SDK_INT >= 15) {
                    views.setContentDescription(R.id.butPlay, getString(R.string.play_label));
                }
            }
            views.setOnClickPendingIntent(R.id.butPlay, createMediaButtonIntent());
        } else {
            nothingPlaying = true;
        }

        if (nothingPlaying) {
            // start the app if they click anything
            views.setOnClickPendingIntent(R.id.layout_left, startAppPending);
            views.setOnClickPendingIntent(R.id.butPlay, startAppPending);
            views.setViewVisibility(R.id.txtvProgress, View.GONE);
            views.setViewVisibility(R.id.txtvTitle, View.GONE);
            views.setViewVisibility(R.id.txtNoPlaying, View.VISIBLE);
            views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_foreground);
            views.setImageViewResource(R.id.butPlay, R.drawable.ic_play_arrow_white_24dp);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            for (int id : widgetIds) {
                Bundle options = manager.getAppWidgetOptions(id);
                int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                int columns = getCellsForSize(minWidth);
                if (columns < 3) {
                    views.setViewVisibility(R.id.layout_center, View.INVISIBLE);
                } else {
                    views.setViewVisibility(R.id.layout_center, View.VISIBLE);
                }
                SharedPreferences prefs = getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
                int backgroundColor = prefs.getInt(WIDGET_COLOR + id, 0x262C31);
                views.setInt(R.id.widgetLayout,"setBackgroundColor", backgroundColor);
                manager.updateAppWidget(id, views);
            }
        } else {
            manager.updateAppWidget(playerWidget, views);
        }
    }

    /**
     * Creates an intent which fakes a mediabutton press
     */
    private PendingIntent createMediaButtonIntent() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        Intent startingIntent = new Intent(getBaseContext(), MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(this, 0, startingIntent, 0);
    }

    private String getProgressString(int position, int duration, float speed) {
        if (position > 0 && duration > 0) {
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
