package de.danoeh.antennapodSA.adapter.actionbutton;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.feed.FeedMedia;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.storage.DBWriter;
import de.danoeh.antennapodSA.core.storage.DownloadRequester;

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
