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
package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.Set;

/**
 * An element matcher which checks if a string is in a set of strings.
 */
@HashCodeAndEqualsPlugin.Enhance
public class StringSetMatcher extends ElementMatcher.Junction.AbstractBase<String> {

    /**
     * The values to check against.
     */
    private final Set<String> values;

    /**
     * Creates a new string set matcher.
     *
     * @param values The values to check against.
     */
    public StringSetMatcher(Set<String> values) {
        this.values = values;
    }

    @Override
    public boolean matches(String target) {
        return values.contains(target);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder().append("in(");
        boolean first = true;
        for (String value : values) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(", ");
            }
            stringBuilder.append(value);
        }
        return stringBuilder.append(")").toString();
    }
}
