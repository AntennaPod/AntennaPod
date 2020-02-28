package de.danoeh.antennapod.dialog;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;

public class StreamingConfirmationDialog {
    private final Context context;
    private final FeedMedia media;

    public StreamingConfirmationDialog(Context context, FeedMedia media) {
        this.context = context;
        this.media = media;
    }

    public void show() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.stream_label)
                .setMessage(R.string.confirm_mobile_streaming_notification_message)
                .setPositiveButton(R.string.stream_label, (dialog, which) -> {
                    new PlaybackServiceStarter(context, media)
                            .callEvenIfRunning(true)
                            .startWhenPrepared(true)
                            .shouldStream(true)
                            .shouldStreamThisTime(true)
                            .start();
                })
                .setNegativeButton(R.string.cancel_label, null)
                .setNeutralButton(R.string.confirm_mobile_streaming_button_always, (dialog, which) -> {
                    UserPreferences.setAllowMobileStreaming(true);
                    DBTasks.playMedia(context, media, false, true, true);
                })
                .show();
    }
}
