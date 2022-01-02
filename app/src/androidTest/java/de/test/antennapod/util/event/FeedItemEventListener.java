package de.test.antennapod.util.event;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.event.FeedItemEvent;
import io.reactivex.functions.Consumer;

/**
 * Test helpers to listen {@link FeedItemEvent} and handle them accordingly
 *
 */
public class FeedItemEventListener {
    private final List<FeedItemEvent> events = new ArrayList<>();

    /**
     * Provides an listener subscribing to {@link FeedItemEvent} that the callers can use
     *
     * Note: it uses RxJava's version of {@link Consumer} because it allows exceptions to be thrown.
     */
    public static void withFeedItemEventListener(@NonNull Consumer<FeedItemEventListener> consumer)
            throws Exception {
        FeedItemEventListener feedItemEventListener = new FeedItemEventListener();
        try {
            EventBus.getDefault().register(feedItemEventListener);
            consumer.accept(feedItemEventListener);
        } finally {
            EventBus.getDefault().unregister(feedItemEventListener);
        }
    }

    @Subscribe
    public void onEvent(FeedItemEvent event) {
        events.add(event);
    }

    @NonNull
    public List<? extends FeedItemEvent> getEvents() {
        return events;
    }
}
