package de.danoeh.antennapod.playback.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaButtonReceiver;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@OptIn(markerClass = UnstableApi.class)
public class Media3MediaButtonReceiver extends MediaButtonReceiver {
    private static final String TAG = "Media3MediaButtonRecv";

    @Override
    public void onReceive(Context context, @Nullable Intent intent) {
        KeyEvent keyEvent = intent != null ? intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) : null;
        if (keyEvent == null || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                || keyEvent.getAction() != KeyEvent.ACTION_DOWN || keyEvent.getRepeatCount() != 0) {
            super.onReceive(context, intent);
            return;
        }
        Log.d(TAG, "onReceive: " + KeyEvent.keyCodeToString(keyEvent.getKeyCode())
                + ", isRunning=" + PlaybackService.isRunning);

        if (PlaybackService.isRunning) {
            Intent serviceIntent = new Intent(intent);
            serviceIntent.setComponent(new ComponentName(context, Media3PlaybackService.class));
            try {
                context.startService(serviceIntent);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unable to start service", e);
            }
            return;
        }

        if (!isPlayKey(keyEvent.getKeyCode())
                || PlaybackPreferences.getCurrentlyPlayingFeedMediaId() != PlaybackPreferences.NO_MEDIA_PLAYING) {
            super.onReceive(context, intent);
            return;
        }

        PendingResult pendingResult = goAsync();
        Single.fromCallable(() -> DBReader.getPausedQueue(1))
                .subscribeOn(Schedulers.io())
                .doFinally(pendingResult::finish)
                .subscribe(resumable -> {
                    Log.d(TAG, "Resumable: " + resumable.size());
                    if (!resumable.isEmpty()) {
                        handleIntentAndMaybeStartTheService(context, intent);
                    }
                }, error -> Log.e(TAG, "Unable to load media for resumption", error));
    }

    private static boolean isPlayKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_HEADSETHOOK;
    }
}
