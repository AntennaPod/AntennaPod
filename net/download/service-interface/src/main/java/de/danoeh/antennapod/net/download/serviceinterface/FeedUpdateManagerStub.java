package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Feed;

public class FeedUpdateManagerStub extends FeedUpdateManager {

    @Override
    public void restartUpdateAlarm(Context context, boolean replace) {
    }

    @Override
    public void runOnce(Context context) {
    }

    @Override
    public void runOnce(Context context, Feed feed) {
    }

    @Override
    public void runOnce(Context context, Feed feed, boolean nextPage) {
    }

    @Override
    public void runOnceOrAsk(@NonNull Context context) {
    }

    @Override
    public void runOnceOrAsk(@NonNull Context context, @Nullable Feed feed) {
    }
}
