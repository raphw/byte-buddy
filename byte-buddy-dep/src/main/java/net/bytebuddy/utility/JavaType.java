package net.bytebuddy.utility;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.io.Serializable;

/**
 * Representations of Java types that do not exist in Java 6 but that have a special meaning to the JVM.
 */
public enum JavaType {

    /**
     * The Java 7 {@code java.lang.invoke.MethodHandle} type.
     */
    METHOD_HANDLE("java.lang.invoke.MethodHandle") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    },

    /**
     * The Java 7 {@code java.lang.invoke.MethodType} type.
     */
    METHOD_TYPE("java.lang.invoke.MethodType") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class) || typeDescription.represents(Serializable.class);
        }
    },

    /**
     * The Java 7 {@code java.lang.invoke.MethodTypes.Lookup} type.
     */
    METHOD_TYPES_LOOKUP("java.lang.invoke.MethodHandles$Lookup") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    },

    /**
     * The Java 7 {@code java.lang.invoke.CallSite} type.
     */
    CALL_SITE("java.lang.invoke.CallSite") {
        @Override
        protected boolean isSubTypeOf(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class);
        }
    };

    /**
     * A handler that is responsible for the lookup of a given type.
     */
    private final TypeLookup typeLookup;

    /**
     * Creates a new Java type representative.
     *
     * @param typeName The fully-qualified name of the type.
     */
    JavaType(String typeName) {
        TypeLookup typeLookup;
        try {
            typeLookup = new TypeLookup.ForLoadedType(Class.forName(typeName));
        } catch (Exception ignored) {
            typeLookup = new TypeLookup.ForNamedType(typeName);
        }
        this.typeLookup = typeLookup;
    }

    /**
     * Checks if a type is assignable from this type.
     *
     * @param typeDescription The type that is to be checked.
     * @return {@code true} if this type is assignable from the provided type.
     */
    public boolean isAssignableFrom(TypeDescription typeDescription) {
        return typeLookup.isAssignableFrom(typeDescription);
    }

    /**
     * Checks if a type is assignable to this type.
     *
     * @param typeDescription The type that is to be checked.
     * @return {@code true} if this type is assignable to the provided type.
     */
    public boolean isAssignableTo(TypeDescription typeDescription) {
        return representedBy(typeDescription) || isSubTypeOf(typeDescription);
    }

    /**
     * Checks if a given type is a subtype of the given type.
     *
     * @param typeDescription The type that is to be checked.
     * @return {@code true} if this type is a subtype of the provided type.
     */
    protected abstract boolean isSubTypeOf(TypeDescription typeDescription);

    /**
     * Checks if this type represents the provided type.
     *
     * @param typeDescription The type that is to be checked.
     * @return {@code true} if this type represents the provided type.
     */
    public boolean representedBy(TypeDescription typeDescription) {
        return typeLookup.represents(typeDescription);
    }

    /**
     * Loads the provided type if this is possible or throws an exception if not.
     *
     * @return The loaded type.
     */
    public Class<?> load() {
        return typeLookup.load();
    }

    /**
     * Checks if the given instance is of the represented type.
     *
     * @param value The value to check.
     * @return {@code true} if the given value is of this represented type.
     */
    public boolean isInstance(Object value) {
        return representedBy(new TypeDescription.ForLoadedType(value.getClass()));
    }

    @Override
    public String toString() {
        return "JavaType." + name();
    }

    /**
     * A handler for querying type information.
     */
    protected interface TypeLookup {

        /**
         * Checks if a type is assignable from this type.
         *
         * @param typeDescription The type that is to be checked.
         * @return {@code true} if this type is assignable from the provided type.
         */
        boolean isAssignableFrom(TypeDescription typeDescription);

        /**
         * Checks if this type represents the provided type.
         *
         * @param typeDescription The type that is to be checked.
         * @return {@code true} if this type represents the provided type.
         */
        boolean represents(TypeDescription typeDescription);

        /**
         * Loads the provided type if this is possible or throws an exception if not.
         *
         * @return The loaded type.
         */
        Class<?> load();

        /**
         * Represents information on a type that cannot be loaded.
         */
        class ForNamedType implements TypeLookup {

            /**
             * The name of the type.
             */
            private final String typeName;

            /**
             * Creates a new type lookup handler for a type that cannot be loaded.
             *
             * @param typeName The name of the type.
             */
            public ForNamedType(String typeName) {
                this.typeName = typeName;
            }

            @Override
            public boolean isAssignableFrom(TypeDescription typeDescription) {
                do {
                    if (typeDescription.getName().equals(typeName)) {
                        return true;
                    }
                    typeDescription = typeDescription.getSupertype();
                } while (typeDescription != null);
                return false;
            }

            @Override
            public boolean represents(TypeDescription typeDescription) {
                return typeDescription.getName().equals(typeName);
            }

            @Override
            public Class<?> load() {
                throw new IllegalStateException("Could not load " + typeName);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeName.equals(((ForNamedType) other).typeName);
            }

            @Override
            public int hashCode() {
                return typeName.hashCode();
            }

            @Override
            public String toString() {
                return "JavaType.TypeLookup.ForNamedType{" +
                        "typeName='" + typeName + '\'' +
                        '}';
            }
        }

        /**
         * Represents information on a loaded type.
         */
        class ForLoadedType implements TypeLookup {

            /**
             * The loaded type.
             */
            private final Class<?> type;

            /**
             * Creates a new handler for a loaded type.
             *
             * @param type The loaded type.
             */
            public ForLoadedType(Class<?> type) {
                this.type = type;
            }

            @Override
            public boolean isAssignableFrom(TypeDescription typeDescription) {
                return typeDescription.isAssignableTo(type);
            }

            @Override
            public boolean represents(TypeDescription typeDescription) {
                return typeDescription.represents(type);
            }

            @Override
            public Class<?> load() {
                return type;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && type.equals(((ForLoadedType) other).type);
            }

            @Override
            public int hashCode() {
                return type.hashCode();
            }

            @Override
            public String toString() {
                return "JavaType.TypeLookup.ForLoadedType{" +
                        "type=" + type +
                        '}';
            }
        }
    }
}
