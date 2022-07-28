package net.bytebuddy.build.gradle.android.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class Many {

    public static <T, R> ArrayList<R> map(Collection<T> original, Function<T, R> transformation) {
        ArrayList<R> list = new ArrayList<>();
        original.forEach(item -> list.add(transformation.apply(item)));

        return list;
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return collection.size() > 0;
    }

    public static <T> Set<T> setOf(T... items) {
        return toSet(Arrays.asList(items));
    }

    public static <T> ArrayList<T> listOf(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    public static <T> T[] arrayOf(T... items) {
        return items;
    }

    public static <T> Set<T> toSet(Collection<T> collection) {
        return new HashSet<>(collection);
    }

    public static <T> T find(Collection<T> items, Function<T, Boolean> found) {
        for (T item : items) {
            if (found.apply(item)) {
                return item;
            }
        }

        throw new IllegalStateException();
    }
}