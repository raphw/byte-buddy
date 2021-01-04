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
import net.bytebuddy.description.NamedElement;

/**
 * An element matcher that matches a Java descriptor.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DescriptorMatcher<T extends NamedElement.WithDescriptor> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * A matcher to apply to the descriptor.
     */
    private final ElementMatcher<String> matcher;

    /**
     * Creates a new matcher for an element's descriptor.
     *
     * @param matcher A matcher to apply to the descriptor.
     */
    public DescriptorMatcher(ElementMatcher<String> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getDescriptor());
    }

    @Override
    public String toString() {
        return "hasDescriptor(" + matcher + ")";
    }
}
