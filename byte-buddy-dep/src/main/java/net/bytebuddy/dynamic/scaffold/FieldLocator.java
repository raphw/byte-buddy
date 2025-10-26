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
package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.fieldType;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A field locator offers an interface for locating a field that is declared by a specified type.
 */
public interface FieldLocator {

    /**
     * Locates a field with the given name and throws an exception if no such type exists.
     *
     * @param name The name of the field to locate.
     * @return A resolution for a field lookup.
     */
    Resolution locate(String name);

    /**
     * Locates a field with the given name and type and throws an exception if no such type exists.
     *
     * @param name The name of the field to locate.
     * @param type The type fo the field to locate.
     * @return A resolution for a field lookup.
     */
    Resolution locate(String name, TypeDescription type);

    /**
     * A resolution for a field lookup.
     */
    interface Resolution {

        /**
         * Returns {@code true} if a field was located.
         *
         * @return {@code true} if a field was located.
         */
        boolean isResolved();

        /**
         * Returns the field description if a field was located. This method must only be called if
         * this resolution was actually resolved.
         *
         * @return The located field.
         */
        FieldDescription getField();

        /**
         * An illegal resolution.
         */
        enum Illegal implements Resolution {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public FieldDescription getField() {
                throw new IllegalStateException("Could not locate field");
            }
        }

        /**
         * A simple implementation for a field resolution.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Simple implements Resolution {

            /**
             * A description of the located field.
             */
            private final FieldDescription fieldDescription;

            /**
             * Creates a new simple resolution for a field.
             *
             * @param fieldDescription A description of the located field.
             */
            protected Simple(FieldDescription fieldDescription) {
                this.fieldDescription = fieldDescription;
            }

            /**
             * Resolves a field locator for a potential accessor method. If the provided method is not a bean accessor,
             * an illegal resolution is returned.
             *
             * @param fieldLocator      The field locator to use.
             * @param methodDescription The method description that is the potential accessor.
             * @return A resolution for a field locator.
             */
            public static FieldLocator.Resolution ofBeanAccessor(FieldLocator fieldLocator, MethodDescription methodDescription) {
                String name;
                if (isSetter().matches(methodDescription)) {
                    name = methodDescription.getInternalName().substring(3);
                } else if (isGetter().matches(methodDescription)) {
                    name = methodDescription.getInternalName().substring(methodDescription.getInternalName().startsWith("is") ? 2 : 3);
                } else {
                    return FieldLocator.Resolution.Illegal.INSTANCE;
                }
                Resolution resolution = fieldLocator.locate(Character.toLowerCase(name.charAt(0)) + name.substring(1));
                return resolution.isResolved()
                        ? resolution
                        : fieldLocator.locate(Character.toUpperCase(name.charAt(0)) + name.substring(1));
            }

