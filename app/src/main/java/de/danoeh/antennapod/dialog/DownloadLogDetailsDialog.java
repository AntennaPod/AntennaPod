package de.danoeh.antennapod.dialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.util.DownloadErrorLabel;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.greenrobot.eventbus.EventBus;

public class DownloadLogDetailsDialog extends MaterialAlertDialogBuilder {

    public DownloadLogDetailsDialog(@NonNull Context context, DownloadResult status) {
        super(context);

        String url = "unknown";
        if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
            if (media != null) {
                url = media.getDownload_url();
            }
        } else if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            Feed feed = DBReader.getFeed(status.getFeedfileId());
            if (feed != null) {
                url = feed.getDownload_url();
            }
        }

        String message = context.getString(R.string.download_successful);
        if (!status.isSuccessful()) {
            message = status.getReasonDetailed();
        }

        String messageFull = context.getString(R.string.download_log_details_message,
                context.getString(DownloadErrorLabel.from(status.getReason())), message, url);
        setTitle(R.string.download_error_details);
        setMessage(messageFull);
        setPositiveButton(android.R.string.ok, null);
        setNeutralButton(R.string.copy_to_clipboard, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(context.getString(R.string.download_error_details), messageFull);
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT < 32) {
                EventBus.getDefault().post(new MessageEvent(context.getString(R.string.copied_to_clipboard)));
            }
        });
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setTextIsSelectable(true);
        return dialog;
    }
}
