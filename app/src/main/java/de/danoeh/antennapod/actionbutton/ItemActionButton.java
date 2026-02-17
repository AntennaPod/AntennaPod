package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.view.View;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.danoeh.antennapod.playback.service.PlaybackStatus;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class ItemActionButton {
    FeedItem item;
    private final boolean queueContext;
    private static final String DEBUG_LOG_FILE = "autoplay_debug.log";

    ItemActionButton(FeedItem item, boolean queueContext) {
        this.item = item;
        this.queueContext = queueContext;
    }

    @StringRes
    public abstract int getLabel();

    @DrawableRes
    public abstract int getDrawable();

    public abstract void onClick(Context context);

    protected boolean isQueueContext() {
        return queueContext;
    }

    public int getVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    public static ItemActionButton forItem(@NonNull FeedItem item) {
        return forItem(item, false);
    }

    @NonNull
    public static ItemActionButton forItem(@NonNull FeedItem item, boolean queueContext) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return new MarkAsPlayedActionButton(item, queueContext);
        }

        final boolean isDownloadingMedia = DownloadServiceInterface.get().isDownloadingEpisode(media.getDownloadUrl());
        if (PlaybackStatus.isCurrentlyPlaying(media)) {
            return new PauseActionButton(item, queueContext);
        } else if (item.getFeed().isLocalFeed()) {
            return new PlayLocalActionButton(item, queueContext);
        } else if (media.isDownloaded()) {
            return new PlayActionButton(item, queueContext);
        } else if (isDownloadingMedia) {
            return new CancelDownloadActionButton(item, queueContext);
        } else if (UserPreferences.isStreamOverDownload()) {
            return new StreamActionButton(item, queueContext);
        } else {
            return new DownloadActionButton(item, queueContext);
        }
    }

    public void configure(@NonNull View button, @NonNull ImageView icon, Context context) {
        button.setVisibility(getVisibility());
        button.setContentDescription(context.getString(getLabel()));
        button.setOnClickListener((view) -> onClick(context));
        icon.setImageResource(getDrawable());
    }

    protected void logPlaybackDebug(Context context, String message) {
        Log.d(getClass().getSimpleName(), message);
        if (context == null) {
            return;
        }
        try {
            File logFile = new File(context.getExternalFilesDir(null), DEBUG_LOG_FILE);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(System.currentTimeMillis() + ": " + message + "\n");
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "logPlaybackDebug write failed", e);
        }
    }
}
