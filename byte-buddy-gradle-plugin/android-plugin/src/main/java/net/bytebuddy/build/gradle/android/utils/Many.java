/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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