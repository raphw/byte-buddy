/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the type of an annotation description.
 *
 * @param <T> The exact type of the annotation description that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AnnotationTypeMatcher<T extends AnnotationDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to apply to an annotation's type.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new matcher for an annotation description's type.
     *
     * @param matcher The type matcher to apply to an annotation's type.
     */
    public AnnotationTypeMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getAnnotationType());
    }

    @Override
    public String toString() {
        return "ofAnnotationType(" + matcher + ')';
    }
}
