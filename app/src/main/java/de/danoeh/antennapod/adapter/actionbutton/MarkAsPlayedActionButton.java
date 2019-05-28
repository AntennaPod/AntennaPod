package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.StringRes;
import android.view.View;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBWriter;

class MarkAsPlayedActionButton extends ItemActionButton {

    MarkAsPlayedActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.mark_read_label;
    }

    @Override
    @AttrRes
    public int getDrawable() {
        return R.attr.navigation_accept;
    }

    @Override
    public void onClick(Context context) {
        if (!item.isPlayed()) {
            DBWriter.markItemPlayed(item, FeedItem.PLAYED, true);
        }
    }

    @Override
    public int getVisibility() {
        return (item.isPlayed()) ? View.INVISIBLE : View.VISIBLE;
    }
}
