package de.danoeh.antennapod.core.service.playback;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.core.util.playback.Playable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.ArrayUtils;

public class PlaybackServiceNotificationBuilder {
    private static final String TAG = "PlaybackSrvNotification";
    private static Bitmap defaultIcon = null;

    private Context context;
    private Playable playable;
    private MediaSessionCompat.Token mediaSessionToken;
    private PlayerStatus playerStatus;
    private boolean isCasting;
    private Bitmap icon;
    private String position;

    public PlaybackServiceNotificationBuilder(@NonNull Context context) {
        this.context = context;
    }

    public void setPlayable(Playable playable) {
        if (playable != this.playable) {
            clearCache();
        }
        this.playable = playable;
    }

    private void clearCache() {
        this.icon = null;
        this.position = null;
    }

    public void updatePosition(int position, float speed) {
        TimeSpeedConverter converter = new TimeSpeedConverter(speed);
        this.position = Converter.getDurationStringLong(converter.convert(position));
    }

    public boolean isIconCached() {
        return icon != null;
    }

    public void loadIcon() {
        int iconSize = (int) (128 * context.getResources().getDisplayMetrics().density);
        try {
            icon = Glide.with(context)
                    .asBitmap()
                    .load(ImageResourceUtils.getImageLocation(playable))
                    .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                    .apply(new RequestOptions().centerCrop())
                    .submit(iconSize, iconSize)
                    .get();
        } catch (ExecutionException e) {
            try {
                icon = Glide.with(context)
                        .asBitmap()
                        .load(ImageResourceUtils.getFallbackImageLocation(playable))
                        .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                        .apply(new RequestOptions().centerCrop())
                        .submit(iconSize, iconSize)
                        .get();
            } catch (Throwable tr) {
                Log.e(TAG, "Error loading the media icon for the notification", tr);
            }
        } catch (Throwable tr) {
            Log.e(TAG, "Error loading the media icon for the notification", tr);
        }
    }

    private Bitmap getDefaultIcon() {
        if (defaultIcon == null) {
            defaultIcon = getBitmap(context, R.mipmap.ic_launcher);
        }
        return defaultIcon;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            return null;
        }
    }

    public Notification build() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                NotificationUtils.CHANNEL_ID_PLAYING);

        if (playable != null) {
            notification.setContentTitle(playable.getFeedTitle());
            notification.setContentText(playable.getEpisodeTitle());
            addActions(notification, mediaSessionToken, playerStatus, isCasting);

            if (icon != null) {
                notification.setLargeIcon(icon);
            } else {
                notification.setLargeIcon(getDefaultIcon());
            }

            if (Build.VERSION.SDK_INT < 29) {
                notification.setSubText(position);
            }
        } else {
            notification.setContentTitle(context.getString(R.string.app_name));
            notification.setContentText("Loading. If this does not go away, play any episode and contact us.");
        }

        notification.setContentIntent(getPlayerActivityPendingIntent());
        notification.setWhen(0);
        notification.setSmallIcon(R.drawable.ic_notification);
        notification.setOngoing(false);
        notification.setOnlyAlertOnce(true);
        notification.setShowWhen(false);
        notification.setPriority(UserPreferences.getNotifyPriority());
        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notification.setColor(NotificationCompat.COLOR_DEFAULT);
        return notification.build();
    }

    private PendingIntent getPlayerActivityPendingIntent() {
        return PendingIntent.getActivity(context, R.id.pending_intent_player_activity,
                PlaybackService.getPlayerActivityIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addActions(NotificationCompat.Builder notification, MediaSessionCompat.Token mediaSessionToken,
                            PlayerStatus playerStatus, boolean isCasting) {
        ArrayList<Integer> compactActionList = new ArrayList<>();

        int numActions = 0; // we start and 0 and then increment by 1 for each call to addAction

        if (isCasting) {
            Intent stopCastingIntent = new Intent(context, PlaybackService.class);
            stopCastingIntent.putExtra(PlaybackService.EXTRA_CAST_DISCONNECT, true);
            PendingIntent stopCastingPendingIntent = PendingIntent.getService(context,
                    numActions, stopCastingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification.addAction(R.drawable.ic_notification_cast_off,
                    context.getString(R.string.cast_disconnect_label),
                    stopCastingPendingIntent);
            numActions++;
        }

        // always let them rewind
        PendingIntent rewindButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_REWIND, numActions);
        notification.addAction(R.drawable.ic_notification_fast_rewind, context.getString(R.string.rewind_label),
                rewindButtonPendingIntent);
        if (UserPreferences.showRewindOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (playerStatus == PlayerStatus.PLAYING) {
            PendingIntent pauseButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PAUSE, numActions);
            notification.addAction(R.drawable.ic_notification_pause, //pause action
                    context.getString(R.string.pause_label),
                    pauseButtonPendingIntent);
        } else {
            PendingIntent playButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PLAY, numActions);
            notification.addAction(R.drawable.ic_notification_play, //play action
                    context.getString(R.string.play_label),
                    playButtonPendingIntent);
        }
        compactActionList.add(numActions++);

        // ff follows play, then we have skip (if it's present)
        PendingIntent ffButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, numActions);
        notification.addAction(R.drawable.ic_notification_fast_forward, context.getString(R.string.fast_forward_label),
                ffButtonPendingIntent);
        if (UserPreferences.showFastForwardOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (UserPreferences.isFollowQueue()) {
            PendingIntent skipButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_NEXT, numActions);
            notification.addAction(R.drawable.ic_notification_skip,
                    context.getString(R.string.skip_episode_label),
                    skipButtonPendingIntent);
            if (UserPreferences.showSkipOnCompactNotification()) {
                compactActionList.add(numActions);
            }
            numActions++;
        }

        PendingIntent stopButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_STOP, numActions);
        notification.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionToken)
                .setShowActionsInCompactView(ArrayUtils.toPrimitive(compactActionList.toArray(new Integer[0])))
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopButtonPendingIntent));
    }

    private PendingIntent getPendingIntentForMediaAction(int keycodeValue, int requestCode) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction("MediaCode" + keycodeValue);
        intent.putExtra(MediaButtonReceiver.EXTRA_KEYCODE, keycodeValue);

        if (Build.VERSION.SDK_INT >= 26) {
            return PendingIntent.getForegroundService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    public void setMediaSessionToken(MediaSessionCompat.Token mediaSessionToken) {
        this.mediaSessionToken = mediaSessionToken;
    }

    public void setPlayerStatus(PlayerStatus playerStatus) {
        this.playerStatus = playerStatus;
    }

    public void setCasting(boolean casting) {
        isCasting = casting;
    }

    public PlayerStatus getPlayerStatus() {
        return playerStatus;
    }
}