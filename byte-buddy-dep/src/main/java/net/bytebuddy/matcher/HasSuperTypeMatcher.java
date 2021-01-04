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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * An element matcher that matches a super type.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class HasSuperTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to any super type of the matched type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for a super type.
     *
     * @param matcher The matcher to apply to any super type of the matched type.
     */
    public HasSuperTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        Set<TypeDescription> previous = new HashSet<TypeDescription>();
        for (TypeDefinition typeDefinition : target) {
            if (!previous.add(typeDefinition.asErasure())) { // Main type can be an interface.
                return false; // Avoids a life-lock when encountering a recursive type-definition.
            } else if (matcher.matches(typeDefinition.asGenericType())) {
                return true;
            }
            LinkedList<TypeDefinition> interfaceTypes = new LinkedList<TypeDefinition>(typeDefinition.getInterfaces());
            while (!interfaceTypes.isEmpty()) {
                TypeDefinition interfaceType = interfaceTypes.removeFirst();
                if (previous.add(interfaceType.asErasure())) {
                    if (matcher.matches(interfaceType.asGenericType())) {
                        return true;
                    } else {
                        interfaceTypes.addAll(interfaceType.getInterfaces());
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "hasSuperType(" + matcher + ")";
    }
}
