package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.view.LocalDeleteModal;

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

        LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(context, Collections.singletonList(item), () -> {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable deleteRunnable = () -> DBWriter.deleteFeedMediaOfItem(context, media);

            handler.postDelayed(deleteRunnable, 5000);

            String message = context.getResources().getQuantityString(R.plurals.deleted_episode_message, 1, 1);
            EventBus.getDefault().post(new MessageEvent(
                    message,
                    ctx -> handler.removeCallbacks(deleteRunnable),
                    context.getString(R.string.undo)
            ));
        });
    }

    @Override
    public int getVisibility() {
        if (item.getMedia() != null && item.getMedia().isDownloaded()) {
            return View.VISIBLE;
        }

        return View.INVISIBLE;
    }
}
