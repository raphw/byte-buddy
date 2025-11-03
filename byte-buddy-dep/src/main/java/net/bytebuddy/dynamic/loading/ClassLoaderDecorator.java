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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.util.Map;

/**
 * A class loader decorator allows for the decoration of class loaders that are created by a {@link net.bytebuddy.dynamic.loading.ClassLoadingStrategy}.
 * This way, it is possible to define custom class loaders that wrap Byte Buddy's built-in class loaders.
 */
public interface ClassLoaderDecorator {

    /**
     * Determines if a type should be skipped from class loader decoration.
     *
     * @param typeDescription The type description to check.
     * @return {@code true} if the type should be skipped from decoration.
     */
    boolean isSkipped(TypeDescription typeDescription);

    /**
     * Applies this decorator to resolve a class loader for the given type.
     *
     * @param typeDescription The type description for which to resolve a class loader.
     * @return The class loader to use for the given type or {@code null} for the bootstrap class loader.
     */
    @MaybeNull
    ClassLoader apply(TypeDescription typeDescription);

    /**
     * A no-operation implementation of a class loader decorator that always returns the same class loader
     * without applying any decoration.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class NoOp implements ClassLoaderDecorator {

        /**
         * The class loader to return for all type descriptions or {@code null} for the bootstrap class loader.
         */
        @MaybeNull
        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
        private final ClassLoader classLoader;

        /**
         * Creates a new no-operation class loader decorator.
         *
         * @param classLoader The class loader to return for all type descriptions  or {@code null} for the bootstrap class loader.
         */
        public NoOp(@MaybeNull ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isSkipped(TypeDescription typeDescription) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @MaybeNull
        public ClassLoader apply(TypeDescription typeDescription) {
            return classLoader;
        }
    }

    /**
     * A factory for creating class loader decorators.
     */
    interface Factory {

        /**
         * Creates a class loader decorator for the given class loader and type definitions.
         *
         * @param classLoader     The class loader to decorate or {@code null} if the bootstrap class loader is used.
         * @param typeDefinitions A map of type names to their binary representations for types being loaded.
         * @return A class loader decorator instance.
         */
        ClassLoaderDecorator make(@MaybeNull ClassLoader classLoader, Map<String, byte[]> typeDefinitions);

        /**
         * A no-operation factory that creates no-operation class loader decorators.
         */
        enum NoOp implements ClassLoaderDecorator.Factory {

            /**
             * The singleton instance of this no-operation factory.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public ClassLoaderDecorator make(@MaybeNull ClassLoader classLoader, Map<String, byte[]> typeDefinitions) {
                return new ClassLoaderDecorator.NoOp(classLoader);
            }
        }
    }
}
