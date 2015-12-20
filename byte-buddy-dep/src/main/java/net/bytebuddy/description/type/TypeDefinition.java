package net.bytebuddy.description.type;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.bytecode.StackSize;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;

public interface TypeDefinition extends NamedElement, Iterable<TypeDefinition> {

    GenericTypeDescription asGenericType();

    GenericTypeDescription getSuperType();

    GenericTypeList getInterfaces();

    FieldList<?> getDeclaredFields();

    MethodList<?> getDeclaredMethods();

    /**
     * <p>
     * Returns the component type of this type.
     * </p>
     * <p>
     * Only non-generic types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and generic array types
     * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}) define a component type. For other
     * types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The component type of this type or {@code null} if this type does not represent an array type.
     */
    TypeDefinition getComponentType();

    /**
     * Returns the erasure of this type. Wildcard types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD})
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
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD} do not have a well-defined a stack size and
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
