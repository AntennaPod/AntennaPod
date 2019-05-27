package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
import android.widget.Toast;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;

class CancelDownloadActionButton extends ItemActionButton {

    CancelDownloadActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.cancel_download_label;
    }

    @Override
    @AttrRes
    public int getDrawable() {
        return R.attr.navigation_cancel;
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        DownloadRequester.getInstance().cancelDownload(context, media);
        if (UserPreferences.isEnableAutodownload()) {
            DBWriter.setFeedItemAutoDownload(media.getItem(), false);
            Toast.makeText(context, R.string.download_canceled_autodownload_enabled_msg, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, R.string.download_canceled_msg, Toast.LENGTH_LONG).show();
        }
    }
}
