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
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument's {@link TypeDescription.Generic} raw type against the
 * given matcher for a {@link TypeDescription}. As a wildcard does not define an erasure, a runtime
 * exception is thrown when this matcher is applied to a wildcard.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ErasureMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the raw type of the matched element.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new raw type matcher.
     *
     * @param matcher The matcher to apply to the raw type.
     */
    public ErasureMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.asErasure());
    }

    @Override
    public String toString() {
        return "erasure(" + matcher + ")";
    }
}
