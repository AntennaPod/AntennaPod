package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UsageStatistics;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class DownloadActionButton extends ItemActionButton {

    public DownloadActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.download_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_download;
    }

    @Override
    public int getVisibility() {
        return item.getFeed().isLocalFeed() ? View.INVISIBLE : View.VISIBLE;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null || shouldNotDownload(media)) {
            return;
        }

        UsageStatistics.logAction(UsageStatistics.ACTION_DOWNLOAD);

        if (NetworkUtils.isEpisodeDownloadAllowed()) {
            DownloadServiceInterface.get().downloadNow(context, item, false);
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.confirm_mobile_download_dialog_title)
                    .setPositiveButton(R.string.confirm_mobile_download_dialog_download_later,
                            (d, w) -> DownloadServiceInterface.get().downloadNow(context, item, false))
                    .setNeutralButton(R.string.confirm_mobile_download_dialog_allow_this_time,
                            (d, w) -> DownloadServiceInterface.get().downloadNow(context, item, true))
                    .setNegativeButton(R.string.cancel_label, null);
            if (NetworkUtils.isNetworkRestricted() && NetworkUtils.isVpnOverWifi()) {
                builder.setMessage(R.string.confirm_mobile_download_dialog_message_vpn);
            } else {
                builder.setMessage(R.string.confirm_mobile_download_dialog_message);
            }

            builder.show();
        }
    }

    private boolean shouldNotDownload(@NonNull FeedMedia media) {
        boolean isDownloading = DownloadServiceInterface.get().isDownloadingEpisode(media.getDownload_url());
        return isDownloading || media.isDownloaded();
    }
}
