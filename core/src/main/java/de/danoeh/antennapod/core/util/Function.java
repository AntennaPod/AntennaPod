package de.danoeh.antennapod.core.util;

import io.reactivex.annotations.NonNull;

public interface Function<T, R> {
    R apply(@NonNull T t);
}
