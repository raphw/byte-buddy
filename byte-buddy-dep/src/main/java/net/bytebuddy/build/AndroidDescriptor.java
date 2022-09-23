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
package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;

/**
 * Provides Android context information to {@link Plugin} instances that include it as their constructor
 * parameter.
 */
public interface AndroidDescriptor {
    /**
     * Provides the scope of the passed {@link TypeDescription} instance.
     *
     * @param typeDescription The type to get the scope for.
     * @return a {@link TypeScope} describing the type's origin.
     */
    TypeScope getTypeScope(TypeDescription typeDescription);

    /**
     * Describes the origin of a type.
     */
    enum TypeScope {
        /**
         * Denotes a type that comes from the local project.
         */
        LOCAL,

        /**
         * Denotes a type that comes from a dependency of the local project.
         */
        EXTERNAL
    }
}
