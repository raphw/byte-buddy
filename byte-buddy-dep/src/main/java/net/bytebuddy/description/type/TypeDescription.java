package net.bytebuddy.description.type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface. Instances of this interface always
 * represent non-generic types of sort {@link Generic.Sort#NON_GENERIC}.
 */
public interface TypeDescription extends TypeDefinition, TypeVariableSource {

    /**
     * A representation of the {@link java.lang.Object} type.
     */
    TypeDescription OBJECT = new ForLoadedType(Object.class);

    /**
     * A representation of the {@link java.lang.String} type.
     */
    TypeDescription STRING = new ForLoadedType(String.class);

    /**
     * A representation of the {@link java.lang.Class} type.
     */
    TypeDescription CLASS = new ForLoadedType(Class.class);

    /**
     * A representation of the {@code void} non-type.
     */
    TypeDescription VOID = new ForLoadedType(void.class);

    /**
     * The modifiers of any array type.
     */
    int ARRAY_MODIFIERS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT;

    /**
     * A list of interfaces that are implicitly implemented by any array type.
     */
    TypeList.Generic ARRAY_INTERFACES = new TypeList.Generic.ForLoadedTypes(Cloneable.class, Serializable.class);

    /**
     * Represents any undefined property of a type description that is instead represented as {@code null} in order
     * to resemble the Java reflection API which returns {@code null} and is intuitive to many Java developers.
     */
    TypeDescription UNDEFINED = null;

    @Override
    FieldList<FieldDescription.InDefinedShape> getDeclaredFields();

    @Override
    MethodList<MethodDescription.InDefinedShape> getDeclaredMethods();

    /**
     * Checks if {@code value} is an instance of the type represented by this instance.
     *
     * @param value The object of interest.
     * @return {@code true} if the object is an instance of the type described by this instance.
     */
    boolean isInstance(Object value);

    /**
     * Checks if {@code value} is an instance of the type represented by this instance or a wrapper instance of the
     * corresponding primitive value.
     *
     * @param value The object of interest.
     * @return {@code true} if the object is an instance or wrapper of the type described by this instance.
     */
    boolean isInstanceOrWrapper(Object value);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Foo.class.isAssignableFrom(Bar.class)}.
     *
     * @param type The type of interest.
     * @return {@code true} if this type is assignable from {@code type}.
     */
    boolean isAssignableFrom(Class<?> type);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Foo.class.isAssignableFrom(Bar.class)}.
     * <p>&nbsp;</p>
     * Implementations of this methods are allowed to delegate to
     * {@link TypeDescription#isAssignableFrom(Class)}
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type is assignable from {@code type}.
     */
    boolean isAssignableFrom(TypeDescription typeDescription);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Bar.class.isAssignableTo(Foo.class)}.
     *
     * @param type The type of interest.
     * @return {@code true} if this type is assignable to {@code type}.
     */
    boolean isAssignableTo(Class<?> type);

    /**
     * Checks if this type is assignable from the type described by this instance, for example for
     * {@code class Foo} and {@code class Bar extends Foo}, this method would return {@code true} for
     * {@code Bar.class.isAssignableFrom(Foo.class)}.
     * <p>&nbsp;</p>
     * Implementations of this methods are allowed to delegate to
     * {@link TypeDescription#isAssignableTo(Class)}
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type is assignable to {@code type}.
     */
    boolean isAssignableTo(TypeDescription typeDescription);

    @Override
    TypeDescription getComponentType();

    @Override
    TypeDescription getDeclaringType();

    /**
     * Returns a list of types that are declared by this type excluding anonymous classes.
     *
     * @return A list of types that are declared within this type.
     */
    TypeList getDeclaredTypes();

    /**
     * Returns a description of the enclosing method of this type.
     *
     * @return A description of the enclosing method of this type or {@code null} if there is no such method.
     */
    MethodDescription getEnclosingMethod();

    /**
     * Returns a description of the enclosing type of this type.
     *
     * @return A  description of the enclosing type of this type or {@code null} if there is no such type.
     */
    TypeDescription getEnclosingType();

    /**
     * <p>
     * Returns the type's actual modifiers as present in the class file. For example, a type cannot be {@code private}.
     * but it modifiers might reflect this property nevertheless if a class was defined as a private inner class.
     * </p>
     * <p>
     * Unfortunately, the modifier for marking a {@code static} class collides with the {@code SUPER} modifier such
     * that these flags are indistinguishable. Therefore, the flag must be specified manually.
     * </p>
     *
     * @param superFlag {@code true} if the super flag should be set.
     * @return The type's actual modifiers.
     */
    int getActualModifiers(boolean superFlag);

    /**
     * Returns the simple internalName of this type.
     *
     * @return The simple internalName of this type.
     */
    String getSimpleName();

    /**
     * Returns the canonical name of this type if it exists.
     *
     * @return The canonical name of this type. Might be {@code null}.
     */
    String getCanonicalName();

    /**
     * Checks if this type description represents an anonymous type.
     *
     * @return {@code true} if this type description represents an anonymous type.
     */
    boolean isAnonymousClass();

    /**
     * Checks if this type description represents a local type.
     *
     * @return {@code true} if this type description represents a local type.
     */
    boolean isLocalClass();

    /**
     * Checks if this type description represents a member type.
     *
     * @return {@code true} if this type description represents a member type.
     */
    boolean isMemberClass();

    /**
     * Returns the package internalName of the type described by this instance.
     *
     * @return The package internalName of the type described by this instance.
     */
    PackageDescription getPackage();

    /**
     * Returns the annotations that this type declares or inherits from super types.
     *
     * @return A list of all inherited annotations.
     */
    AnnotationList getInheritedAnnotations();

    /**
     * Checks if two types are defined in the same package.
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type and the given type are in the same package.
     */
    boolean isSamePackage(TypeDescription typeDescription);

    /**
     * Checks if instances of this type can be stored in the constant pool of a class. Note that any primitive
     * type that is smaller than an {@code int} cannot be stored in the constant pool as those types are represented
     * as {@code int} values internally.
     *
     * @return {@code true} if instances of this type can be stored in the constant pool of a class.
     */
    boolean isConstantPool();

    /**
     * Checks if this type represents a wrapper type for a primitive type. The {@link java.lang.Void} type is
     * not considered to be a wrapper type.
     *
     * @return {@code true} if this type represents a wrapper type.
     */
    boolean isPrimitiveWrapper();

    /**
     * Checks if instances of this type can be returned from an annotation method.
     *
     * @return {@code true} if instances of this type can be returned from an annotation method.
     */
    boolean isAnnotationReturnType();

    /**
     * Checks if instances of this type can be used for describing an annotation value.
     *
     * @return {@code true} if instances of this type can be used for describing an annotation value.
     */
    boolean isAnnotationValue();

    /**
     * Checks if instances of this type can be used for describing the given annotation value.
     *
     * @param value The value that is supposed to describe the annotation value for this instance.
     * @return {@code true} if instances of this type can be used for describing the given annotation value..
     */
    boolean isAnnotationValue(Object value);

    /**
     * Represents a generic type of the Java programming language. A non-generic {@link TypeDescription} is considered to be
     * a specialization of a generic type.
     */
    interface Generic extends TypeDefinition {

        /**
         * A representation of the {@link Object} type.
         */
        Generic OBJECT = new OfNonGenericType.ForLoadedType(Object.class);

        /**
         * A representation of the {@code void} non-type.
         */
        Generic VOID = new OfNonGenericType.ForLoadedType(void.class);

        Generic ANNOTATION = new OfNonGenericType.ForLoadedType(Annotation.class);

        Generic UNDEFINED = null;

        Generic asRawType();

        /**
         * <p>
         * Returns the upper bounds of this type. Any type with a well-defined upper bound is bound by at least one type. If no such
         * type is defined, the bound is implicitly {@link Object}.
         * </p>
         * <p>
         * Only non-symbolic type variables ({@link net.bytebuddy.description.type.TypeDefinition.Sort#VARIABLE}, and wildcard types
         * ({@link net.bytebuddy.description.type.TypeDefinition.Sort#WILDCARD}) have well-defined upper bounds. For other
         * types, an {@link IllegalStateException} is thrown.
         * </p>
         *
         * @return The upper bounds of this type.
         */
        TypeList.Generic getUpperBounds();

        /**
         * <p>
         * Returns the lower bounds of this type.
         * </p>
         * <p>
         * Only wildcard types ({@link Sort#WILDCARD}) define a lower bound. For other
         * types, an {@link IllegalStateException} is thrown.
         * </p>
         *
         * @return The lower bounds of this type.
         */
        TypeList.Generic getLowerBounds();

        /**
         * <p>
         * Returns the type parameters of this type.
         * </p>
         * <p>
         * Parameters are only well-defined for parameterized types ({@link Sort#PARAMETERIZED}).
         * For all other types, this method throws an {@link IllegalStateException}.
         * </p>
         *
         * @return A list of this type's type parameters.
         */
        TypeList.Generic getParameters();

        /**
         * <p>
         * Returns the owner type of this type.
         * </p>
         * <p>
         * An owner type is only well-defined for parameterized types ({@link Sort#PARAMETERIZED}).
         * For all other types, this method throws an {@link IllegalStateException}.
         * </p>
         *
         * @return This type's owner type or {@code null} if no such owner type exists.
         */
        Generic getOwnerType();

        /**
         * Returns the source of this type variable. A type variable source is only well-defined for an attached type variable
         * ({@link Sort#VARIABLE}. For other types, this method
         * throws an {@link IllegalStateException}.
         *
         * @return This type's type variable source.
         */
        TypeVariableSource getVariableSource();

        /**
         * Returns the symbol of this type variable. A symbol is only well-defined for type variables
         * ({@link Sort#VARIABLE}, {@link Sort#VARIABLE_SYMBOLIC}). For other types, this method
         * throws an {@link IllegalStateException}.
         *
         * @return This type's type variable symbol.
         */
        String getSymbol();

        @Override
        Generic getComponentType();

        @Override
        FieldList<FieldDescription.InGenericShape> getDeclaredFields();

        @Override
        MethodList<MethodDescription.InGenericShape> getDeclaredMethods();

        /**
         * Applies a visitor to this generic type description.
         *
         * @param visitor The visitor to apply.
         * @param <T>     The value that this visitor yields.
         * @return The visitor's return value.
         */
        <T> T accept(Visitor<T> visitor);

        /**
         * A visitor that can be applied to a {@link Generic} for differentiating on the sort of the visited type.
         *
         * @param <T> The visitor's return value's type.
         */
        interface Visitor<T> {

            /**
             * Visits a generic array type ({@link Sort#GENERIC_ARRAY}).
             *
             * @param genericArray The generic array type.
             * @return The visitor's return value.
             */
            T onGenericArray(Generic genericArray);

            /**
             * Visits a wildcard ({@link Sort#WILDCARD}).
             *
             * @param wildcard The wildcard.
             * @return The visitor's return value.
             */
            T onWildcard(Generic wildcard);

            /**
             * Visits a parameterized type ({@link Sort#PARAMETERIZED}).
             *
             * @param parameterizedType The generic array type.
             * @return The visitor's return value.
             */
            T onParameterizedType(Generic parameterizedType);

            /**
             * Visits a type variable ({@link Sort#VARIABLE}, {@link Sort#VARIABLE_SYMBOLIC}).
             *
             * @param typeVariable The generic array type.
             * @return The visitor's return value.
             */
            T onTypeVariable(Generic typeVariable);

            /**
             * Visits a non-generic type ({@link Sort#NON_GENERIC}).
             *
             * @param typeDescription The non-generic type.
             * @return The visitor's return value.
             */
            T onNonGenericType(Generic typeDescription);

            /**
             * A non-operational generic type visitor. Any visited type is returned in its existing form.
             */
            enum NoOp implements Visitor<Generic> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Generic onGenericArray(Generic genericArray) {
                    return genericArray;
                }

                @Override
                public Generic onWildcard(Generic wildcard) {
                    return wildcard;
                }

                @Override
                public Generic onParameterizedType(Generic parameterizedType) {
                    return parameterizedType;
                }

                @Override
                public Generic onTypeVariable(Generic typeVariable) {
                    return typeVariable;
                }

                @Override
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription;
                }

                @Override
                public String toString() {
                    return "TypeDescription.Generic.Visitor.NoOp." + name();
                }
            }

            /**
             * A visitor that returns the erasure of any visited type. For wildcard types, an exception is thrown.
             */
            enum TypeErasing implements Visitor<Generic> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Generic onGenericArray(Generic genericArray) {
                    return genericArray.asRawType();
                }

                @Override
                public Generic onWildcard(Generic wildcard) {
                    throw new IllegalArgumentException("Cannot erase a wildcard type: " + wildcard);
                }

                @Override
                public Generic onParameterizedType(Generic parameterizedType) {
                    return parameterizedType.asRawType();
                }

                @Override
                public Generic onTypeVariable(Generic typeVariable) {
                    return typeVariable.asRawType();
                }

                @Override
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription.asRawType();
                }

                @Override
                public String toString() {
                    return "TypeDescription.Generic.Visitor.TypeErasing." + name();
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
            enum TypeVariableErasing implements Visitor<Generic> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Generic onGenericArray(Generic genericArray) {
                    return OfGenericArray.Latent.of(genericArray.getComponentType().accept(this), 1);
                }

                @Override
                public Generic onWildcard(Generic wildcard) {
                    // Wildcards which are used within parameterized types are taken care of by the calling method.
                    TypeList.Generic lowerBounds = wildcard.getLowerBounds();
                    return lowerBounds.isEmpty()
                            ? OfWildcardType.Latent.boundedAbove(wildcard.getUpperBounds().getOnly().accept(this))
                            : OfWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
                }

                @Override
                public Generic onParameterizedType(Generic parameterizedType) {
                    List<Generic> parameters = new ArrayList<Generic>(parameterizedType.getParameters().size());
                    for (Generic parameter : parameterizedType.getParameters()) {
                        if (parameter.accept(TypeVariableErasing.PartialErasureReviser.INSTANCE)) {
                            return parameterizedType.asRawType();
                        }
                        parameters.add(parameter.accept(this));
                    }
                    Generic ownerType = parameterizedType.getOwnerType();
                    return new OfParameterizedType.Latent(parameterizedType.asErasure(),
                            ownerType == null
                                    ? UNDEFINED
                                    : ownerType.accept(this),
                            parameters);
                }

                @Override
                public Generic onTypeVariable(Generic typeVariable) {
                    return typeVariable.asRawType();
                }

                @Override
                public Generic onNonGenericType(Generic typeDescription) {
                    return new OfNonGenericType.Latent(typeDescription.asErasure());
                }

                @Override
                public String toString() {
                    return "TypeDescription.Generic.Visitor.TypeVariableErasing." + name();
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
                    public Boolean onGenericArray(Generic genericArray) {
                        return genericArray.getComponentType().accept(this);
                    }

                    @Override
                    public Boolean onWildcard(Generic wildcard) {
                        TypeList.Generic lowerBounds = wildcard.getLowerBounds();
                        return lowerBounds.isEmpty()
                                ? wildcard.getUpperBounds().getOnly().accept(this)
                                : lowerBounds.getOnly().accept(this);
                    }

                    @Override
                    public Boolean onParameterizedType(Generic parameterizedType) {
                        return false;
                    }

                    @Override
                    public Boolean onTypeVariable(Generic typeVariable) {
                        return true;
                    }

                    @Override
                    public Boolean onNonGenericType(Generic typeDescription) {
                        return false;
                    }

                    @Override
                    public String toString() {
                        return "TypeDescription.Generic.Visitor.TypeVariableErasing.PartialErasureReviser." + name();
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
                public SignatureVisitor onGenericArray(Generic genericArray) {
                    genericArray.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onWildcard(Generic wildcard) {
                    throw new IllegalStateException("Unexpected wildcard: " + wildcard);
                }

                @Override
                public SignatureVisitor onParameterizedType(Generic parameterizedType) {
                    onOwnableType(parameterizedType);
                    signatureVisitor.visitEnd();
                    return signatureVisitor;
                }

                /**
                 * Visits a type which might define an owner type.
                 *
                 * @param ownableType The visited generic type.
                 */
                private void onOwnableType(Generic ownableType) {
                    Generic ownerType = ownableType.getOwnerType();
                    if (ownerType != null && ownerType.getSort().isParameterized()) {
                        onOwnableType(ownerType);
                        signatureVisitor.visitInnerClassType(ownableType.asErasure().getSimpleName());
                    } else {
                        signatureVisitor.visitClassType(ownableType.asErasure().getInternalName());
                    }
                    for (Generic upperBound : ownableType.getParameters()) {
                        upperBound.accept(new ForSignatureVisitor.OfParameter(signatureVisitor));
                    }
                }

                @Override
                public SignatureVisitor onTypeVariable(Generic typeVariable) {
                    signatureVisitor.visitTypeVariable(typeVariable.getSymbol());
                    return signatureVisitor;
                }

                @Override
                public SignatureVisitor onNonGenericType(Generic typeDescription) {
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
                    return "TypeDescription.Generic.Visitor.ForSignatureVisitor{" +
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
                    public SignatureVisitor onWildcard(Generic wildcard) {
                        TypeList.Generic upperBounds = wildcard.getUpperBounds();
                        TypeList.Generic lowerBounds = wildcard.getLowerBounds();
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
                    public SignatureVisitor onGenericArray(Generic genericArray) {
                        genericArray.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    @Override
                    public SignatureVisitor onParameterizedType(Generic parameterizedType) {
                        parameterizedType.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    @Override
                    public SignatureVisitor onTypeVariable(Generic typeVariable) {
                        typeVariable.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    @Override
                    public SignatureVisitor onNonGenericType(Generic typeDescription) {
                        typeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    @Override
                    public String toString() {
                        return "TypeDescription.Generic.Visitor.ForSignatureVisitor.OfParameter{}";
                    }
                }
            }

            /**
             * An abstract implementation of a visitor that substitutes generic types by replacing (nested)
             * type variables and/or non-generic component types.
             */
            abstract class Substitutor implements Visitor<Generic> {

                @Override
                public Generic onParameterizedType(Generic parameterizedType) {
                    Generic ownerType = parameterizedType.getOwnerType();
                    List<Generic> parameters = new ArrayList<Generic>(parameterizedType.getParameters().size());
                    for (Generic parameter : parameterizedType.getParameters()) {
                        parameters.add(parameter.accept(this));
                    }
                    return new OfParameterizedType.Latent(parameterizedType.asRawType().accept(this).asErasure(),
                            ownerType == null
                                    ? UNDEFINED
                                    : ownerType.accept(this),
                            parameters);
                }

                @Override
                public Generic onGenericArray(Generic genericArray) {
                    return OfGenericArray.Latent.of(genericArray.getComponentType().accept(this), 1);
                }

                @Override
                public Generic onWildcard(Generic wildcard) {
                    TypeList.Generic lowerBounds = wildcard.getLowerBounds();
                    return lowerBounds.isEmpty()
                            ? OfWildcardType.Latent.boundedAbove(wildcard.getUpperBounds().getOnly().accept(this))
                            : OfWildcardType.Latent.boundedBelow(lowerBounds.getOnly().accept(this));
                }

                @Override
                public Generic onNonGenericType(Generic typeDescription) {
                    int arity = 0;
                    while (typeDescription.isArray()) {
                        typeDescription = typeDescription.getComponentType();
                        arity++;
                    }
                    return OfGenericArray.Latent.of(onSimpleType(typeDescription), arity);
                }

                /**
                 * Visits a simple, non-generic type, i.e. either a component type of an array or a non-array type.
                 *
                 * @param typeDescription The type that is visited.
                 * @return The substituted type.
                 */
                protected abstract Generic onSimpleType(Generic typeDescription);

                /**
                 * A substitutor that attaches type variables to a type variable source and replaces representations of
                 * {@link TargetType} with a given declaring type.
                 */
                public static class ForAttachment extends Substitutor {

                    /**
                     * The declaring type which is filled in for {@link TargetType}.
                     */
                    private final Generic declaringType;

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
                    protected ForAttachment(Generic declaringType, TypeVariableSource typeVariableSource) {
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
                        return new ForAttachment(fieldDescription.getDeclaringType().asGenericType(), fieldDescription.getDeclaringType().asErasure());
                    }

                    /**
                     * Attaches all types to the given method description.
                     *
                     * @param methodDescription The method description to which visited types should be attached to.
                     * @return A substitutor that attaches visited types to the given method's type context.
                     */
                    public static ForAttachment of(MethodDescription methodDescription) {
                        return new ForAttachment(methodDescription.getDeclaringType().asGenericType(), methodDescription);
                    }

                    /**
                     * Attaches all types to the given parameter description.
                     *
                     * @param parameterDescription The parameter description to which visited types should be attached to.
                     * @return A substitutor that attaches visited types to the given parameter's type context.
                     */
                    public static ForAttachment of(ParameterDescription parameterDescription) {
                        return new ForAttachment(parameterDescription.getDeclaringMethod().getDeclaringType().asGenericType(), parameterDescription.getDeclaringMethod());
                    }

                    /**
                     * Attaches all types to the given type description.
                     *
                     * @param typeDescription The type description to which visited types should be attached to.
                     * @return A substitutor that attaches visited types to the given type's type context.
                     */
                    public static ForAttachment of(TypeDescription typeDescription) {
                        return new ForAttachment(typeDescription.asGenericType(), typeDescription);
                    }

                    @Override
                    public Generic onTypeVariable(Generic typeVariable) {
                        Generic attachedVariable = typeVariableSource.findVariable(typeVariable.getSymbol());
                        if (attachedVariable == null) {
                            throw new IllegalArgumentException("Cannot attach undefined variable: " + typeVariable);
                        } else {
                            return attachedVariable;
                        }
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
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
                        return "TypeDescription.Generic.Visitor.Substitutor.ForAttachment{" +
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
                     * Creates a visitor for detaching a type.
                     *
                     * @param typeMatcher A type matcher for identifying the declaring type.
                     */
                    public ForDetachment(ElementMatcher<? super TypeDescription> typeMatcher) {
                        this.typeMatcher = typeMatcher;
                    }

                    @Override
                    public Generic onTypeVariable(Generic typeVariable) {
                        return new OfTypeVariable.Symbolic(typeVariable.getSymbol());
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
                        return typeMatcher.matches(typeDescription.asErasure())
                                ? TargetType.GENERIC_DESCRIPTION
                                : typeDescription;
                    }

                    @Override
                    public String toString() {
                        return "TypeDescription.Generic.Visitor.Substitutor.ForDetachment{" +
                                "typeMatcher=" + typeMatcher +
                                '}';
                    }
                }

                /**
                 * A visitor for binding type variables to their values.
                 */
                public static class ForTypeVariableBinding extends Substitutor {

                    /**
                     * Bindings of type variables to their substitution values.
                     */
                    private final Map<Generic, Generic> bindings;

                    /**
                     * Creates a new visitor for a type variable bindings.
                     *
                     * @param bindings Bindings of type variables to their substitution values.
                     */
                    protected ForTypeVariableBinding(Map<Generic, Generic> bindings) {
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
                    public static Visitor<Generic> bind(Generic typeDescription) {
                        Map<Generic, Generic> bindings = new HashMap<Generic, Generic>();
                        do {
                            TypeList.Generic parameters = typeDescription.getParameters();
                            TypeList.Generic typeVariables = typeDescription.asErasure().getTypeVariables();
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
                    public Generic onTypeVariable(Generic typeVariable) {
                        Generic substitution = bindings.get(typeVariable);
                        if (substitution == null) {
                            throw new IllegalStateException("Unknown type variable: " + typeVariable);
                        } else {
                            return substitution;
                        }
                    }

                    @Override
                    public Generic onNonGenericType(Generic typeDescription) {
                        return typeDescription;
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
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
                        return "TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding{" +
                                "bindings=" + bindings +
                                '}';
                    }
                }
            }
        }

        abstract class AbstractBase implements Generic {

            @Override
            public Generic asGenericType() {
                return this;
            }

            @Override
            public Generic asRawType() {
                return asErasure().asGenericType();
            }

            @Override
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
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
        abstract class OfNonGenericType extends AbstractBase {

            @Override
            public Generic getSuperType() {
                Generic superType = asErasure().getSuperType();
                return superType == null
                        ? UNDEFINED
                        : superType.accept(Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public TypeList.Generic getInterfaces() {
                return new TypeList.Generic.ForDetachedTypes(asErasure().getInterfaces(), Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return new FieldList.TypeSubstituting(this, asErasure().getDeclaredFields(), Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return new MethodList.TypeSubstituting(this, asErasure().getDeclaredMethods(), Visitor.TypeVariableErasing.INSTANCE);
            }

            @Override
            public Generic getOwnerType() {
                throw new IllegalStateException("A non-generic type does not imply an owner type: " + this);
            }

            @Override
            public Sort getSort() {
                return Sort.NON_GENERIC;
            }

            @Override
            public TypeList.Generic getParameters() {
                throw new IllegalStateException("A non-generic type does not imply an parameter types: " + this);
            }

            @Override
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onNonGenericType(this);
            }

            @Override
            public String getTypeName() {
                return asErasure().getTypeName();
            }

            @Override
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A non-generic type does not imply upper type bounds: " + this);
            }

            @Override
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A non-generic type does not imply lower type bounds: " + this);
            }

            @Override
            public Generic getComponentType() {
                TypeDescription componentType = asErasure().getComponentType();
                return componentType == null
                        ? UNDEFINED
                        : new OfNonGenericType.Latent(componentType);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                throw new IllegalStateException("A non-generic type does not imply a type variable source: " + this);
            }

            @Override
            public String getSymbol() {
                throw new IllegalStateException("A non-generic type does not imply a symbol: " + this);
            }

            @Override
            public StackSize getStackSize() {
                return asErasure().getStackSize();
            }

            @Override
            public String getSourceCodeName() {
                return asErasure().getSourceCodeName();
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
            public boolean represents(java.lang.reflect.Type type) {
                return asErasure().represents(type);
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
                return new SuperTypeIterator(this);
            }

            @Override
            public int hashCode() {
                return asErasure().hashCode();
            }

            @Override
            @SuppressFBWarnings(value = "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", justification = "Type check is performed by erasure instance")
            public boolean equals(Object other) {
                return asErasure().equals(other);
            }

            @Override
            public String toString() {
                return asErasure().toString();
            }

            /**
             * Represents a non-generic type for a loaded {@link Class}.
             */
            public static class ForLoadedType extends OfNonGenericType {

                /**
                 * The type that this instance represents.
                 */
                private final Class<?> type;

                /**
                 * Creates a new description of a generic type of a loaded type.
                 *
                 * @param type The represented type.
                 */
                public ForLoadedType(Class<?> type) {
                    this.type = type;
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(type);
                }
            }

            /**
             * Represents a non-generic type for a loaded {@link TypeDescription}.
             */
            public static class Latent extends OfNonGenericType {

                /**
                 * The represented non-generic type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new raw type representation.
                 *
                 * @param typeDescription The represented non-generic type.
                 */
                public Latent(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                @Override
                public TypeDescription asErasure() {
                    return typeDescription;
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a potentially generic array. Instances represent a non-generic type
         * if the given component type is non-generic.
         */
        abstract class OfGenericArray extends AbstractBase {

            @Override
            public Sort getSort() {
                return getComponentType().getSort().isNonGeneric()
                        ? Sort.NON_GENERIC
                        : Sort.GENERIC_ARRAY;
            }

            @Override
            public TypeDescription asErasure() {
                return ArrayProjection.of(getComponentType().asErasure(), 1);
            }

            @Override
            public Generic getSuperType() {
                return TypeDescription.Generic.OBJECT;
            }

            @Override
            public TypeList.Generic getInterfaces() {
                return ARRAY_INTERFACES;
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return new FieldList.Empty<FieldDescription.InGenericShape>();
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return new MethodList.Empty<MethodDescription.InGenericShape>();
            }

            @Override
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A generic array type does not imply upper type bounds: " + this);
            }

            @Override
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A generic array type does not imply lower type bounds: " + this);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                throw new IllegalStateException("A generic array type does not imply a type variable source: " + this);
            }

            @Override
            public TypeList.Generic getParameters() {
                throw new IllegalStateException("A generic array type does not imply type parameters: " + this);
            }

            @Override
            public Generic getOwnerType() {
                throw new IllegalStateException("A generic array type does not imply an owner type: " + this);
            }

            @Override
            public String getSymbol() {
                throw new IllegalStateException("A generic array type does not imply a symbol: " + this);
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
            public boolean isArray() {
                return true;
            }

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
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
                if (!(other instanceof Generic)) return false;
                Generic typeDescription = (Generic) other;
                return typeDescription.getSort().isGenericArray() && getComponentType().equals(typeDescription.getComponentType());
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
            public static class ForLoadedType extends OfGenericArray {

                /**
                 * The loaded generic array type.
                 */
                private final GenericArrayType genericArrayType;

                /**
                 * Creates a type description of the given generic array type.
                 *
                 * @param genericArrayType The loaded generic array type.
                 */
                public ForLoadedType(GenericArrayType genericArrayType) {
                    this.genericArrayType = genericArrayType;
                }

                @Override
                public Generic getComponentType() {
                    return Sort.describe(genericArrayType.getGenericComponentType());
                }
            }

            /**
             * A latent implementation of a generic array type.
             */
            public static class Latent extends OfGenericArray {

                /**
                 * The component type of the generic array.
                 */
                private final Generic componentType;

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
                protected Latent(Generic componentType, int arity) {
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
                public static Generic of(Generic componentType, int arity) {
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
                public Generic getComponentType() {
                    return arity == 1
                            ? componentType
                            : new Latent(componentType, arity - 1);
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a wildcard type.
         */
        abstract class OfWildcardType extends AbstractBase {

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
            public Generic getSuperType() {
                throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
            }

            @Override
            public TypeList.Generic getInterfaces() {
                throw new IllegalStateException("A wildcard does not imply an interface type definition: " + this);
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                throw new IllegalStateException("A wildcard does not imply field definitions: " + this);
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                throw new IllegalStateException("A wildcard does not imply method definitions: " + this);
            }

            @Override
            public Generic getComponentType() {
                throw new IllegalStateException("A wildcard does not imply a component type: " + this);
            }

            @Override
            public TypeVariableSource getVariableSource() {
                throw new IllegalStateException("A wildcard does not imply a type variable source: " + this);
            }

            @Override
            public TypeList.Generic getParameters() {
                throw new IllegalStateException("A wildcard does not imply type parameters: " + this);
            }

            @Override
            public Generic getOwnerType() {
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
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
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
                for (Generic lowerBound : getLowerBounds()) {
                    lowerHash = 31 * lowerHash + lowerBound.hashCode();
                }
                for (Generic upperBound : getUpperBounds()) {
                    upperHash = 31 * upperHash + upperBound.hashCode();
                }
                return lowerHash ^ upperHash;
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Generic)) return false;
                Generic typeDescription = (Generic) other;
                return typeDescription.getSort().isWildcard()
                        && getUpperBounds().equals(typeDescription.getUpperBounds())
                        && getLowerBounds().equals(typeDescription.getLowerBounds());
            }

            @Override
            public String toString() {
                StringBuilder stringBuilder = new StringBuilder(SYMBOL);
                TypeList.Generic bounds = getLowerBounds();
                if (!bounds.isEmpty()) {
                    stringBuilder.append(" super ");
                } else {
                    bounds = getUpperBounds();
                    if (bounds.getOnly().equals(TypeDescription.Generic.OBJECT)) {
                        return SYMBOL;
                    }
                    stringBuilder.append(" extends ");
                }
                return stringBuilder.append(bounds.getOnly().getTypeName()).toString();
            }

            /**
             * Description of a loaded wildcard.
             */
            public static class ForLoadedType extends OfWildcardType {

                /**
                 * The represented loaded wildcard type.
                 */
                private final WildcardType wildcardType;

                /**
                 * Creates a description of a loaded wildcard.
                 *
                 * @param wildcardType The represented loaded wildcard type.
                 */
                public ForLoadedType(WildcardType wildcardType) {
                    this.wildcardType = wildcardType;
                }

                @Override
                public TypeList.Generic getLowerBounds() {
                    return new TypeList.Generic.ForLoadedTypes(wildcardType.getLowerBounds());
                }

                @Override
                public TypeList.Generic getUpperBounds() {
                    return new TypeList.Generic.ForLoadedTypes(wildcardType.getUpperBounds());
                }
            }

            /**
             * A latent description of a wildcard type.
             */
            public static class Latent extends OfWildcardType {

                /**
                 * The wildcard's upper bounds.
                 */
                private final List<? extends Generic> upperBounds;

                /**
                 * The wildcard's lower bounds.
                 */
                private final List<? extends Generic> lowerBounds;

                /**
                 * Creates a description of a latent wildcard.
                 *
                 * @param upperBounds The wildcard's upper bounds.
                 * @param lowerBounds The wildcard's lower bounds.
                 */
                protected Latent(List<? extends Generic> upperBounds, List<? extends Generic> lowerBounds) {
                    this.upperBounds = upperBounds;
                    this.lowerBounds = lowerBounds;
                }

                /**
                 * Creates an unbounded wildcard. Such a wildcard is implicitly bound above by the {@link Object} type.
                 *
                 * @return A description of an unbounded wildcard.
                 */
                public static Generic unbounded() {
                    return new Latent(Collections.singletonList(TypeDescription.Generic.OBJECT), Collections.<Generic>emptyList());
                }

                /**
                 * Creates a wildcard with an upper bound.
                 *
                 * @param upperBound The upper bound of the wildcard.
                 * @return A wildcard with the given upper bound.
                 */
                public static Generic boundedAbove(Generic upperBound) {
                    return new Latent(Collections.singletonList(upperBound), Collections.<Generic>emptyList());
                }

                /**
                 * Creates a wildcard with a lower bound. Such a wildcard is implicitly bounded above by the {@link Object} type.
                 *
                 * @param lowerBound The lower bound of the wildcard.
                 * @return A wildcard with the given lower bound.
                 */
                public static Generic boundedBelow(Generic lowerBound) {
                    return new Latent(Collections.singletonList(TypeDescription.Generic.OBJECT), Collections.singletonList(lowerBound));
                }

                @Override
                public TypeList.Generic getUpperBounds() {
                    return new TypeList.Generic.Explicit(upperBounds);
                }

                @Override
                public TypeList.Generic getLowerBounds() {
                    return new TypeList.Generic.Explicit(lowerBounds);
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a parameterized type.
         */
        abstract class OfParameterizedType extends AbstractBase {

            @Override
            public Sort getSort() {
                return Sort.PARAMETERIZED;
            }

            @Override
            public Generic getSuperType() {
                Generic superType = asErasure().getSuperType();
                return superType == null
                        ? UNDEFINED
                        : superType.accept(Visitor.Substitutor.ForTypeVariableBinding.bind(this));
            }

            @Override
            public TypeList.Generic getInterfaces() {
                return new TypeList.Generic.ForDetachedTypes(asErasure().getInterfaces(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return new FieldList.TypeSubstituting(this, asErasure().getDeclaredFields(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return new MethodList.TypeSubstituting(this, asErasure().getDeclaredMethods(), Visitor.Substitutor.ForTypeVariableBinding.bind(this));
            }

            @Override
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A parameterized type does not imply upper bounds: " + this);
            }

            @Override
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A parameterized type does not imply lower bounds: " + this);
            }

            @Override
            public Generic getComponentType() {
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
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
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
                for (Generic parameterType : getParameters()) {
                    result = 31 * result + parameterType.hashCode();
                }
                Generic ownerType = getOwnerType();
                return result ^ (ownerType == null
                        ? asErasure().hashCode()
                        : ownerType.hashCode());
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Generic)) return false;
                Generic typeDescription = (Generic) other;
                if (!typeDescription.getSort().isParameterized()) return false;
                Generic ownerType = getOwnerType(), otherOwnerType = typeDescription.getOwnerType();
                return asErasure().equals(typeDescription.asErasure())
                        && !(ownerType == null && otherOwnerType != null) && !(ownerType != null && !ownerType.equals(otherOwnerType))
                        && getParameters().equals(typeDescription.getParameters());
            }

            @Override
            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                Generic ownerType = getOwnerType();
                if (ownerType != null) {
                    stringBuilder.append(ownerType.getTypeName());
                    stringBuilder.append(".");
                    stringBuilder.append(ownerType.getSort().isParameterized()
                            ? asErasure().getName().replace(ownerType.asErasure().getName() + "$", "")
                            : asErasure().getName());
                } else {
                    stringBuilder.append(asErasure().getName());
                }
                TypeList.Generic actualTypeArguments = getParameters();
                if (!actualTypeArguments.isEmpty()) {
                    stringBuilder.append("<");
                    boolean multiple = false;
                    for (Generic typeArgument : actualTypeArguments) {
                        if (multiple) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(typeArgument.getTypeName());
                        multiple = true;
                    }
                    stringBuilder.append(">");
                }
                return stringBuilder.toString();
            }

            /**
             * Description of a loaded parameterized type.
             */
            public static class ForLoadedType extends OfParameterizedType {

                /**
                 * The represented parameterized type.
                 */
                private final ParameterizedType parameterizedType;

                /**
                 * Creates a description of the loaded parameterized type.
                 *
                 * @param parameterizedType The represented parameterized type.
                 */
                public ForLoadedType(ParameterizedType parameterizedType) {
                    this.parameterizedType = parameterizedType;
                }

                @Override
                public TypeList.Generic getParameters() {
                    return new TypeList.Generic.ForLoadedTypes(parameterizedType.getActualTypeArguments());
                }

                @Override
                public Generic getOwnerType() {
                    java.lang.reflect.Type ownerType = parameterizedType.getOwnerType();
                    return ownerType == null
                            ? UNDEFINED
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
            public static class Latent extends OfParameterizedType {

                /**
                 * The raw type of the described parameterized type.
                 */
                private final TypeDescription rawType;

                /**
                 * This parameterized type's owner type or {@code null} if no owner type exists.
                 */
                private final Generic ownerType;

                /**
                 * The parameters of this parameterized type.
                 */
                private final List<? extends Generic> parameters;

                /**
                 * Creates a description of a latent parameterized type.
                 *
                 * @param rawType    The raw type of the described parameterized type.
                 * @param parameters The parameters of this parameterized type.
                 * @param ownerType  This parameterized type's owner type or {@code null} if no owner type exists.
                 */
                public Latent(TypeDescription rawType, Generic ownerType, List<? extends Generic> parameters) {
                    this.rawType = rawType;
                    this.ownerType = ownerType;
                    this.parameters = parameters;
                }

                @Override
                public TypeDescription asErasure() {
                    return rawType;
                }

                @Override
                public Generic getOwnerType() {
                    return ownerType;
                }

                @Override
                public TypeList.Generic getParameters() {
                    return new TypeList.Generic.Explicit(parameters);
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a type variable.
         */
        abstract class OfTypeVariable extends AbstractBase {

            @Override
            public Sort getSort() {
                return Sort.VARIABLE;
            }

            @Override
            public TypeDescription asErasure() {
                TypeList.Generic upperBounds = getUpperBounds();
                return upperBounds.isEmpty()
                        ? TypeDescription.OBJECT
                        : upperBounds.get(0).asErasure();
            }

            @Override
            public Generic getSuperType() {
                throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
            }

            @Override
            public TypeList.Generic getInterfaces() {
                throw new IllegalStateException("A type variable does not imply an interface type definition: " + this);
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                throw new IllegalStateException("A type variable does not imply field definitions: " + this);
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                throw new IllegalStateException("A type variable does not imply method definitions: " + this);
            }

            @Override
            public Generic getComponentType() {
                throw new IllegalStateException("A type variable does not imply a component type: " + this);
            }

            @Override
            public TypeList.Generic getParameters() {
                throw new IllegalStateException("A type variable does not imply type parameters: " + this);
            }

            @Override
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A type variable does not imply lower bounds: " + this);
            }

            @Override
            public Generic getOwnerType() {
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
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
                throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
            }

            @Override
            public int hashCode() {
                return getVariableSource().hashCode() ^ getSymbol().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (!(other instanceof Generic)) return false;
                Generic typeDescription = (Generic) other;
                return typeDescription.getSort().isTypeVariable()
                        && getSymbol().equals(typeDescription.getSymbol())
                        && getVariableSource().equals(typeDescription.getVariableSource());
            }

            @Override
            public String toString() {
                return getSymbol();
            }

            public static class Symbolic extends AbstractBase {

                private final String symbol;

                public Symbolic(String symbol) {
                    this.symbol = symbol;
                }

                @Override
                public Sort getSort() {
                    return Sort.VARIABLE_SYMBOLIC;
                }

                @Override
                public String getSymbol() {
                    return symbol;
                }

                @Override
                public TypeDescription asErasure() {
                    throw new IllegalStateException("A symbolic type variable does not imply an erasure: " + this);
                }

                @Override
                public TypeList.Generic getUpperBounds() {
                    throw new IllegalStateException("A symbolic type variable does not imply an upper type bound: " + this);
                }

                @Override
                public TypeVariableSource getVariableSource() {
                    throw new IllegalStateException("A symbolic type variable does not imply a variable source: " + this);
                }

                @Override
                public Generic getSuperType() {
                    throw new IllegalStateException("A symbolic type variable does not imply a super type definition: " + this);
                }

                @Override
                public TypeList.Generic getInterfaces() {
                    throw new IllegalStateException("A symbolic type variable does not imply an interface type definition: " + this);
                }

                @Override
                public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                    throw new IllegalStateException("A symbolic type variable does not imply field definitions: " + this);
                }

                @Override
                public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                    throw new IllegalStateException("A symbolic type variable does not imply method definitions: " + this);
                }

                @Override
                public Generic getComponentType() {
                    throw new IllegalStateException("A symbolic type variable does not imply a component type: " + this);
                }

                @Override
                public TypeList.Generic getParameters() {
                    throw new IllegalStateException("A symbolic type variable does not imply type parameters: " + this);
                }

                @Override
                public TypeList.Generic getLowerBounds() {
                    throw new IllegalStateException("A symbolic type variable does not imply lower bounds: " + this);
                }

                @Override
                public Generic getOwnerType() {
                    throw new IllegalStateException("A symbolic type variable does not imply an owner type: " + this);
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
                public boolean represents(java.lang.reflect.Type type) {
                    return equals(Sort.describe(type));
                }

                @Override
                public Iterator<TypeDefinition> iterator() {
                    throw new IllegalStateException("A symbolic type variable does not imply a super type definition: " + this);
                }

                @Override
                public int hashCode() {
                    return symbol.hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    if (!(other instanceof Generic)) return false;
                    Generic typeDescription = (Generic) other;
                    return typeDescription.getSort().isTypeVariable()
                            && getSymbol().equals(typeDescription.getSymbol());
                }

                @Override
                public String toString() {
                    return getSymbol();
                }
            }

            /**
             * Description of a loaded type variable.
             */
            public static class ForLoadedType extends OfTypeVariable {

                /**
                 * The represented type variable.
                 */
                private final TypeVariable<?> typeVariable;

                /**
                 * Creates a description of a loaded type variable.
                 *
                 * @param typeVariable The represented type variable.
                 */
                public ForLoadedType(TypeVariable<?> typeVariable) {
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
                public TypeList.Generic getUpperBounds() {
                    return new TypeList.Generic.ForLoadedTypes(typeVariable.getBounds());
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
        abstract class LazyProjection extends AbstractBase {

            /**
             * Resolves the actual generic type.
             *
             * @return An actual description of the represented generic type.
             */
            protected abstract Generic resolve();

            @Override
            public Sort getSort() {
                return resolve().getSort();
            }

            @Override
            public TypeList.Generic getInterfaces() {
                return resolve().getInterfaces();
            }

            @Override
            public Generic getSuperType() {
                return resolve().getSuperType();
            }

            @Override
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return resolve().getDeclaredFields();
            }

            @Override
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return resolve().getDeclaredMethods();
            }

            @Override
            public TypeList.Generic getUpperBounds() {
                return resolve().getUpperBounds();
            }

            @Override
            public TypeList.Generic getLowerBounds() {
                return resolve().getLowerBounds();
            }

            @Override
            public Generic getComponentType() {
                return resolve().getComponentType();
            }

            @Override
            public TypeList.Generic getParameters() {
                return resolve().getParameters();
            }

            @Override
            public TypeVariableSource getVariableSource() {
                return resolve().getVariableSource();
            }

            @Override
            public Generic getOwnerType() {
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
            public boolean represents(java.lang.reflect.Type type) {
                return resolve().represents(type);
            }

            @Override
            public Iterator<TypeDefinition> iterator() {
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
             * A lazy projection of a generic super type.
             */
            public static class ForLoadedSuperType extends LazyProjection {

                /**
                 * The type of which the super class is represented.
                 */
                private final Class<?> type;

                /**
                 * Creates a new lazy projection of a type's super class.
                 *
                 * @param type The type of which the super class is represented.
                 */
                public ForLoadedSuperType(Class<?> type) {
                    this.type = type;
                }

                @Override
                protected Generic resolve() {
                    java.lang.reflect.Type superClass = type.getGenericSuperclass();
                    return superClass == null
                            ? UNDEFINED
                            : Sort.describe(superClass);
                }

                @Override
                public TypeDescription asErasure() {
                    Class<?> superClass = type.getSuperclass();
                    return superClass == null
                            ? TypeDescription.UNDEFINED
                            : new ForLoadedType(superClass);
                }
            }

            /**
             * A lazy projection of a field's type.
             */
            public static class ForLoadedFieldType extends LazyProjection {

                /**
                 * The field of which the type is represented.
                 */
                private final Field field;

                /**
                 * Create's a lazy projection of a field type.
                 *
                 * @param field The field of which the type is represented.
                 */
                public ForLoadedFieldType(Field field) {
                    this.field = field;
                }

                @Override
                protected Generic resolve() {
                    return Sort.describe(field.getGenericType());
                }

                @Override
                public TypeDescription asErasure() {
                    return new ForLoadedType(field.getType());
                }
            }

            /**
             * A lazy projection of a method's generic return type.
             */
            public static class ForLoadedReturnType extends LazyProjection {

                /**
                 * The method which defines the return type.
                 */
                private final Method method;

                /**
                 * Creates a new lazy projection of a method's return type.
                 *
                 * @param method The method which defines the return type.
                 */
                public ForLoadedReturnType(Method method) {
                    this.method = method;
                }

                @Override
                protected Generic resolve() {
                    return Sort.describe(method.getGenericReturnType());
                }

                @Override
                public TypeDescription asErasure() {
                    return new ForLoadedType(method.getReturnType());
                }
            }

            /**
             * A lazy projection of the parameter type of a {@link Constructor}.
             */
            public static class OfConstructorParameter extends LazyProjection {

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
                public OfConstructorParameter(Constructor<?> constructor, int index, Class<?> erasure) {
                    this.constructor = constructor;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected Generic resolve() {
                    java.lang.reflect.Type[] type = constructor.getGenericParameterTypes();
                    return index < type.length
                            ? Sort.describe(type[index])
                            : new OfNonGenericType.ForLoadedType(erasure);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }

            /**
             * A lazy projection of the parameter type of a {@link Method}.
             */
            public static class OfMethodParameter extends LazyProjection {

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
                public OfMethodParameter(Method method, int index, Class<?> erasure) {
                    this.method = method;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                protected Generic resolve() {
                    java.lang.reflect.Type[] type = method.getGenericParameterTypes();
                    return index < type.length
                            ? Sort.describe(type[index])
                            : new OfNonGenericType.ForLoadedType(erasure);
                }

                @Override
                public TypeDescription asErasure() {
                    return new TypeDescription.ForLoadedType(erasure);
                }
            }
        }

        class Builder {

            private static final java.lang.reflect.Type UNDEFINED = null;

            private final Generic typeDescription;

            protected Builder(Generic typeDescription) {
                this.typeDescription = typeDescription;
            }

            public static Builder rawType(Class<?> type) {
                return rawType(new ForLoadedType(type));
            }

            public static Builder rawType(TypeDescription typeDescription) {
                return new Builder(typeDescription.asGenericType());
            }

            public static Generic unboundWildcard() {
                return OfWildcardType.Latent.unbounded();
            }

            public static Generic typeVariable(String symbol) {
                return new OfTypeVariable.Symbolic(symbol);
            }

            public static Builder parameterizedType(Class<?> rawType, java.lang.reflect.Type... parameter) {
                return parameterizedType(rawType, Arrays.asList(parameter));
            }

            public static Builder parameterizedType(Class<?> rawType, List<? extends java.lang.reflect.Type> parameters) {
                return parameterizedType(rawType, UNDEFINED, parameters);
            }

            public static Builder parameterizedType(Class<?> rawType, java.lang.reflect.Type ownerType, List<? extends java.lang.reflect.Type> parameters) {
                return parameterizedType(new ForLoadedType(rawType),
                        ownerType == null
                                ? null
                                : Sort.describe(ownerType),
                        new TypeList.Generic.ForLoadedTypes(parameters));
            }

            public static Builder parameterizedType(TypeDescription rawType, TypeDefinition... parameter) {
                return parameterizedType(rawType, Arrays.asList(parameter));
            }

            public static Builder parameterizedType(TypeDescription rawType, List<? extends TypeDefinition> parameters) {
                return parameterizedType(rawType, Generic.UNDEFINED, parameters);
            }

            public static Builder parameterizedType(TypeDescription rawType, Generic ownerType, List<? extends TypeDefinition> parameters) {
                TypeDescription declaringType = rawType.getDeclaringType();
                if (ownerType == null && declaringType != null && rawType.isStatic()) {
                    ownerType = declaringType.asGenericType();
                }
                if (ownerType == null && declaringType != null && !rawType.isStatic()) {
                    throw new IllegalArgumentException(rawType + " requires an owner type");
                } else if (ownerType != null && !ownerType.asErasure().equals(declaringType)) {
                    throw new IllegalArgumentException(ownerType + " does not represent required owner for " + rawType);
                } else if (ownerType != null && (rawType.isStatic() ^ ownerType.getSort().isNonGeneric())) {
                    throw new IllegalArgumentException(ownerType + " does not define the correct parameters for owning " + rawType);
                } else if (rawType.getTypeVariables().size() != parameters.size()) {
                    throw new IllegalArgumentException(parameters + " does not contain number of required parameters for " + rawType);
                }
                return new Builder(new OfParameterizedType.Latent(rawType, ownerType, new TypeList.Generic.Explicit(parameters)));
            }

            public Generic asWildcardUpperBound() {
                return OfWildcardType.Latent.boundedAbove(typeDescription);
            }

            public Generic asWildcardLowerBound() {
                return OfWildcardType.Latent.boundedBelow(typeDescription);
            }

            public Builder asArray() {
                return asArray(1);
            }

            public Builder asArray(int arity) {
                return new Builder(OfGenericArray.Latent.of(typeDescription, arity));
            }

            public Generic asType() {
                return typeDescription;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Builder builder = (Builder) other;
                return typeDescription.equals(builder.typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "TypeDescription.Generic.Builder{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }
    }

    /**
     * An abstract base implementation of a type description.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements TypeDescription {

        /**
         * Checks if a specific type is assignable to another type where the source type must be a super
         * type of the target type.
         *
         * @param sourceType The source type to which another type is to be assigned to.
         * @param targetType The target type that is to be assigned to the source type.
         * @return {@code true} if the target type is assignable to the source type.
         */
        private static boolean isAssignable(TypeDescription sourceType, TypeDescription targetType) {
            // Means that '[sourceType] var = ([targetType]) val;' is a valid assignment. This is true, if:
            // (1) Both types are equal (implies primitive types.)
            if (sourceType.equals(targetType)) {
                return true;
            }
            // (3) For arrays, there are special assignment rules.
            if (targetType.isArray()) {
                return sourceType.isArray()
                        ? isAssignable(sourceType.getComponentType(), targetType.getComponentType())
                        : sourceType.represents(Object.class) || ARRAY_INTERFACES.contains(sourceType.asGenericType());
            }
            // (2) Interfaces do not extend the Object type but are assignable to the Object type.
            if (sourceType.represents(Object.class)) {
                return !targetType.isPrimitive();
            }
            // (4) The sub type has a super type and this super type is assignable to the super type.
            Generic superType = targetType.getSuperType();
            if (superType != null && sourceType.isAssignableFrom(superType.asErasure())) {
                return true;
            }
            // (5) If the target type is an interface, any of this type's interfaces might be assignable to it.
            if (sourceType.isInterface()) {
                for (TypeDescription interfaceType : targetType.getInterfaces().asErasures()) {
                    if (sourceType.isAssignableFrom(interfaceType)) {
                        return true;
                    }
                }
            }
            // (6) None of these criteria are true, i.e. the types are not assignable.
            return false;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return isAssignable(this, typeDescription);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return isAssignable(typeDescription, this);
        }

        @Override
        public TypeDescription asErasure() {
            return this;
        }

        @Override
        public Generic asGenericType() {
            return new Generic.OfNonGenericType.Latent(this);
        }

        @Override
        public Sort getSort() {
            return Sort.NON_GENERIC;
        }

        @Override
        public boolean isInstance(Object value) {
            return isAssignableFrom(value.getClass());
        }

        @Override
        public boolean isInstanceOrWrapper(Object value) {
            return isInstance(value)
                    || (represents(boolean.class) && value instanceof Boolean)
                    || (represents(byte.class) && value instanceof Byte)
                    || (represents(short.class) && value instanceof Short)
                    || (represents(char.class) && value instanceof Character)
                    || (represents(int.class) && value instanceof Integer)
                    || (represents(long.class) && value instanceof Long)
                    || (represents(float.class) && value instanceof Float)
                    || (represents(double.class) && value instanceof Double);
        }

        @Override
        public boolean isAnnotationValue(Object value) {
            if ((represents(Class.class) && value instanceof TypeDescription)
                    || (value instanceof AnnotationDescription && ((AnnotationDescription) value).getAnnotationType().equals(this))
                    || (value instanceof EnumerationDescription && ((EnumerationDescription) value).getEnumerationType().equals(this))
                    || (represents(String.class) && value instanceof String)
                    || (represents(boolean.class) && value instanceof Boolean)
                    || (represents(byte.class) && value instanceof Byte)
                    || (represents(short.class) && value instanceof Short)
                    || (represents(char.class) && value instanceof Character)
                    || (represents(int.class) && value instanceof Integer)
                    || (represents(long.class) && value instanceof Long)
                    || (represents(float.class) && value instanceof Float)
                    || (represents(double.class) && value instanceof Double)
                    || (represents(String[].class) && value instanceof String[])
                    || (represents(boolean[].class) && value instanceof boolean[])
                    || (represents(byte[].class) && value instanceof byte[])
                    || (represents(short[].class) && value instanceof short[])
                    || (represents(char[].class) && value instanceof char[])
                    || (represents(int[].class) && value instanceof int[])
                    || (represents(long[].class) && value instanceof long[])
                    || (represents(float[].class) && value instanceof float[])
                    || (represents(double[].class) && value instanceof double[])
                    || (represents(Class[].class) && value instanceof TypeDescription[])) {
                return true;
            } else if (isAssignableTo(Annotation[].class) && value instanceof AnnotationDescription[]) {
                for (AnnotationDescription annotationDescription : (AnnotationDescription[]) value) {
                    if (!annotationDescription.getAnnotationType().equals(getComponentType())) {
                        return false;
                    }
                }
                return true;
            } else if (isAssignableTo(Enum[].class) && value instanceof EnumerationDescription[]) {
                for (EnumerationDescription enumerationDescription : (EnumerationDescription[]) value) {
                    if (!enumerationDescription.getEnumerationType().equals(getComponentType())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public int getActualModifiers(boolean superFlag) {
            int actualModifiers;
            if (isPrivate()) {
                actualModifiers = getModifiers() & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            } else if (isProtected()) {
                actualModifiers = getModifiers() & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC) | Opcodes.ACC_PUBLIC;
            } else {
                actualModifiers = getModifiers() & ~Opcodes.ACC_STATIC;
            }
            return superFlag ? (actualModifiers | Opcodes.ACC_SUPER) : actualModifiers;
        }

        @Override
        public String getGenericSignature() {
            try {
                SignatureWriter signatureWriter = new SignatureWriter();
                boolean generic = false;
                for (Generic typeVariable : getTypeVariables()) {
                    signatureWriter.visitFormalTypeParameter(typeVariable.getSymbol());
                    for (Generic upperBound : typeVariable.getUpperBounds()) {
                        upperBound.accept(new Generic.Visitor.ForSignatureVisitor(upperBound.asErasure().isInterface()
                                ? signatureWriter.visitInterfaceBound()
                                : signatureWriter.visitClassBound()));
                    }
                    generic = true;
                }
                Generic superType = getSuperType();
                // The object type itself is non generic and implicitly returns a non-generic signature
                if (superType == null) {
                    superType = TypeDescription.Generic.OBJECT;
                }
                superType.accept(new Generic.Visitor.ForSignatureVisitor(signatureWriter.visitSuperclass()));
                generic = generic || !superType.getSort().isNonGeneric();
                for (Generic interfaceType : getInterfaces()) {
                    interfaceType.accept(new Generic.Visitor.ForSignatureVisitor(signatureWriter.visitInterface()));
                    generic = generic || !interfaceType.getSort().isNonGeneric();
                }
                return generic
                        ? signatureWriter.toString()
                        : NON_GENERIC_SIGNATURE;
            } catch (GenericSignatureFormatError ignored) {
                return NON_GENERIC_SIGNATURE;
            }
        }

        @Override
        public boolean isSamePackage(TypeDescription typeDescription) {
            PackageDescription thisPackage = getPackage(), otherPackage = typeDescription.getPackage();
            return thisPackage == null || otherPackage == null
                    ? thisPackage == otherPackage
                    : thisPackage.equals(otherPackage);
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPrimitive() || (isArray()
                    ? getComponentType().isVisibleTo(typeDescription)
                    : isPublic() || isProtected() || isSamePackage(typeDescription)/* || equals(typeDescription) */);
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            AnnotationList declaredAnnotations = getDeclaredAnnotations();
            if (getSuperType() == null) {
                return declaredAnnotations;
            } else {
                Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>();
                for (AnnotationDescription annotationDescription : declaredAnnotations) {
                    annotationTypes.add(annotationDescription.getAnnotationType());
                }
                return new AnnotationList.Explicit(CompoundList.of(declaredAnnotations, getSuperType().asErasure().getInheritedAnnotations().inherited(annotationTypes)));
            }
        }

        @Override
        public String getSourceCodeName() {
            if (isArray()) {
                TypeDescription typeDescription = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    typeDescription = typeDescription.getComponentType();
                } while (typeDescription.isArray());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(typeDescription.getSourceCodeName());
                for (int i = 0; i < dimensions; i++) {
                    stringBuilder.append("[]");
                }
                return stringBuilder.toString();
            } else {
                return getName();
            }
        }

        @Override
        public boolean isConstantPool() {
            return represents(int.class)
                    || represents(long.class)
                    || represents(float.class)
                    || represents(double.class)
                    || represents(String.class)
                    || represents(Class.class)
                    || JavaType.METHOD_HANDLE.getTypeStub().equals(this)
                    || JavaType.METHOD_TYPE.getTypeStub().equals(this);
        }

        @Override
        public boolean isPrimitiveWrapper() {
            return represents(Boolean.class)
                    || represents(Byte.class)
                    || represents(Short.class)
                    || represents(Character.class)
                    || represents(Integer.class)
                    || represents(Long.class)
                    || represents(Float.class)
                    || represents(Double.class);
        }

        @Override
        public boolean isAnnotationReturnType() {
            return isPrimitive()
                    || represents(String.class)
                    || (isAssignableTo(Enum.class) && !represents(Enum.class))
                    || (isAssignableTo(Annotation.class) && !represents(Annotation.class))
                    || represents(Class.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationReturnType());
        }

        @Override
        public boolean isAnnotationValue() {
            return isPrimitive()
                    || represents(String.class)
                    || isAssignableTo(TypeDescription.class)
                    || isAssignableTo(AnnotationDescription.class)
                    || isAssignableTo(EnumerationDescription.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationValue());
        }

        @Override
        public boolean represents(java.lang.reflect.Type type) {
            if (type == null) { // Exception is thrown implicitly by TypeDescription.Generic implementations
                throw new NullPointerException();
            }
            return type instanceof Class && equals(new ForLoadedType((Class<?>) type));
        }

        @Override
        public String getTypeName() {
            return getName();
        }

        @Override
        public TypeVariableSource getEnclosingSource() {
            MethodDescription enclosingMethod = getEnclosingMethod();
            return enclosingMethod == null
                    ? getEnclosingType()
                    : enclosingMethod;
        }

        @Override
        public Generic findVariable(String symbol) {
            TypeList.Generic typeVariables = getTypeVariables().filter(named(symbol));
            if (typeVariables.isEmpty()) {
                TypeVariableSource enclosingSource = getEnclosingSource();
                return enclosingSource == null
                        ? TypeDescription.Generic.UNDEFINED
                        : enclosingSource.findVariable(symbol);
            } else {
                return typeVariables.getOnly();
            }
        }

        @Override
        public <T> T accept(TypeVariableSource.Visitor<T> visitor) {
            return visitor.onType(this);
        }

        @Override
        public Iterator<TypeDefinition> iterator() {
            return new SuperTypeIterator(this);
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof TypeDefinition
                    && ((TypeDefinition) other).getSort().isNonGeneric()
                    && getInternalName().equals(((TypeDefinition) other).asErasure().getInternalName());
        }

        @Override
        public int hashCode() {
            return getInternalName().hashCode();
        }

        @Override
        public String toString() {
            return (isPrimitive() ? "" : (isInterface() ? "interface" : "class") + " ") + getName();
        }

        /**
         * An adapter implementation of a {@link TypeDescription} that
         * describes any type that is not an array or a primitive type.
         */
        public abstract static class OfSimpleType extends TypeDescription.AbstractBase {

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isArray() {
                return false;
            }

            @Override
            public TypeDescription getComponentType() {
                return UNDEFINED;
            }

            @Override
            public String getDescriptor() {
                return "L" + getInternalName() + ";";
            }

            @Override
            public String getCanonicalName() {
                return isAnonymousClass() || isLocalClass()
                        ? NO_NAME
                        : getName().replace('$', '.');
            }

            @Override
            public String getSimpleName() {
                String internalName = getInternalName();
                int simpleNameIndex = internalName.lastIndexOf('$');
                simpleNameIndex = simpleNameIndex == -1
                        ? internalName.lastIndexOf('/')
                        : simpleNameIndex;
                if (simpleNameIndex == -1) {
                    return internalName;
                } else {
                    while (simpleNameIndex < internalName.length() && !Character.isLetter(internalName.charAt(simpleNameIndex))) {
                        simpleNameIndex += 1;
                    }
                    return internalName.substring(simpleNameIndex);
                }
            }

            @Override
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }
        }
    }

    /**
     * A type description implementation that represents a loaded type.
     */
    class ForLoadedType extends AbstractBase {

        /**
         * The loaded type this instance represents.
         */
        private final Class<?> type;

        /**
         * Creates a new immutable type description for a loaded type.
         *
         * @param type The type to be represented by this type description.
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return this.type.isAssignableFrom(type) || (this.type.getClassLoader() != type.getClassLoader() && super.isAssignableFrom(type));
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return type.isAssignableFrom(this.type) || (this.type.getClassLoader() != type.getClassLoader() && super.isAssignableTo(type));
        }

        @Override
        public boolean represents(java.lang.reflect.Type type) {
            // The JVM conducts more efficient assignability lookups of loaded types what is attempted first.
            return type == this.type || super.represents(type);
        }

        @Override
        public TypeDescription getComponentType() {
            Class<?> componentType = type.getComponentType();
            return componentType == null
                    ? TypeDescription.UNDEFINED
                    : new ForLoadedType(componentType);
        }

        @Override
        public boolean isArray() {
            return type.isArray();
        }

        @Override
        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        @Override
        public boolean isAnnotation() {
            return type.isAnnotation();
        }

        @Override
        public Generic getSuperType() {
            return type.getSuperclass() == null
                    ? TypeDescription.Generic.UNDEFINED
                    : new Generic.LazyProjection.ForLoadedSuperType(type);
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return isArray()
                    ? ARRAY_INTERFACES
                    : new TypeList.Generic.OfLoadedInterfaceTypes(type);
        }

        @Override
        public TypeDescription getDeclaringType() {
            Class<?> declaringType = type.getDeclaringClass();
            return declaringType == null
                    ? TypeDescription.UNDEFINED
                    : new ForLoadedType(declaringType);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            Method enclosingMethod = type.getEnclosingMethod();
            Constructor<?> enclosingConstructor = type.getEnclosingConstructor();
            if (enclosingMethod != null) {
                return new MethodDescription.ForLoadedMethod(enclosingMethod);
            } else if (enclosingConstructor != null) {
                return new MethodDescription.ForLoadedConstructor(enclosingConstructor);
            } else {
                return MethodDescription.UNDEFINED;
            }
        }

        @Override
        public TypeDescription getEnclosingType() {
            Class<?> enclosingType = type.getEnclosingClass();
            return enclosingType == null
                    ? TypeDescription.UNDEFINED
                    : new ForLoadedType(enclosingType);
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.ForLoadedTypes(type.getDeclaredClasses());
        }

        @Override
        public String getSimpleName() {
            String simpleName = type.getSimpleName();
            int anonymousLoaderIndex = simpleName.indexOf('/');
            if (anonymousLoaderIndex == -1) {
                return simpleName;
            } else {
                StringBuilder normalized = new StringBuilder(simpleName.substring(0, anonymousLoaderIndex));
                Class<?> type = this.type;
                while (type.isArray()) {
                    normalized.append("[]");
                    type = type.getComponentType();
                }
                return normalized.toString();
            }
        }

        @Override
        public boolean isAnonymousClass() {
            return type.isAnonymousClass();
        }

        @Override
        public boolean isLocalClass() {
            return type.isLocalClass();
        }

        @Override
        public boolean isMemberClass() {
            return type.isMemberClass();
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.ForLoadedFields(type.getDeclaredFields());
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.ForLoadedType(type);
        }

        @Override
        public PackageDescription getPackage() {
            Package aPackage = type.getPackage();
            return aPackage == null
                    ? PackageDescription.UNDEFINED
                    : new PackageDescription.ForLoadedPackage(aPackage);
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.of(type);
        }

        @Override
        public String getName() {
            String name = type.getName();
            int anonymousLoaderIndex = name.indexOf('/');
            return anonymousLoaderIndex == -1
                    ? name
                    : name.substring(0, anonymousLoaderIndex);
        }

        @Override
        public String getCanonicalName() {
            String canonicalName = type.getCanonicalName();
            if (canonicalName == null) {
                return NO_NAME;
            }
            int anonymousLoaderIndex = canonicalName.indexOf('/');
            if (anonymousLoaderIndex == -1) {
                return canonicalName;
            } else {
                StringBuilder normalized = new StringBuilder(canonicalName.substring(0, anonymousLoaderIndex));
                Class<?> type = this.type;
                while (type.isArray()) {
                    normalized.append("[]");
                    type = type.getComponentType();
                }
                return normalized.toString();
            }
        }

        @Override
        public String getDescriptor() {
            String name = type.getName();
            int anonymousLoaderIndex = name.indexOf('/');
            return anonymousLoaderIndex == -1
                    ? Type.getDescriptor(type)
                    : "L" + name.substring(0, anonymousLoaderIndex).replace('.', '/') + ";";
        }

        @Override
        public int getModifiers() {
            return type.getModifiers();
        }

        @Override
        public TypeList.Generic getTypeVariables() {
            return new TypeList.Generic.ForLoadedTypes(type.getTypeParameters());
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(type.getDeclaredAnnotations());
        }
    }

    /**
     * A projection for an array type based on an existing {@link TypeDescription}.
     */
    class ArrayProjection extends AbstractBase {

        /**
         * The base component type which is itself not an array.
         */
        private final TypeDescription componentType;

        /**
         * The arity of this array.
         */
        private final int arity;

        /**
         * Crrates a new array projection.
         *
         * @param componentType The base component type of the array which is itself not an array.
         * @param arity         The arity of this array.
         */
        protected ArrayProjection(TypeDescription componentType, int arity) {
            this.componentType = componentType;
            this.arity = arity;
        }

        /**
         * Creates an array projection.
         *
         * @param componentType The component type of the array.
         * @param arity         The arity of this array.
         * @return A projection of the component type as an arity of the given value.
         */
        public static TypeDescription of(TypeDescription componentType, int arity) {
            if (arity < 0) {
                throw new IllegalArgumentException("Arrays cannot have a negative arity");
            }
            while (componentType.isArray()) {
                componentType = componentType.getComponentType();
                arity++;
            }
            return arity == 0
                    ? componentType
                    : new ArrayProjection(componentType, arity);
        }

        @Override
        public boolean isArray() {
            return true;
        }

        @Override
        public TypeDescription getComponentType() {
            return arity == 1
                    ? componentType
                    : new ArrayProjection(componentType, arity - 1);
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public Generic getSuperType() {
            return TypeDescription.Generic.OBJECT;
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return ARRAY_INTERFACES;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return UNDEFINED;
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        @Override
        public String getSimpleName() {
            StringBuilder stringBuilder = new StringBuilder(componentType.getSimpleName());
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        @Override
        public String getCanonicalName() {
            String canonicalName = componentType.getCanonicalName();
            if (canonicalName == null) {
                return NO_NAME;
            }
            StringBuilder stringBuilder = new StringBuilder(canonicalName);
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty<FieldDescription.InDefinedShape>();
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty<MethodDescription.InDefinedShape>();
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public PackageDescription getPackage() {
            return PackageDescription.UNDEFINED;
        }

        @Override
        public String getName() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getDescriptor().replace('/', '.')).toString();
        }

        @Override
        public String getDescriptor() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getDescriptor()).toString();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return UNDEFINED;
        }

        @Override
        public int getModifiers() {
            return ARRAY_MODIFIERS;
        }

        @Override
        public TypeList.Generic getTypeVariables() {
            return new TypeList.Generic.Empty();
        }
    }

    /**
     * A latent type description for a type without methods or fields.
     */
    class Latent extends AbstractBase.OfSimpleType {

        /**
         * The name of the type.
         */
        private final String name;

        /**
         * The modifiers of the type.
         */
        private final int modifiers;

        /**
         * The super type or {@code null} if no such type exists.
         */
        private final Generic superType;

        /**
         * The interfaces that this type implements.
         */
        private final List<? extends Generic> interfaces;

        /**
         * Creates a new latent type.
         *
         * @param name       The name of the type.
         * @param modifiers  The modifiers of the type.
         * @param superType  The super type or {@code null} if no such type exists.
         * @param interfaces The interfaces that this type implements.
         */
        public Latent(String name, int modifiers, Generic superType, List<? extends Generic> interfaces) {
            this.name = name;
            this.modifiers = modifiers;
            this.superType = superType;
            this.interfaces = interfaces;
        }

        @Override
        public Generic getSuperType() {
            return superType;
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.Explicit(interfaces);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            throw new IllegalStateException("Cannot resolve enclosing method of a latent type description: " + this);
        }

        @Override
        public TypeDescription getEnclosingType() {
            throw new IllegalStateException("Cannot resolve enclosing type of a latent type description: " + this);
        }

        @Override
        public TypeList getDeclaredTypes() {
            throw new IllegalStateException("Cannot resolve inner types of a latent type description: " + this);
        }

        @Override
        public boolean isAnonymousClass() {
            throw new IllegalStateException("Cannot resolve anonymous type property of a latent type description: " + this);
        }

        @Override
        public boolean isLocalClass() {
            throw new IllegalStateException("Cannot resolve local class property of a latent type description: " + this);
        }

        @Override
        public boolean isMemberClass() {
            throw new IllegalStateException("Cannot resolve member class property of a latent type description: " + this);
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            throw new IllegalStateException("Cannot resolve declared fields of a latent type description: " + this);
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            throw new IllegalStateException("Cannot resolve declared methods of a latent type description: " + this);
        }

        @Override
        public PackageDescription getPackage() {
            String name = getName();
            int index = name.lastIndexOf('.');
            return index == -1
                    ? PackageDescription.UNDEFINED
                    : new PackageDescription.Simple(name.substring(0, index));
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            throw new IllegalStateException("Cannot resolve declared annotations of a latent type description: " + this);
        }

        @Override
        public TypeDescription getDeclaringType() {
            throw new IllegalStateException("Cannot resolve declared type of a latent type description: " + this);
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TypeList.Generic getTypeVariables() {
            throw new IllegalStateException("Cannot resolve type variables of a latent type description: " + this);
        }
    }

    /**
     * A type representation of a package description.
     */
    class ForPackageDescription extends AbstractBase.OfSimpleType {

        /**
         * The package to be described as a type.
         */
        private final PackageDescription packageDescription;

        /**
         * Creates a new type description of a package description.
         *
         * @param packageDescription The package to be described as a type.
         */
        public ForPackageDescription(PackageDescription packageDescription) {
            this.packageDescription = packageDescription;
        }

        @Override
        public Generic getSuperType() {
            return TypeDescription.Generic.OBJECT;
        }

        @Override
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.Empty();
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return UNDEFINED;
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        @Override
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty<FieldDescription.InDefinedShape>();
        }

        @Override
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty<MethodDescription.InDefinedShape>();
        }

        @Override
        public PackageDescription getPackage() {
            return packageDescription;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return packageDescription.getDeclaredAnnotations();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return UNDEFINED;
        }

        @Override
        public TypeList.Generic getTypeVariables() {
            return new TypeList.Generic.Empty();
        }

        @Override
        public int getModifiers() {
            return PackageDescription.PACKAGE_MODIFIERS;
        }

        @Override
        public String getName() {
            return packageDescription.getName() + "." + PackageDescription.PACKAGE_CLASS_NAME;
        }
    }
}
