package net.bytebuddy.description.type;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.implementation.bytecode.StackSize;

import java.lang.reflect.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public interface TypeDefinition extends NamedElement, Iterable<TypeDefinition> {

    TypeDescription.Generic asGenericType();

    TypeDescription.Generic getSuperType();

    TypeList.Generic getInterfaces();

    FieldList<?> getDeclaredFields();

    MethodList<?> getDeclaredMethods();

    /**
     * <p>
     * Returns the component type of this type.
     * </p>
     * <p>
     * Only non-generic types ({@link TypeDescription.Generic.Sort#NON_GENERIC}) and generic array types
     * {@link TypeDescription.Generic.Sort#GENERIC_ARRAY}) define a component type. For other
     * types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The component type of this type or {@code null} if this type does not represent an array type.
     */
    TypeDefinition getComponentType();

    /**
     * Returns the sort of the generic type this instance represents.
     *
     * @return The sort of the generic type.
     */
    Sort getSort();

    /**
     * Returns the erasure of this type. Wildcard types ({@link TypeDescription.Generic.Sort#WILDCARD})
     * do not have a well-defined erasure and cause an {@link IllegalStateException} to be thrown.
     *
     * @return The erasure of this type.
     */
    TypeDescription asErasure();

    /**
     * Returns the name of the type. For generic types, this name is their {@link Object#toString()} representations. For a non-generic
     * type, it is the fully qualified binary name of the type.
     *
     * @return The name of this type.
     */
    String getTypeName();

    /**
     * Returns the size of the type described by this instance. Wildcard types
     * ({@link TypeDescription.Generic.Sort#WILDCARD} do not have a well-defined a stack size and
     * cause an {@link IllegalStateException} to be thrown.
     *
     * @return The size of the type described by this instance.
     */
    StackSize getStackSize();

    /**
     * Checks if the type described by this entity is an array.
     *
     * @return {@code true} if this type description represents an array.
     */
    boolean isArray();

    /**
     * Checks if the type described by this entity is a primitive type.
     *
     * @return {@code true} if this type description represents a primitive type.
     */
    boolean isPrimitive();

    /**
     * Checks if the type described by this instance represents {@code type}.
     *
     * @param type The type of interest.
     * @return {@code true} if the type described by this instance represents {@code type}.
     */
    boolean represents(Type type);

    /**
     * Represents a {@link TypeDescription.Generic}'s form.
     */
    enum Sort {

        /**
         * Represents a non-generic type.
         */
        NON_GENERIC,

        /**
         * Represents a generic array type.
         */
        GENERIC_ARRAY,

        /**
         * Represents a parameterized type.
         */
        PARAMETERIZED,

        /**
         * Represents a wildcard type.
         */
        WILDCARD,

        /**
         * Represents a type variable that is attached to a {@link TypeVariableSource}.
         */
        VARIABLE,

        /**
         * Represents a type variable that is merely symbolic and is not attached to a {@link TypeVariableSource} and does not defined bounds.
         */
        VARIABLE_SYMBOLIC;

        /**
         * Describes a loaded generic type as a {@link TypeDescription.Generic}.
         *
         * @param type The type to describe.
         * @return A description of the provided generic type.
         */
        public static TypeDescription.Generic describe(Type type) {
            if (type instanceof Class<?>) {
                return new TypeDescription.Generic.OfNonGenericType.ForLoadedType((Class<?>) type);
            } else if (type instanceof GenericArrayType) {
                return new TypeDescription.Generic.OfGenericArray.ForLoadedType((GenericArrayType) type);
            } else if (type instanceof ParameterizedType) {
                return new TypeDescription.Generic.OfParameterizedType.ForLoadedType((ParameterizedType) type);
            } else if (type instanceof TypeVariable) {
                return new TypeDescription.Generic.OfTypeVariable.ForLoadedType((TypeVariable<?>) type);
            } else if (type instanceof WildcardType) {
                return new TypeDescription.Generic.OfWildcardType.ForLoadedType((WildcardType) type);
            } else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }

        /**
         * Checks if this type sort represents a non-generic type.
         *
         * @return {@code true} if this sort form represents a non-generic.
         */
        public boolean isNonGeneric() {
            return this == NON_GENERIC;
        }

        /**
         * Checks if this type sort represents a parameterized type.
         *
         * @return {@code true} if this sort form represents a parameterized type.
         */
        public boolean isParameterized() {
            return this == PARAMETERIZED;
        }

        /**
         * Checks if this type sort represents a generic array.
         *
         * @return {@code true} if this type sort represents a generic array.
         */
        public boolean isGenericArray() {
            return this == GENERIC_ARRAY;
        }

        /**
         * Checks if this type sort represents a wildcard.
         *
         * @return {@code true} if this type sort represents a wildcard.
         */
        public boolean isWildcard() {
            return this == WILDCARD;
        }

        /**
         * Checks if this type sort represents a type variable of any form.
         *
         * @return {@code true} if this type sort represents an attached type variable.
         */
        public boolean isTypeVariable() {
            return this == VARIABLE || this == VARIABLE_SYMBOLIC;
        }

        @Override
        public String toString() {
            return "TypeDefinition.Sort." + name();
        }
    }

    /**
     * An iterator that iterates over a type's class hierarchy.
     */
    class SuperTypeIterator implements Iterator<TypeDefinition> {

        /**
         * The next type to represent.
         */
        private TypeDefinition nextType;

        /**
         * Creates a new iterator.
         *
         * @param initialType The initial type of this iterator.
         */
        public SuperTypeIterator(TypeDefinition initialType) {
            nextType = initialType;
        }

        @Override
        public boolean hasNext() {
            return nextType != null;
        }

        @Override
        public TypeDefinition next() {
            if (!hasNext()) {
                throw new NoSuchElementException("End of type hierarchy");
            }
            try {
                return nextType;
            } finally {
                nextType = nextType.getSuperType();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public String toString() {
            return "TypeDefinition.SuperTypeIterator{" +
                    "nextType=" + nextType +
                    '}';
        }
    }
}
