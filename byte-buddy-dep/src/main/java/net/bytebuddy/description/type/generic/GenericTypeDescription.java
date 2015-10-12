package net.bytebuddy.description.type.generic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.signature.SignatureVisitor;

import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a generic type of the Java programming language. A non-generic {@link TypeDescription} is considered to be
 * a specialization of a generic type.
 */
public interface GenericTypeDescription extends NamedElement, Iterable<GenericTypeDescription> {

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
     * Returns the erasure of this type. Wildcard types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD})
     * do not have a well-defined erasure and cause an {@link IllegalStateException} to be thrown.
     *
     * @return The erasure of this type.
     */
    TypeDescription asErasure();

    /**
     * <p>
     * Returns the generic super type of this type.
     * </p>
     * <p>
     * Only non-generic types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) define a super type. For a generic array type,
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}), a description of {@link Object} is returned.
     * For other generic types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The generic super type of this type or {@code null} if no such type exists.
     */
    GenericTypeDescription getSuperType();

    /**
     * <p>
     * Returns the generic interface types of this type.
     * </p>
     * <p>
     * Only non-generic types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) define a super type. For a generic array type,
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}), a list of {@link java.io.Serializable} and
     * {@link Cloneable}) is returned. For other generic types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The generic interface types of this type.
     */
    GenericTypeList getInterfaces();

    /**
     * <p>
     * Returns a list of field descriptions that are declared by this type. For parameterized types, all type variables of these fields are
     * resolved to the values of the type variables.
     * </p>
     * <p>
     * Only non-generic types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) define a super type. For a generic array type,
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}), an empty list is returned. For other generic
     * types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return A list of fields that are declared by this type.
     */
    FieldList<?> getDeclaredFields();

    /**
     * <p>
     * Returns a list of method descriptions that are declared by this type. For parameterized types, all type variables used by these methods
     * are resolved to the values of the type variables.
     * </p>
     * <p>
     * Only non-generic types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) define a super type. For a generic array type,
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}), an empty list is returned. For other
     * generic types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return A list of methods that are declared by this type.
     */
    MethodList<?> getDeclaredMethods();

    /**
     * <p>
     * Returns the upper bounds of this type. Any type with a well-defined upper bound is bound by at least one type. If no such
     * type is defined, the bound is implicitly {@link Object}.
     * </p>
     * <p>
     * Only non-symbolic type variables ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE},
     * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE_DETACHED}) and wildcard types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD}) have well-defined upper bounds. For other
     * types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The upper bounds of this type.
     */
    GenericTypeList getUpperBounds();

    /**
     * <p>
     * Returns the lower bounds of this type.
     * </p>
     * <p>
     * Only wildcard types ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD}) define a lower bound. For other
     * types, an {@link IllegalStateException} is thrown.
     * </p>
     *
     * @return The lower bounds of this type.
     */
    GenericTypeList getLowerBounds();

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
    GenericTypeDescription getComponentType();

    /**
     * <p>
     * Returns the type parameters of this type.
     * </p>
     * <p>
     * Parameters are only well-defined for parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}), generic array types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}) and non-generic types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}). For non-generic and generic array types,
     * the returned list is always empty. For all other types, this method throws an {@link IllegalStateException}.
     * </p>
     *
     * @return A list of this type's type parameters.
     */
    GenericTypeList getParameters();

    /**
     * <p>
     * Returns the owner type of this type.
     * </p>
     * <p>
     * An owner type is only well-defined for parameterized types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) , generic array types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}) and non-generic types
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}). Non-generic types and generic array types do
     * never have an owner type. For all other types, this method throws an {@link IllegalStateException}.
     * </p>
     *
     * @return This type's owner type or {@code null} if no such owner type exists.
     */
    GenericTypeDescription getOwnerType();

    /**
     * Returns the source of this type variable. A type variable source is only well-defined for an attached type variable
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE}. For other types, this method
     * throws an {@link IllegalStateException}.
     *
     * @return This type's type variable source.
     */
    TypeVariableSource getVariableSource();

    /**
     * Returns the symbol of this type variable. A symbol is only well-defined for type variables
     * ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE},
     * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE_DETACHED},
     * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE_SYMBOLIC}). For other types, this method
     * throws an {@link IllegalStateException}.
     *
     * @return This type's type variable symbol.
     */
    String getSymbol();

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
     * Applies a visitor to this generic type description.
     *
     * @param visitor The visitor to apply.
     * @param <T>     The value that this visitor yields.
     * @return The visitor's return value.
     */
    <T> T accept(Visitor<T> visitor);

    /**
     * Represents a {@link GenericTypeDescription}'s form.
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
         * Represents a type variable that is not attached to a {@link TypeVariableSource} but defines type bounds.
         */
        VARIABLE_DETACHED,

        /**
         * Represents a type variable that is merely symbolic and is not attached to a {@link TypeVariableSource} and does not defined bounds.
         */
        VARIABLE_SYMBOLIC;

        /**
         * Describes a loaded generic type as a {@link GenericTypeDescription}.
         *
         * @param type The type to describe.
         * @return A description of the provided generic type.
         */
        public static GenericTypeDescription describe(Type type) {
            if (type instanceof Class<?>) {
                return new TypeDescription.ForLoadedType((Class<?>) type);
            } else if (type instanceof GenericArrayType) {
                return new ForGenericArray.OfLoadedType((GenericArrayType) type);
            } else if (type instanceof ParameterizedType) {
                return new ForParameterizedType.OfLoadedType((ParameterizedType) type);
            } else if (type instanceof TypeVariable) {
                return new ForTypeVariable.OfLoadedType((TypeVariable<?>) type);
            } else if (type instanceof WildcardType) {
                return new ForWildcardType.OfLoadedType((WildcardType) type);
            } else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }

        /**
         * Checks if this type form represents a non-generic type.
         *
         * @return {@code true} if this type form represents a non-generic.
         */
        public boolean isNonGeneric() {
            return this == NON_GENERIC;
        }

        /**
         * Checks if this type form represents a parameterized type.
         *
         * @return {@code true} if this type form represents a parameterized type.
         */
        public boolean isParameterized() {
            return this == PARAMETERIZED;
        }

        /**
         * Checks if this type form represents a generic array.
         *
         * @return {@code true} if this type form represents a generic array.
         */
        public boolean isGenericArray() {
            return this == GENERIC_ARRAY;
        }

        /**
         * Checks if this type form represents a wildcard.
         *
         * @return {@code true} if this type form represents a wildcard.
         */
        public boolean isWildcard() {
            return this == WILDCARD;
        }

        /**
         * Checks if this type form represents an attached type variable.
         *
         * @return {@code true} if this type form represents an attached type variable.
         */
        public boolean isTypeVariable() {
            return this == VARIABLE;
        }

        /**
         * Checks if this type form represents a detached type variable.
         *
         * @return {@code true} if this type form represents a detached type variable.
         */
        public boolean isDetachedTypeVariable() {
            return this == VARIABLE_DETACHED;
        }

        /**
         * Checks if this type form represents a symbolic type variable.
         *
         * @return {@code true} if this type form represents a symbolic type variable.
         */
        public boolean isSymbolicTypeVariable() {
            return this == VARIABLE_SYMBOLIC;
        }

        @Override
        public String toString() {
            return "GenericTypeDescription.Sort." + name();
        }
    }

    /**
     * A visitor that can be applied to a {@link GenericTypeDescription} for differentiating on the sort of the visited type.
     *
     * @param <T> The visitor's return value's type.
     */
    interface Visitor<T> {

        /**
         * Visits a generic array type ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#GENERIC_ARRAY}).
         *
         * @param genericArray The generic array type.
         * @return The visitor's return value.
         */
        T onGenericArray(GenericTypeDescription genericArray);

        /**
         * Visits a wildcard ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#WILDCARD}).
         *
         * @param wildcard The wildcard.
         * @return The visitor's return value.
         */
        T onWildcard(GenericTypeDescription wildcard);

        /**
         * Visits a parameterized type ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}).
         *
         * @param parameterizedType The generic array type.
         * @return The visitor's return value.
         */
        T onParameterizedType(GenericTypeDescription parameterizedType);

        /**
         * Visits a type variable ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE},
         * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE_DETACHED},
         * {@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#VARIABLE_SYMBOLIC}).
         *
         * @param typeVariable The generic array type.
         * @return The visitor's return value.
         */
        T onTypeVariable(GenericTypeDescription typeVariable);

        /**
         * Visits a non-generic type ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}).
         *
         * @param typeDescription The non-generic type.
         * @return The visitor's return value.
         */
        T onNonGenericType(GenericTypeDescription typeDescription);

        /**
         * A non-operational generic type visitor. Any visited type is returned in its existing form.
         */
        enum NoOp implements Visitor<GenericTypeDescription> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public GenericTypeDescription onGenericArray(GenericTypeDescription genericArray) {
                return genericArray;
            }

            @Override
            public GenericTypeDescription onWildcard(GenericTypeDescription wildcard) {
                return wildcard;
            }

            @Override
            public GenericTypeDescription onParameterizedType(GenericTypeDescription parameterizedType) {
                return parameterizedType;
            }

            @Override
            public GenericTypeDescription onTypeVariable(GenericTypeDescription typeVariable) {
                return typeVariable;
            }

            @Override
            public GenericTypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                return typeDescription;
            }

            @Override
            public String toString() {
                return "GenericTypeDescription.Visitor.NoOp." + name();
            }
        }

        /**
         * A visitor that returns the erasure of any visited type. For wildcard types, an exception is thrown.
         */
        enum TypeErasing implements Visitor<TypeDescription> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public TypeDescription onGenericArray(GenericTypeDescription genericArray) {
                return genericArray.asErasure();
            }

            @Override
            public TypeDescription onWildcard(GenericTypeDescription wildcard) {
                throw new IllegalArgumentException("Cannot erase a wilcard type");
            }

            @Override
            public TypeDescription onParameterizedType(GenericTypeDescription parameterizedType) {
                return parameterizedType.asErasure();
            }

            @Override
            public TypeDescription onTypeVariable(GenericTypeDescription typeVariable) {
                return typeVariable.asErasure();
            }

            @Override
            public TypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                return typeDescription.asErasure();
            }

            @Override
            public String toString() {
                return "GenericTypeDescription.Visitor.TypeErasing." + name();
            }
        }

        /**
         * A visitor for erasing type variables on the most fine-grained level. In practice, this means:
         * <ul>
         * <li>Parameterized types are reduced to their erasure if one of its parameters represents a type variable or a wildcard with a bound
         * that is a type variable.</li>
         * <li>Wildcards have their bound erased, if required.</li>
         * <li>Type variables are erased.</li>
         * <li>Generic arrays have their component type erased, if required.</li>
         * <li>Non-generic types are transformed into raw-type representations of the same type.</li>
         * </ul>
         */
        enum TypeVariableErasing implements Visitor<GenericTypeDescription> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public GenericTypeDescription onGenericArray(GenericTypeDescription genericArray) {
                return ForGenericArray.Latent.of(genericArray.getComponentType().accept(this), 1);
            }

            @Override
            public GenericTypeDescription onWildcard(GenericTypeDescription wildcard) {
                // Wildcards which are used within parameterized types are taken care of by the calling method.
                GenericTypeList lowerBounds = wildcard.getLowerBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent.boundedAbove(wildcard.getUpperBounds().getOnly().accept(this))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
            }

            @Override
            public GenericTypeDescription onParameterizedType(GenericTypeDescription parameterizedType) {
                List<GenericTypeDescription> parameters = new ArrayList<GenericTypeDescription>(parameterizedType.getParameters().size());
                for (GenericTypeDescription parameter : parameterizedType.getParameters()) {
                    if (parameter.accept(PartialErasureReviser.INSTANCE)) {
                        return parameterizedType.asErasure();
                    }
                    parameters.add(parameter.accept(this));
                }
                GenericTypeDescription ownerType = parameterizedType.getOwnerType();
                return new GenericTypeDescription.ForParameterizedType.Latent(parameterizedType.asErasure(),
                        parameters,
                        ownerType == null
                                ? TypeDescription.UNDEFINED
                                : ownerType.accept(this));
            }

            @Override
            public GenericTypeDescription onTypeVariable(GenericTypeDescription typeVariable) {
                return typeVariable.asErasure();
            }

            @Override
            public GenericTypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                return new ForParameterizedType.Raw(typeDescription.asErasure());
            }

            @Override
            public String toString() {
                return "GenericTypeDescription.Visitor.TypeVariableErasing." + name();
            }

            /**
             * A visitor for checking if a type can be erased partially when defined as a parameter of a parameterized type.
             * If this condition is true, a parameterized type must be erased instead of erasing the parameterized type's
             * parameters.
             */
            protected enum PartialErasureReviser implements Visitor<Boolean> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Boolean onGenericArray(GenericTypeDescription genericArray) {
                    return genericArray.getComponentType().accept(this);
                }

                @Override
                public Boolean onWildcard(GenericTypeDescription wildcard) {
                    GenericTypeList lowerBounds = wildcard.getLowerBounds();
                    return lowerBounds.isEmpty()
                            ? wildcard.getUpperBounds().getOnly().accept(this)
                            : lowerBounds.getOnly().accept(this);
                }

                @Override
                public Boolean onParameterizedType(GenericTypeDescription parameterizedType) {
                    return false;
                }

                @Override
                public Boolean onTypeVariable(GenericTypeDescription typeVariable) {
                    return true;
                }

                @Override
                public Boolean onNonGenericType(GenericTypeDescription typeDescription) {
                    return false;
                }

                @Override
                public String toString() {
                    return "GenericTypeDescription.Visitor.TypeVariableErasing.PartialErasureReviser." + name();
                }
            }
        }

        /**
         * Visits a generic type and appends the discovered type to the supplied signature visitor.
         */
        class ForSignatureVisitor implements Visitor<SignatureVisitor> {

            /**
             * Index of a {@link String}'s only character to improve code readabilty.
             */
            private static final int ONLY_CHARACTER = 0;

            /**
             * The signature visitor that receives the discovered generic type.
             */
            protected final SignatureVisitor signatureVisitor;

            /**
             * Creates a new visitor for the given signature visitor.
             *
             * @param signatureVisitor The signature visitor that receives the discovered generic type.
             */
            public ForSignatureVisitor(SignatureVisitor signatureVisitor) {
                this.signatureVisitor = signatureVisitor;
            }

            @Override
            public SignatureVisitor onGenericArray(GenericTypeDescription genericArray) {
                genericArray.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
                return signatureVisitor;
            }

            @Override
            public SignatureVisitor onWildcard(GenericTypeDescription wildcard) {
                throw new IllegalStateException("Unexpected wildcard: " + wildcard);
            }

            @Override
            public SignatureVisitor onParameterizedType(GenericTypeDescription parameterizedType) {
                onOwnableType(parameterizedType);
                signatureVisitor.visitEnd();
                return signatureVisitor;
            }

            /**
             * Visits a type which might define an owner type.
             *
             * @param genericTypeDescription The visited generic type.
             */
            private void onOwnableType(GenericTypeDescription genericTypeDescription) {
                GenericTypeDescription ownerType = genericTypeDescription.getOwnerType();
                if (ownerType != null && ownerType.getSort().isParameterized()) {
                    onOwnableType(ownerType);
                    signatureVisitor.visitInnerClassType(genericTypeDescription.asErasure().getSimpleName());
                } else {
                    signatureVisitor.visitClassType(genericTypeDescription.asErasure().getInternalName());
                }
                for (GenericTypeDescription upperBound : genericTypeDescription.getParameters()) {
                    upperBound.accept(new OfParameter(signatureVisitor));
                }
            }

            @Override
            public SignatureVisitor onTypeVariable(GenericTypeDescription typeVariable) {
                signatureVisitor.visitTypeVariable(typeVariable.getSymbol());
                return signatureVisitor;
            }

            @Override
            public SignatureVisitor onNonGenericType(GenericTypeDescription typeDescription) {
                if (typeDescription.isArray()) {
                    typeDescription.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
                } else if (typeDescription.isPrimitive()) {
                    signatureVisitor.visitBaseType(typeDescription.asErasure().getDescriptor().charAt(ONLY_CHARACTER));
                } else {
                    signatureVisitor.visitClassType(typeDescription.asErasure().getInternalName());
                    signatureVisitor.visitEnd();
                }
                return signatureVisitor;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || other instanceof ForSignatureVisitor
                        && signatureVisitor.equals(((ForSignatureVisitor) other).signatureVisitor);
            }

            @Override
            public int hashCode() {
                return signatureVisitor.hashCode();
            }

            @Override
            public String toString() {
                return "GenericTypeDescription.Visitor.ForSignatureVisitor{" +
                        "signatureVisitor=" + signatureVisitor +
                        '}';
            }

            /**
             * Visits a parameter while visiting a generic type for delegating discoveries to a signature visitor.
             */
            protected static class OfParameter extends ForSignatureVisitor {

                /**
                 * Creates a new parameter visitor.
                 *
                 * @param signatureVisitor The signature visitor which is notified over visited types.
                 */
                protected OfParameter(SignatureVisitor signatureVisitor) {
                    super(signatureVisitor);
                }

                @Override
                public SignatureVisitor onWildcard(GenericTypeDescription wildcard) {
                    GenericTypeList upperBounds = wildcard.getUpperBounds();
                    GenericTypeList lowerBounds = wildcard.getLowerBounds();
                    if (lowerBounds.isEmpty() && upperBounds.getOnly().represents(Object.class)) {
                        signatureVisitor.visitTypeArgument();
                    } else if (!lowerBounds.isEmpty() /* && upperBounds.isEmpty() */) {
                        lowerBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.SUPER)));
                    } else /* if (!upperBounds.isEmpty() && lowerBounds.isEmpty()) */ {
                        upperBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.EXTENDS)));
                    }
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onGenericArray(GenericTypeDescription genericArray) {
                    genericArray.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onParameterizedType(GenericTypeDescription parameterizedType) {
                    parameterizedType.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onTypeVariable(GenericTypeDescription typeVariable) {
                    typeVariable.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onNonGenericType(GenericTypeDescription typeDescription) {
                    typeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                    return signatureVisitor;
                }

                @Override
                public String toString() {
                    return "GenericTypeDescription.Visitor.ForSignatureVisitor.OfParameter{}";
                }
            }
        }

        /**
         * An abstract implementation of a visitor that substitutes generic types by replacing (nested)
         * type variables and/or non-generic component types.
         */
        abstract class Substitutor implements Visitor<GenericTypeDescription> {

            @Override
            public GenericTypeDescription onParameterizedType(GenericTypeDescription parameterizedType) {
                GenericTypeDescription ownerType = parameterizedType.getOwnerType();
                List<GenericTypeDescription> parameters = new ArrayList<GenericTypeDescription>(parameterizedType.getParameters().size());
                for (GenericTypeDescription parameter : parameterizedType.getParameters()) {
                    parameters.add(parameter.accept(this));
                }
                return new GenericTypeDescription.ForParameterizedType.Latent(parameterizedType.asErasure().accept(this).asErasure(),
                        parameters,
                        ownerType == null
                                ? TypeDescription.UNDEFINED
                                : ownerType.accept(this));
            }

            @Override
            public GenericTypeDescription onGenericArray(GenericTypeDescription genericArray) {
                return GenericTypeDescription.ForGenericArray.Latent.of(genericArray.getComponentType().accept(this), 1);
            }

            @Override
            public GenericTypeDescription onWildcard(GenericTypeDescription wildcard) {
                GenericTypeList lowerBounds = wildcard.getLowerBounds();
                return lowerBounds.isEmpty()
                        ? GenericTypeDescription.ForWildcardType.Latent.boundedAbove(wildcard.getUpperBounds().getOnly().accept(this))
                        : GenericTypeDescription.ForWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
            }

            @Override
            public GenericTypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                int arity = 0;
                while (typeDescription.isArray()) {
                    typeDescription = typeDescription.getComponentType();
                    arity++;
                }
                return ForGenericArray.Latent.of(onSimpleType(typeDescription), arity);
            }

            /**
             * Visits a simple, non-generic type, i.e. either a component type of an array or a non-array type.
             *
             * @param typeDescription The type that is visited.
             * @return The substituted type.
             */
            protected abstract GenericTypeDescription onSimpleType(GenericTypeDescription typeDescription);

            /**
             * A substitutor that attaches type variables to a type variable source and replaces representations of
             * {@link TargetType} with a given declaring type.
             */
            public static class ForAttachment extends Substitutor {

                /**
                 * The declaring type which is filled in for {@link TargetType}.
                 */
                private final TypeDescription declaringType;

                /**
                 * The source which is used for locating type variables.
                 */
                private final TypeVariableSource typeVariableSource;

                /**
                 * Creates a visitor for attaching type variables.
                 *
                 * @param declaringType      The declaring type which is filled in for {@link TargetType}.
                 * @param typeVariableSource The source which is used for locating type variables.
                 */
                protected ForAttachment(TypeDescription declaringType, TypeVariableSource typeVariableSource) {
                    this.declaringType = declaringType;
                    this.typeVariableSource = typeVariableSource;
                }

                /**
                 * Attaches all types to the given field description.
                 *
                 * @param fieldDescription The field description to which visited types should be attached to.
                 * @return A substitutor that attaches visited types to the given field's type context.
                 */
                public static ForAttachment of(FieldDescription fieldDescription) {
                    return new ForAttachment(fieldDescription.getDeclaringType().asErasure(), fieldDescription.getDeclaringType().asErasure());
                }

                /**
                 * Attaches all types to the given method description.
                 *
                 * @param methodDescription The method description to which visited types should be attached to.
                 * @return A substitutor that attaches visited types to the given method's type context.
                 */
                public static ForAttachment of(MethodDescription methodDescription) {
                    return new ForAttachment(methodDescription.getDeclaringType().asErasure(), methodDescription);
                }

                /**
                 * Attaches all types to the given parameter description.
                 *
                 * @param parameterDescription The parameter description to which visited types should be attached to.
                 * @return A substitutor that attaches visited types to the given parameter's type context.
                 */
                public static ForAttachment of(ParameterDescription parameterDescription) {
                    return new ForAttachment(parameterDescription.getDeclaringMethod().getDeclaringType().asErasure(), parameterDescription.getDeclaringMethod());
                }

                /**
                 * Attaches all types to the given type description.
                 *
                 * @param typeDescription The type description to which visited types should be attached to.
                 * @return A substitutor that attaches visited types to the given type's type context.
                 */
                public static ForAttachment of(TypeDescription typeDescription) {
                    return new ForAttachment(typeDescription, typeDescription);
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    GenericTypeDescription typeVariable = typeVariableSource.findVariable(genericTypeDescription.getSymbol());
                    return typeVariable == null
                            ? genericTypeDescription.asErasure()
                            : typeVariable;
                }

                @Override
                protected GenericTypeDescription onSimpleType(GenericTypeDescription typeDescription) {
                    return typeDescription.equals(TargetType.DESCRIPTION)
                            ? declaringType
                            : typeDescription;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof ForAttachment)) return false;
                    ForAttachment that = (ForAttachment) other;
                    return declaringType.equals(that.declaringType)
                            && typeVariableSource.equals(that.typeVariableSource);
                }

                @Override
                public int hashCode() {
                    int result = declaringType.hashCode();
                    result = 31 * result + typeVariableSource.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "GenericTypeDescription.Visitor.Substitutor.ForAttachment{" +
                            "declaringType=" + declaringType +
                            ", typeVariableSource=" + typeVariableSource +
                            '}';
                }
            }

            /**
             * A visitor for detaching a type from its declaration context by detaching type variables. This is achieved by
             * detaching type variables and by replacing the declaring type which is identified by a provided {@link ElementMatcher}
             * with {@link TargetType}.
             */
            public static class ForDetachment extends Substitutor {

                /**
                 * A type matcher for identifying the declaring type.
                 */
                private final ElementMatcher<? super TypeDescription> typeMatcher;

                /**
                 * A cache of detached type variables in order to resolve recursive types.
                 */
                private final Map<String, GenericTypeDescription> detachedVariables;

                /**
                 * Creates a visitor for detaching a type.
                 *
                 * @param typeMatcher A type matcher for identifying the declaring type.
                 */
                public ForDetachment(ElementMatcher<? super TypeDescription> typeMatcher) {
                    this.typeMatcher = typeMatcher;
                    detachedVariables = new HashMap<String, GenericTypeDescription>();
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription genericTypeDescription) {
                    GenericTypeDescription typeVariable = detachedVariables.get(genericTypeDescription.getSymbol());
                    return typeVariable == null
                            ? new DetachedTypeVariable(genericTypeDescription.getSymbol(), genericTypeDescription.getUpperBounds(), this)
                            : typeVariable;
                }

                @Override
                protected GenericTypeDescription onSimpleType(GenericTypeDescription typeDescription) {
                    return typeMatcher.matches(typeDescription.asErasure())
                            ? TargetType.DESCRIPTION
                            : typeDescription;
                }

                /**
                 * Registers a generic type variable that was not yet visited. This addresses the possibility of defining recursive type variables.
                 * When this method is called, the provided type variable is not yet fully constructed and must not be used.
                 *
                 * @param symbol       The type variable's symbol.
                 * @param typeVariable A description of the generic type variable.
                 */
                protected void register(String symbol, GenericTypeDescription typeVariable) {
                    detachedVariables.put(symbol, typeVariable);
                }

                @Override
                public String toString() {
                    return "GenericTypeDescription.Visitor.Substitutor.ForDetachment{" +
                            "typeMatcher=" + typeMatcher +
                            ", detachedVariables=" + detachedVariables +
                            '}';
                }

                /**
                 * A description of a detached type variable.
                 */
                protected static class DetachedTypeVariable extends GenericTypeDescription.ForTypeVariable.InDetachedForm {

                    /**
                     * The symbol of this variable.
                     */
                    private final String symbol;

                    /**
                     * The bounds of the type variable.
                     */
                    private final List<GenericTypeDescription> bounds;

                    /**
                     * Creates a description of a detached type variable.
                     *
                     * @param symbol  The symbol of this variable.
                     * @param bounds  The bounds of the type variable.
                     * @param visitor The visitor to apply to the bounds.
                     */
                    protected DetachedTypeVariable(String symbol, List<GenericTypeDescription> bounds, ForDetachment visitor) {
                        this.symbol = symbol;
                        visitor.register(symbol, this);
                        this.bounds = new ArrayList<GenericTypeDescription>(bounds.size());
                        for (GenericTypeDescription bound : bounds) {
                            this.bounds.add(bound.accept(visitor));
                        }
                    }

                    @Override
                    public GenericTypeList getUpperBounds() {
                        return new GenericTypeList.Explicit(bounds);
                    }

                    @Override
                    public String getSymbol() {
                        return symbol;
                    }
                }
            }

            /**
             * A visitor for binding type variables to their values.
             */
            public static class ForTypeVariableBinding extends Substitutor {

                /**
                 * Bindings of type variables to their substitution values.
                 */
                private final Map<GenericTypeDescription, GenericTypeDescription> bindings;

                /**
                 * Creates a new visitor for a type variable bindings.
                 *
                 * @param bindings Bindings of type variables to their substitution values.
                 */
                protected ForTypeVariableBinding(Map<GenericTypeDescription, GenericTypeDescription> bindings) {
                    this.bindings = bindings;
                }

                /**
                 * Creates a visitor that binds the variables of the given generic type by the generic type's values. If the provided type
                 * represents a raw generic type or if the generic type is incomplete, the returned visitor erases all found type variables
                 * instead.
                 *
                 * @param typeDescription The type description to be bound.
                 * @return A visitor that binds any type variables
                 */
                public static Visitor<GenericTypeDescription> bind(GenericTypeDescription typeDescription) {
                    Map<GenericTypeDescription, GenericTypeDescription> bindings = new HashMap<GenericTypeDescription, GenericTypeDescription>();
                    do {
                        GenericTypeList parameters = typeDescription.getParameters();
                        GenericTypeList typeVariables = typeDescription.asErasure().getTypeVariables();
                        if (parameters.size() != typeVariables.size()) {
                            return TypeVariableErasing.INSTANCE;
                        }
                        for (int index = 0; index < typeVariables.size(); index++) {
                            bindings.put(typeVariables.get(index), parameters.get(index));
                        }
                        typeDescription = typeDescription.getOwnerType();
                    } while (typeDescription != null && typeDescription.getSort().isParameterized());
                    return new ForTypeVariableBinding(bindings);
                }

                @Override
                public GenericTypeDescription onTypeVariable(GenericTypeDescription typeVariable) {
                    GenericTypeDescription substitution = bindings.get(typeVariable);
                    return substitution == null
                            ? typeVariable.asErasure() // Fallback: Never happens for well-defined generic types.
                            : substitution;
                }

                @Override
                public GenericTypeDescription onNonGenericType(GenericTypeDescription typeDescription) {
                    return typeDescription;
                }

                @Override
                protected GenericTypeDescription onSimpleType(GenericTypeDescription typeDescription) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof ForTypeVariableBinding)) return false;
                    ForTypeVariableBinding that = (ForTypeVariableBinding) other;
                    return bindings.equals(that.bindings);
                }

                @Override
                public int hashCode() {
                    return bindings.hashCode();
                }

                @Override
                public String toString() {
                    return "GenericTypeDescription.Visitor.Substitutor.ForTypeVariableBinding{" +
                            "bindings=" + bindings +
                            '}';
                }
            }
        }
    }

    /**
     * A base implementation of a generic type description that represents a potentially generic array. Instances represent a non-generic type
     * if the given component type is non-generic.
     */
    abstract class ForGenericArray implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return getComponentType().getSort().isNonGeneric()
                    ? Sort.NON_GENERIC
                    : Sort.GENERIC_ARRAY;
        }

        @Override
        public TypeDescription asErasure() {
            return TypeDescription.ArrayProjection.of(getComponentType().asErasure(), 1);
        }

        @Override
        public GenericTypeDescription getSuperType() {
            return TypeDescription.OBJECT;
        }

        @Override
        public GenericTypeList getInterfaces() {
            return TypeDescription.ARRAY_INTERFACES;
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Empty();
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Empty();
        }

        @Override
        public GenericTypeList getUpperBounds() {
            throw new IllegalStateException("An array type does not imply upper type bounds: " + this);
        }

        @Override
        public GenericTypeList getLowerBounds() {
            throw new IllegalStateException("An array type does not imply lower type bounds: " + this);
        }

        @Override
        public TypeVariableSource getVariableSource() {
            throw new IllegalStateException("An array type does not imply a type variable source: " + this);
        }

        @Override
        public GenericTypeList getParameters() {
            return new GenericTypeList.Empty();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return TypeDescription.UNDEFINED;
        }

        @Override
        public String getSymbol() {
            throw new IllegalStateException("An array type does not imply a symbol: " + this);
        }

        @Override
        public String getTypeName() {
            return getSort().isNonGeneric()
                    ? asErasure().getTypeName()
                    : toString();
        }

        @Override
        public String getSourceCodeName() {
            return getSort().isNonGeneric()
                    ? asErasure().getSourceCodeName()
                    : toString();
        }

        @Override
        public boolean represents(Type type) {
            return equals(Sort.describe(type));
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            return new SuperTypeIterator(this);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return getSort().isNonGeneric()
                    ? visitor.onNonGenericType(this)
                    : visitor.onGenericArray(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        @SuppressFBWarnings(value = "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", justification = "Type check is performed by erasure instance")
        public boolean equals(Object other) {
            if (getSort().isNonGeneric()) {
                return asErasure().equals(other);
            }
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isGenericArray() && getComponentType().equals(genericTypeDescription.getComponentType());
        }

        @Override
        public int hashCode() {
            return getSort().isNonGeneric()
                    ? asErasure().hashCode()
                    : getComponentType().hashCode();
        }

        @Override
        public String toString() {
            return getSort().isNonGeneric()
                    ? asErasure().toString()
                    : getComponentType().getTypeName() + "[]";
        }

        /**
         * A description of a loaded generic array type.
         */
        public static class OfLoadedType extends ForGenericArray {

            /**
             * The loaded generic array type.
             */
            private final GenericArrayType genericArrayType;

            /**
             * Creates a type description of the given generic array type.
             *
             * @param genericArrayType The loaded generic array type.
             */
            public OfLoadedType(GenericArrayType genericArrayType) {
                this.genericArrayType = genericArrayType;
            }

            @Override
            public GenericTypeDescription getComponentType() {
                return Sort.describe(genericArrayType.getGenericComponentType());
            }
        }

        /**
         * A latent implementation of a generic array type.
         */
        public static class Latent extends ForGenericArray {

            /**
             * The component type of the generic array.
             */
            private final GenericTypeDescription componentType;

            /**
             * The arity of the generic array.
             */
            private final int arity;

            /**
             * Creates a latent representation of a generic array type.
             *
             * @param componentType The component type.
             * @param arity         The arity of this array.
             */
            protected Latent(GenericTypeDescription componentType, int arity) {
                this.componentType = componentType;
                this.arity = arity;
            }

            /**
             * Returns a description of the given component type.
             *
             * @param componentType The component type of the array type to create.
             * @param arity         The arity of the generic array to create.
             * @return A description of the requested array. If the component type is non-generic, a non-generic array type is returned.
             */
            public static GenericTypeDescription of(GenericTypeDescription componentType, int arity) {
                if (arity < 0) {
                    throw new IllegalArgumentException("Arrays cannot have a negative arity");
                }
                while (componentType.getSort().isGenericArray()) {
                    componentType = componentType.getComponentType();
                    arity++;
                }
                return arity == 0
                        ? componentType
                        : new Latent(componentType, arity);
            }

            @Override
            public GenericTypeDescription getComponentType() {
                return arity == 1
                        ? componentType
                        : new Latent(componentType, arity - 1);
            }
        }
    }

    /**
     * A base implementation of a generic type description that represents a wildcard type.
     */
    abstract class ForWildcardType implements GenericTypeDescription {

        /**
         * The source code representation of a wildcard.
         */
        public static final String SYMBOL = "?";

        @Override
        public Sort getSort() {
            return Sort.WILDCARD;
        }

        @Override
        public TypeDescription asErasure() {
            throw new IllegalStateException("A wildcard does not represent an erasable type: " + this);
        }

        @Override
        public GenericTypeDescription getSuperType() {
            throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
        }

        @Override
        public GenericTypeList getInterfaces() {
            throw new IllegalStateException("A wildcard does not imply an interface type definition: " + this);
        }

        @Override
        public FieldList getDeclaredFields() {
            throw new IllegalStateException("A wildcard does not imply field definitions: " + this);
        }

        @Override
        public MethodList getDeclaredMethods() {
            throw new IllegalStateException("A wildcard does not imply method definitions: " + this);
        }

        @Override
        public GenericTypeDescription getComponentType() {
            throw new IllegalStateException("A wildcard does not imply a component type: " + this);
        }

        @Override
        public TypeVariableSource getVariableSource() {
            throw new IllegalStateException("A wildcard does not imply a type variable source: " + this);
        }

        @Override
        public GenericTypeList getParameters() {
            throw new IllegalStateException("A wildcard does not imply type parameters: " + this);
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            throw new IllegalStateException("A wildcard does not imply an owner type: " + this);
        }

        @Override
        public String getSymbol() {
            throw new IllegalStateException("A wildcard does not imply a symbol: " + this);
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return toString();
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean represents(Type type) {
            return equals(Sort.describe(type));
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onWildcard(this);
        }

        @Override
        public StackSize getStackSize() {
            throw new IllegalStateException("A wildcard does not imply an operand stack size: " + this);
        }

        @Override
        public int hashCode() {
            int lowerHash = 1, upperHash = 1;
            for (GenericTypeDescription genericTypeDescription : getLowerBounds()) {
                lowerHash = 31 * lowerHash + genericTypeDescription.hashCode();
            }
            for (GenericTypeDescription genericTypeDescription : getUpperBounds()) {
                upperHash = 31 * upperHash + genericTypeDescription.hashCode();
            }
            return lowerHash ^ upperHash;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isWildcard()
                    && getUpperBounds().equals(genericTypeDescription.getUpperBounds())
                    && getLowerBounds().equals(genericTypeDescription.getLowerBounds());
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder(SYMBOL);
            GenericTypeList bounds = getLowerBounds();
            if (!bounds.isEmpty()) {
                stringBuilder.append(" super ");
            } else {
                bounds = getUpperBounds();
                if (bounds.getOnly().equals(TypeDescription.OBJECT)) {
                    return SYMBOL;
                }
                stringBuilder.append(" extends ");
            }
            return stringBuilder.append(bounds.getOnly().getTypeName()).toString();
        }

        /**
         * Description of a loaded wildcard.
         */
        public static class OfLoadedType extends ForWildcardType {

            /**
             * The represented loaded wildcard type.
             */
            private final WildcardType wildcardType;

            /**
             * Creates a description of a loaded wildcard.
             *
             * @param wildcardType The represented loaded wildcard type.
             */
            public OfLoadedType(WildcardType wildcardType) {
                this.wildcardType = wildcardType;
            }

            @Override
            public GenericTypeList getLowerBounds() {
                return new GenericTypeList.ForLoadedType(wildcardType.getLowerBounds());
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.ForLoadedType(wildcardType.getUpperBounds());
            }
        }

        /**
         * A latent description of a wildcard type.
         */
        public static class Latent extends ForWildcardType {

            /**
             * The wildcard's upper bounds.
             */
            private final List<? extends GenericTypeDescription> upperBounds;

            /**
             * The wildcard's lower bounds.
             */
            private final List<? extends GenericTypeDescription> lowerBounds;

            /**
             * Creates a description of a latent wildcard.
             *
             * @param upperBounds The wildcard's upper bounds.
             * @param lowerBounds The wildcard's lower bounds.
             */
            protected Latent(List<? extends GenericTypeDescription> upperBounds, List<? extends GenericTypeDescription> lowerBounds) {
                this.upperBounds = upperBounds;
                this.lowerBounds = lowerBounds;
            }

            /**
             * Creates an unbounded wildcard. Such a wildcard is implicitly bound above by the {@link Object} type.
             *
             * @return A description of an unbounded wildcard.
             */
            public static GenericTypeDescription unbounded() {
                return new Latent(Collections.singletonList(TypeDescription.OBJECT), Collections.<GenericTypeDescription>emptyList());
            }

            /**
             * Creates a wildcard with an upper bound.
             *
             * @param upperBound The upper bound of the wildcard.
             * @return A wildcard with the given upper bound.
             */
            public static GenericTypeDescription boundedAbove(GenericTypeDescription upperBound) {
                return new Latent(Collections.singletonList(upperBound), Collections.<GenericTypeDescription>emptyList());
            }

            /**
             * Creates a wildcard with a lower bound. Such a wildcard is implicitly bounded above by the {@link Object} type.
             *
             * @param lowerBound The lower bound of the wildcard.
             * @return A wildcard with the given lower bound.
             */
            public static GenericTypeDescription boundedBelow(GenericTypeDescription lowerBound) {
                return new Latent(Collections.singletonList(TypeDescription.OBJECT), Collections.singletonList(lowerBound));
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.Explicit(upperBounds);
            }

            @Override
            public GenericTypeList getLowerBounds() {
                return new GenericTypeList.Explicit(lowerBounds);
            }
        }
    }

    /**
     * A base implementation of a generic type description that represents a parameterized type.
     */
    abstract class ForParameterizedType implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return Sort.PARAMETERIZED;
        }

        @Override
        public GenericTypeDescription getSuperType() {
            return LazyProjection.OfPotentiallyRawType.of(asErasure().getSuperType(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
        }

        @Override
        public GenericTypeList getInterfaces() {
            return new GenericTypeList.OfPotentiallyRawType(asErasure().getInterfaces(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.TypeSubstituting(this, asErasure().getDeclaredFields(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.TypeSubstituting(this, asErasure().getDeclaredMethods(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
        }

        @Override
        public GenericTypeList getUpperBounds() {
            throw new IllegalStateException("A parameterized type does not imply upper bounds: " + this);
        }

        @Override
        public GenericTypeList getLowerBounds() {
            throw new IllegalStateException("A parameterized type does not imply lower bounds: " + this);
        }

        @Override
        public GenericTypeDescription getComponentType() {
            throw new IllegalStateException("A parameterized type does not imply a component type: " + this);
        }

        @Override
        public TypeVariableSource getVariableSource() {
            throw new IllegalStateException("A parameterized type does not imply a type variable source: " + this);
        }

        @Override
        public String getSymbol() {
            throw new IllegalStateException("A parameterized type does not imply a symbol: " + this);
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return toString();
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean represents(Type type) {
            return equals(Sort.describe(type));
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            return new SuperTypeIterator(this);
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onParameterizedType(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (GenericTypeDescription genericTypeDescription : getParameters()) {
                result = 31 * result + genericTypeDescription.hashCode();
            }
            GenericTypeDescription ownerType = getOwnerType();
            return result ^ (ownerType == null
                    ? asErasure().hashCode()
                    : ownerType.hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            if (!genericTypeDescription.getSort().isParameterized()) return false;
            GenericTypeDescription ownerType = getOwnerType(), otherOwnerType = genericTypeDescription.getOwnerType();
            return asErasure().equals(genericTypeDescription.asErasure())
                    && !(ownerType == null && otherOwnerType != null) && !(ownerType != null && !ownerType.equals(otherOwnerType))
                    && getParameters().equals(genericTypeDescription.getParameters());
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            GenericTypeDescription ownerType = getOwnerType();
            if (ownerType != null) {
                stringBuilder.append(ownerType.getTypeName());
                stringBuilder.append(".");
                stringBuilder.append(ownerType.getSort().isParameterized()
                        ? asErasure().getName().replace(ownerType.asErasure().getName() + "$", "")
                        : asErasure().getName());
            } else {
                stringBuilder.append(asErasure().getName());
            }
            GenericTypeList actualTypeArguments = getParameters();
            if (!actualTypeArguments.isEmpty()) {
                stringBuilder.append("<");
                boolean multiple = false;
                for (GenericTypeDescription genericTypeDescription : actualTypeArguments) {
                    if (multiple) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(genericTypeDescription.getTypeName());
                    multiple = true;
                }
                stringBuilder.append(">");
            }
            return stringBuilder.toString();
        }

        /**
         * Description of a loaded parameterized type.
         */
        public static class OfLoadedType extends ForParameterizedType {

            /**
             * The represented parameterized type.
             */
            private final ParameterizedType parameterizedType;

            /**
             * Creates a description of the loaded parameterized type.
             *
             * @param parameterizedType The represented parameterized type.
             */
            public OfLoadedType(ParameterizedType parameterizedType) {
                this.parameterizedType = parameterizedType;
            }

            @Override
            public GenericTypeList getParameters() {
                return new GenericTypeList.ForLoadedType(parameterizedType.getActualTypeArguments());
            }

            @Override
            public GenericTypeDescription getOwnerType() {
                Type ownerType = parameterizedType.getOwnerType();
                return ownerType == null
                        ? TypeDescription.UNDEFINED
                        : Sort.describe(ownerType);
            }

            @Override
            public TypeDescription asErasure() {
                return new TypeDescription.ForLoadedType((Class<?>) parameterizedType.getRawType());
            }
        }

        /**
         * A latent description of a parameterized type.
         */
        public static class Latent extends ForParameterizedType {

            /**
             * The raw type of the described parameterized type.
             */
            private final TypeDescription rawType;

            /**
             * The parameters of this parameterized type.
             */
            private final List<? extends GenericTypeDescription> parameters;

            /**
             * This parameterized type's owner type or {@code null} if no owner type exists.
             */
            private final GenericTypeDescription ownerType;

            /**
             * Creates a description of a latent parameterized type.
             *
             * @param rawType    The raw type of the described parameterized type.
             * @param parameters The parameters of this parameterized type.
             * @param ownerType  This parameterized type's owner type or {@code null} if no owner type exists.
             */
            public Latent(TypeDescription rawType, List<? extends GenericTypeDescription> parameters, GenericTypeDescription ownerType) {
                this.rawType = rawType;
                this.parameters = parameters;
                this.ownerType = ownerType;
            }

            @Override
            public TypeDescription asErasure() {
                return rawType;
            }

            @Override
            public GenericTypeList getParameters() {
                return new GenericTypeList.Explicit(parameters);
            }

            @Override
            public GenericTypeDescription getOwnerType() {
                return ownerType;
            }
        }

        /**
         * <p>
         * A raw type representation of a non-generic type. This raw type differs from a raw type in the Java programming language by
         * representing a minimal erasure compared to Java's full erasure. This means that generic types are preserved as long as they
         * do not involve a type variable. Nested type variables are erased on the deepest possible level.
         * </p>
         * <p>
         * All fields, methods, interfaces and the super type that are returned from this instance represent appropriately erased types.
         * </p>
         */
        public static class Raw implements GenericTypeDescription {

            /**
             * The represented non-generic type.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new raw type representation.
             *
             * @param typeDescription The represented non-generic type.
             */
            protected Raw(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            /**
             * Resolves a generic type to a potentially raw type. A raw type is returned if the given type declares a different number of type variables
             * than parameters.
             *
             * @param typeDescription The type to resolve as a potentially raw type. Only non-generic types
             *                        ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#NON_GENERIC}) and parameterized types
             *                        ({@link net.bytebuddy.description.type.generic.GenericTypeDescription.Sort#PARAMETERIZED}) are well-defined input.
             * @param transformer     A transformer to apply to a non-raw types.
             * @return Either a raw type, or a transformed generic type.
             */
            public static GenericTypeDescription check(GenericTypeDescription typeDescription, Visitor<? extends GenericTypeDescription> transformer) {
                return typeDescription.getParameters().size() != typeDescription.asErasure().getTypeVariables().size()
                        ? new Raw(typeDescription.asErasure())
                        : typeDescription.accept(transformer);
            }

            @Override
            public GenericTypeDescription getSuperType() {
                GenericTypeDescription superType = typeDescription.getSuperType();
                return superType == null
                        ? TypeDescription.UNDEFINED
                        : superType.accept(Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public GenericTypeList getInterfaces() {
                return typeDescription.getInterfaces().accept(Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public FieldList getDeclaredFields() {
                return new FieldList.TypeSubstituting(this, typeDescription.getDeclaredFields(), Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public MethodList getDeclaredMethods() {
                return new MethodList.TypeSubstituting(this, typeDescription.getDeclaredMethods(), Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public GenericTypeDescription getOwnerType() {
                TypeDescription ownerType = typeDescription.getOwnerType();
                return ownerType == null
                        ? TypeDescription.UNDEFINED
                        : new Raw(ownerType);
            }

            @Override
            public TypeDescription asErasure() {
                return typeDescription;
            }

            @Override
            public Sort getSort() {
                return typeDescription.getSort();
            }

            @Override
            public GenericTypeList getParameters() {
                return typeDescription.getParameters();
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onNonGenericType(this);
            }

            @Override
            public String getTypeName() {
                return typeDescription.getTypeName();
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return typeDescription.getUpperBounds();
            }

            @Override
            public GenericTypeList getLowerBounds() {
                return typeDescription.getLowerBounds();
            }

            @Override
            public GenericTypeDescription getComponentType() {
                TypeDescription componentType = typeDescription.getComponentType();
                return componentType == null
                        ? TypeDescription.UNDEFINED
                        : new Raw(componentType);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                return typeDescription.getVariableSource();
            }

            @Override
            public String getSymbol() {
                return typeDescription.getSymbol();
            }

            @Override
            public StackSize getStackSize() {
                return typeDescription.getStackSize();
            }

            @Override
            public String getSourceCodeName() {
                return typeDescription.getSourceCodeName();
            }

            @Override
            public boolean isArray() {
                return typeDescription.isArray();
            }

            @Override
            public boolean isPrimitive() {
                return typeDescription.isPrimitive();
            }

            @Override
            public boolean represents(Type type) {
                return typeDescription.represents(type);
            }

            @Override
            public Iterator<GenericTypeDescription> iterator() {
                return new SuperTypeIterator(this);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            @SuppressFBWarnings(value = "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", justification = "Type check is performed by erasure instance")
            public boolean equals(Object other) {
                return typeDescription.equals(other);
            }

            @Override
            public String toString() {
                return typeDescription.toString();
            }
        }
    }

    /**
     * A base implementation of a generic type description that represents a type variable.
     */
    abstract class ForTypeVariable implements GenericTypeDescription {

        @Override
        public Sort getSort() {
            return Sort.VARIABLE;
        }

        @Override
        public TypeDescription asErasure() {
            GenericTypeList upperBounds = getUpperBounds();
            return upperBounds.isEmpty()
                    ? TypeDescription.OBJECT
                    : upperBounds.get(0).asErasure();
        }

        @Override
        public GenericTypeDescription getSuperType() {
            throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
        }

        @Override
        public GenericTypeList getInterfaces() {
            throw new IllegalStateException("A type variable does not imply an interface type definition: " + this);
        }

        @Override
        public FieldList getDeclaredFields() {
            throw new IllegalStateException("A type variable does not imply field definitions: " + this);
        }

        @Override
        public MethodList getDeclaredMethods() {
            throw new IllegalStateException("A type variable does not imply method definitions: " + this);
        }

        @Override
        public GenericTypeDescription getComponentType() {
            throw new IllegalStateException("A type variable does not imply a component type: " + this);
        }

        @Override
        public GenericTypeList getParameters() {
            throw new IllegalStateException("A type variable does not imply type parameters: " + this);
        }

        @Override
        public GenericTypeList getLowerBounds() {
            throw new IllegalStateException("A type variable does not imply lower bounds: " + this);
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            throw new IllegalStateException("A type variable does not imply an owner type: " + this);
        }

        @Override
        public String getTypeName() {
            return toString();
        }

        @Override
        public String getSourceCodeName() {
            return getSymbol();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.onTypeVariable(this);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean represents(Type type) {
            return equals(Sort.describe(type));
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
        }

        @Override
        public int hashCode() {
            return getVariableSource().hashCode() ^ getSymbol().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GenericTypeDescription)) return false;
            GenericTypeDescription genericTypeDescription = (GenericTypeDescription) other;
            return genericTypeDescription.getSort().isTypeVariable()
                    && getSymbol().equals(genericTypeDescription.getSymbol())
                    && getVariableSource().equals(genericTypeDescription.getVariableSource());
        }

        @Override
        public String toString() {
            return getSymbol();
        }

        /**
         * An abstract implementation of a description of a type variable in detached form.
         */
        public abstract static class InDetachedForm extends ForTypeVariable {

            @Override
            public Sort getSort() {
                return Sort.VARIABLE_DETACHED;
            }

            @Override
            public TypeVariableSource getVariableSource() {
                throw new IllegalStateException("A detached type variable does not imply a source: " + this);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof GenericTypeDescription)) return false;
                GenericTypeDescription typeVariable = (GenericTypeDescription) other;
                return typeVariable.getSort().isDetachedTypeVariable()
                        && getSymbol().equals(typeVariable.getSymbol());
            }

            @Override
            public int hashCode() {
                return getSymbol().hashCode();
            }
        }

        /**
         * Description of a loaded type variable.
         */
        public static class OfLoadedType extends ForTypeVariable {

            /**
             * The represented type variable.
             */
            private final TypeVariable<?> typeVariable;

            /**
             * Creates a description of a loaded type variable.
             *
             * @param typeVariable The represented type variable.
             */
            public OfLoadedType(TypeVariable<?> typeVariable) {
                this.typeVariable = typeVariable;
            }

            @Override
            public TypeVariableSource getVariableSource() {
                GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                if (genericDeclaration instanceof Class) {
                    return new TypeDescription.ForLoadedType((Class<?>) genericDeclaration);
                } else if (genericDeclaration instanceof Method) {
                    return new MethodDescription.ForLoadedMethod((Method) genericDeclaration);
                } else if (genericDeclaration instanceof Constructor) {
                    return new MethodDescription.ForLoadedConstructor((Constructor<?>) genericDeclaration);
                } else {
                    throw new IllegalStateException("Unknown declaration: " + genericDeclaration);
                }
            }

            @Override
            public GenericTypeList getUpperBounds() {
                return new GenericTypeList.ForLoadedType(typeVariable.getBounds());
            }

            @Override
            public String getSymbol() {
                return typeVariable.getName();
            }
        }
    }

    /**
     * A lazy projection of a generic type. Such projections allow to only read generic type information in case it is required. This
     * is meaningful as the Java virtual needs to process generic type information which requires extra ressources. Also, this allows
     * the extraction of non-generic type information even if the generic type information is invalid.
     */
    abstract class LazyProjection implements GenericTypeDescription {

        /**
         * Resolves the actual generic type.
         *
         * @return An actual description of the represented generic type.
         */
        protected abstract GenericTypeDescription resolve();

        @Override
        public Sort getSort() {
            return resolve().getSort();
        }

        @Override
        public GenericTypeList getInterfaces() {
            return resolve().getInterfaces();
        }

        @Override
        public GenericTypeDescription getSuperType() {
            return resolve().getSuperType();
        }

        @Override
        public FieldList getDeclaredFields() {
            return resolve().getDeclaredFields();
        }

        @Override
        public MethodList getDeclaredMethods() {
            return resolve().getDeclaredMethods();
        }

        @Override
        public GenericTypeList getUpperBounds() {
            return resolve().getUpperBounds();
        }

        @Override
        public GenericTypeList getLowerBounds() {
            return resolve().getLowerBounds();
        }

        @Override
        public GenericTypeDescription getComponentType() {
            return resolve().getComponentType();
        }

        @Override
        public GenericTypeList getParameters() {
            return resolve().getParameters();
        }

        @Override
        public TypeVariableSource getVariableSource() {
            return resolve().getVariableSource();
        }

        @Override
        public GenericTypeDescription getOwnerType() {
            return resolve().getOwnerType();
        }

        @Override
        public String getTypeName() {
            return resolve().getTypeName();
        }

        @Override
        public String getSymbol() {
            return resolve().getSymbol();
        }

        @Override
        public String getSourceCodeName() {
            return resolve().getSourceCodeName();
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return resolve().accept(visitor);
        }

        @Override
        public StackSize getStackSize() {
            return asErasure().getStackSize();
        }

        @Override
        public boolean isArray() {
            return asErasure().isArray();
        }

        @Override
        public boolean isPrimitive() {
            return asErasure().isPrimitive();
        }

        @Override
        public boolean represents(Type type) {
            return resolve().represents(type);
        }

        @Override
        public Iterator<GenericTypeDescription> iterator() {
            return resolve().iterator();
        }

        @Override
        public int hashCode() {
            return resolve().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return resolve().equals(other);
        }

        @Override
        public String toString() {
            return resolve().toString();
        }

        /**
         * A lazy projection of potentially raw types.
         */
        public static class OfPotentiallyRawType extends LazyProjection {

            /**
             * The unresolved type to resolve.
             */
            private final GenericTypeDescription unresolvedType;

            /**
             * The transformer to apply to non-raw types.
             */
            private final GenericTypeDescription.Visitor<? extends GenericTypeDescription> transformer;

            /**
             * Creates a new lazy projection of a potentially raw type.
             *
             * @param unresolvedType The unresolved type to resolve.
             * @param transformer    The transformer to apply to non-raw types.
             */
            public OfPotentiallyRawType(GenericTypeDescription unresolvedType, Visitor<? extends GenericTypeDescription> transformer) {
                this.unresolvedType = unresolvedType;
                this.transformer = transformer;
            }

            /**
             * Creates a generic type description, either as a raw type or as a bound parameterized type.
             *
             * @param unresolvedType The unresolved type.
             * @param transformer    A transformer to apply to a non-raw type.
             * @return A lazy projection of the provided unresolved type.
             */
            public static GenericTypeDescription of(GenericTypeDescription unresolvedType, Visitor<? extends GenericTypeDescription> transformer) {
                return unresolvedType == null
                        ? TypeDescription.UNDEFINED
                        : new OfPotentiallyRawType(unresolvedType, transformer);
            }

            @Override
            protected GenericTypeDescription resolve() {
                return GenericTypeDescription.ForParameterizedType.Raw.check(unresolvedType, transformer);
            }

            @Override
            public TypeDescription asErasure() {
                return unresolvedType.asErasure();
            }
        }

        /**
         * A lazy projection of a generic super type.
         */
        public static class OfLoadedSuperType extends LazyProjection {

            /**
             * The type of which the super class is represented.
             */
            private final Class<?> type;

            /**
             * Creates a new lazy projection of a type's super class.
             *
             * @param type The type of which the super class is represented.
             */
            public OfLoadedSuperType(Class<?> type) {
                this.type = type;
            }

            @Override
            protected GenericTypeDescription resolve() {
                Type superClass = type.getGenericSuperclass();
                return superClass == null
                        ? TypeDescription.UNDEFINED
                        : Sort.describe(superClass);
            }

            @Override
            public TypeDescription asErasure() {
                Class<?> superClass = type.getSuperclass();
                return superClass == null
                        ? TypeDescription.UNDEFINED
                        : new TypeDescription.ForLoadedType(superClass);
            }
        }

        /**
         * A lazy projection of a field's type.
         */
        public static class OfLoadedFieldType extends LazyProjection {

            /**
             * The field of which the type is represented.
             */
            private final Field field;

            /**
             * Create's a lazy projection of a field type.
             *
             * @param field The field of which the type is represented.
             */
            public OfLoadedFieldType(Field field) {
                this.field = field;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(field.getGenericType());
            }

            @Override
            public TypeDescription asErasure() {
                return new TypeDescription.ForLoadedType(field.getType());
            }
        }

        /**
         * A lazy projection of a method's generic return type.
         */
        public static class OfLoadedReturnType extends LazyProjection {

            /**
             * The method which defines the return type.
             */
            private final Method method;

            /**
             * Creates a new lazy projection of a method's return type.
             *
             * @param method The method which defines the return type.
             */
            public OfLoadedReturnType(Method method) {
                this.method = method;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(method.getGenericReturnType());
            }

            @Override
            public TypeDescription asErasure() {
                return new TypeDescription.ForLoadedType(method.getReturnType());
            }
        }

        /**
         * A lazy projection of a loaded parameter.
         */
        public static class OfLoadedParameter extends LazyProjection {

            /**
             * A dispatcher for introspecting a parameter's type.
             */
            private static final Dispatcher DISPATCHER;

            /*
             * Looks up Java 7+ specific methods if possible.
             */
            static {
                Dispatcher dispatcher;
                try {
                    Class<?> parameterType = Class.forName("java.lang.reflect.Parameter");
                    dispatcher = new Dispatcher.ForModernVm(parameterType.getDeclaredMethod("getType"), parameterType.getDeclaredMethod("getParameterizedType"));
                } catch (RuntimeException exception) {
                    throw exception;
                } catch (Exception ignored) {
                    dispatcher = Dispatcher.ForLegacyVm.INSTANCE;
                }
                DISPATCHER = dispatcher;
            }

            /**
             * The represented parameter.
             */
            private final Object parameter;

            /**
             * Creates a lazy projection of a loaded parameter's type.
             *
             * @param parameter The represented parameter.
             */
            public OfLoadedParameter(Object parameter) {
                this.parameter = parameter;
            }

            @Override
            protected GenericTypeDescription resolve() {
                return Sort.describe(DISPATCHER.getParameterizedType(parameter));
            }

            @Override
            public TypeDescription asErasure() {
                return new TypeDescription.ForLoadedType(DISPATCHER.getType(parameter));
            }

            /**
             * A dispatcher for introspecting a method's types.
             */
            protected interface Dispatcher {

                /**
                 * Returns a parameter's parameterized type.
                 *
                 * @param parameter The parameter to extract the paramaterized type of.
                 * @return The parameter's generic type.
                 */
                Type getParameterizedType(Object parameter);

                /**
                 * Returns the parameter's type.
                 *
                 * @param parameter The parameter to extract the non-generic type of.
                 * @return The parameter's non-generic type.
                 */
                Class<?> getType(Object parameter);

                /**
                 * A dispatcher for a modern VM that supports the {@code java.lang.reflect.Parameter} API for Java 8+.
                 */
                class ForModernVm implements Dispatcher {

                    /**
                     * A reference to {@code java.lang.reflect.Parameter#getType}.
                     */
                    private final Method getType;

                    /**
                     * A reference to {@code java.lang.reflect.Parameter#getParameterizedType}.
                     */
                    private final Method getParameterizedType;

                    /**
                     * Creates a new dispatcher.
                     *
                     * @param getType              A reference to {@code java.lang.reflect.Parameter#getType}.
                     * @param getParameterizedType A reference to {@code java.lang.reflect.Parameter#getParameterizedType}.
                     */
                    protected ForModernVm(Method getType, Method getParameterizedType) {
                        this.getType = getType;
                        this.getParameterizedType = getParameterizedType;
                    }

                    @Override
                    public Type getParameterizedType(Object parameter) {
                        try {
                            return (Type) getParameterizedType.invoke(parameter);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access java.lang.reflect.Parameter#getParameterizedType", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.reflect.Parameter#getParameterizedType", exception.getCause());
                        }
                    }

                    @Override
                    public Class<?> getType(Object parameter) {
                        try {
                            return (Class<?>) getType.invoke(parameter);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access java.lang.reflect.Parameter#getType", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.reflect.Parameter#getType", exception.getCause());
                        }
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        ForModernVm legal = (ForModernVm) other;
                        return getType.equals(legal.getType) && getParameterizedType.equals(legal.getParameterizedType);
                    }

                    @Override
                    public int hashCode() {
                        int result = getType.hashCode();
                        result = 31 * result + getParameterizedType.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "GenericTypeDescription.LazyProjection.OfLoadedParameter.Dispatcher.ForModernVm{" +
                                "getType=" + getType +
                                ", getParameterizedType=" + getParameterizedType +
                                '}';
                    }
                }

                /**
                 * A dispatcher for a VM that does not support the {@code java.lang.reflect.Parameter} API that throws an exception
                 * for any property.
                 */
                enum ForLegacyVm implements Dispatcher {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Type getParameterizedType(Object parameter) {
                        throw new IllegalStateException("Cannot dispatch method for java.lang.reflect.Parameter");
                    }

                    @Override
                    public Class<?> getType(Object parameter) {
                        throw new IllegalStateException("Unsupported type for current JVM: java,lang.Parameter");
                    }

                    @Override
                    public String toString() {
                        return "GenericTypeDescription.LazyProjection.OfLoadedParameter.Dispatcher.ForLegacyVm." + name();
                    }
                }
            }

            /**
             * Represents a constructor's parameter on a JVM that does not know the {@code java.lang.reflect.Parameter} type.
             */
            public static class OfLegacyVmConstructor extends LazyProjection {

                /**
                 * The constructor of which a parameter type is represented.
                 */
                private final Constructor<?> constructor;

                /**
                 * The parameter's index.
                 */
                private final int index;

                /**
                 * The erasure of the parameter type.
                 */
                private final Class<?> erasure;

                /**
                 * Creates a lazy projection of a constructor's parameter.
                 *
                 * @param constructor The constructor of which a parameter type is represented.
                 * @param index       The parameter's index.
                 * @param erasure     The erasure of the parameter type.
                 */
                public OfLegacyVmConstructor(Constructor<?> constructor, int index, Class<?> erasure) {
                    this.constructor = constructor;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return Sort.describe(constructor.getGenericParameterTypes()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }

            /**
             * Represents a method's parameter on a JVM that does not know the {@code java.lang.reflect.Parameter} type.
             */
            public static class OfLegacyVmMethod extends LazyProjection {

                /**
                 * The method of which a parameter type is represented.
                 */
                private final Method method;

                /**
                 * The parameter's index.
                 */
                private final int index;

                /**
                 * The erasure of the parameter type.
                 */
                private final Class<?> erasure;

                /**
                 * Creates a lazy projection of a constructor's parameter.
                 *
                 * @param method  The method of which a parameter type is represented.
                 * @param index   The parameter's index.
                 * @param erasure The erasure of the parameter's type.
                 */
                public OfLegacyVmMethod(Method method, int index, Class<?> erasure) {
                    this.method = method;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    return Sort.describe(method.getGenericParameterTypes()[index]);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }
    }

    /**
     * An iterator that iterates over a type's class hierarchy.
     */
    class SuperTypeIterator implements Iterator<GenericTypeDescription> {

        /**
         * The next type to represent.
         */
        private GenericTypeDescription nextType;

        /**
         * Creates a new iterator.
         *
         * @param initialType The initial type of this iterator.
         */
        public SuperTypeIterator(GenericTypeDescription initialType) {
            nextType = initialType;
        }

        @Override
        public boolean hasNext() {
            return nextType != null;
        }

        @Override
        public GenericTypeDescription next() {
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
            return "GenericTypeDescription.SuperTypeIterator{" +
                    "nextType=" + nextType +
                    '}';
        }
    }
}
