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
package net.bytebuddy.utility;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * A comparator for guaranteeing a stable order for declared {@link Field}s.
 */
public enum FieldComparator implements Comparator<Field> {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public int compare(Field left, Field right) {
        if (left == right) {
            return 0;
        }
        int comparison = left.getName().compareTo(right.getName());
        if (comparison == 0) {
            return left.getType().getName().compareTo(right.getType().getName());
        } else {
            return comparison;
        }
    }
}
