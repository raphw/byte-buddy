package net.bytebuddy.description.type;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.implementation.bytecode.StackSize;

import java.lang.reflect.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementations define a type, either as a {@link TypeDescription} or as a {@link TypeDescription.Generic}.
 */
public interface TypeDefinition extends NamedElement, ModifierReviewable.ForTypeDefinition, Iterable<TypeDefinition> {

    /**
     * <p>
     * If this property is set to {@code true}, non-generic {@link TypeDefinition}s do no longer resolve their referenced
     * generic types when traversing type hierarchies. Setting this property can cause unexpected side effects such as
     * {@link ClassCastException}s from overridden methods as type variables are resolved to their erasures where a method
     * might return that is unexpected by the callee. Setting this property also makes type annotations unavailable using
     * such type navigation.
     * </p>
     * <p>
     * Setting this property can be useful if generic type information is not required in order to avoid bugs in
     * implementations of the JVM where processing generic types can cause segmentation faults. Byte Buddy will undertake
     * a best effort to retain the generic type information and information about type annotations within the redefined
     * types' class files. Typically, this property can be meaningful in combination with a Java agent that only changes
     * byte code without changing a class type's structure.
     * </p>
     */
    String RAW_TYPES_PROPERTY = "net.bytebuddy.raw";

    /**
     * Returns this type definition as a generic type.
     *
     * @return This type definition represented as a generic type.
     */
    TypeDescription.Generic asGenericType();

    /**
     * Returns the erasure of this type. Wildcard types ({@link TypeDescription.Generic.Sort#WILDCARD})
     * do not have a well-defined erasure and cause an {@link IllegalStateException} to be thrown.
     *
     * @return The erasure of this type.
     */
    TypeDescription asErasure();

    /**
     * Returns the super class of this type. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Interface types
     * and the {@link Object} class do not define a super class where {@code null} is returned. Array types define {@link Object}
     * as their direct super class.
     *
     * @return The super class of this type or {@code null} if no super class exists for this type.
     */
    TypeDescription.Generic getSuperClass();

    /**
     * Returns the interfaces that this type implements. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types.
     *
     * @return The interfaces that this type implements.
     */
    TypeList.Generic getInterfaces();

    /**
     * Returns the fields that this type declares. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Generic array
     * types never define fields and the returned list is always empty for such types.
     *
     * @return The fields that this type declares. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Generic array
     * types never define methods and the returned list is always empty for such types.
     */
    FieldList<?> getDeclaredFields();

    /**
     * Returns the methods that this type declares.
     *
     * @return The methods that this type declares.
     */
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
         * Represents a type variable that is attached to a {@link net.bytebuddy.description.TypeVariableSource}.
         */
        VARIABLE,

        /**
         * Represents a type variable that is merely symbolic and is not attached to a {@link net.bytebuddy.description.TypeVariableSource}
         * and does not defined bounds.
         */
        VARIABLE_SYMBOLIC;

        /**
         * Describes a loaded generic type as a {@link TypeDescription.Generic}.
         *
         * @param type The type to describe.
         * @return A description of the provided generic type.
         */
        public static TypeDescription.Generic describe(Type type) {
            return describe(type, TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE);
        }

        /**
         * Describes the generic type while using the supplied annotation reader for resolving type annotations if this
         * language feature is available on the current JVM.
         *
         * @param type             The type to describe.
         * @param annotationReader The annotation reader for extracting type annotations.
         * @return A description of the provided generic annotated type.
         */
        protected static TypeDescription.Generic describe(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
            if (type instanceof Class<?>) {
                return new TypeDescription.Generic.OfNonGenericType.ForLoadedType((Class<?>) type, annotationReader);
            } else if (type instanceof GenericArrayType) {
                return new TypeDescription.Generic.OfGenericArray.ForLoadedType((GenericArrayType) type, annotationReader);
            } else if (type instanceof ParameterizedType) {
                return new TypeDescription.Generic.OfParameterizedType.ForLoadedType((ParameterizedType) type, annotationReader);
            } else if (type instanceof TypeVariable) {
                return new TypeDescription.Generic.OfTypeVariable.ForLoadedType((TypeVariable<?>) type, annotationReader);
            } else if (type instanceof WildcardType) {
                return new TypeDescription.Generic.OfWildcardType.ForLoadedType((WildcardType) type, annotationReader);
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
    }

    /**
     * An iterator that iterates over a type's class hierarchy.
     */
    class SuperClassIterator implements Iterator<TypeDefinition> {

        /**
         * The next class to represent.
         */
        private TypeDefinition nextClass;

        /**
         * Creates a new iterator.
         *
         * @param initialType The initial type of this iterator.
         */
        public SuperClassIterator(TypeDefinition initialType) {
            nextClass = initialType;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return nextClass != null;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDefinition next() {
            if (!hasNext()) {
                throw new NoSuchElementException("End of type hierarchy");
            }
            try {
                return nextClass;
            } finally {
                nextClass = nextClass.getSuperClass();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
