package de.danoeh.antennapod.core.tests.util;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.util.LongIntMap;

public class LongLongMapTest extends AndroidTestCase {

    public void testEmptyMap() {
        LongIntMap map = new LongIntMap();
        assertEquals(0, map.size());
        assertEquals("LongLongMap{}", map.toString());
        assertEquals(0, map.get(42));
        assertEquals(-1, map.get(42, -1));
        assertEquals(false, map.delete(42));
        assertEquals(-1, map.indexOfKey(42));
        assertEquals(-1, map.indexOfValue(42));
        assertEquals(1, map.hashCode());
    }

    public void testSingleElement() {
        LongIntMap map = new LongIntMap();
        map.put(17, 42);
        assertEquals(1, map.size());
        assertEquals("LongLongMap{17=42}", map.toString());
        assertEquals(42, map.get(17));
        assertEquals(42, map.get(17, -1));
        assertEquals(0, map.indexOfKey(17));
        assertEquals(0, map.indexOfValue(42));
        assertEquals(true, map.delete(17));
    }

    public void testAddAndDelete() {
        LongIntMap map = new LongIntMap();
        for(int i=0; i < 100; i++) {
            map.put(i * 17, i * 42);
        }
        assertEquals(100, map.size());
        assertEquals(0, map.get(0));
        assertEquals(42, map.get(17));
        assertEquals(42, map.get(17, -1));
        assertEquals(1, map.indexOfKey(17));
        assertEquals(1, map.indexOfValue(42));
        for(int i=0; i < 100; i++) {
            assertEquals(true, map.delete(i * 17));
        }
    }

    public void testOverwrite() {
        LongIntMap map = new LongIntMap();
        map.put(17, 42);
        assertEquals(1, map.size());
        assertEquals("LongLongMap{17=42}", map.toString());
        assertEquals(42, map.get(17));
        map.put(17, 23);
        assertEquals(1, map.size());
        assertEquals("LongLongMap{17=23}", map.toString());
        assertEquals(23, map.get(17));
    }

}
