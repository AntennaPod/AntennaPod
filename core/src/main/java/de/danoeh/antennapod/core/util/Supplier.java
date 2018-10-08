package de.danoeh.antennapod.core.util;

import android.support.annotation.NonNull;

public interface Supplier<T> {
    @NonNull
    T get();
}
