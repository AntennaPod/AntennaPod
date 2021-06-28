package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;

class AddToQueueActionButton extends ItemActionButton {

    AddToQueueActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.add_to_queue_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_add;
    }

    @Override
    public void onClick(Context context) {
        MobileDownloadHelper.confirmMobileDownload(context, item);
    }
}
