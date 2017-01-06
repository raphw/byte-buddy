package net.bytebuddy.dynamic.scaffold;

import lombok.EqualsAndHashCode;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

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

            @Override
            public boolean isResolved() {
                return false;
            }

            @Override
            public FieldDescription getField() {
                throw new IllegalStateException("Could not locate field");
            }
        }

        /**
         * A simple implementation for a field resolution.
         */
        @EqualsAndHashCode
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

            @Override
            public boolean isResolved() {
                return true;
            }

            @Override
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

        @Override
        public FieldLocator make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public Resolution locate(String name) {
            return Resolution.Illegal.INSTANCE;
        }

        @Override
        public Resolution locate(String name, TypeDescription type) {
            return Resolution.Illegal.INSTANCE;
        }
    }

    /**
     * An abstract base implementation of a field locator.
     */
    @EqualsAndHashCode
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

        @Override
        public Resolution locate(String name) {
            FieldList<?> candidates = locate(named(name).and(isVisibleTo(accessingType)));
            return candidates.size() == 1
                    ? new Resolution.Simple(candidates.getOnly())
                    : Resolution.Illegal.INSTANCE;
        }

        @Override
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
    @EqualsAndHashCode(callSuper = true)
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
        @EqualsAndHashCode
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

            @Override
            public FieldLocator make(TypeDescription typeDescription) {
                return new ForExactType(this.typeDescription, typeDescription);
            }
        }
    }

    /**
     * A field locator that looks up fields that are declared within a class's class hierarchy.
     */
    @EqualsAndHashCode(callSuper = true)
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

            @Override
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

            @Override
            public FieldLocator make(TypeDescription typeDescription) {
                return new ForTopLevelType(typeDescription);
            }
        }
    }
}
