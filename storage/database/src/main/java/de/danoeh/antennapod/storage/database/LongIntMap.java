package de.danoeh.antennapod.storage.database;


import java.util.Arrays;

/**
 * Fast and memory efficient long to long map
 */
public class LongIntMap {

    private long[] keys;
    private int[] values;
    private int size;

    /**
     * Creates a new LongLongMap containing no mappings.
     */
    public LongIntMap() {
        this(10);
    }

    /**
     * Creates a new SparseLongArray containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.  If you supply an initial capacity of 0, the
     * sparse array will be initialized with a light-weight representation
     * not requiring any additional array allocations.
     */
    public LongIntMap(int initialCapacity) {
        if(initialCapacity < 0) {
            throw new IllegalArgumentException("initial capacity must be 0 or higher");
        }
        keys = new long[initialCapacity];
        values = new int[initialCapacity];
        size = 0;
    }

    /**
     * Increases size of array if needed
     */
    private void growIfNeeded() {
        if (size == keys.length) {
            // Resize.
            long[] newKeysArray = new long[size * 3 / 2 + 10];
            int[] newValuesArray = new int[size * 3 / 2 + 10];
            System.arraycopy(keys, 0, newKeysArray, 0, size);
            System.arraycopy(values, 0, newValuesArray, 0, size);
            keys = newKeysArray;
            values = newValuesArray;
        }
    }

    /**
     * Gets the long mapped from the specified key, or <code>0</code>
     * if no such mapping has been made.
     */
    public int get(long key) {
        return get(key, 0);
    }

    /**
     * Gets the long mapped from the specified key, or the specified value
     * if no such mapping has been made.
     */
    public int get(long key, int valueIfKeyNotFound) {
        int index = indexOfKey(key);
        if(index >= 0) {
            return values[index];
        } else {
            return valueIfKeyNotFound;
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    public boolean delete(long key) {
        int index = indexOfKey(key);

        if (index >= 0) {
            removeAt(index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the mapping at the given index.
     */
    private void removeAt(int index) {
        System.arraycopy(keys, index + 1, keys, index, size - (index + 1));
        System.arraycopy(values, index + 1, values, index, size - (index + 1));
        size--;
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    public void put(long key, int value) {
        int index = indexOfKey(key);

        if (index >= 0) {
            values[index] = value;
        } else {
            growIfNeeded();
            keys[size] = key;
            values[size] = value;
            size++;
        }
    }

    /**
     * Returns the number of key-value mappings that this SparseIntArray
     * currently stores.
     */
    public int size() {
        return size;
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the key from the <code>index</code>th key-value mapping that this
     * SparseLongArray stores.
     *
     * <p>The keys corresponding to indices in ascending order are guaranteed to
     * be in ascending order, e.g., <code>keyAt(0)</code> will return the
     * smallest key and <code>keyAt(size()-1)</code> will return the largest
     * key.</p>
     */
    private long keyAt(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(index < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        return keys[index];
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the value from the <code>index</code>th key-value mapping that this
     * SparseLongArray stores.
     *
     * <p>The values corresponding to indices in ascending order are guaranteed
     * to be associated with keys in ascending order, e.g.,
     * <code>valueAt(0)</code> will return the value associated with the
     * smallest key and <code>valueAt(size()-1)</code> will return the value
     * associated with the largest key.</p>
     */
    private int valueAt(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(index < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        return values[index];
    }

    /**
     * Returns the index for which {@link #keyAt} would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public int indexOfKey(long key) {
        for(int i=0; i < size; i++) {
            if(keys[i] == key) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified key, or a negative number if no keys map to the
     * specified value.
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     */
    public int indexOfValue(long value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all key-value mappings from this SparseIntArray.
     */
    public void clear() {
        keys = new long[10];
        values = new int[10];
        size = 0;
    }

    /**
     * Returns a copy of the values contained in this map.
     *
     * @return a copy of the values contained in this map
     */
    public int[] values() {
        return Arrays.copyOf(values, size);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (! (other instanceof LongIntMap)) {
            return false;
        }
        LongIntMap otherMap = (LongIntMap) other;
        if (size != otherMap.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (keys[i] != otherMap.keys[i] ||
                values[i] != otherMap.values[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < size; i++) {
            long value = values[i];
            hashCode = 31 * hashCode + (int)(value ^ (value >>> 32));
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (size() <= 0) {
            return "LongLongMap{}";
        }

        StringBuilder buffer = new StringBuilder(size * 28);
        buffer.append("LongLongMap{");
        for (int i=0; i < size; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            long key = keyAt(i);
            buffer.append(key);
            buffer.append('=');
            long value = valueAt(i);
            buffer.append(value);
        }
        buffer.append('}');
        return buffer.toString();
    }
}
