package de.danoeh.antennapod.core.util;

import java.util.Arrays;

/**
 * Fast and memory efficient int list
 */
public final class IntList {

    private int[] values;
    private int size;

    /**
     * Constructs an empty instance with a default initial capacity.
     */
    public IntList() {
        this(4);
    }

    /**
     * Constructs an empty instance.
     *
     * @param initialCapacity {@code >= 0;} initial capacity of the list
     */
    private IntList(int initialCapacity) {
        if(initialCapacity < 0) {
            throw new IllegalArgumentException("initial capacity must be 0 or higher");
        }
        values = new int[initialCapacity];
        size = 0;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < size; i++) {
            int value = values[i];
            hashCode = 31 * hashCode + value;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (! (other instanceof IntList)) {
            return false;
        }
        IntList otherList = (IntList) other;
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
        sb.append("IntList{");
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
    public int get(int n) {
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
    public int set(int index, int value) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("n >= size()");
        } else if(index < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        int result = values[index];
        values[index] = value;
        return result;
    }

    /**
     * Adds an element to the end of the list. This will increase the
     * list's capacity if necessary.
     *
     * @param value the value to add
     */
    public void add(int value) {
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

        System.arraycopy (values, n, values, n+1, size - n);
        values[n] = value;
        size++;
    }

    /**
     * Removes value from this list.
     *
     * @param value  value to remove
     * return {@code true} if the value was removed, {@code false} otherwise
     */
    public boolean remove(int value) {
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
        System.arraycopy (values, index + 1, values, index, size - index);
    }

    /**
     * Increases size of array if needed
     */
    private void growIfNeeded() {
        if (size == values.length) {
            // Resize.
            int[] newArray = new int[size * 3 / 2 + 10];
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
    private int indexOf(int value) {
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
        values = new int[4];
        size = 0;
    }


    /**
     * Returns true if the given value is contained in the list
     *
     * @param value value to look for
     * @return {@code true} if this list contains {@code value}, {@code false} otherwise
     */
    public boolean contains(int value) {
        return indexOf(value) >= 0;
    }

    /**
     * Returns an array with a copy of this list's values
     *
     * @return array with a copy of this list's values
     */
    public int[] toArray() {
        return Arrays.copyOf(values, size);

    }
}
