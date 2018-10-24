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
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the list of inherited annotations of a type description.
 *
 * @param <T> The actual matched type of this matcher.
 */
@HashCodeAndEqualsPlugin.Enhance
public class InheritedAnnotationMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to be applied to the provided annotation list.
     */
    private final ElementMatcher<? super AnnotationList> matcher;

    /**
     * Creates a new matcher for the inherited annotations of a type description.
     *
     * @param matcher The matcher to be applied to the provided annotation list.
     */
    public InheritedAnnotationMatcher(ElementMatcher<? super AnnotationList> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getInheritedAnnotations());
    }

    @Override
    public String toString() {
        return "inheritsAnnotations(" + matcher + ")";
    }
}
