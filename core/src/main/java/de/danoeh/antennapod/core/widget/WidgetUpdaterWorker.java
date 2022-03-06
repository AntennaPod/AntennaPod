package de.danoeh.antennapod.core.widget;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.util.playback.PlayableUtils;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;

public class WidgetUpdaterWorker extends Worker {

    private static final String TAG = "WidgetUpdaterWorker";

    public WidgetUpdaterWorker(@NonNull final Context context,
                               @NonNull final WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void enqueueWork(final Context context) {
        final WorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdaterWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            updateWidget();
        } catch (final Exception e) {
            Log.d(TAG, "Failed to update AntennaPod widget: ", e);
            return Result.failure();
        }
        return Result.success();
    }

    /**
     * Loads the current media from the database and updates the widget in a background job.
     */
    private void updateWidget() {
        final Playable media = PlayableUtils.createInstanceFromPreferences(getApplicationContext());
        if (media != null) {
            WidgetUpdater.updateWidget(getApplicationContext(),
                    new WidgetUpdater.WidgetState(media, PlayerStatus.STOPPED,
                            media.getPosition(), media.getDuration(),
                            PlaybackSpeedUtils.getCurrentPlaybackSpeed(media)));
        } else {
            WidgetUpdater.updateWidget(getApplicationContext(),
                    new WidgetUpdater.WidgetState(PlayerStatus.STOPPED));
        }
    }
}
