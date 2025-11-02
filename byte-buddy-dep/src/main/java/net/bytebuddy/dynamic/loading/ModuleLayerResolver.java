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
package net.bytebuddy.dynamic.loading;

import net.bytebuddy.utility.nullability.MaybeNull;

import java.util.Map;

/**
 * A resolver for a class loader that considers a module layer.
 */
public interface ModuleLayerResolver {

    /**
     * Resolves a class loader for a module that is contained by this layer.
     *
     * @param classLoader          The class loader that is used for loading types.
     * @param binaryRepresentation The binary representations.
     * @return The class loader to use for the modules that are contained by the class loader.
     */
    @MaybeNull
    ClassLoader resolve(@MaybeNull ClassLoader classLoader, Map<String, byte[]> binaryRepresentation);

    /**
     * A disabled module layer resolver that always throws an exception if a {@code module-info} is discovered.
     */
    enum Disabled implements ModuleLayerResolver {

        /**
         * The singleton instance
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public ClassLoader resolve(@MaybeNull ClassLoader classLoader, Map<String, byte[]> binaryRepresentation) {
            throw new UnsupportedOperationException("Loading modules is not supported by the current handler");
        }
    }
}
