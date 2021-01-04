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
import net.bytebuddy.description.annotation.AnnotationDescription;

import java.lang.annotation.ElementType;

/**
 * A matcher for annotations that target a given element type.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AnnotationTargetMatcher<T extends AnnotationDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The targeted element type.
     */
    private final ElementType elementType;

    /**
     * Creates a new matcher for an annotation target.
     *
     * @param elementType The targeted element type.
     */
    public AnnotationTargetMatcher(ElementType elementType) {
        this.elementType = elementType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target.getElementTypes().contains(elementType);
    }

    @Override
    public String toString() {
        return "targetsElement(" + elementType + ")";
    }
}
