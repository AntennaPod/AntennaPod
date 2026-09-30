package de.danoeh.antennapod.playback.service.internal;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheEvictor;
import androidx.media3.datasource.cache.CacheSpan;

import java.util.Comparator;
import java.util.TreeSet;

@OptIn(markerClass = UnstableApi.class)
public class SlidingWindowCacheEvictor implements CacheEvictor, Comparator<CacheSpan> {
    public static final long DEFAULT_MAX_BYTES = 50L * 1024 * 1024;

    private final long maxBytes;
    private final TreeSet<CacheSpan> spans;
    private long currentSize;

    public SlidingWindowCacheEvictor() {
        this(DEFAULT_MAX_BYTES);
    }

    public SlidingWindowCacheEvictor(long maxBytes) {
        this.maxBytes = maxBytes;
        this.spans = new TreeSet<>(this);
    }

    @Override
    public boolean requiresCacheSpanTouches() {
        return false;
    }

    @Override
    public void onCacheInitialized() {
    }

    @Override
    public void onStartFile(Cache cache, String key, long position, long length) {
        if (length != C.LENGTH_UNSET) {
            evict(cache, length);
        }
    }

    @Override
    public void onSpanAdded(Cache cache, CacheSpan span) {
        spans.add(span);
        currentSize += span.length;
        evict(cache, 0);
    }

    @Override
    public void onSpanRemoved(Cache cache, CacheSpan span) {
        spans.remove(span);
        currentSize -= span.length;
    }

    @Override
    public void onSpanTouched(Cache cache, CacheSpan oldSpan, CacheSpan newSpan) {
        onSpanRemoved(cache, oldSpan);
        onSpanAdded(cache, newSpan);
    }

    @Override
    public int compare(CacheSpan lhs, CacheSpan rhs) {
        int positionCompare = Long.compare(lhs.position, rhs.position);
        if (positionCompare != 0) {
            return positionCompare;
        }
        return lhs.compareTo(rhs);
    }

    long getCurrentSize() {
        return currentSize;
    }

    private void evict(Cache cache, long requiredSpace) {
        while (currentSize + requiredSpace > maxBytes && !spans.isEmpty()) {
            cache.removeSpan(spans.first());
        }
    }
}