            /**
             * {@inheritDoc}
             */
            public boolean isResolved() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public FieldDescription getField() {
                return fieldDescription;
            }
        }
    }

    /**
     * A factory for creating a {@link FieldLocator}.
     */
    interface Factory {

        /**
         * Creates a field locator for a given type.
         *
         * @param typeDescription The type for which to create a field locator.
         * @return A suitable field locator.
         */
        FieldLocator make(TypeDescription typeDescription);
    }

    /**
     * A field locator that never discovers a field.
     */
    enum NoOp implements FieldLocator, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public FieldLocator make(TypeDescription typeDescription) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            return Resolution.Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name, TypeDescription type) {
            return Resolution.Illegal.INSTANCE;
        }
    }

    /**
     * An abstract base implementation of a field locator.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class AbstractBase implements FieldLocator {

        /**
         * The type accessing the field.
         */
        protected final TypeDescription accessingType;

        /**
         * Creates a new field locator.
         *
         * @param accessingType The type accessing the field.
         */
        protected AbstractBase(TypeDescription accessingType) {
            this.accessingType = accessingType;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name) {
            FieldList<?> candidates = locate(named(name).and(isVisibleTo(accessingType)));
            return candidates.size() == 1
                    ? new Resolution.Simple(candidates.getOnly())
                    : Resolution.Illegal.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        public Resolution locate(String name, TypeDescription type) {
            FieldList<?> candidates = locate(named(name).and(fieldType(type)).and(isVisibleTo(accessingType)));
            return candidates.size() == 1
                    ? new Resolution.Simple(candidates.getOnly())
                    : Resolution.Illegal.INSTANCE;
        }

        /**
         * Locates fields that match the given matcher.
         *
         * @param matcher The matcher that identifies fields of interest.
         * @return A list of fields that match the specified matcher.
         */
        protected abstract FieldList<?> locate(ElementMatcher<? super FieldDescription> matcher);
    }

    /**
     * A field locator that only looks up fields that are declared by a specific type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForExactType extends AbstractBase {

        /**
         * The type for which to look up fields.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a new field locator for locating fields from a declared type.
         *
         * @param typeDescription The type for which to look up fields that is also providing the accessing type.
         */
        public ForExactType(TypeDescription typeDescription) {
            this(typeDescription, typeDescription);
        }

        /**
         * Creates a new field locator for locating fields from a declared type.
         *
         * @param typeDescription The type for which to look up fields.
         * @param accessingType   The accessing type.
         */
        public ForExactType(TypeDescription typeDescription, TypeDescription accessingType) {
            super(accessingType);
            this.typeDescription = typeDescription;
        }

        @Override
        protected FieldList<?> locate(ElementMatcher<? super FieldDescription> matcher) {
            return typeDescription.getDeclaredFields().filter(matcher);
        }

        /**
         * A factory for creating a {@link ForExactType}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class Factory implements FieldLocator.Factory {

            /**
             * The type for which to locate a field.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new factory for a field locator that locates a field for an exact type.
             *
             * @param typeDescription The type for which to locate a field.
             */
            public Factory(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            /**
             * {@inheritDoc}
             */
            public FieldLocator make(TypeDescription typeDescription) {
                return new ForExactType(this.typeDescription, typeDescription);
            }
        }
    }

    /**
     * A field locator that looks up fields that are declared within a class's class hierarchy.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForClassHierarchy extends AbstractBase {

        /**
         * The type for which to look up a field within its class hierarchy.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a field locator that looks up fields that are declared within a class's class hierarchy.
         *
         * @param typeDescription The type for which to look up a field within its class hierarchy which is also the accessing type.
         */
        public ForClassHierarchy(TypeDescription typeDescription) {
            this(typeDescription, typeDescription);
        }

        /**
         * Creates a field locator that looks up fields that are declared within a class's class hierarchy.
         *
         * @param typeDescription The type for which to look up a field within its class hierarchy.
         * @param accessingType   The accessing type.
         */
        public ForClassHierarchy(TypeDescription typeDescription, TypeDescription accessingType) {
            super(accessingType);
            this.typeDescription = typeDescription;
        }

        @Override
        protected FieldList<?> locate(ElementMatcher<? super FieldDescription> matcher) {
            for (TypeDefinition typeDefinition : typeDescription) {
                FieldList<?> candidates = typeDefinition.getDeclaredFields().filter(matcher);
                if (!candidates.isEmpty()) {
                    return candidates;
                }
            }
            return new FieldList.Empty<FieldDescription>();
        }

        /**
         * A factory for creating a {@link ForClassHierarchy}.
         */
        public enum Factory implements FieldLocator.Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public FieldLocator make(TypeDescription typeDescription) {
                return new ForClassHierarchy(typeDescription);
            }
        }
    }

    /**
     * A field locator that only locates fields in the top-level type.
     */
    class ForTopLevelType extends AbstractBase {

        /**
         * Creates a new type locator for a top-level type.
         *
         * @param typeDescription The type to access.
         */
        protected ForTopLevelType(TypeDescription typeDescription) {
            super(typeDescription);
        }

        @Override
        protected FieldList<?> locate(ElementMatcher<? super FieldDescription> matcher) {
            return accessingType.getDeclaredFields().filter(matcher);
        }

        /**
         * A factory for locating a field in a top-level type.
         */
        public enum Factory implements FieldLocator.Factory {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public FieldLocator make(TypeDescription typeDescription) {
                return new ForTopLevelType(typeDescription);
            }
        }
    }
}
