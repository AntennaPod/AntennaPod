package de.danoeh.antennapod.core.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.PlaybackSpeedActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.VideoPlayerActivityStarter;

/**
 * Updates the state of the player widget.
 */
public abstract class WidgetUpdater {
    private static final String TAG = "WidgetUpdater";

    public static class WidgetState {
        final Playable media;
        final PlayerStatus status;
        final int position;
        final int duration;
        final float playbackSpeed;

        public WidgetState(Playable media, PlayerStatus status, int position, int duration, float playbackSpeed) {
            this.media = media;
            this.status = status;
            this.position = position;
            this.duration = duration;
            this.playbackSpeed = playbackSpeed;
        }

        public WidgetState(PlayerStatus status) {
            this(null, status, Playable.INVALID_TIME, Playable.INVALID_TIME, 1.0f);
        }
    }

    /**
     * Update the widgets with the given parameters. Must be called in a background thread.
     */
    public static void updateWidget(Context context, WidgetState widgetState) {
        if (!PlayerWidget.isEnabled(context) || widgetState == null) {
            return;
        }

        PendingIntent startMediaPlayer;
        if (widgetState.media != null && widgetState.media.getMediaType() == MediaType.VIDEO) {
            startMediaPlayer = new VideoPlayerActivityStarter(context).getPendingIntent();
        } else {
            startMediaPlayer = new MainActivityStarter(context).withOpenPlayer().getPendingIntent();
        }

        PendingIntent startPlaybackSpeedDialog = new PlaybackSpeedActivityStarter(context).getPendingIntent();

        RemoteViews views;
        views = new RemoteViews(context.getPackageName(), R.layout.player_widget);

        if (widgetState.media != null) {
            Bitmap icon;
            int iconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
            views.setOnClickPendingIntent(R.id.layout_left, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.imgvCover, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.butPlaybackSpeed, startPlaybackSpeedDialog);

            try {
                icon = Glide.with(context)
                        .asBitmap()
                        .load(widgetState.media.getImageLocation())
                        .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                        .submit(iconSize, iconSize)
                        .get(500, TimeUnit.MILLISECONDS);
                views.setImageViewBitmap(R.id.imgvCover, icon);
            } catch (Throwable tr1) {
                try {
                    icon = Glide.with(context)
                            .asBitmap()
                            .load(ImageResourceUtils.getFallbackImageLocation(widgetState.media))
                            .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                            .submit(iconSize, iconSize)
                            .get(500, TimeUnit.MILLISECONDS);
                    views.setImageViewBitmap(R.id.imgvCover, icon);
                } catch (Throwable tr2) {
                    Log.e(TAG, "Error loading the media icon for the widget", tr2);
                    views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_round);
                }
            }

            views.setTextViewText(R.id.txtvTitle, widgetState.media.getEpisodeTitle());
            views.setViewVisibility(R.id.txtvTitle, View.VISIBLE);
            views.setViewVisibility(R.id.txtNoPlaying, View.GONE);

            String progressString = getProgressString(widgetState.position,
                    widgetState.duration, widgetState.playbackSpeed);
            if (progressString != null) {
                views.setViewVisibility(R.id.txtvProgress, View.VISIBLE);
                views.setTextViewText(R.id.txtvProgress, progressString);
            }

            if (widgetState.status == PlayerStatus.PLAYING) {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_widget_pause);
                views.setContentDescription(R.id.butPlay, context.getString(R.string.pause_label));
                views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_widget_pause);
                views.setContentDescription(R.id.butPlayExtended, context.getString(R.string.pause_label));
            } else {
                views.setImageViewResource(R.id.butPlay, R.drawable.ic_widget_play);
                views.setContentDescription(R.id.butPlay, context.getString(R.string.play_label));
                views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_widget_play);
                views.setContentDescription(R.id.butPlayExtended, context.getString(R.string.play_label));
            }
            views.setOnClickPendingIntent(R.id.butPlay,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setOnClickPendingIntent(R.id.butPlayExtended,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setOnClickPendingIntent(R.id.butRew,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_REWIND));
            views.setOnClickPendingIntent(R.id.butFastForward,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));
            views.setOnClickPendingIntent(R.id.butSkip,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT));
        } else {
            // start the app if they click anything
            views.setOnClickPendingIntent(R.id.layout_left, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.butPlay, startMediaPlayer);
            views.setOnClickPendingIntent(R.id.butPlayExtended,
                    createMediaButtonIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            views.setViewVisibility(R.id.txtvProgress, View.GONE);
            views.setViewVisibility(R.id.txtvTitle, View.GONE);
            views.setViewVisibility(R.id.txtNoPlaying, View.VISIBLE);
            views.setImageViewResource(R.id.imgvCover, R.mipmap.ic_launcher_round);
            views.setImageViewResource(R.id.butPlay, R.drawable.ic_widget_play);
            views.setImageViewResource(R.id.butPlayExtended, R.drawable.ic_widget_play);
        }

        ComponentName playerWidget = new ComponentName(context, PlayerWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(playerWidget);

        for (int id : widgetIds) {
            Bundle options = manager.getAppWidgetOptions(id);
            SharedPreferences prefs = context.getSharedPreferences(PlayerWidget.PREFS_NAME, Context.MODE_PRIVATE);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int columns = getCellsForSize(minWidth);
            if (columns < 3) {
                views.setViewVisibility(R.id.layout_center, View.INVISIBLE);
            } else {
                views.setViewVisibility(R.id.layout_center, View.VISIBLE);
            }
            boolean showPlaybackSpeed = prefs.getBoolean(PlayerWidget.KEY_WIDGET_PLAYBACK_SPEED + id, false);
            boolean showRewind = prefs.getBoolean(PlayerWidget.KEY_WIDGET_REWIND + id, false);
            boolean showFastForward = prefs.getBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + id, false);
            boolean showSkip = prefs.getBoolean(PlayerWidget.KEY_WIDGET_SKIP + id, false);

            if (showPlaybackSpeed || showRewind || showSkip || showFastForward) {
                views.setInt(R.id.extendedButtonsContainer, "setVisibility", View.VISIBLE);
                views.setInt(R.id.butPlay, "setVisibility", View.GONE);
                views.setInt(R.id.butPlaybackSpeed, "setVisibility", showPlaybackSpeed ? View.VISIBLE : View.GONE);
                views.setInt(R.id.butRew, "setVisibility", showRewind ? View.VISIBLE : View.GONE);
                views.setInt(R.id.butFastForward, "setVisibility", showFastForward ? View.VISIBLE : View.GONE);
                views.setInt(R.id.butSkip, "setVisibility", showSkip ? View.VISIBLE : View.GONE);
            }

            int backgroundColor = prefs.getInt(PlayerWidget.KEY_WIDGET_COLOR + id, PlayerWidget.DEFAULT_COLOR);
            views.setInt(R.id.widgetLayout, "setBackgroundColor", backgroundColor);

            manager.updateAppWidget(id, views);
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

    /**
     * Creates an intent which fakes a mediabutton press.
     */
    private static PendingIntent createMediaButtonIntent(Context context, int eventCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
        Intent startingIntent = new Intent(context, MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(context, eventCode, startingIntent,
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    private static String getProgressString(int position, int duration, float speed) {
        if (position < 0 || duration <= 0) {
            return null;
        }
        TimeSpeedConverter converter = new TimeSpeedConverter(speed);
        if (UserPreferences.shouldShowRemainingTime()) {
            return Converter.getDurationStringLong(converter.convert(position)) + " / -"
                    + Converter.getDurationStringLong(converter.convert(Math.max(0, duration - position)));
        } else {
            return Converter.getDurationStringLong(converter.convert(position)) + " / "
                    + Converter.getDurationStringLong(converter.convert(duration));
        }
    }
}
