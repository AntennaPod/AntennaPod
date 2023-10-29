package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.util.Collections;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.view.LocalDeleteModal;

public class DeleteActionButton extends ItemActionButton {

    public DeleteActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.delete_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_delete;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(context, Collections.singletonList(item),
                () -> DBWriter.deleteFeedMediaOfItem(context, media.getId()));
    }

    @Override
    public int getVisibility() {
        if (item.getMedia() != null && (item.getMedia().isDownloaded() || item.getFeed().isLocalFeed())) {
            return View.VISIBLE;
        }

        return View.INVISIBLE;
    }
}
