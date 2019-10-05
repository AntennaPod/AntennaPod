package de.danoeh.antennapod.core.service.playback;

import android.annotation.TargetApi;
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
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntList;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

public class PlaybackServiceNotificationBuilder extends NotificationCompat.Builder {
    private static final String TAG = "PlaybackSrvNotification";
    private static Bitmap defaultIcon = null;

    private Context context;
    private boolean actionsInitialized = false;

    public PlaybackServiceNotificationBuilder(@NonNull Context context) {
        super(context, NotificationUtils.CHANNEL_ID_PLAYING);
        this.context = context;

        final int smallIcon = ClientConfig.playbackServiceCallbacks.getNotificationIconResource(context);

        final PendingIntent pIntent = PendingIntent.getActivity(context, 0,
                PlaybackService.getPlayerActivityIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);

        setContentTitle(context.getString(R.string.app_name));
        setContentText("Service is running"); // Just in case the notification is not updated (should not occur)
        setOngoing(false);
        setContentIntent(pIntent);
        setWhen(0); // we don't need the time
        setSmallIcon(smallIcon);
        setPriority(NotificationCompat.PRIORITY_MIN);
        setOnlyAlertOnce(true);
    }

    public void setMetadata(Playable playable, MediaSessionCompat.Token mediaSessionToken, PlayerStatus playerStatus, boolean isCasting) {
        Log.v(TAG, "notificationSetupTask: playerStatus=" + playerStatus);
        setContentTitle(playable.getFeedTitle());
        setContentText(playable.getEpisodeTitle());
        setPriority(UserPreferences.getNotifyPriority());
        addActions(mediaSessionToken, playerStatus, isCasting);
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        setColor(NotificationCompat.COLOR_DEFAULT);
    }

    public void updatePosition(int position,float speed) {
        TimeSpeedConverter converter = new TimeSpeedConverter(speed);
        setSubText(Converter.getDurationStringLong(converter.convert(position)));
    }

    public boolean isIconCached(Playable playable) {
        int iconSize = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        try {
            Bitmap icon = Glide.with(context)
                    .asBitmap()
                    .load(playable.getImageLocation())
                    .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                    .apply(new RequestOptions()
                            .centerCrop()
                            .onlyRetrieveFromCache(true))
                    .submit(iconSize, iconSize)
                    .get();
            return icon != null;
        } catch (Throwable tr) {
            return false;
        }
    }

    public void loadIcon(Playable playable) {
        Bitmap icon = null;
        int iconSize = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        try {
            icon = Glide.with(context)
                    .asBitmap()
                    .load(playable.getImageLocation())
                    .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                    .apply(new RequestOptions().centerCrop())
                    .submit(iconSize, iconSize)
                    .get();
        } catch (Throwable tr) {
            Log.e(TAG, "Error loading the media icon for the notification", tr);
        }

        if (icon == null) {
            loadDefaultIcon();
        } else {
            setLargeIcon(icon);
        }
    }

    public void loadDefaultIcon() {
        if (defaultIcon == null) {
            defaultIcon = getBitmap(context, R.drawable.notification_default_large_icon);
        }
        setLargeIcon(defaultIcon);
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

    private void addActions(MediaSessionCompat.Token mediaSessionToken, PlayerStatus playerStatus, boolean isCasting) {
        if (actionsInitialized) {
            throw new IllegalStateException("Notification actions must not be added multiple times");
        }
        actionsInitialized = true;
        IntList compactActionList = new IntList();

        int numActions = 0; // we start and 0 and then increment by 1 for each call to addAction

        if (isCasting) {
            Intent stopCastingIntent = new Intent(context, PlaybackService.class);
            stopCastingIntent.putExtra(PlaybackService.EXTRA_CAST_DISCONNECT, true);
            PendingIntent stopCastingPendingIntent = PendingIntent.getService(context,
                    numActions, stopCastingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            addAction(R.drawable.ic_notification_cast_off,
                    context.getString(R.string.cast_disconnect_label),
                    stopCastingPendingIntent);
            numActions++;
        }

        // always let them rewind
        PendingIntent rewindButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_REWIND, numActions);
        addAction(R.drawable.ic_notification_fast_rewind, context.getString(R.string.rewind_label), rewindButtonPendingIntent);
        if (UserPreferences.showRewindOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (playerStatus == PlayerStatus.PLAYING) {
            PendingIntent pauseButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PAUSE, numActions);
            addAction(R.drawable.ic_notification_pause, //pause action
                    context.getString(R.string.pause_label),
                    pauseButtonPendingIntent);
            compactActionList.add(numActions++);
        } else {
            PendingIntent playButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PLAY, numActions);
            addAction(R.drawable.ic_notification_play, //play action
                    context.getString(R.string.play_label),
                    playButtonPendingIntent);
            compactActionList.add(numActions++);
        }

        // ff follows play, then we have skip (if it's present)
        PendingIntent ffButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, numActions);
        addAction(R.drawable.ic_notification_fast_forward, context.getString(R.string.fast_forward_label), ffButtonPendingIntent);
        if (UserPreferences.showFastForwardOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (UserPreferences.isFollowQueue()) {
            PendingIntent skipButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_NEXT, numActions);
            addAction(R.drawable.ic_notification_skip,
                    context.getString(R.string.skip_episode_label),
                    skipButtonPendingIntent);
            if (UserPreferences.showSkipOnCompactNotification()) {
                compactActionList.add(numActions);
            }
            numActions++;
        }

        PendingIntent stopButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_STOP, numActions);
        setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionToken)
                .setShowActionsInCompactView(compactActionList.toArray())
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
}