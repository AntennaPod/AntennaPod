package de.danoeh.antennapod.core.widget;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.SafeJobIntentService;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlayableUtils;

public class WidgetUpdaterJobService extends SafeJobIntentService {
    private static final int JOB_ID = -17001;

    /**
     * Loads the current media from the database and updates the widget in a background job.
     */
    public static void performBackgroundUpdate(Context context) {
        enqueueWork(context, WidgetUpdaterJobService.class,
                WidgetUpdaterJobService.JOB_ID, new Intent(context, WidgetUpdaterJobService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Playable media = PlayableUtils.createInstanceFromPreferences(getApplicationContext());
        if (media != null) {
            WidgetUpdater.updateWidget(this, new WidgetUpdater.WidgetState(media, PlayerStatus.STOPPED,
                    media.getPosition(), media.getDuration(), PlaybackSpeedUtils.getCurrentPlaybackSpeed(media),
                    PlaybackPreferences.getCurrentEpisodeIsStream()));
        } else {
            WidgetUpdater.updateWidget(this, new WidgetUpdater.WidgetState(PlayerStatus.STOPPED));
        }
    }
}