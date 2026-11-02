package de.danoeh.antennapod.playback.service.internal;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheSpan;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@OptIn(markerClass = UnstableApi.class)
public class SlidingWindowCacheEvictorTest {
    private static final String KEY = "episode";
    private static final long MAX_BYTES = 100;

    private SlidingWindowCacheEvictor evictor;
    private Cache cache;

    @Before
    public void setUp() {
        evictor = new SlidingWindowCacheEvictor(MAX_BYTES);
        cache = mock(Cache.class);
        doAnswer(invocation -> {
            evictor.onSpanRemoved(cache, invocation.getArgument(0));
            return null;
        }).when(cache).removeSpan(any(CacheSpan.class));
    }

    @Test
    public void keepsSpansUntilMaxBytesIsReached() {
        evictor.onSpanAdded(cache, span(0, 40));
        evictor.onSpanAdded(cache, span(40, 40));

        assertEquals(80, evictor.getCurrentSize());
    }

    @Test
    public void evictsAlreadyPlayedPrefixWhenNewDataIsAdded() {
        evictor.onSpanAdded(cache, span(0, 60));
        evictor.onSpanAdded(cache, span(60, 60));

        assertEquals(60, evictor.getCurrentSize());
        assertTrue(evictor.compare(span(0, 60), span(60, 60)) < 0);
    }

    @Test
    public void evictsOldestPositionFirstAcrossMultipleAdds() {
        evictor.onSpanAdded(cache, span(0, 50));
        evictor.onSpanAdded(cache, span(50, 50));
        evictor.onSpanAdded(cache, span(100, 50));

        assertEquals(100, evictor.getCurrentSize());
    }

    private static CacheSpan span(long position, long length) {
        return new CacheSpan(KEY, position, length);
    }
}
