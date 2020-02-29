package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;

public class StreamingConfirmationDialog {
    private final Context context;
    private final FeedMedia media;

    public StreamingConfirmationDialog(Context context, FeedMedia media) {
        this.context = context;
        this.media = media;
    }

    public void show() {
        View view = View.inflate(context, R.layout.checkbox_do_not_show_again, null);
        CheckBox checkDoNotShowAgain = view.findViewById(R.id.checkbox_do_not_show_again);

        new AlertDialog.Builder(context)
                .setTitle(R.string.stream_label)
                .setMessage(R.string.confirm_mobile_streaming_notification_message)
                .setView(view)
                .setPositiveButton(R.string.stream_label, (dialog, which) -> {
                    if (checkDoNotShowAgain.isChecked()) {
                        UserPreferences.setAllowMobileStreaming(true);
                    }
                    new PlaybackServiceStarter(context, media)
                            .callEvenIfRunning(true)
                            .startWhenPrepared(true)
                            .shouldStream(true)
                            .shouldStreamThisTime(true)
                            .start();
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }
}
