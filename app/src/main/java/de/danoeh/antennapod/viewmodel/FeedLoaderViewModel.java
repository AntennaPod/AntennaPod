package de.danoeh.antennapod.viewmodel;

import android.arch.lifecycle.ViewModel;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.DBReader;
import io.reactivex.Maybe;

public class FeedLoaderViewModel extends ViewModel {
    private Feed feed;

    public Maybe<Feed> getFeed(long feedId) {
        if (feed == null) {
            return loadFeed(feedId);
        } else {
            return Maybe.just(feed);
        }
    }

    private Maybe<Feed> loadFeed(long feedId) {
        return Maybe.create(emitter -> {
            Feed feed = DBReader.getFeed(feedId);
            if (feed != null) {
                this.feed = feed;
                emitter.onSuccess(feed);
            } else {
                emitter.onComplete();
            }
        });
    }
}
