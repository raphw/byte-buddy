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
package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;

/**
 * This type is used as a place holder for creating methods or fields that refer to the type that currently subject
 * of creation within a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
 */
public final class TargetType {

    /**
     * A description of the {@link net.bytebuddy.dynamic.TargetType}.
     */
    public static final TypeDescription DESCRIPTION = TypeDescription.ForLoadedType.of(TargetType.class);

    /**
     * Resolves the given type description to the supplied target type if it represents the {@link TargetType} placeholder.
     * Array types are resolved to their component type and rebuilt as an array of the actual target type, if necessary.
     *
     * @param typeDescription The type description that might represent the {@link TargetType} placeholder.
     * @param targetType      The actual target type.
     * @return A description of the resolved type.
     */
    public static TypeDescription resolve(TypeDescription typeDescription, TypeDescription targetType) {
        int arity = 0;
        TypeDescription componentType = typeDescription;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
            arity++;
        }
        return componentType.represents(TargetType.class)
                ? TypeDescription.ArrayProjection.of(targetType, arity)
                : typeDescription;
    }

    /**
     * An unusable constructor to avoid instance creation.
     */
    private TargetType() {
        throw new UnsupportedOperationException("This class only serves as a marker type and should not be instantiated");
    }
}
