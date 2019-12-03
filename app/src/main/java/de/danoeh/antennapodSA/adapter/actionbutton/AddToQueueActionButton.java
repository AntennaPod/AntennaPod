package de.danoeh.antennapodSA.adapter.actionbutton;

import android.content.Context;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.feed.FeedItem;

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
