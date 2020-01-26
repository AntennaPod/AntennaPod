package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.ImageButton;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DownloadRequester;

public abstract class ItemActionButton {
    FeedItem item;

    ItemActionButton(FeedItem item) {
        this.item = item;
    }

    @StringRes
    public abstract int getLabel();

    @AttrRes
    public abstract int getDrawable();

    public abstract void onClick(Context context);

    public int getVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    public static ItemActionButton forItem(@NonNull FeedItem item, boolean isInQueue, boolean allowStream) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return new MarkAsPlayedActionButton(item);
        }

        final boolean isDownloadingMedia = DownloadRequester.getInstance().isDownloadingFile(media);
        if (media.isDownloaded()) {
            return new PlayActionButton(item);
        } else if (isDownloadingMedia) {
            return new CancelDownloadActionButton(item);
        } else if (UserPreferences.streamOverDownload() && allowStream) {
            return new StreamActionButton(item);
        } else if (MobileDownloadHelper.userAllowedMobileDownloads()
                || !MobileDownloadHelper.userChoseAddToQueue() || isInQueue) {
            return new DownloadActionButton(item, isInQueue);
        } else {
            return new AddToQueueActionButton(item);
        }
    }

    public void configure(@NonNull ImageButton button, Context context) {
        TypedArray drawables = context.obtainStyledAttributes(new int[]{getDrawable()});

        button.setVisibility(getVisibility());
        button.setContentDescription(context.getString(getLabel()));
        button.setImageDrawable(drawables.getDrawable(0));
        button.setOnClickListener((view) -> onClick(context));

        drawables.recycle();
    }
}
