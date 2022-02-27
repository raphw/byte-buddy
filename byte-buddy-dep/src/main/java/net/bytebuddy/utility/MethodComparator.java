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

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * A comparator for guaranteeing a stable order for declared {@link Method}s.
 */
public enum MethodComparator implements Comparator<Method> {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public int compare(Method left, Method right) {
        if (left == right) {
            return 0;
        }
        int comparison = left.getName().compareTo(right.getName());
        if (comparison == 0) {
            Class<?>[] leftParameterType = left.getParameterTypes(), rightParameterType = right.getParameterTypes();
            if (leftParameterType.length < rightParameterType.length) {
                return -1;
            } else if (leftParameterType.length > rightParameterType.length) {
                return 1;
            } else {
                for (int index = 0; index < leftParameterType.length; index++) {
                    int comparisonParameterType = leftParameterType[index].getName().compareTo(rightParameterType[index].getName());
                    if (comparisonParameterType != 0) {
                        return comparisonParameterType;
                    }
                }
                return left.getReturnType().getName().compareTo(right.getReturnType().getName());
            }
        } else {
            return comparison;
        }
    }
}
