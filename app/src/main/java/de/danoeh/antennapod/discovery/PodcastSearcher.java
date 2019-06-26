package de.danoeh.antennapod.discovery;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.util.List;

public interface PodcastSearcher {
    Disposable search(Consumer<? super List<PodcastSearchResult>> successHandler, Consumer<? super Throwable> errorHandler);
}
