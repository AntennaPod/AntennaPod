package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;

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
    @AttrRes
    public int getDrawable() {
        return R.attr.content_new;
    }

    @Override
    public void onClick(Context context) {
        MobileDownloadHelper.confirmMobileDownload(context, item);
    }
}
