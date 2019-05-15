package de.danoeh.antennapod.core.service.playback;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.util.IntList;
import de.danoeh.antennapod.core.util.playback.Playable;

public class PlaybackServiceNotificationBuilder extends NotificationCompat.Builder {
    private static final String TAG = "PlaybackSrvNotification";
    private Context context;

    public PlaybackServiceNotificationBuilder(@NonNull Context context, @NonNull String channelId) {
        super(context, channelId);
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
    }

    public void setup(Playable playable, MediaSessionCompat.Token mediaSessionToken, PlayerStatus playerStatus, boolean isCasting) {
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
            icon = BitmapFactory.decodeResource(context.getResources(),
                    ClientConfig.playbackServiceCallbacks.getNotificationIconResource(context));
        }

        Log.v(TAG, "notificationSetupTask: playerStatus=" + playerStatus);

        String contentText = playable.getEpisodeTitle();
        String contentTitle = playable.getFeedTitle();

        setContentTitle(contentTitle);
        setContentText(contentText);
        setPriority(UserPreferences.getNotifyPriority());
        setLargeIcon(icon);
        IntList compactActionList = new IntList();

        int numActions = 0; // we start and 0 and then increment by 1 for each call to addAction

        if (isCasting) {
            Intent stopCastingIntent = new Intent(context, PlaybackService.class);
            stopCastingIntent.putExtra(PlaybackService.EXTRA_CAST_DISCONNECT, true);
            PendingIntent stopCastingPendingIntent = PendingIntent.getService(context,
                    numActions, stopCastingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            addAction(R.drawable.ic_media_cast_disconnect,
                    context.getString(R.string.cast_disconnect_label),
                    stopCastingPendingIntent);
            numActions++;
        }

        // always let them rewind
        PendingIntent rewindButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_REWIND, numActions);
        addAction(android.R.drawable.ic_media_rew, context.getString(R.string.rewind_label), rewindButtonPendingIntent);
        if (UserPreferences.showRewindOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (playerStatus == PlayerStatus.PLAYING) {
            PendingIntent pauseButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PAUSE, numActions);
            addAction(android.R.drawable.ic_media_pause, //pause action
                    context.getString(R.string.pause_label),
                    pauseButtonPendingIntent);
            compactActionList.add(numActions++);
        } else {
            PendingIntent playButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_PLAY, numActions);
            addAction(android.R.drawable.ic_media_play, //play action
                    context.getString(R.string.play_label),
                    playButtonPendingIntent);
            compactActionList.add(numActions++);
        }

        // ff follows play, then we have skip (if it's present)
        PendingIntent ffButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, numActions);
        addAction(android.R.drawable.ic_media_ff, context.getString(R.string.fast_forward_label), ffButtonPendingIntent);
        if (UserPreferences.showFastForwardOnCompactNotification()) {
            compactActionList.add(numActions);
        }
        numActions++;

        if (UserPreferences.isFollowQueue()) {
            PendingIntent skipButtonPendingIntent = getPendingIntentForMediaAction(
                    KeyEvent.KEYCODE_MEDIA_NEXT, numActions);
            addAction(android.R.drawable.ic_media_next,
                    context.getString(R.string.skip_episode_label),
                    skipButtonPendingIntent);
            if (UserPreferences.showSkipOnCompactNotification()) {
                compactActionList.add(numActions);
            }
            numActions++;
        }

        PendingIntent stopButtonPendingIntent = getPendingIntentForMediaAction(
                KeyEvent.KEYCODE_MEDIA_STOP, numActions);
        setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSessionToken)
            .setShowActionsInCompactView(compactActionList.toArray())
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopButtonPendingIntent));
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        setColor(NotificationCompat.COLOR_DEFAULT);
    }


    private PendingIntent getPendingIntentForMediaAction(int keycodeValue, int requestCode) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.putExtra(MediaButtonReceiver.EXTRA_KEYCODE, keycodeValue);
        return PendingIntent .getService(context, requestCode,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
