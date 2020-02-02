package de.danoeh.antennapod.core.util;

import java.util.Arrays;

/**
 * Fast and memory efficient long list
 */
public final class LongList {

    private long[] values;
    private int size;

    /**
     * Constructs an empty instance with a default initial capacity.
     */
    public LongList() {
        this(4);
    }

    /**
     * Constructs an empty instance.
     *
     * @param initialCapacity {@code >= 0;} initial capacity of the list
     */
    public LongList(int initialCapacity) {
        if(initialCapacity < 0) {
            throw new IllegalArgumentException("initial capacity must be 0 or higher");
        }
        values = new long[initialCapacity];
        size = 0;
    }

    public static LongList of(long... values) {
        if(values == null || values.length == 0) {
            return new LongList(0);
        }
        LongList result = new LongList(values.length);
        for(long value : values) {
            result.add(value);
        }
        return result;
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
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (! (other instanceof LongList)) {
            return false;
        }
        LongList otherList = (LongList) other;
        if (size != otherList.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (values[i] != otherList.values[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size * 5 + 10);
        sb.append("LongList{");
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(values[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Gets the number of elements in this list.
     */
    public int size() {
        return size;
    }

    /**
     * Gets the indicated value.
     *
     * @param n {@code >= 0, < size();} which element
     * @return the indicated element's value
     */
    public long get(int n) {
        if (n >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(n < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        return values[n];
    }

    /**
     * Sets the value at the given index.
     *
     * @param index the index at which to put the specified object.
     * @param value the object to add.
     * @return the previous element at the index.
     */
    public long set(int index, long value) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(index < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        long result = values[index];
        values[index] = value;
        return result;
    }

    /**
     * Adds an element to the end of the list. This will increase the
     * list's capacity if necessary.
     *
     * @param value the value to add
     */
    public void add(long value) {
        growIfNeeded();
        values[size++] = value;
    }

    /**
     * Inserts element into specified index, moving elements at and above
     * that index up one. May not be used to insert at an index beyond the
     * current size (that is, insertion as a last element is legal but
     * no further).
     *
     * @param n {@code >= 0, <=size();} index of where to insert
     * @param value value to insert
     */
    public void insert(int n, int value) {
        if (n > size) {
            throw new IndexOutOfBoundsException("n > size()");
        } else if(n < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }

        growIfNeeded();

        System.arraycopy(values, n, values, n+1, size - n);
        values[n] = value;
        size++;
    }

    /**
     * Removes value from this list.
     *
     * @param value  value to remove
     * return {@code true} if the value was removed, {@code false} otherwise
     */
    public boolean remove(long value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                size--;
                System.arraycopy(values, i+1, values, i, size-i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes values from this list.
     *
     * @param values  values to remove
     */
    public void removeAll(long[] values) {
        for(long value : values) {
            remove(value);
        }
    }

    /**
     * Removes values from this list.
     *
     * @param list List with values to remove
     */
    public void removeAll(LongList list) {
        for(long value : list.values) {
            remove(value);
        }
    }

    /**
     * Removes an element at a given index, shifting elements at greater
     * indicies down one.
     *
     * @param index index of element to remove
     */
    public void removeIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(index < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        size--;
        System.arraycopy(values, index + 1, values, index, size - index);
    }

    /**
     * Increases size of array if needed
     */
    private void growIfNeeded() {
        if (size == values.length) {
            // Resize.
            long[] newArray = new long[size * 3 / 2 + 10];
            System.arraycopy(values, 0, newArray, 0, size);
            values = newArray;
        }
    }

    /**
     * Returns the index of the given value, or -1 if the value does not
     * appear in the list.
     *
     * @param value value to find
     * @return index of value or -1
     */
    public int indexOf(long value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all values from this list.
     */
    public void clear() {
        values = new long[4];
        size = 0;
    }


    /**
     * Returns true if the given value is contained in the list
     *
     * @param value value to look for
     * @return {@code true} if this list contains {@code value}, {@code false} otherwise
     */
    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    /**
     * Returns an array with a copy of this list's values
     *
     * @return array with a copy of this list's values
     */
    public long[] toArray() {
        return Arrays.copyOf(values, size);

    }
}
