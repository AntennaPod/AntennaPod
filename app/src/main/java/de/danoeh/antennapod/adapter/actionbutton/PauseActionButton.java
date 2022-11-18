package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.view.KeyEvent;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.core.util.PlaybackStatus;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

public class PauseActionButton extends ItemActionButton {

    public PauseActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.pause_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_pause;
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        if (PlaybackStatus.isCurrentlyPlaying(media)) {
            context.sendBroadcast(MediaButtonReceiver.createIntent(context, KeyEvent.KEYCODE_MEDIA_PAUSE));
        }
    }
}
