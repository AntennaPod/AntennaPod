package de.danoeh.antennapod.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CollectionTestUtil {

    public static <T> List<? extends T> concat(T item, List<? extends T> list) {
        List<T> res = new ArrayList<>(list);
        res.add(0, item);
        return res;
    }

    public static <T> List<? extends T> concat(List<? extends T> list, T item) {
        List<T> res = new ArrayList<>(list);
        res.add(item);
        return res;
    }

    public static <T> List<? extends T> concat(List<? extends T> list1, List<? extends T> list2) {
        List<T> res = new ArrayList<>(list1);
        res.addAll(list2);
        return res;
    }

    public static <T> List<T> list(T... a) {
        return Arrays.asList(a);
    }
}
