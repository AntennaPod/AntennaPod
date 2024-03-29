package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Feed;

public abstract class FeedUpdateManager {
    private static FeedUpdateManager instance;

    public static FeedUpdateManager getInstance() {
        return instance;
    }

    public static void setInstance(FeedUpdateManager instance) {
        FeedUpdateManager.instance = instance;
    }

    public abstract void restartUpdateAlarm(Context context, boolean replace);

    public abstract void runOnce(Context context);

    public abstract void runOnce(Context context, Feed feed);

    public abstract void runOnce(Context context, Feed feed, boolean nextPage);

    public abstract void runOnceOrAsk(@NonNull Context context);

    public abstract void runOnceOrAsk(@NonNull Context context, @Nullable Feed feed);
}
