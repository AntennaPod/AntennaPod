package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.net.common.NetworkUtils;

public class DownloadActionButton extends ItemActionButton {
    private static final int TIMEOUT_NETWORK_WARN_SECONDS = 300;
    private static final int BYPASS_TYPE_NOW = 1;
    private static final int BYPASS_TYPE_LATER = 2;

    private static int bypassCellularNetworkType = 0;
    private static long bypassCellularNetworkWarningTimer = 0;

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

        long timeSinceBypass = System.currentTimeMillis() / 1000 - bypassCellularNetworkWarningTimer;
        boolean shouldBypass = timeSinceBypass < TIMEOUT_NETWORK_WARN_SECONDS;
        if (shouldBypass && bypassCellularNetworkType == BYPASS_TYPE_NOW) {
            Toast.makeText(context, context.getResources().getQuantityString(R.plurals.mobile_download_notice,
                    TIMEOUT_NETWORK_WARN_SECONDS / 60, TIMEOUT_NETWORK_WARN_SECONDS / 60), Toast.LENGTH_LONG).show();
        }
        if (NetworkUtils.isEpisodeDownloadAllowed() || shouldBypass) {
            DownloadServiceInterface.get().downloadNow(context, item, bypassCellularNetworkType == BYPASS_TYPE_NOW);
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.confirm_mobile_download_dialog_title)
                    .setPositiveButton(R.string.confirm_mobile_download_dialog_download_later,
                            (d, w) -> {
                                bypassCellularNetworkType = BYPASS_TYPE_LATER;
                                bypassCellularNetworkWarningTimer = System.currentTimeMillis() / 1000;
                                DownloadServiceInterface.get().downloadNow(context, item, false);
                            })
                    .setNeutralButton(R.string.confirm_mobile_download_dialog_allow_this_time,
                            (d, w) -> {
                                bypassCellularNetworkType = BYPASS_TYPE_NOW;
                                bypassCellularNetworkWarningTimer = System.currentTimeMillis() / 1000;
                                DownloadServiceInterface.get().downloadNow(context, item, true);
                            })
                    .setNegativeButton(R.string.cancel_label, null);
            if (NetworkUtils.isNetworkRestricted() && NetworkUtils.isVpnOverWifi()) {
                builder.setMessage(context.getString(R.string.confirm_mobile_download_dialog_message)
                        + "\n\n" + context.getString(R.string.confirm_mobile_download_dialog_message_vpn));
            } else {
                builder.setMessage(R.string.confirm_mobile_download_dialog_message);
            }

            builder.show();
        }
    }

    private boolean shouldNotDownload(@NonNull FeedMedia media) {
        boolean isDownloading = DownloadServiceInterface.get().isDownloadingEpisode(media.getDownloadUrl());
        return isDownloading || media.isDownloaded();
    }
}
