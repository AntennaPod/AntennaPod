package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;

public class CancelDownloadActionButton extends ItemActionButton {

    public CancelDownloadActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.cancel_download_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_cancel;
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        DownloadServiceInterface.get().cancel(context, media);
        if (UserPreferences.isEnableAutodownload()) {
            item.disableAutoDownload();
            DBWriter.setFeedItem(item);
        }
    }
}
