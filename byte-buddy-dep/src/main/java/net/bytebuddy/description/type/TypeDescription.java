/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.description.type;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationSource;
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
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface. Instances of this interface always
 * represent non-generic types of sort {@link Generic.Sort#NON_GENERIC}.
 */
public interface TypeDescription extends TypeDefinition, ByteCodeElement, TypeVariableSource {

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
     * A representation of the {@link java.lang.Throwable} type.
     */
    TypeDescription THROWABLE = new ForLoadedType(Throwable.class);

    /**
     * A representation of the {@code void} non-type.
     */
    TypeDescription VOID = new ForLoadedType(void.class);

    /**
     * A list of interfaces that are implicitly implemented by any array type.
     */
    TypeList.Generic ARRAY_INTERFACES = new TypeList.Generic.ForLoadedTypes(Cloneable.class, Serializable.class);

    /**
     * Represents any undefined property representing a type description that is instead represented as {@code null} in order
     * to resemble the Java reflection API which returns {@code null} and is intuitive to many Java developers.
     */
    TypeDescription UNDEFINED = null;

    /**
     * {@inheritDoc}
     */
    FieldList<FieldDescription.InDefinedShape> getDeclaredFields();

    /**
     * {@inheritDoc}
     */
    MethodList<MethodDescription.InDefinedShape> getDeclaredMethods();

    /**
     * Checks if {@code value} is an instance of the type represented by this instance.
     *
     * @param value The object of interest.
     * @return {@code true} if the object is an instance of the type described by this instance.
     */
    boolean isInstance(Object value);

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

    /**
     * Returns {@code true} if this type and the supplied type are in a type hierarchy with each other, i.e. if this type is assignable
     * to the supplied type or the other way around.
     *
     * @param type The type of interest.
     * @return {@code true} if this type and the supplied type are in a type hierarchy with each other.
     */
    boolean isInHierarchyWith(Class<?> type);

    /**
     * Returns {@code true} if this type and the supplied type are in a type hierarchy with each other, i.e. if this type is assignable
     * to the supplied type or the other way around.
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type and the supplied type are in a type hierarchy with each other.
     */
    boolean isInHierarchyWith(TypeDescription typeDescription);

    /**
     * {@inheritDoc}
     */
    TypeDescription getComponentType();

    /**
     * {@inheritDoc}
     */
    TypeDescription getDeclaringType();

    /**
     * Returns a list of types that are declared by this type. This list does not normally include anonymous types but might
     * include additional types if they are explicitly added to an instrumented type.
     *
     * @return A list of types that are declared within this type.
     */
    TypeList getDeclaredTypes();

    /**
     * Returns a description of the method that encloses this type. If this method is not enclosed by any type or is
     * enclosed by the type initializer, {@code null} is returned by this method.
     *
     * @return A description of the enclosing method of this type or {@code null} if there is no such method.
     */
    MethodDescription.InDefinedShape getEnclosingMethod();

    /**
     * Returns a description of this type's enclosing type if any.
     *
     * @return A description of the enclosing type of this type or {@code null} if there is no such type.
     */
    TypeDescription getEnclosingType();

    /**
     * Returns the type's actual modifiers as present in the class file. For example, a type cannot be {@code private}.
     * but it modifiers might reflect this property nevertheless if a class was defined as a private inner class. The
     * returned modifiers take also into account if the type is marked as {@link Deprecated}. Anonymous classes that are
     * enclosed in a static method or the type initializer are additionally marked as {@code final} as it is also done
     * by the Java compiler.
     *
     * @param superFlag {@code true} if the modifier's super flag should be set.
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
    boolean isAnonymousType();

    /**
     * Checks if this type description represents a local type.
     *
     * @return {@code true} if this type description represents a local type.
     */
    boolean isLocalType();

    /**
     * Checks if this type description represents a member type.
     *
     * @return {@code true} if this type description represents a member type.
     */
    boolean isMemberType();

    /**
     * Returns the package of the type described by this instance or {@code null} if the described type does not imply a package.
     *
     * @return The package of the type described by this instance or {@code null} if the described type does not imply a package.
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
     * Checks if this type represents a class that is a place holder for a package description.
     *
     * @return {@code true} if this type represents a package description.
     */
    boolean isPackageType();

    /**
     * Returns the amount of outer classes this type defines. If this type is not an inner type of another class, {@code 0} is returned.
     *
     * @return The number of outer classes relatively to this type.
     */
    int getInnerClassCount();

    /**
     * Indicates if this class is an inner class.
     *
     * @return {@code true} if this class is an inner class.
     */
    boolean isInnerClass();

    /**
     * Indicates if this class is a nested class.
     *
     * @return {@code true} if this class is a nested class.
     */
    boolean isNestedClass();

    /**
     * Returns a description of this type that represents this type as a boxed type for primitive types, unless its {@code void}.
     *
     * @return A description of this type in its boxed form.
     */
    TypeDescription asBoxed();

    /**
     * Returns a description of this type that represents this type as an unboxed type for boxing types, unless its {@link Void}.
     *
     * @return A description of this type in its unboxed form.
     */
    TypeDescription asUnboxed();

    /**
     * Returns the default value for this type, i.e. the zero value for a primitive type and {@code null} for a reference type.
     * For {@code void}, {@code null} is returned.
     *
     * @return This types default value.
     */
    Object getDefaultValue();

    /**
     * Returns the nest host of this type. For types prior to Java 11, this type is returned which is the default nest host.
     *
     * @return The nest host of this type.
     */
    TypeDescription getNestHost();

    /**
     * Returns a list of members that are part of a nesting group. Prior to Java 11, a list that only contains this type is returned which is
     * the default nest group.
     *
     * @return A list of members of this nest group.
     */
    TypeList getNestMembers();

    /**
     * Checks if this class is the host of a nest group.
     *
     * @return {@code true} if this class is a nest group's host.
     */
    boolean isNestHost();

    /**
     * Checks if this type and the supplied type are members of the same nest group.
     *
     * @param type The type for which to check if it is a member of the same nest group.
     * @return {@code true} if this type and the supplied type are members of the same nest group.
     */
    boolean isNestMateOf(Class<?> type);

    /**
     * Checks if this type and the supplied type are members of the same nest group.
     *
     * @param typeDescription The type for which to check if it is a member of the same nest group.
     * @return {@code true} if this type and the supplied type are members of the same nest group.
     */
    boolean isNestMateOf(TypeDescription typeDescription);

    /**
     * <p>
     * Represents a generic type of the Java programming language. A non-generic {@link TypeDescription} is considered to be
     * a specialization of a generic type.
     * </p>
     * <p>
     * Note that annotations that are declared on an annotated type refer to any type annotations that are declared by this
     * generic type. For reading annotations of the erasure type, {@link TypeDefinition#asErasure()} must be called before.
     * </p>
     */
    interface Generic extends TypeDefinition, AnnotationSource {

        /**
         * A representation of the {@link Object} type.
         */
        Generic OBJECT = new OfNonGenericType.ForLoadedType(Object.class);

        /**
         * A representation of the {@link Class} non-type.
         */
        Generic CLASS = new OfNonGenericType.ForLoadedType(Class.class);

        /**
         * A representation of the {@code void} non-type.
         */
        Generic VOID = new OfNonGenericType.ForLoadedType(void.class);

        /**
         * A representation of the {@link Annotation} type.
         */
        Generic ANNOTATION = new OfNonGenericType.ForLoadedType(Annotation.class);

        /**
         * Represents any undefined property representing a generic type description that is instead represented as {@code null} in order
         * to resemble the Java reflection API which returns {@code null} and is intuitive to many Java developers.
         */
        Generic UNDEFINED = null;

        /**
         * Returns this type as a raw type. This resembles calling {@code asErasure().asGenericType()}.
         *
         * @return This type as a raw type.
         */
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
         * Returns the type arguments of this type.
         * </p>
         * <p>
         * Parameters are only well-defined for parameterized types ({@link Sort#PARAMETERIZED}).
         * For all other types, this method throws an {@link IllegalStateException}.
         * </p>
         *
         * @return A list of this type's type parameters.
         */
        TypeList.Generic getTypeArguments();

        /**
         * <p>
         * Returns the owner type of this type. A type's owner type describes a nested type's declaring type.
         * If it exists, the returned type can be a non-generic or parameterized type. If a class has no
         * declaring type, {@code null} is returned.
         * </p>
         * <p>
         * An owner type is only well-defined for parameterized types ({@link Sort#PARAMETERIZED}),
         * for non-generic types ({@link Sort#NON_GENERIC}) and for generic arrays ({@link Sort#GENERIC_ARRAY}).
         * For all other types, this method throws an {@link IllegalStateException}.
         * </p>
         *
         * @return This type's owner type or {@code null} if no owner type exists.
         */
        Generic getOwnerType();

        /**
         * <p>
         * Returns the parameter binding of the supplied type variable.
         * </p>
         * <p>
         * This method must only be called for parameterized types ({@link Sort#PARAMETERIZED}). For all other types,
         * this method throws an {@link IllegalStateException}.
         * </p>
         *
         * @param typeVariable The type variable for which a value should be located.
         * @return The value that is bound to the supplied type variable or {@code null} if the type variable
         * is not bound by this parameterized type.
         */
        Generic findBindingOf(Generic typeVariable);

        /**
         * Returns the source of this type variable. A type variable source is only well-defined for an attached type variable
         * ({@link Sort#VARIABLE}. For other types, this method
         * throws an {@link IllegalStateException}.
         *
         * @return This type's type variable source.
         */
        TypeVariableSource getTypeVariableSource();

        /**
         * Returns the symbol of this type variable. A symbol is only well-defined for type variables
         * ({@link Sort#VARIABLE}, {@link Sort#VARIABLE_SYMBOLIC}). For other types, this method
         * throws an {@link IllegalStateException}.
         *
         * @return This type's type variable symbol.
         */
        String getSymbol();

        /**
         * {@inheritDoc}
         */
        Generic getComponentType();

        /**
         * {@inheritDoc}
         */
        FieldList<FieldDescription.InGenericShape> getDeclaredFields();

        /**
         * {@inheritDoc}
         */
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

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    return genericArray;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    return wildcard;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onParameterizedType(Generic parameterizedType) {
                    return parameterizedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onTypeVariable(Generic typeVariable) {
                    return typeVariable;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription;
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

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    return genericArray.asRawType();
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    throw new IllegalArgumentException("Cannot erase a wildcard type: " + wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onParameterizedType(Generic parameterizedType) {
                    return parameterizedType.asRawType();
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onTypeVariable(Generic typeVariable) {
                    return typeVariable.asRawType();
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription.asRawType();
                }
            }

            /**
             * A visitor that strips all type annotations of all types.
             */
            enum AnnotationStripper implements Visitor<Generic> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    return new OfGenericArray.Latent(genericArray.getComponentType().accept(this), Empty.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    return new OfWildcardType.Latent(wildcard.getUpperBounds().accept(this), wildcard.getLowerBounds().accept(this), Empty.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onParameterizedType(Generic parameterizedType) {
                    Generic ownerType = parameterizedType.getOwnerType();
                    return new OfParameterizedType.Latent(parameterizedType.asErasure(),
                            ownerType == null
                                    ? UNDEFINED
                                    : ownerType.accept(this),
                            parameterizedType.getTypeArguments().accept(this),
                            Empty.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onTypeVariable(Generic typeVariable) {
                    return new NonAnnotatedTypeVariable(typeVariable);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription.isArray()
                            ? new OfGenericArray.Latent(onNonGenericType(typeDescription.getComponentType()), Empty.INSTANCE)
                            : new OfNonGenericType.Latent(typeDescription.asErasure(), Empty.INSTANCE);
                }

                /**
                 * Representation of a type variable without annotations.
                 */
                protected static class NonAnnotatedTypeVariable extends OfTypeVariable {

                    /**
                     * The represented type variable.
                     */
                    private final Generic typeVariable;

                    /**
                     * Creates a new non-annotated type variable.
                     *
                     * @param typeVariable The represented type variable.
                     */
                    protected NonAnnotatedTypeVariable(Generic typeVariable) {
                        this.typeVariable = typeVariable;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeList.Generic getUpperBounds() {
                        return typeVariable.getUpperBounds();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeVariableSource getTypeVariableSource() {
                        return typeVariable.getTypeVariableSource();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getSymbol() {
                        return typeVariable.getSymbol();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return new AnnotationList.Empty();
                    }
                }
            }

            /**
             * A visitor that determines the direct assignability of a type to another generic type. This visitor only checks
             * for strict assignability and does not perform any form of boxing or primitive type widening that are allowed
             * in the Java language.
             */
            enum Assigner implements Visitor<Assigner.Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher onGenericArray(Generic genericArray) {
                    return new Dispatcher.ForGenericArray(genericArray);
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher onWildcard(Generic wildcard) {
                    throw new IllegalArgumentException("A wildcard is not a first level type: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher onParameterizedType(Generic parameterizedType) {
                    return new Dispatcher.ForParameterizedType(parameterizedType);
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher onTypeVariable(Generic typeVariable) {
                    return new Dispatcher.ForTypeVariable(typeVariable);
                }

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher onNonGenericType(Generic typeDescription) {
                    return new Dispatcher.ForNonGenericType(typeDescription.asErasure());
                }

                /**
                 * A dispatcher that allows to check if the visited generic type is assignable to the supplied type.
                 */
                public interface Dispatcher {

                    /**
                     * Checks if the represented type is a super type of the type that is supplied as an argument.
                     *
                     * @param typeDescription The type to check for being assignable to the represented type.
                     * @return {@code true} if the represented type is assignable to the supplied type.
                     */
                    boolean isAssignableFrom(Generic typeDescription);

                    /**
                     * An abstract base implementation of a dispatcher that forwards the decision to a visitor implementation.
                     */
                    abstract class AbstractBase implements Dispatcher, Visitor<Boolean> {

                        /**
                         * {@inheritDoc}
                         */
                        public boolean isAssignableFrom(Generic typeDescription) {
                            return typeDescription.accept(this);
                        }
                    }

                    /**
                     * A dispatcher for checking the assignability of a non-generic type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class ForNonGenericType extends AbstractBase {

                        /**
                         * The description of the type to which another type is assigned.
                         */
                        private final TypeDescription typeDescription;

                        /**
                         * Creates a new dispatcher of a non-generic type.
                         *
                         * @param typeDescription The description of the type to which another type is assigned.
                         */
                        protected ForNonGenericType(TypeDescription typeDescription) {
                            this.typeDescription = typeDescription;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onGenericArray(Generic genericArray) {
                            return typeDescription.isArray()
                                    ? genericArray.getComponentType().accept(new ForNonGenericType(typeDescription.getComponentType()))
                                    : typeDescription.represents(Object.class) || TypeDescription.ARRAY_INTERFACES.contains(typeDescription.asGenericType());
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onWildcard(Generic wildcard) {
                            throw new IllegalArgumentException("A wildcard is not a first-level type: " + wildcard);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onParameterizedType(Generic parameterizedType) {
                            if (typeDescription.equals(parameterizedType.asErasure())) {
                                return true;
                            }
                            Generic superClass = parameterizedType.getSuperClass();
                            if (superClass != null && isAssignableFrom(superClass)) {
                                return true;
                            }
                            for (Generic interfaceType : parameterizedType.getInterfaces()) {
                                if (isAssignableFrom(interfaceType)) {
                                    return true;
                                }
                            }
                            return typeDescription.represents(Object.class);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onTypeVariable(Generic typeVariable) {
                            for (Generic upperBound : typeVariable.getUpperBounds()) {
                                if (isAssignableFrom(upperBound)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onNonGenericType(Generic typeDescription) {
                            return this.typeDescription.isAssignableFrom(typeDescription.asErasure());
                        }
                    }

                    /**
                     * A dispatcher for checking the assignability of a type variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class ForTypeVariable extends AbstractBase {

                        /**
                         * The description of the type variable to which another type is assigned.
                         */
                        private final Generic typeVariable;

                        /**
                         * Creates a new dispatcher of a type variable.
                         *
                         * @param typeVariable The description of the type variable to which another type is assigned.
                         */
                        protected ForTypeVariable(Generic typeVariable) {
                            this.typeVariable = typeVariable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onGenericArray(Generic genericArray) {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onWildcard(Generic wildcard) {
                            throw new IllegalArgumentException("A wildcard is not a first-level type: " + wildcard);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onParameterizedType(Generic parameterizedType) {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onTypeVariable(Generic typeVariable) {
                            if (typeVariable.equals(this.typeVariable)) {
                                return true;
                            }
                            for (Generic upperBound : typeVariable.getUpperBounds()) {
                                if (isAssignableFrom(upperBound)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onNonGenericType(Generic typeDescription) {
                            return false;
                        }
                    }

                    /**
                     * A dispatcher for checking the assignability of a parameterized type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class ForParameterizedType extends AbstractBase {

                        /**
                         * The parameterized type to which another type is assigned.
                         */
                        private final Generic parameterizedType;

                        /**
                         * Creates a new dispatcher for checking the assignability of a parameterized type.
                         *
                         * @param parameterizedType The parameterized type to which another type is assigned.
                         */
                        protected ForParameterizedType(Generic parameterizedType) {
                            this.parameterizedType = parameterizedType;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onGenericArray(Generic genericArray) {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onWildcard(Generic wildcard) {
                            throw new IllegalArgumentException("A wildcard is not a first-level type: " + wildcard);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onParameterizedType(Generic parameterizedType) {
                            if (this.parameterizedType.asErasure().equals(parameterizedType.asErasure())) {
                                Generic fromOwner = this.parameterizedType.getOwnerType(), toOwner = parameterizedType.getOwnerType();
                                if (fromOwner != null && toOwner != null && !fromOwner.accept(Assigner.INSTANCE).isAssignableFrom(toOwner)) {
                                    return false;
                                }
                                TypeList.Generic fromArguments = this.parameterizedType.getTypeArguments(), toArguments = parameterizedType.getTypeArguments();
                                if (fromArguments.size() == toArguments.size()) {
                                    for (int index = 0; index < fromArguments.size(); index++) {
                                        if (!fromArguments.get(index).accept(ParameterAssigner.INSTANCE).isAssignableFrom(toArguments.get(index))) {
                                            return false;
                                        }
                                    }
                                    return true;
                                } else {
                                    throw new IllegalArgumentException("Incompatible generic types: " + parameterizedType + " and " + this.parameterizedType);
                                }
                            }
                            Generic superClass = parameterizedType.getSuperClass();
                            if (superClass != null && isAssignableFrom(superClass)) {
                                return true;
                            }
                            for (Generic interfaceType : parameterizedType.getInterfaces()) {
                                if (isAssignableFrom(interfaceType)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onTypeVariable(Generic typeVariable) {
                            for (Generic upperBound : typeVariable.getUpperBounds()) {
                                if (isAssignableFrom(upperBound)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onNonGenericType(Generic typeDescription) {
                            if (parameterizedType.asErasure().equals(typeDescription.asErasure())) {
                                return true;
                            }
                            Generic superClass = typeDescription.getSuperClass();
                            if (superClass != null && isAssignableFrom(superClass)) {
                                return true;
                            }
                            for (Generic interfaceType : typeDescription.getInterfaces()) {
                                if (isAssignableFrom(interfaceType)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        /**
                         * An assigner for a parameter of a parameterized type.
                         */
                        protected enum ParameterAssigner implements Visitor<Dispatcher> {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher onGenericArray(Generic genericArray) {
                                return new InvariantBinding(genericArray);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher onWildcard(Generic wildcard) {
                                TypeList.Generic lowerBounds = wildcard.getLowerBounds();
                                return lowerBounds.isEmpty()
                                        ? new CovariantBinding(wildcard.getUpperBounds().getOnly())
                                        : new ContravariantBinding(lowerBounds.getOnly());
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher onParameterizedType(Generic parameterizedType) {
                                return new InvariantBinding(parameterizedType);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher onTypeVariable(Generic typeVariable) {
                                return new InvariantBinding(typeVariable);
                            }

                            /**
                             * {@inheritDoc}
                             */
                            public Dispatcher onNonGenericType(Generic typeDescription) {
                                return new InvariantBinding(typeDescription);
                            }

                            /**
                             * A dispatcher for an invariant parameter of a parameterized type, i.e. a type without a wildcard.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class InvariantBinding implements Dispatcher {

                                /**
                                 * The invariant type of the parameter.
                                 */
                                private final Generic typeDescription;

                                /**
                                 * Creates a new dispatcher for an invariant parameter of a parameterized type.
                                 *
                                 * @param typeDescription The invariant type of the parameter.
                                 */
                                protected InvariantBinding(Generic typeDescription) {
                                    this.typeDescription = typeDescription;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public boolean isAssignableFrom(Generic typeDescription) {
                                    return typeDescription.equals(this.typeDescription);
                                }
                            }

                            /**
                             * A dispatcher for an covariant parameter of a parameterized type, i.e. a type that is the lower bound of a wildcard.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class CovariantBinding implements Dispatcher {

                                /**
                                 * The lower bound type of a covariant parameter.
                                 */
                                private final Generic upperBound;

                                /**
                                 * Creates a new dispatcher for covariant parameter of a parameterized type.
                                 *
                                 * @param upperBound The upper bound type of a covariant parameter.
                                 */
                                protected CovariantBinding(Generic upperBound) {
                                    this.upperBound = upperBound;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public boolean isAssignableFrom(Generic typeDescription) {
                                    if (typeDescription.getSort().isWildcard()) {
                                        return typeDescription.getLowerBounds().isEmpty() && upperBound.accept(Assigner.INSTANCE)
                                                .isAssignableFrom(typeDescription.getUpperBounds().getOnly());
                                    } else {
                                        return upperBound.accept(Assigner.INSTANCE).isAssignableFrom(typeDescription);
                                    }
                                }
                            }

                            /**
                             * A dispatcher for an contravariant parameter of a parameterized type, i.e. a type that is the lower bound of a wildcard.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class ContravariantBinding implements Dispatcher {

                                /**
                                 * The lower bound type of a contravariant parameter.
                                 */
                                private final Generic lowerBound;

                                /**
                                 * Creates a new dispatcher for contravariant parameter of a parameterized type.
                                 *
                                 * @param lowerBound The lower bound type of a contravariant parameter.
                                 */
                                protected ContravariantBinding(Generic lowerBound) {
                                    this.lowerBound = lowerBound;
                                }

                                /**
                                 * {@inheritDoc}
                                 */
                                public boolean isAssignableFrom(Generic typeDescription) {
                                    if (typeDescription.getSort().isWildcard()) {
                                        TypeList.Generic lowerBounds = typeDescription.getLowerBounds();
                                        return !lowerBounds.isEmpty() && lowerBounds.getOnly().accept(Assigner.INSTANCE).isAssignableFrom(lowerBound);
                                    } else {
                                        return typeDescription.getSort().isWildcard() || typeDescription.accept(Assigner.INSTANCE).isAssignableFrom(lowerBound);
                                    }
                                }
                            }
                        }
                    }

                    /**
                     * A dispatcher for checking the assignability of a generic array type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class ForGenericArray extends AbstractBase {

                        /**
                         * The generic array type to which another type is assigned.
                         */
                        private final Generic genericArray;

                        /**
                         * Creates a new dispatcher for checking the assignability of a generic array type.
                         *
                         * @param genericArray The generic array type to which another type is assigned.
                         */
                        protected ForGenericArray(Generic genericArray) {
                            this.genericArray = genericArray;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onGenericArray(Generic genericArray) {
                            return this.genericArray.getComponentType().accept(Assigner.INSTANCE).isAssignableFrom(genericArray.getComponentType());
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onWildcard(Generic wildcard) {
                            throw new IllegalArgumentException("A wildcard is not a first-level type: " + wildcard);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onParameterizedType(Generic parameterizedType) {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onTypeVariable(Generic typeVariable) {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Boolean onNonGenericType(Generic typeDescription) {
                            return typeDescription.isArray()
                                    && genericArray.getComponentType().accept(Assigner.INSTANCE).isAssignableFrom(typeDescription.getComponentType());
                        }
                    }
                }
            }

            /**
             * A validator for Java types that are defined for a specified type use within a Java class file.
             */
            enum Validator implements Visitor<Boolean> {

                /**
                 * A validator for checking a type's non-null super class.
                 */
                SUPER_CLASS(false, false, false, false) {
                    /** {@inheritDoc} */
                    public Boolean onNonGenericType(Generic typeDescription) {
                        return super.onNonGenericType(typeDescription) && !typeDescription.isInterface();
                    }

                    /** {@inheritDoc} */
                    public Boolean onParameterizedType(Generic parameterizedType) {
                        return !parameterizedType.isInterface();
                    }
                },

                /**
                 * A validator for an interface type.
                 */
                INTERFACE(false, false, false, false) {
                    /** {@inheritDoc} */
                    public Boolean onNonGenericType(Generic typeDescription) {
                        return super.onNonGenericType(typeDescription) && typeDescription.isInterface();
                    }

                    /** {@inheritDoc} */
                    public Boolean onParameterizedType(Generic parameterizedType) {
                        return parameterizedType.isInterface();
                    }
                },

                /**
                 * A validator for a type variable.
                 */
                TYPE_VARIABLE(false, false, true, false),

                /**
                 * A validator for a field type.
                 */
                FIELD(true, true, true, false),

                /**
                 * A validator for a method return type.
                 */
                METHOD_RETURN(true, true, true, true),

                /**
                 * A validator for a method parameter type.
                 */
                METHOD_PARAMETER(true, true, true, false),

                /**
                 * A validator for a method exception type.
                 */
                EXCEPTION(false, false, true, false) {
                    /** {@inheritDoc} */
                    public Boolean onParameterizedType(Generic parameterizedType) {
                        return false;
                    }

                    /** {@inheritDoc} */
                    public Boolean onTypeVariable(Generic typeVariable) {
                        for (TypeDescription.Generic bound : typeVariable.getUpperBounds()) {
                            if (bound.accept(this)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    /** {@inheritDoc} */
                    public Boolean onNonGenericType(Generic typeDescription) {
                        return typeDescription.asErasure().isAssignableTo(Throwable.class);
                    }
                },

                /**
                 * A validator for a method receiver type.
                 */
                RECEIVER(false, false, false, false);

                /**
                 * {@code true} if this validator accepts array types.
                 */
                private final boolean acceptsArray;

                /**
                 * {@code true} if this validator accepts primitive types.
                 */
                private final boolean acceptsPrimitive;

                /**
                 * {@code true} if this validator accepts type variables.
                 */
                private final boolean acceptsVariable;

                /**
                 * {@code true} if this validator accepts the {@code void} type.
                 */
                private final boolean acceptsVoid;

                /**
                 * Creates a new validator.
                 *
                 * @param acceptsArray     {@code true} if this validator accepts array types.
                 * @param acceptsPrimitive {@code true} if this validator accepts primitive types.
                 * @param acceptsVariable  {@code true} if this validator accepts type variables.
                 * @param acceptsVoid      {@code true} if this validator accepts the {@code void} type.
                 */
                Validator(boolean acceptsArray, boolean acceptsPrimitive, boolean acceptsVariable, boolean acceptsVoid) {
                    this.acceptsArray = acceptsArray;
                    this.acceptsPrimitive = acceptsPrimitive;
                    this.acceptsVariable = acceptsVariable;
                    this.acceptsVoid = acceptsVoid;
                }

                /**
                 * {@inheritDoc}
                 */
                public Boolean onGenericArray(Generic genericArray) {
                    return acceptsArray;
                }

                /**
                 * {@inheritDoc}
                 */
                public Boolean onWildcard(Generic wildcard) {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public Boolean onParameterizedType(Generic parameterizedType) {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public Boolean onTypeVariable(Generic typeVariable) {
                    return acceptsVariable;
                }

                /**
                 * {@inheritDoc}
                 */
                public Boolean onNonGenericType(Generic typeDescription) {
                    return (acceptsArray || !typeDescription.isArray())
                            && (acceptsPrimitive || !typeDescription.isPrimitive())
                            && (acceptsVoid || !typeDescription.represents(void.class));
                }

                /**
                 * A type validator for checking type annotations.
                 */
                public enum ForTypeAnnotations implements Visitor<Boolean> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * The {@link ElementType}'s {@code TYPE_USE} constant.
                     */
                    private final ElementType typeUse;

                    /**
                     * The {@link ElementType}'s {@code TYPE_PARAMETER} constant.
                     */
                    private final ElementType typeParameter;

                    /**
                     * Creates a new type annotation validator.
                     */
                    ForTypeAnnotations() {
                        ElementType typeUse, typeParameter;
                        try {
                            typeUse = Enum.valueOf(ElementType.class, "TYPE_USE");
                            typeParameter = Enum.valueOf(ElementType.class, "TYPE_PARAMETER");
                        } catch (IllegalArgumentException ignored) {
                            // Setting these values null results in this validator always failing for pre Java-8 VMs.
                            typeUse = null;
                            typeParameter = null;
                        }
                        this.typeUse = typeUse;
                        this.typeParameter = typeParameter;
                    }

                    /**
                     * Validates the type annotations on a formal type variable but not on its bounds..
                     *
                     * @param typeVariable The type variable to validate.
                     * @return {@code true} if the formal type variable declares invalid type annotations.
                     */
                    public static boolean ofFormalTypeVariable(Generic typeVariable) {
                        Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>();
                        for (AnnotationDescription annotationDescription : typeVariable.getDeclaredAnnotations()) {
                            if (!annotationDescription.getElementTypes().contains(INSTANCE.typeParameter) || !annotationTypes.add(annotationDescription.getAnnotationType())) {
                                return false;
                            }
                        }
                        return true;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Boolean onGenericArray(Generic genericArray) {
                        return isValid(genericArray) && genericArray.getComponentType().accept(this);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Boolean onWildcard(Generic wildcard) {
                        if (!isValid(wildcard)) {
                            return false;
                        }
                        TypeList.Generic lowerBounds = wildcard.getLowerBounds();
                        return (lowerBounds.isEmpty()
                                ? wildcard.getUpperBounds()
                                : lowerBounds).getOnly().accept(this);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Boolean onParameterizedType(Generic parameterizedType) {
                        if (!isValid(parameterizedType)) {
                            return false;
                        }
                        Generic ownerType = parameterizedType.getOwnerType();
                        if (ownerType != null && !ownerType.accept(this)) {
                            return false;
                        }
                        for (Generic typeArgument : parameterizedType.getTypeArguments()) {
                            if (!typeArgument.accept(this)) {
                                return false;
                            }
                        }
                        return true;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Boolean onTypeVariable(Generic typeVariable) {
                        return isValid(typeVariable);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Boolean onNonGenericType(Generic typeDescription) {
                        return isValid(typeDescription) && (!typeDescription.isArray() || typeDescription.getComponentType().accept(this));
                    }

                    /**
                     * Checks if the supplied type's type annotations are valid.
                     *
                     * @param typeDescription The type to validate.
                     * @return {@code true} if the supplied type's type annotations are valid.
                     */
                    private boolean isValid(Generic typeDescription) {
                        Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>();
                        for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
                            if (!annotationDescription.getElementTypes().contains(typeUse) || !annotationTypes.add(annotationDescription.getAnnotationType())) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }

            /**
             * A visitor that reifies type descriptions if they represent raw types.
             */
            enum Reifying implements Visitor<Generic> {

                /**
                 * A visitor that reifies non-generic types if they represent raw types. This visitor should be applied when
                 * visiting a potential raw type.
                 */
                INITIATING {
                    /** {@inheritDoc} */
                    public Generic onParameterizedType(Generic parameterizedType) {
                        return parameterizedType;
                    }
                },

                /**
                 * A visitor that reifies non-generic types if they represent raw types or are parameterized types. This visitor
                 * should only be applied when a type was inherited from a reified type.
                 */
                INHERITING {
                    /** {@inheritDoc} */
                    public Generic onParameterizedType(Generic parameterizedType) {
                        return new OfParameterizedType.ForReifiedType(parameterizedType);
                    }
                };

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    throw new IllegalArgumentException("Cannot reify a generic array: " + genericArray);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    throw new IllegalArgumentException("Cannot reify a wildcard: " + wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onTypeVariable(Generic typeVariable) {
                    throw new IllegalArgumentException("Cannot reify a type variable: " + typeVariable);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    TypeDescription erasure = typeDescription.asErasure();
                    return erasure.isGenerified()
                            ? new OfNonGenericType.ForReifiedErasure(erasure)
                            : typeDescription;
                }
            }

            /**
             * Visits a generic type and appends the discovered type to the supplied signature visitor.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForSignatureVisitor implements Visitor<SignatureVisitor> {

                /**
                 * Index of a {@link String}'s only character to improve code readability.
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

                /**
                 * {@inheritDoc}
                 */
                public SignatureVisitor onGenericArray(Generic genericArray) {
                    genericArray.getComponentType().accept(new ForSignatureVisitor(signatureVisitor.visitArrayType()));
                    return signatureVisitor;
                }

                /**
                 * {@inheritDoc}
                 */
                public SignatureVisitor onWildcard(Generic wildcard) {
                    throw new IllegalStateException("Unexpected wildcard: " + wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
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
                    for (Generic typeArgument : ownableType.getTypeArguments()) {
                        typeArgument.accept(new OfTypeArgument(signatureVisitor));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public SignatureVisitor onTypeVariable(Generic typeVariable) {
                    signatureVisitor.visitTypeVariable(typeVariable.getSymbol());
                    return signatureVisitor;
                }

                /**
                 * {@inheritDoc}
                 */
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

                /**
                 * Visits a parameter while visiting a generic type for delegating discoveries to a signature visitor.
                 */
                protected static class OfTypeArgument extends ForSignatureVisitor {

                    /**
                     * Creates a new parameter visitor.
                     *
                     * @param signatureVisitor The signature visitor which is notified over visited types.
                     */
                    protected OfTypeArgument(SignatureVisitor signatureVisitor) {
                        super(signatureVisitor);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public SignatureVisitor onWildcard(Generic wildcard) {
                        TypeList.Generic upperBounds = wildcard.getUpperBounds(), lowerBounds = wildcard.getLowerBounds();
                        if (lowerBounds.isEmpty() && upperBounds.getOnly().represents(Object.class)) {
                            signatureVisitor.visitTypeArgument();
                        } else if (!lowerBounds.isEmpty() /* && upperBounds.isEmpty() */) {
                            lowerBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.SUPER)));
                        } else /* if (!upperBounds.isEmpty() && lowerBounds.isEmpty()) */ {
                            upperBounds.getOnly().accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.EXTENDS)));
                        }
                        return signatureVisitor;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public SignatureVisitor onGenericArray(Generic genericArray) {
                        genericArray.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public SignatureVisitor onParameterizedType(Generic parameterizedType) {
                        parameterizedType.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public SignatureVisitor onTypeVariable(Generic typeVariable) {
                        typeVariable.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public SignatureVisitor onNonGenericType(Generic typeDescription) {
                        typeDescription.accept(new ForSignatureVisitor(signatureVisitor.visitTypeArgument(SignatureVisitor.INSTANCEOF)));
                        return signatureVisitor;
                    }
                }
            }

            /**
             * An abstract implementation of a visitor that substitutes generic types by replacing (nested)
             * type variables and/or non-generic component types.
             */
            abstract class Substitutor implements Visitor<Generic> {

                /**
                 * {@inheritDoc}
                 */
                public Generic onParameterizedType(Generic parameterizedType) {
                    Generic ownerType = parameterizedType.getOwnerType();
                    List<Generic> typeArguments = new ArrayList<Generic>(parameterizedType.getTypeArguments().size());
                    for (Generic typeArgument : parameterizedType.getTypeArguments()) {
                        typeArguments.add(typeArgument.accept(this));
                    }
                    return new OfParameterizedType.Latent(parameterizedType.asRawType().accept(this).asErasure(),
                            ownerType == null
                                    ? UNDEFINED
                                    : ownerType.accept(this),
                            typeArguments,
                            parameterizedType);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    return new OfGenericArray.Latent(genericArray.getComponentType().accept(this), genericArray);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    return new OfWildcardType.Latent(wildcard.getUpperBounds().accept(this), wildcard.getLowerBounds().accept(this), wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription.isArray()
                            ? new OfGenericArray.Latent(typeDescription.getComponentType().accept(this), typeDescription)
                            : onSimpleType(typeDescription);
                }

                /**
                 * Visits a simple, non-generic type, i.e. either a component type of an array or a non-array type.
                 *
                 * @param typeDescription The type that is visited.
                 * @return The substituted type.
                 */
                protected abstract Generic onSimpleType(Generic typeDescription);

                /**
                 * A {@link Substitutor} that only substitutes type variables but fully preserves non-generic type definitions.
                 */
                public abstract static class WithoutTypeSubstitution extends Substitutor {

                    /**
                     * {@inheritDoc}
                     */
                    public Generic onNonGenericType(Generic typeDescription) {
                        return typeDescription;
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
                        return typeDescription;
                    }
                }

                /**
                 * A substitutor that attaches type variables to a type variable source and replaces representations of
                 * {@link TargetType} with a given declaring type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
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
                     * @param declaringType      The declaring type which is filled in for {@link TargetType} in its erased form.
                     * @param typeVariableSource The source which is used for locating type variables.
                     */
                    protected ForAttachment(TypeDefinition declaringType, TypeVariableSource typeVariableSource) {
                        this(declaringType.asErasure(), typeVariableSource);
                    }

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
                        return new ForAttachment(fieldDescription.getDeclaringType(), fieldDescription.getDeclaringType().asErasure());
                    }

                    /**
                     * Attaches all types to the given method description.
                     *
                     * @param methodDescription The method description to which visited types should be attached to.
                     * @return A substitutor that attaches visited types to the given method's type context.
                     */
                    public static ForAttachment of(MethodDescription methodDescription) {
                        return new ForAttachment(methodDescription.getDeclaringType(), methodDescription);
                    }

                    /**
                     * Attaches all types to the given parameter description.
                     *
                     * @param parameterDescription The parameter description to which visited types should be attached to.
                     * @return A substitutor that attaches visited types to the given parameter's type context.
                     */
                    public static ForAttachment of(ParameterDescription parameterDescription) {
                        return new ForAttachment(parameterDescription.getDeclaringMethod().getDeclaringType(), parameterDescription.getDeclaringMethod());
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

                    /**
                     * {@inheritDoc}
                     */
                    public Generic onTypeVariable(Generic typeVariable) {
                        Generic attachedVariable = typeVariableSource.findVariable(typeVariable.getSymbol());
                        if (attachedVariable == null) {
                            throw new IllegalArgumentException("Cannot attach undefined variable: " + typeVariable);
                        } else {
                            return new OfTypeVariable.WithAnnotationOverlay(attachedVariable, typeVariable);
                        }
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
                        return typeDescription.represents(TargetType.class)
                                ? new OfNonGenericType.Latent(declaringType, typeDescription)
                                : typeDescription;
                    }
                }

                /**
                 * A visitor for detaching a type from its declaration context by detaching type variables. This is achieved by
                 * detaching type variables and by replacing the declaring type which is identified by a provided {@link ElementMatcher}
                 * with {@link TargetType}.
                 */
                @HashCodeAndEqualsPlugin.Enhance
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

                    /**
                     * Returns a new detachment visitor that detaches any type matching the supplied type description.
                     *
                     * @param typeDefinition The type to detach.
                     * @return A detachment visitor for the supplied type description.
                     */
                    public static Visitor<Generic> of(TypeDefinition typeDefinition) {
                        return new ForDetachment(is(typeDefinition));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic onTypeVariable(Generic typeVariable) {
                        return new OfTypeVariable.Symbolic(typeVariable.getSymbol(), typeVariable);
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
                        return typeMatcher.matches(typeDescription.asErasure())
                                ? new OfNonGenericType.Latent(TargetType.DESCRIPTION, typeDescription.getOwnerType(), typeDescription)
                                : typeDescription;
                    }
                }

                /**
                 * A visitor for binding type variables to their values.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class ForTypeVariableBinding extends WithoutTypeSubstitution {

                    /**
                     * The parameterized type for which type variables are bound.
                     */
                    private final Generic parameterizedType;

                    /**
                     * Creates a new visitor for binding a parameterized type's type arguments to its type variables.
                     *
                     * @param parameterizedType The parameterized type for which type variables are bound.
                     */
                    protected ForTypeVariableBinding(Generic parameterizedType) {
                        this.parameterizedType = parameterizedType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic onTypeVariable(Generic typeVariable) {
                        return typeVariable.getTypeVariableSource().accept(new TypeVariableSubstitutor(typeVariable));
                    }

                    /**
                     * Substitutes a type variable, either with a new binding if the variable is defined by a type or with a
                     * retained type variable if the variable is defined by a method.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class TypeVariableSubstitutor implements TypeVariableSource.Visitor<Generic> {

                        /**
                         * The discovered type variable.
                         */
                        private final Generic typeVariable;

                        /**
                         * Creates a new type variable substitutor.
                         *
                         * @param typeVariable The discovered type variable.
                         */
                        protected TypeVariableSubstitutor(Generic typeVariable) {
                            this.typeVariable = typeVariable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Generic onType(TypeDescription typeDescription) {
                            // A type variable might be undeclared due to breaking inner class semantics or due to incorrect scoping by a compiler.
                            Generic typeArgument = parameterizedType.findBindingOf(typeVariable);
                            return typeArgument == null
                                    ? typeVariable.asRawType()
                                    : typeArgument;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Generic onMethod(MethodDescription.InDefinedShape methodDescription) {
                            return new RetainedMethodTypeVariable(typeVariable);
                        }
                    }

                    /**
                     * Implementation of a type variable on a method that is not substituted.
                     */
                    protected class RetainedMethodTypeVariable extends OfTypeVariable {

                        /**
                         * The discovered type variable.
                         */
                        private final Generic typeVariable;

                        /**
                         * Creates a new retained type variable.
                         *
                         * @param typeVariable The discovered type variable.
                         */
                        protected RetainedMethodTypeVariable(Generic typeVariable) {
                            this.typeVariable = typeVariable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeList.Generic getUpperBounds() {
                            return typeVariable.getUpperBounds().accept(ForTypeVariableBinding.this);
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public TypeVariableSource getTypeVariableSource() {
                            return typeVariable.getTypeVariableSource();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public String getSymbol() {
                            return typeVariable.getSymbol();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotationList getDeclaredAnnotations() {
                            return typeVariable.getDeclaredAnnotations();
                        }
                    }
                }

                /**
                 * A substitutor that normalizes a token to represent all {@link TargetType} by a given type and that symbolizes all type variables.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class ForTokenNormalization extends Substitutor {

                    /**
                     * The type description to substitute all {@link TargetType} representations with.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new token normalization visitor.
                     *
                     * @param typeDescription The type description to substitute all {@link TargetType}
                     */
                    public ForTokenNormalization(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    protected Generic onSimpleType(Generic typeDescription) {
                        return typeDescription.represents(TargetType.class)
                                ? new OfNonGenericType.Latent(this.typeDescription, typeDescription)
                                : typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic onTypeVariable(Generic typeVariable) {
                        return new OfTypeVariable.Symbolic(typeVariable.getSymbol(), typeVariable);
                    }
                }
            }

            /**
             * A visitor that transforms any type into a raw type if declaring type is generified. If the declaring type is
             * not generified, the original type description is returned.
             */
            class ForRawType implements Visitor<Generic> {

                /**
                 * The type description that is potentially a raw type.
                 */
                private final TypeDescription declaringType;

                /**
                 * Creates a visitor for representing declared types of a potentially raw type.
                 *
                 * @param declaringType The type description that is potentially a raw type.
                 */
                public ForRawType(TypeDescription declaringType) {
                    this.declaringType = declaringType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onGenericArray(Generic genericArray) {
                    return declaringType.isGenerified()
                            ? new Generic.OfNonGenericType.Latent(genericArray.asErasure(), genericArray)
                            : genericArray;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onWildcard(Generic wildcard) {
                    throw new IllegalStateException("Did not expect wildcard on top-level: " + wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onParameterizedType(Generic parameterizedType) {
                    return declaringType.isGenerified()
                            ? new Generic.OfNonGenericType.Latent(parameterizedType.asErasure(), parameterizedType)
                            : parameterizedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onTypeVariable(Generic typeVariable) {
                    return declaringType.isGenerified()
                            ? new Generic.OfNonGenericType.Latent(typeVariable.asErasure(), typeVariable)
                            : typeVariable;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic onNonGenericType(Generic typeDescription) {
                    return typeDescription;
                }
            }

            /**
             * A visitor that reduces a detached generic type to its erasure.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Reducing implements Visitor<TypeDescription> {

                /**
                 * The generic type's declaring type.
                 */
                private final TypeDescription declaringType;

                /**
                 * Any type variables that are directly declared by the member that declares the type being reduced.
                 */
                private final List<? extends TypeVariableToken> typeVariableTokens;

                /**
                 * Creates a new reducing type visitor.
                 *
                 * @param declaringType     The generic type's declaring type.
                 * @param typeVariableToken Any type variables that are directly declared by the member that declares the type being reduced.
                 */
                public Reducing(TypeDescription declaringType, TypeVariableToken... typeVariableToken) {
                    this(declaringType, Arrays.asList(typeVariableToken));
                }

                /**
                 * Creates a new reducing type visitor.
                 *
                 * @param declaringType      The generic type's declaring type.
                 * @param typeVariableTokens Any type variables that are directly declared by the member that declares the type being reduced.
                 */
                public Reducing(TypeDescription declaringType, List<? extends TypeVariableToken> typeVariableTokens) {
                    this.declaringType = declaringType;
                    this.typeVariableTokens = typeVariableTokens;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription onGenericArray(Generic genericArray) {
                    return TargetType.resolve(genericArray.asErasure(), declaringType);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription onWildcard(Generic wildcard) {
                    throw new IllegalStateException("A wildcard cannot be a top-level type: " + wildcard);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription onParameterizedType(Generic parameterizedType) {
                    return TargetType.resolve(parameterizedType.asErasure(), declaringType);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription onTypeVariable(Generic typeVariable) {
                    for (TypeVariableToken typeVariableToken : typeVariableTokens) {
                        if (typeVariable.getSymbol().equals(typeVariableToken.getSymbol())) {
                            return typeVariableToken.getBounds().get(0).accept(this);
                        }
                    }
                    return TargetType.resolve(declaringType.findVariable(typeVariable.getSymbol()).asErasure(), declaringType);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription onNonGenericType(Generic typeDescription) {
                    return TargetType.resolve(typeDescription.asErasure(), declaringType);
                }
            }
        }

        /**
         * An annotation reader is responsible for lazily evaluating type annotations if this language
         * feature is available on the current JVM.
         */
        interface AnnotationReader {

            /**
             * The dispatcher to use.
             */
            Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

            /**
             * Resolves the underlying {@link AnnotatedElement}.
             *
             * @return The underlying annotated element.
             */
            AnnotatedElement resolve();

            /**
             * Returns the underlying type annotations as a list.
             *
             * @return The underlying type annotations as a list.
             */
            AnnotationList asList();

            /**
             * Returns a reader for type annotations of an represented element's wildcard upper bound.
             *
             * @param index The wildcard bound's index.
             * @return An annotation reader for the underlying annotated upper bound.
             */
            AnnotationReader ofWildcardUpperBoundType(int index);

            /**
             * Returns a reader for type annotations of an represented element's wildcard lower bound.
             *
             * @param index The wildcard bound's index.
             * @return An annotation reader for the underlying annotated lower bound.
             */
            AnnotationReader ofWildcardLowerBoundType(int index);

            /**
             * Returns a reader for type annotations of a type variable's bound.
             *
             * @param index The bound's index.
             * @return An annotation reader for the underlying annotated bound.
             */
            AnnotationReader ofTypeVariableBoundType(int index);

            /**
             * Returns a reader for type annotations of a parameterized type's type argument.
             *
             * @param index The bound's index.
             * @return An annotation reader for the underlying annotated bound..
             */
            AnnotationReader ofTypeArgument(int index);

            /**
             * <p>
             * Returns a reader for type annotations of a parameterized type's owner type.
             * </p>
             * <p>
             * <b>Important</b>: This feature is not currently implemented by the Java reflection API.
             * </p>
             *
             * @return An annotation reader for the underlying owner type.
             */
            AnnotationReader ofOwnerType();

            /**
             * <p>
             * Returns a reader for type annotations of an inner class type's outer type.
             * </p>
             * <p>
             * <b>Important</b>: This feature is not currently implemented by the Java reflection API.
             * </p>
             *
             * @return An annotation reader for the underlying owner type.
             */
            AnnotationReader ofOuterClass();

            /**
             * Returns a reader for type annotations of an array's component type.
             *
             * @return An annotation reader for the underlying component type.
             */
            AnnotationReader ofComponentType();

            /**
             * A dispatcher that represents the type annotation API via reflective calls if the language feature is available on the current JVM.
             */
            interface Dispatcher {

                /**
                 * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
                 */
                Object[] NO_ARGUMENTS = new Object[0];

                /**
                 * Resolves a formal type variable's type annotations.
                 *
                 * @param typeVariable The type variable to represent.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveTypeVariable(TypeVariable<?> typeVariable);

                /**
                 * Resolves a loaded type's super class's type annotations.
                 *
                 * @param type The type to represent.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveSuperClassType(Class<?> type);

                /**
                 * Resolves a loaded type's interface type's type annotations.
                 *
                 * @param type  The type to represent.
                 * @param index The index of the interface.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveInterfaceType(Class<?> type, int index);

                /**
                 * Resolves a loaded field's type's type annotations.
                 *
                 * @param field The field to represent.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveFieldType(Field field);

                /**
                 * Resolves a loaded method's return type's type annotations.
                 *
                 * @param method The method to represent.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveReturnType(Method method);

                /**
                 * Resolves a loaded executable's type argument type's type annotations.
                 *
                 * @param executable The executable to represent.
                 * @param index      The type argument's index.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveParameterType(AccessibleObject executable, int index);

                /**
                 * Resolves a loaded executable's exception type's type annotations.
                 *
                 * @param executable The executable to represent.
                 * @param index      The type argument's index.
                 * @return A suitable annotation reader.
                 */
                AnnotationReader resolveExceptionType(AccessibleObject executable, int index);

                /**
                 * Resolves a method's or constructor's receiver type. If receiver types are not available on the executing VM,
                 * {@code null} is returned.
                 *
                 * @param executable The executable for which the receiver type should be resolved.
                 * @return The executable's receiver type or {@code null}.
                 */
                Generic resolveReceiverType(AccessibleObject executable);

                /**
                 * Resolves the annotated type as generic type description.
                 *
                 * @param annotatedType The loaded annotated type.
                 * @return A description of the supplied annotated type.
                 */
                Generic resolve(AnnotatedElement annotatedType);

                /**
                 * A creation action for a dispatcher.
                 */
                enum CreationAction implements PrivilegedAction<Dispatcher> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher run() {
                        try {
                            return new ForJava8CapableVm(Class.class.getMethod("getAnnotatedSuperclass"),
                                    Class.class.getMethod("getAnnotatedInterfaces"),
                                    Field.class.getMethod("getAnnotatedType"),
                                    Method.class.getMethod("getAnnotatedReturnType"),
                                    Class.forName("java.lang.reflect.Executable").getMethod("getAnnotatedParameterTypes"),
                                    Class.forName("java.lang.reflect.Executable").getMethod("getAnnotatedExceptionTypes"),
                                    Class.forName("java.lang.reflect.Executable").getMethod("getAnnotatedReceiverType"),
                                    Class.forName("java.lang.reflect.AnnotatedType").getMethod("getType"));
                        } catch (RuntimeException exception) {
                            throw exception;
                        } catch (Exception ignored) {
                            return Dispatcher.ForLegacyVm.INSTANCE;
                        }
                    }
                }

                /**
                 * A dispatcher for {@link AnnotationReader}s on a legacy VM that does not support type annotations.
                 */
                enum ForLegacyVm implements Dispatcher {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveTypeVariable(TypeVariable<?> typeVariable) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveSuperClassType(Class<?> type) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveInterfaceType(Class<?> type, int index) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveFieldType(Field field) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveReturnType(Method method) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveParameterType(AccessibleObject executable, int index) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveExceptionType(AccessibleObject executable, int index) {
                        return NoOp.INSTANCE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic resolveReceiverType(AccessibleObject executable) {
                        return UNDEFINED;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic resolve(AnnotatedElement annotatedType) {
                        throw new UnsupportedOperationException("Loaded annotated type cannot be represented on this VM");
                    }
                }

                /**
                 * A dispatcher for a modern JVM that supports type annotations.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForJava8CapableVm implements Dispatcher {

                    /**
                     * The {@code java.lang.Class#getAnnotatedSuperclass} method.
                     */
                    private final Method getAnnotatedSuperclass;

                    /**
                     * The {@code java.lang.Class#getAnnotatedInterfaces} method.
                     */
                    private final Method getAnnotatedInterfaces;

                    /**
                     * The {@code java.lang.reflect.Field#getAnnotatedType} method.
                     */
                    private final Method getAnnotatedType;

                    /**
                     * The {@code java.lang.reflect.Method#getAnnotatedReturnType} method.
                     */
                    private final Method getAnnotatedReturnType;

                    /**
                     * The {@code java.lang.reflect.Executable#getAnnotatedParameterTypes} method.
                     */
                    private final Method getAnnotatedParameterTypes;

                    /**
                     * The {@code java.lang.reflect.Executable#getAnnotatedExceptionTypes} method.
                     */
                    private final Method getAnnotatedExceptionTypes;

                    /**
                     * The {@code java.lang.reflect.Executable#getAnnotatedReceiverType} method.
                     */
                    private final Method getAnnotatedReceiverType;

                    /**
                     * The {@code java.lang.reflect.AnnotatedType#getType} method.
                     */
                    private final Method getType;

                    /**
                     * Creates a new dispatcher for a VM that supports type annotations.
                     *
                     * @param getAnnotatedSuperclass     The {@code java.lang.Class#getAnnotatedSuperclass} method.
                     * @param getAnnotatedInterfaces     The {@code java.lang.Class#getAnnotatedInterfaces} method.
                     * @param getAnnotatedType           The {@code java.lang.reflect.Field#getAnnotatedType} method.
                     * @param getAnnotatedReturnType     The {@code java.lang.reflect.Method#getAnnotatedReturnType} method.
                     * @param getAnnotatedParameterTypes The {@code java.lang.reflect.Executable#getAnnotatedParameterTypes} method.
                     * @param getAnnotatedExceptionTypes The {@code java.lang.reflect.Executable#getAnnotatedExceptionTypes} method.
                     * @param getAnnotatedReceiverType   The {@code java.lang.reflect.Executable#getAnnotatedReceiverType} method.
                     * @param getType                    The {@code java.lang.reflect.AnnotatedType#getType} method.
                     */
                    protected ForJava8CapableVm(Method getAnnotatedSuperclass,
                                                Method getAnnotatedInterfaces,
                                                Method getAnnotatedType,
                                                Method getAnnotatedReturnType,
                                                Method getAnnotatedParameterTypes,
                                                Method getAnnotatedExceptionTypes,
                                                Method getAnnotatedReceiverType,
                                                Method getType) {
                        this.getAnnotatedSuperclass = getAnnotatedSuperclass;
                        this.getAnnotatedInterfaces = getAnnotatedInterfaces;
                        this.getAnnotatedType = getAnnotatedType;
                        this.getAnnotatedReturnType = getAnnotatedReturnType;
                        this.getAnnotatedParameterTypes = getAnnotatedParameterTypes;
                        this.getAnnotatedExceptionTypes = getAnnotatedExceptionTypes;
                        this.getAnnotatedReceiverType = getAnnotatedReceiverType;
                        this.getType = getType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveTypeVariable(TypeVariable<?> typeVariable) {
                        return new AnnotatedTypeVariableType(typeVariable);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveSuperClassType(Class<?> type) {
                        return new AnnotatedSuperClass(type);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveInterfaceType(Class<?> type, int index) {
                        return new AnnotatedInterfaceType(type, index);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveFieldType(Field field) {
                        return new AnnotatedFieldType(field);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveReturnType(Method method) {
                        return new AnnotatedReturnType(method);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveParameterType(AccessibleObject executable, int index) {
                        return new AnnotatedParameterizedType(executable, index);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationReader resolveExceptionType(AccessibleObject executable, int index) {
                        return new AnnotatedExceptionType(executable, index);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic resolveReceiverType(AccessibleObject executable) {
                        try {
                            return resolve((AnnotatedElement) getAnnotatedReceiverType.invoke(executable, NO_ARGUMENTS));
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access java.lang.reflect.Executable#getAnnotatedReceiverType", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.reflect.Executable#getAnnotatedReceiverType", exception.getCause());
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic resolve(AnnotatedElement annotatedType) {
                        try {
                            return annotatedType == null
                                    ? UNDEFINED
                                    : Sort.describe((java.lang.reflect.Type) getType.invoke(annotatedType, NO_ARGUMENTS), new Resolved(annotatedType));
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedType#getType", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedType#getType", exception.getCause());
                        }
                    }

                    /**
                     * A delegator for an existing {@code java.lang.reflect.AnnotatedElement}.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class Resolved extends Delegator {

                        /**
                         * The represented annotated element.
                         */
                        private final AnnotatedElement annotatedElement;

                        /**
                         * Creates a new resolved delegator.
                         *
                         * @param annotatedElement The represented annotated element.
                         */
                        protected Resolved(AnnotatedElement annotatedElement) {
                            this.annotatedElement = annotatedElement;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            return annotatedElement;
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated type variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class AnnotatedTypeVariableType extends Delegator {

                        /**
                         * The represented type variable.
                         */
                        private final TypeVariable<?> typeVariable;

                        /**
                         * Creates a new annotation reader for the given type variable.
                         *
                         * @param typeVariable The represented type variable.
                         */
                        protected AnnotatedTypeVariableType(TypeVariable<?> typeVariable) {
                            this.typeVariable = typeVariable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            return (AnnotatedElement) typeVariable;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotationReader ofTypeVariableBoundType(int index) {
                            return new ForTypeVariableBoundType.OfFormalTypeVariable(typeVariable, index);
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated super type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedSuperClass extends Delegator {

                        /**
                         * The represented type.
                         */
                        private final Class<?> type;

                        /**
                         * Creates a new annotation reader for an annotated super type.
                         *
                         * @param type The represented type.
                         */
                        protected AnnotatedSuperClass(Class<?> type) {
                            this.type = type;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) getAnnotatedSuperclass.invoke(type, NO_ARGUMENTS);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.Class#getAnnotatedSuperclass", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.Class#getAnnotatedSuperclass", exception.getCause());
                            }
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated interface type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedInterfaceType extends Delegator {

                        /**
                         * The represented interface type.
                         */
                        private final Class<?> type;

                        /**
                         * The interface type's index.
                         */
                        private final int index;

                        /**
                         * Creates a new annotation reader for an annotated interface type.
                         *
                         * @param type  The represented interface type.
                         * @param index The interface type's index.
                         */
                        protected AnnotatedInterfaceType(Class<?> type, int index) {
                            this.type = type;
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) Array.get(getAnnotatedInterfaces.invoke(type), index);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.Class#getAnnotatedInterfaces", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.Class#getAnnotatedInterfaces", exception.getCause());
                            }
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated field variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedFieldType extends Delegator {

                        /**
                         * The represented field.
                         */
                        private final Field field;

                        /**
                         * Creates a new annotation reader for an annotated field type.
                         *
                         * @param field The represented field.
                         */
                        protected AnnotatedFieldType(Field field) {
                            this.field = field;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) getAnnotatedType.invoke(field, NO_ARGUMENTS);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.reflect.Field#getAnnotatedType", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.reflect.Field#getAnnotatedType", exception.getCause());
                            }
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated return variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedReturnType extends Delegator {

                        /**
                         * The represented method.
                         */
                        private final Method method;

                        /**
                         * Creates a new annotation reader for an annotated return type.
                         *
                         * @param method The represented method.
                         */
                        protected AnnotatedReturnType(Method method) {
                            this.method = method;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) getAnnotatedReturnType.invoke(method, NO_ARGUMENTS);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.reflect.Method#getAnnotatedReturnType", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.reflect.Method#getAnnotatedReturnType", exception.getCause());
                            }
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated parameter variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedParameterizedType extends Delegator {

                        /**
                         * The represented executable.
                         */
                        private final AccessibleObject executable;

                        /**
                         * The type argument's index.
                         */
                        private final int index;

                        /**
                         * Creates a new annotation reader for an annotated type argument type.
                         *
                         * @param executable The represented executable.
                         * @param index      The type argument's index.
                         */
                        protected AnnotatedParameterizedType(AccessibleObject executable, int index) {
                            this.executable = executable;
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) Array.get(getAnnotatedParameterTypes.invoke(executable, NO_ARGUMENTS), index);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.reflect.Executable#getAnnotatedParameterTypes", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.reflect.Executable#getAnnotatedParameterTypes", exception.getCause());
                            }
                        }
                    }

                    /**
                     * A delegating annotation reader for an annotated exception variable.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class AnnotatedExceptionType extends Delegator {

                        /**
                         * The represented executable.
                         */
                        private final AccessibleObject executable;

                        /**
                         * The exception type's index.
                         */
                        private final int index;

                        /**
                         * Creates a new annotation reader for an annotated exception type.
                         *
                         * @param executable The represented executable.
                         * @param index      The exception type's index.
                         */
                        protected AnnotatedExceptionType(AccessibleObject executable, int index) {
                            this.executable = executable;
                            this.index = index;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public AnnotatedElement resolve() {
                            try {
                                return (AnnotatedElement) Array.get(getAnnotatedExceptionTypes.invoke(executable, NO_ARGUMENTS), index);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access java.lang.reflect.Executable#getAnnotatedExceptionTypes", exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Error invoking java.lang.reflect.Executable#getAnnotatedExceptionTypes", exception.getCause());
                            }
                        }
                    }
                }
            }

            /**
             * A non-operational annotation reader.
             */
            enum NoOp implements AnnotationReader, AnnotatedElement {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public AnnotatedElement resolve() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList asList() {
                    return new AnnotationList.Empty();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofWildcardUpperBoundType(int index) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofWildcardLowerBoundType(int index) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofTypeVariableBoundType(int index) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofTypeArgument(int index) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofOwnerType() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofOuterClass() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofComponentType() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                    throw new IllegalStateException("Cannot resolve annotations for no-op reader: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                    throw new IllegalStateException("Cannot resolve annotations for no-op reader: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Annotation[] getAnnotations() {
                    throw new IllegalStateException("Cannot resolve annotations for no-op reader: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Annotation[] getDeclaredAnnotations() {
                    return new Annotation[0];
                }
            }

            /**
             * A delegating annotation reader that delegates all invocations to an annotation reader that wraps the previous one.
             */
            abstract class Delegator implements AnnotationReader {

                /**
                 * An empty array that can be used to indicate no arguments to avoid an allocation on a reflective call.
                 */
                protected static final Object[] NO_ARGUMENTS = new Object[0];

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofWildcardUpperBoundType(int index) {
                    return new ForWildcardUpperBoundType(this, index);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofWildcardLowerBoundType(int index) {
                    return new ForWildcardLowerBoundType(this, index);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofTypeVariableBoundType(int index) {
                    return new ForTypeVariableBoundType(this, index);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofTypeArgument(int index) {
                    return new ForTypeArgument(this, index);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofOwnerType() {
                    return ForOwnerType.of(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofOuterClass() {
                    return ForOwnerType.of(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationReader ofComponentType() {
                    return new ForComponentType(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList asList() {
                    return new AnnotationList.ForLoadedAnnotations(resolve().getDeclaredAnnotations());
                }

                /**
                 * A chained delegator that bases its result on an underlying annotation reader.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected abstract static class Chained extends Delegator {

                    /**
                     * Indicates that a method is not available on the current VM.
                     */
                    protected static final Method NOT_AVAILABLE = null;

                    /**
                     * The underlying annotation reader.
                     */
                    protected final AnnotationReader annotationReader;

                    /**
                     * Creates a new chained annotation reader.
                     *
                     * @param annotationReader The underlying annotation reader.
                     */
                    protected Chained(AnnotationReader annotationReader) {
                        this.annotationReader = annotationReader;
                    }

                    /**
                     * Resolves the method to invoke or returns {@code null} if the method does not exist on the current VM.
                     *
                     * @param typeName   The declaring type's name.
                     * @param methodName The method's name.
                     * @return The resolved method or {@code null}.
                     */
                    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
                    protected static Method of(String typeName, String methodName) {
                        try {
                            return Class.forName(typeName).getMethod(methodName);
                        } catch (Exception exception) {
                            return NOT_AVAILABLE;
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotatedElement resolve() {
                        return resolve(annotationReader.resolve());
                    }

                    /**
                     * Resolves the type annotations from a given annotated element into the annotated element that this instance represents.
                     *
                     * @param annotatedElement The original annotated element.
                     * @return The resolved annotated element.
                     */
                    protected abstract AnnotatedElement resolve(AnnotatedElement annotatedElement);
                }
            }

            /**
             * A chained annotation reader for reading a wildcard type's upper bound type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForWildcardUpperBoundType extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedWildcardType#getAnnotatedUpperBounds} method.
                 */
                private static final Method GET_ANNOTATED_UPPER_BOUNDS = of("java.lang.reflect.AnnotatedWildcardType", "getAnnotatedUpperBounds");

                /**
                 * The wildcard bound's index.
                 */
                private final int index;

                /**
                 * Creates a chained annotation reader for reading a upper-bound wildcard's bound type.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 * @param index            The wildcard bound's index.
                 */
                protected ForWildcardUpperBoundType(AnnotationReader annotationReader, int index) {
                    super(annotationReader);
                    this.index = index;
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        Object annotatedUpperBounds = GET_ANNOTATED_UPPER_BOUNDS.invoke(annotatedElement, NO_ARGUMENTS);
                        return Array.getLength(annotatedUpperBounds) == 0 // Wildcards with a lower bound do not define annotations for their implicit upper bound.
                                ? NoOp.INSTANCE
                                : (AnnotatedElement) Array.get(annotatedUpperBounds, index);
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedWildcardType#getAnnotatedUpperBounds", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedWildcardType#getAnnotatedUpperBounds", exception.getCause());
                    }
                }
            }

            /**
             * A chained annotation reader for reading a wildcard type's lower bound type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForWildcardLowerBoundType extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedWildcardType#getAnnotatedLowerBounds} method.
                 */
                private static final Method GET_ANNOTATED_LOWER_BOUNDS = of("java.lang.reflect.AnnotatedWildcardType", "getAnnotatedLowerBounds");

                /**
                 * The wildcard bound's index.
                 */
                private final int index;

                /**
                 * Creates a chained annotation reader for reading a lower-bound wildcard's bound type.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 * @param index            The wildcard bound's index.
                 */
                protected ForWildcardLowerBoundType(AnnotationReader annotationReader, int index) {
                    super(annotationReader);
                    this.index = index;
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        return (AnnotatedElement) Array.get(GET_ANNOTATED_LOWER_BOUNDS.invoke(annotatedElement, NO_ARGUMENTS), index);
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedWildcardType#getAnnotatedLowerBounds", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedWildcardType#getAnnotatedLowerBounds", exception.getCause());
                    }
                }
            }

            /**
             * A chained annotation reader for reading a type variable's type argument.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForTypeVariableBoundType extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedTypeVariable#getAnnotatedBounds} method.
                 */
                private static final Method GET_ANNOTATED_BOUNDS = of("java.lang.reflect.AnnotatedTypeVariable", "getAnnotatedBounds");

                /**
                 * The type variable's index.
                 */
                private final int index;

                /**
                 * Creates a chained annotation reader for reading a type variable's bound type.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 * @param index            The type variable's index.
                 */
                protected ForTypeVariableBoundType(AnnotationReader annotationReader, int index) {
                    super(annotationReader);
                    this.index = index;
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        return (AnnotatedElement) Array.get(GET_ANNOTATED_BOUNDS.invoke(annotatedElement, NO_ARGUMENTS), index);
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedTypeVariable#getAnnotatedBounds", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedTypeVariable#getAnnotatedBounds", exception.getCause());
                    }
                }

                /**
                 * A chained annotation reader for reading a formal type variable's type argument.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class OfFormalTypeVariable extends Delegator {

                    /**
                     * The {@code java.lang.reflect.TypeVariable#getAnnotatedBounds} method.
                     */
                    private static final Method GET_ANNOTATED_BOUNDS = of(TypeVariable.class.getName(), "getAnnotatedBounds");

                    /**
                     * The represented type variable.
                     */
                    private final TypeVariable<?> typeVariable;

                    /**
                     * The type variable's index.
                     */
                    private final int index;

                    /**
                     * Creates a chained annotation reader for reading a formal type variable's bound type.
                     *
                     * @param typeVariable The represented type variable.
                     * @param index        The type variable's index.
                     */
                    protected OfFormalTypeVariable(TypeVariable<?> typeVariable, int index) {
                        this.typeVariable = typeVariable;
                        this.index = index;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotatedElement resolve() {
                        try {
                            return (AnnotatedElement) Array.get(GET_ANNOTATED_BOUNDS.invoke(typeVariable, NO_ARGUMENTS), index);
                        } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                            return NoOp.INSTANCE;
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access java.lang.reflect.TypeVariable#getAnnotatedBounds", exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error invoking java.lang.reflect.TypeVariable#getAnnotatedBounds", exception.getCause());
                        }
                    }
                }
            }

            /**
             * A chained annotation reader for reading a parameterized type's type argument.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForTypeArgument extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedParameterizedType#getAnnotatedActualTypeArguments} method.
                 */
                private static final Method GET_ANNOTATED_ACTUAL_TYPE_ARGUMENTS = of("java.lang.reflect.AnnotatedParameterizedType", "getAnnotatedActualTypeArguments");

                /**
                 * The type argument's index.
                 */
                private final int index;

                /**
                 * Creates a chained annotation reader for reading a component type.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 * @param index            The type argument's index.
                 */
                protected ForTypeArgument(AnnotationReader annotationReader, int index) {
                    super(annotationReader);
                    this.index = index;
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        return (AnnotatedElement) Array.get(GET_ANNOTATED_ACTUAL_TYPE_ARGUMENTS.invoke(annotatedElement, NO_ARGUMENTS), index);
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedParameterizedType#getAnnotatedActualTypeArguments", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedParameterizedType#getAnnotatedActualTypeArguments", exception.getCause());
                    }
                }
            }

            /**
             * A chained annotation reader for reading a component type.
             */
            class ForComponentType extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedArrayType#getAnnotatedGenericComponentType} method.
                 */
                private static final Method GET_ANNOTATED_GENERIC_COMPONENT_TYPE = of("java.lang.reflect.AnnotatedArrayType", "getAnnotatedGenericComponentType");

                /**
                 * Creates a chained annotation reader for reading a component type.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 */
                protected ForComponentType(AnnotationReader annotationReader) {
                    super(annotationReader);
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        return (AnnotatedElement) GET_ANNOTATED_GENERIC_COMPONENT_TYPE.invoke(annotatedElement, NO_ARGUMENTS);
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedArrayType#getAnnotatedGenericComponentType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedArrayType#getAnnotatedGenericComponentType", exception.getCause());
                    }
                }
            }

            /**
             * A chained annotation reader for reading an owner type.
             */
            class ForOwnerType extends Delegator.Chained {

                /**
                 * The {@code java.lang.reflect.AnnotatedType#getAnnotatedOwnerType} method.
                 */
                private static final Method GET_ANNOTATED_OWNER_TYPE = of("java.lang.reflect.AnnotatedType", "getAnnotatedOwnerType");

                /**
                 * Creates a chained annotation reader for reading an owner type if it is accessible. This method checks if annotated
                 * owner types are available on the executing VM (Java 9+). If this is not the case, a non-operational annotation
                 * reader is returned.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 * @return An annotation reader for the resolved type's owner type.
                 */
                private static AnnotationReader of(AnnotationReader annotationReader) {
                    return GET_ANNOTATED_OWNER_TYPE == null
                            ? NoOp.INSTANCE
                            : new ForOwnerType(annotationReader);
                }

                /**
                 * Creates a chained annotation reader for reading an owner type if it is accessible.
                 *
                 * @param annotationReader The annotation reader from which to delegate.
                 */
                protected ForOwnerType(AnnotationReader annotationReader) {
                    super(annotationReader);
                }

                @Override
                protected AnnotatedElement resolve(AnnotatedElement annotatedElement) {
                    try {
                        AnnotatedElement annotatedOwnerType = (AnnotatedElement) GET_ANNOTATED_OWNER_TYPE.invoke(annotatedElement, NO_ARGUMENTS);
                        return annotatedOwnerType == null
                                ? NoOp.INSTANCE
                                : annotatedOwnerType;
                    } catch (ClassCastException ignored) { // To avoid bug on early releases of Java 8.
                        return NoOp.INSTANCE;
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access java.lang.reflect.AnnotatedType#getAnnotatedOwnerType", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error invoking java.lang.reflect.AnnotatedType#getAnnotatedOwnerType", exception.getCause());
                    }
                }
            }
        }

        /**
         * An abstract base implementation of a generic type description.
         */
        abstract class AbstractBase extends ModifierReviewable.AbstractBase implements Generic {

            /**
             * {@inheritDoc}
             */
            public int getModifiers() {
                return asErasure().getModifiers();
            }

            /**
             * {@inheritDoc}
             */
            public Generic asGenericType() {
                return this;
            }

            /**
             * {@inheritDoc}
             */
            public Generic asRawType() {
                return asErasure().asGenericType();
            }

            /**
             * {@inheritDoc}
             */
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

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return Sort.NON_GENERIC;
            }

            /**
             * {@inheritDoc}
             */
            public Generic getSuperClass() {
                TypeDescription erasure = asErasure();
                Generic superClass = erasure.getSuperClass();
                if (TypeDescription.AbstractBase.RAW_TYPES) {
                    return superClass;
                }
                return superClass == null
                        ? Generic.UNDEFINED
                        : new Generic.LazyProjection.WithResolvedErasure(superClass, new Visitor.ForRawType(erasure), Empty.INSTANCE);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getInterfaces() {
                TypeDescription erasure = asErasure();
                if (TypeDescription.AbstractBase.RAW_TYPES) {
                    return erasure.getInterfaces();
                }
                return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(erasure.getInterfaces(), new Visitor.ForRawType(erasure));
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                TypeDescription erasure = asErasure();
                return new FieldList.TypeSubstituting(this, erasure.getDeclaredFields(), TypeDescription.AbstractBase.RAW_TYPES
                        ? Visitor.NoOp.INSTANCE
                        : new Visitor.ForRawType(erasure));
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                TypeDescription erasure = asErasure();
                return new MethodList.TypeSubstituting(this, erasure.getDeclaredMethods(), TypeDescription.AbstractBase.RAW_TYPES
                        ? Visitor.NoOp.INSTANCE
                        : new Visitor.ForRawType(erasure));
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeArguments() {
                throw new IllegalStateException("A non-generic type does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                throw new IllegalStateException("A non-generic type does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onNonGenericType(this);
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return asErasure().getTypeName();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A non-generic type does not imply upper type bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A non-generic type does not imply lower type bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource getTypeVariableSource() {
                throw new IllegalStateException("A non-generic type does not imply a type variable source: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getSymbol() {
                throw new IllegalStateException("A non-generic type does not imply a symbol: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return asErasure().getStackSize();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return asErasure().getActualName();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return asErasure().isArray();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return asErasure().isPrimitive();
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(java.lang.reflect.Type type) {
                return asErasure().represents(type);
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                return new SuperClassIterator(this);
            }

            @Override
            public int hashCode() {
                return asErasure().hashCode();
            }

            @Override
            @SuppressFBWarnings(value = "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", justification = "Type check is performed by erasure implementation")
            public boolean equals(Object other) {
                return this == other || asErasure().equals(other);
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
                 * A cache of generic type descriptions for commonly used types to avoid unnecessary allocations.
                 */
                @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT", justification = "This collection is not exposed.")
                private static final Map<Class<?>, Generic> TYPE_CACHE;

                /*
                 * Initializes the type cache.
                 */
                static {
                    TYPE_CACHE = new HashMap<Class<?>, Generic>();
                    TYPE_CACHE.put(TargetType.class, new ForLoadedType(TargetType.class));
                    TYPE_CACHE.put(Object.class, new ForLoadedType(Object.class));
                    TYPE_CACHE.put(String.class, new ForLoadedType(String.class));
                    TYPE_CACHE.put(Boolean.class, new ForLoadedType(Boolean.class));
                    TYPE_CACHE.put(Byte.class, new ForLoadedType(Byte.class));
                    TYPE_CACHE.put(Short.class, new ForLoadedType(Short.class));
                    TYPE_CACHE.put(Character.class, new ForLoadedType(Character.class));
                    TYPE_CACHE.put(Integer.class, new ForLoadedType(Integer.class));
                    TYPE_CACHE.put(Long.class, new ForLoadedType(Long.class));
                    TYPE_CACHE.put(Float.class, new ForLoadedType(Float.class));
                    TYPE_CACHE.put(Double.class, new ForLoadedType(Double.class));
                    TYPE_CACHE.put(void.class, new ForLoadedType(void.class));
                    TYPE_CACHE.put(boolean.class, new ForLoadedType(boolean.class));
                    TYPE_CACHE.put(byte.class, new ForLoadedType(byte.class));
                    TYPE_CACHE.put(short.class, new ForLoadedType(short.class));
                    TYPE_CACHE.put(char.class, new ForLoadedType(char.class));
                    TYPE_CACHE.put(int.class, new ForLoadedType(int.class));
                    TYPE_CACHE.put(long.class, new ForLoadedType(long.class));
                    TYPE_CACHE.put(float.class, new ForLoadedType(float.class));
                    TYPE_CACHE.put(double.class, new ForLoadedType(double.class));
                }

                /**
                 * The type that this instance represents.
                 */
                private final Class<?> type;

                /**
                 * The annotation reader to query for the non-generic type's annotations.
                 */
                private final AnnotationReader annotationReader;

                /**
                 * Creates a new description of a generic type of a loaded type. This constructor should not normally be used.
                 * Use {@link ForLoadedType#of(Class)} instead.
                 *
                 * @param type The represented type.
                 */
                public ForLoadedType(Class<?> type) {
                    this(type, AnnotationReader.NoOp.INSTANCE);
                }

                /**
                 * /**
                 * Creates a new description of a generic type of a loaded type.
                 *
                 * @param type             The represented type.
                 * @param annotationReader The annotation reader to query for the non-generic type's annotations.
                 */
                protected ForLoadedType(Class<?> type, AnnotationReader annotationReader) {
                    this.type = type;
                    this.annotationReader = annotationReader;
                }

                /**
                 * Returns a new immutable generic type description for a loaded type.
                 *
                 * @param type The type to be represented by this generic type description.
                 * @return The generic type description representing the given type.
                 */
                public static Generic of(Class<?> type) {
                    Generic typeDescription = TYPE_CACHE.get(type);
                    return typeDescription == null
                            ? new ForLoadedType(type)
                            : typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return TypeDescription.ForLoadedType.of(type);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    Class<?> declaringClass = this.type.getDeclaringClass();
                    return declaringClass == null
                            ? Generic.UNDEFINED
                            : new ForLoadedType(declaringClass, annotationReader.ofOuterClass());
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    Class<?> componentType = type.getComponentType();
                    return componentType == null
                            ? Generic.UNDEFINED
                            : new ForLoadedType(componentType, annotationReader.ofComponentType());
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationReader.asList();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    return this.type == type || super.represents(type);
                }
            }

            /**
             * A type description for a type erasure. Compared to a {@link Latent} representation, this
             * representation does not allow for the specification of any complex properties but does
             * not require any form of navigation on the type.
             */
            public static class ForErasure extends OfNonGenericType {

                /**
                 * The represented type erasure.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new description of a non-generic type as an erasure.
                 *
                 * @param typeDescription The represented type erasure.
                 */
                public ForErasure(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    TypeDescription declaringType = typeDescription.getDeclaringType();
                    return declaringType == null
                            ? Generic.UNDEFINED
                            : declaringType.asGenericType();
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    TypeDescription componentType = typeDescription.getComponentType();
                    return componentType == null
                            ? Generic.UNDEFINED
                            : componentType.asGenericType();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }
            }

            /**
             * A latent description of a non-generic type.
             */
            public static class Latent extends OfNonGenericType {

                /**
                 * The non-generic type's raw type.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The non-generic type's declaring type.
                 */
                private final TypeDescription.Generic declaringType;

                /**
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a non-generic type with an implicit owner type.
                 *
                 * @param typeDescription  The non-generic type's raw type.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                public Latent(TypeDescription typeDescription, AnnotationSource annotationSource) {
                    this(typeDescription, typeDescription.getDeclaringType(), annotationSource);
                }

                /**
                 * Creates a non-generic type with a raw owner type.
                 *
                 * @param typeDescription  The non-generic type's raw type.
                 * @param declaringType    The non-generic type's declaring type.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                private Latent(TypeDescription typeDescription, TypeDescription declaringType, AnnotationSource annotationSource) {
                    this(typeDescription,
                            declaringType == null
                                    ? Generic.UNDEFINED
                                    : declaringType.asGenericType(),
                            annotationSource);
                }

                /**
                 * Creates a non-generic type.
                 *
                 * @param typeDescription  The non-generic type's raw type.
                 * @param declaringType    The non-generic type's declaring type.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                protected Latent(TypeDescription typeDescription, Generic declaringType, AnnotationSource annotationSource) {
                    this.typeDescription = typeDescription;
                    this.declaringType = declaringType;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    return declaringType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    TypeDescription componentType = typeDescription.getComponentType();
                    return componentType == null
                            ? Generic.UNDEFINED
                            : componentType.asGenericType();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return typeDescription;
                }
            }

            /**
             * A representation of a raw type that preserves its generic super types' generic information with a minimum
             * but erases all of their members' types.
             */
            public static class ForReifiedErasure extends OfNonGenericType {

                /**
                 * The represented type erasure.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new reified non-generic type.
                 *
                 * @param typeDescription The represented type erasure.
                 */
                protected ForReifiedErasure(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * Creates a new generic type representation for an erasure where any generified type is reified.
                 *
                 * @param typeDescription The erasure to represent.
                 * @return An appropriate generic type representation where any generified type is reified.
                 */
                protected static Generic of(TypeDescription typeDescription) {
                    return typeDescription.isGenerified()
                            ? new ForReifiedErasure(typeDescription)
                            : new ForErasure(typeDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    Generic superClass = typeDescription.getSuperClass();
                    return superClass == null
                            ? Generic.UNDEFINED
                            : new LazyProjection.WithResolvedErasure(superClass, Visitor.Reifying.INHERITING);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(typeDescription.getInterfaces(), Visitor.Reifying.INHERITING);
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                    return new FieldList.TypeSubstituting(this, typeDescription.getDeclaredFields(), Visitor.TypeErasing.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                    return new MethodList.TypeSubstituting(this, typeDescription.getDeclaredMethods(), Visitor.TypeErasing.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    TypeDescription declaringType = typeDescription.getDeclaringType();
                    return declaringType == null
                            ? Generic.UNDEFINED
                            : of(declaringType);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    TypeDescription componentType = typeDescription.getComponentType();
                    return componentType == null
                            ? Generic.UNDEFINED
                            : of(componentType);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a potentially generic array. Instances represent a non-generic type
         * if the given component type is non-generic.
         */
        abstract class OfGenericArray extends AbstractBase {

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return getComponentType().getSort().isNonGeneric()
                        ? Sort.NON_GENERIC
                        : Sort.GENERIC_ARRAY;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription asErasure() {
                return ArrayProjection.of(getComponentType().asErasure(), 1);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getSuperClass() {
                return TypeDescription.Generic.OBJECT;
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getInterfaces() {
                return ARRAY_INTERFACES;
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return new FieldList.Empty<FieldDescription.InGenericShape>();
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return new MethodList.Empty<MethodDescription.InGenericShape>();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A generic array type does not imply upper type bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A generic array type does not imply lower type bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource getTypeVariableSource() {
                throw new IllegalStateException("A generic array type does not imply a type variable source: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeArguments() {
                throw new IllegalStateException("A generic array type does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                throw new IllegalStateException("A generic array type does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getOwnerType() {
                return Generic.UNDEFINED;
            }

            /**
             * {@inheritDoc}
             */
            public String getSymbol() {
                throw new IllegalStateException("A generic array type does not imply a symbol: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return getSort().isNonGeneric()
                        ? asErasure().getTypeName()
                        : toString();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return getSort().isNonGeneric()
                        ? asErasure().getActualName()
                        : toString();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return true;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                return new SuperClassIterator(this);
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return getSort().isNonGeneric()
                        ? visitor.onNonGenericType(this)
                        : visitor.onGenericArray(this);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }

            @Override
            public int hashCode() {
                return getSort().isNonGeneric()
                        ? asErasure().hashCode()
                        : getComponentType().hashCode();
            }

            @Override
            @SuppressFBWarnings(value = "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", justification = "Type check is performed by erasure implementation")
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (getSort().isNonGeneric()) {
                    return asErasure().equals(other);
                }
                if (!(other instanceof Generic)) {
                    return false;
                }
                Generic typeDescription = (Generic) other;
                return typeDescription.getSort().isGenericArray() && getComponentType().equals(typeDescription.getComponentType());
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
                 * The annotation reader to query for the generic array type's annotations.
                 */
                private final AnnotationReader annotationReader;

                /**
                 * Creates a type description of the given generic array type.
                 *
                 * @param genericArrayType The loaded generic array type.
                 */
                public ForLoadedType(GenericArrayType genericArrayType) {
                    this(genericArrayType, AnnotationReader.NoOp.INSTANCE);
                }

                /**
                 * Creates a type description of the given generic array type.
                 *
                 * @param genericArrayType The loaded generic array type.
                 * @param annotationReader The annotation reader to query for the generic array type's annotations.
                 */
                protected ForLoadedType(GenericArrayType genericArrayType, AnnotationReader annotationReader) {
                    this.genericArrayType = genericArrayType;
                    this.annotationReader = annotationReader;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    return Sort.describe(genericArrayType.getGenericComponentType(), annotationReader.ofComponentType());
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationReader.asList();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    return genericArrayType == type || super.represents(type);
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
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a latent representation of a generic array type.
                 *
                 * @param componentType    The component type.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                public Latent(Generic componentType, AnnotationSource annotationSource) {
                    this.componentType = componentType;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    return componentType;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
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

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return Sort.WILDCARD;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription asErasure() {
                throw new IllegalStateException("A wildcard does not represent an erasable type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getSuperClass() {
                throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getInterfaces() {
                throw new IllegalStateException("A wildcard does not imply an interface type definition: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                throw new IllegalStateException("A wildcard does not imply field definitions: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                throw new IllegalStateException("A wildcard does not imply method definitions: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getComponentType() {
                throw new IllegalStateException("A wildcard does not imply a component type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource getTypeVariableSource() {
                throw new IllegalStateException("A wildcard does not imply a type variable source: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeArguments() {
                throw new IllegalStateException("A wildcard does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                throw new IllegalStateException("A wildcard does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getOwnerType() {
                throw new IllegalStateException("A wildcard does not imply an owner type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getSymbol() {
                throw new IllegalStateException("A wildcard does not imply a symbol: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return toString();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return toString();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                throw new IllegalStateException("A wildcard does not imply a super type definition: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onWildcard(this);
            }

            /**
             * {@inheritDoc}
             */
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
                if (this == other) {
                    return true;
                } else if (!(other instanceof Generic)) {
                    return false;
                }
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
                 * The annotation reader to query for the wildcard type's annotations.
                 */
                private final AnnotationReader annotationReader;

                /**
                 * Creates a description of a loaded wildcard.
                 *
                 * @param wildcardType The represented loaded wildcard type.
                 */
                public ForLoadedType(WildcardType wildcardType) {
                    this(wildcardType, AnnotationReader.NoOp.INSTANCE);
                }

                /**
                 * Creates a description of a loaded wildcard.
                 *
                 * @param wildcardType     The represented loaded wildcard type.
                 * @param annotationReader The annotation reader to query for the wildcard type's annotations.
                 */
                protected ForLoadedType(WildcardType wildcardType, AnnotationReader annotationReader) {
                    this.wildcardType = wildcardType;
                    this.annotationReader = annotationReader;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getUpperBounds() {
                    return new WildcardUpperBoundTypeList(wildcardType.getUpperBounds(), annotationReader);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getLowerBounds() {
                    return new WildcardLowerBoundTypeList(wildcardType.getLowerBounds(), annotationReader);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationReader.asList();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    return wildcardType == type || super.represents(type);
                }

                /**
                 * A type list representing an upper-bound type variable's bound types.
                 */
                protected static class WildcardUpperBoundTypeList extends TypeList.Generic.AbstractBase {

                    /**
                     * The represented upper bounds.
                     */
                    private final java.lang.reflect.Type[] upperBound;

                    /**
                     * The annotation reader to query for type annotations.
                     */
                    private final AnnotationReader annotationReader;

                    /**
                     * Creates a type list for a wildcard type's upper bounds.
                     *
                     * @param upperBound       The represented upper bounds.
                     * @param annotationReader The annotation reader to query for type annotations.
                     */
                    protected WildcardUpperBoundTypeList(java.lang.reflect.Type[] upperBound, AnnotationReader annotationReader) {
                        this.upperBound = upperBound;
                        this.annotationReader = annotationReader;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic get(int index) {
                        return Sort.describe(upperBound[index], annotationReader.ofWildcardUpperBoundType(index));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return upperBound.length;
                    }
                }

                /**
                 * A type list representing an upper-bound type variable's bound types.
                 */
                protected static class WildcardLowerBoundTypeList extends TypeList.Generic.AbstractBase {

                    /**
                     * The represented lower bounds.
                     */
                    private final java.lang.reflect.Type[] lowerBound;

                    /**
                     * The annotation reader to query for type annotations.
                     */
                    private final AnnotationReader annotationReader;

                    /**
                     * Creates a type list for a wildcard type's lower bounds.
                     *
                     * @param lowerBound       The represented lower bounds.
                     * @param annotationReader The annotation reader to query for type annotations.
                     */
                    protected WildcardLowerBoundTypeList(java.lang.reflect.Type[] lowerBound, AnnotationReader annotationReader) {
                        this.lowerBound = lowerBound;
                        this.annotationReader = annotationReader;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic get(int index) {
                        return Sort.describe(lowerBound[index], annotationReader.ofWildcardLowerBoundType(index));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return lowerBound.length;
                    }
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
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a description of a latent wildcard.
                 *
                 * @param upperBounds      The wildcard's upper bounds.
                 * @param lowerBounds      The wildcard's lower bounds.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                protected Latent(List<? extends Generic> upperBounds, List<? extends Generic> lowerBounds, AnnotationSource annotationSource) {
                    this.upperBounds = upperBounds;
                    this.lowerBounds = lowerBounds;
                    this.annotationSource = annotationSource;
                }

                /**
                 * Creates an unbounded wildcard. Such a wildcard is implicitly bound above by the {@link Object} type.
                 *
                 * @param annotationSource The annotation source to query for the declared annotations.
                 * @return A description of an unbounded wildcard.
                 */
                public static Generic unbounded(AnnotationSource annotationSource) {
                    return new Latent(Collections.singletonList(TypeDescription.Generic.OBJECT), Collections.<Generic>emptyList(), annotationSource);
                }

                /**
                 * Creates a wildcard with an upper bound.
                 *
                 * @param upperBound       The upper bound of the wildcard.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 * @return A wildcard with the given upper bound.
                 */
                public static Generic boundedAbove(Generic upperBound, AnnotationSource annotationSource) {
                    return new Latent(Collections.singletonList(upperBound), Collections.<Generic>emptyList(), annotationSource);
                }

                /**
                 * Creates a wildcard with a lower bound. Such a wildcard is implicitly bounded above by the {@link Object} type.
                 *
                 * @param lowerBound       The lower bound of the wildcard.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 * @return A wildcard with the given lower bound.
                 */
                public static Generic boundedBelow(Generic lowerBound, AnnotationSource annotationSource) {
                    return new Latent(Collections.singletonList(TypeDescription.Generic.OBJECT), Collections.singletonList(lowerBound), annotationSource);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getUpperBounds() {
                    return new TypeList.Generic.Explicit(upperBounds);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getLowerBounds() {
                    return new TypeList.Generic.Explicit(lowerBounds);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a parameterized type.
         */
        abstract class OfParameterizedType extends AbstractBase {

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return Sort.PARAMETERIZED;
            }

            /**
             * {@inheritDoc}
             */
            public Generic getSuperClass() {
                Generic superClass = asErasure().getSuperClass();
                return superClass == null
                        ? Generic.UNDEFINED
                        : new LazyProjection.WithResolvedErasure(superClass, new Visitor.Substitutor.ForTypeVariableBinding(this));
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getInterfaces() {
                return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(asErasure().getInterfaces(), new Visitor.Substitutor.ForTypeVariableBinding(this));
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return new FieldList.TypeSubstituting(this, asErasure().getDeclaredFields(), new Visitor.Substitutor.ForTypeVariableBinding(this));
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return new MethodList.TypeSubstituting(this, asErasure().getDeclaredMethods(), new Visitor.Substitutor.ForTypeVariableBinding(this));
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                Generic typeDescription = this;
                do {
                    TypeList.Generic typeArguments = typeDescription.getTypeArguments(), typeVariables = typeDescription.asErasure().getTypeVariables();
                    for (int index = 0; index < Math.min(typeArguments.size(), typeVariables.size()); index++) {
                        if (typeVariable.equals(typeVariables.get(index))) {
                            return typeArguments.get(index);
                        }
                    }
                    typeDescription = typeDescription.getOwnerType();
                } while (typeDescription != null && typeDescription.getSort().isParameterized());
                return Generic.UNDEFINED;
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getUpperBounds() {
                throw new IllegalStateException("A parameterized type does not imply upper bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A parameterized type does not imply lower bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getComponentType() {
                throw new IllegalStateException("A parameterized type does not imply a component type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource getTypeVariableSource() {
                throw new IllegalStateException("A parameterized type does not imply a type variable source: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getSymbol() {
                throw new IllegalStateException("A parameterized type does not imply a symbol: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return toString();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return toString();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                return new SuperClassIterator(this);
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onParameterizedType(this);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }

            @Override
            public int hashCode() {
                int result = 1;
                for (Generic typeArgument : getTypeArguments()) {
                    result = 31 * result + typeArgument.hashCode();
                }
                Generic ownerType = getOwnerType();
                return result ^ (ownerType == null
                        ? asErasure().hashCode()
                        : ownerType.hashCode());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof Generic)) {
                    return false;
                }
                Generic typeDescription = (Generic) other;
                if (!typeDescription.getSort().isParameterized()) {
                    return false;
                }
                Generic ownerType = getOwnerType(), otherOwnerType = typeDescription.getOwnerType();
                return asErasure().equals(typeDescription.asErasure())
                        && !(ownerType == null && otherOwnerType != null) && !(ownerType != null && !ownerType.equals(otherOwnerType))
                        && getTypeArguments().equals(typeDescription.getTypeArguments());
            }

            @Override
            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                RenderingDelegate.CURRENT.apply(stringBuilder, asErasure(), getOwnerType());
                TypeList.Generic typeArguments = getTypeArguments();
                if (!typeArguments.isEmpty()) {
                    stringBuilder.append('<');
                    boolean multiple = false;
                    for (Generic typeArgument : typeArguments) {
                        if (multiple) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(typeArgument.getTypeName());
                        multiple = true;
                    }
                    stringBuilder.append('>');
                }
                return stringBuilder.toString();
            }

            /**
             * A rendering delegate for resolving a parameterized type's {@link Object#toString()} representation.
             */
            protected enum RenderingDelegate {

                /**
                 * A rendering delegate for any VM prior to Java 9 where types are concatenated using a {@code .} character
                 * and where the fully qualified names are appended to non-parameterized types.
                 */
                FOR_LEGACY_VM {
                    @Override
                    protected void apply(StringBuilder stringBuilder, TypeDescription erasure, Generic ownerType) {
                        if (ownerType != null) {
                            stringBuilder.append(ownerType.getTypeName()).append('.').append(ownerType.getSort().isParameterized()
                                    ? erasure.getSimpleName()
                                    : erasure.getName());
                        } else {
                            stringBuilder.append(erasure.getName());
                        }
                    }
                },

                /**
                 * A rendering delegate for any VM supporting Java 8 or newer where a type's simple name is appended.
                 */
                FOR_JAVA_8_CAPABLE_VM {
                    @Override
                    protected void apply(StringBuilder stringBuilder, TypeDescription erasure, Generic ownerType) {
                        if (ownerType != null) {
                            stringBuilder.append(ownerType.getTypeName()).append('$');
                            if (ownerType.getSort().isParameterized()) {
                                stringBuilder.append(erasure.getName().replace(ownerType.asErasure().getName() + "$", ""));
                            } else {
                                stringBuilder.append(erasure.getSimpleName());
                            }
                        } else {
                            stringBuilder.append(erasure.getName());
                        }
                    }
                };

                /**
                 * A rendering delegate for the current VM.
                 */
                protected static final RenderingDelegate CURRENT = ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V6).isAtLeast(ClassFileVersion.JAVA_V8)
                        ? RenderingDelegate.FOR_JAVA_8_CAPABLE_VM
                        : RenderingDelegate.FOR_LEGACY_VM;

                /**
                 * Applies this rendering delegate.
                 *
                 * @param stringBuilder The string builder which is used for creating a parameterized type's string representation.
                 * @param erasure       The rendered type's erasure.
                 * @param ownerType     The rendered type's owner type.
                 */
                protected abstract void apply(StringBuilder stringBuilder, TypeDescription erasure, Generic ownerType);
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
                 * The annotation reader to query for the parameterized type's annotations.
                 */
                private final AnnotationReader annotationReader;

                /**
                 * Creates a description of the loaded parameterized type.
                 *
                 * @param parameterizedType The represented parameterized type.
                 */
                public ForLoadedType(ParameterizedType parameterizedType) {
                    this(parameterizedType, AnnotationReader.NoOp.INSTANCE);
                }

                /**
                 * Creates a description of the loaded parameterized type.
                 *
                 * @param parameterizedType The represented parameterized type.
                 * @param annotationReader  The annotation reader to query for the parameterized type's annotations.
                 */
                protected ForLoadedType(ParameterizedType parameterizedType, AnnotationReader annotationReader) {
                    this.parameterizedType = parameterizedType;
                    this.annotationReader = annotationReader;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeArguments() {
                    return new ParameterArgumentTypeList(parameterizedType.getActualTypeArguments(), annotationReader);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    java.lang.reflect.Type ownerType = parameterizedType.getOwnerType();
                    return ownerType == null
                            ? Generic.UNDEFINED
                            : Sort.describe(ownerType, annotationReader.ofOwnerType());
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return TypeDescription.ForLoadedType.of((Class<?>) parameterizedType.getRawType());
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationReader.asList();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    return parameterizedType == type || super.represents(type);
                }

                /**
                 * A type list that represents a loaded parameterized type's parameter types.
                 */
                protected static class ParameterArgumentTypeList extends TypeList.Generic.AbstractBase {

                    /**
                     * The represented argument types.
                     */
                    private final java.lang.reflect.Type[] argumentType;

                    /**
                     * The annotation reader to query for type annotations.
                     */
                    private final AnnotationReader annotationReader;

                    /**
                     * Creates a list representing a parameterized type's type arguments.
                     *
                     * @param argumentType     The represented argument types.
                     * @param annotationReader The annotation reader to query for type annotations.
                     */
                    protected ParameterArgumentTypeList(java.lang.reflect.Type[] argumentType, AnnotationReader annotationReader) {
                        this.argumentType = argumentType;
                        this.annotationReader = annotationReader;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic get(int index) {
                        // Obfuscators sometimes render parameterized type arguments as null values.
                        return Sort.describe(argumentType[index], annotationReader.ofTypeArgument(index));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return argumentType.length;
                    }
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
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a description of a latent parameterized type.
                 *
                 * @param rawType          The raw type of the described parameterized type.
                 * @param ownerType        This parameterized type's owner type or {@code null} if no owner type exists.
                 * @param parameters       The parameters of this parameterized type.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                public Latent(TypeDescription rawType,
                              Generic ownerType,
                              List<? extends Generic> parameters,
                              AnnotationSource annotationSource) {
                    this.rawType = rawType;
                    this.ownerType = ownerType;
                    this.parameters = parameters;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return rawType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    return ownerType;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeArguments() {
                    return new TypeList.Generic.Explicit(parameters);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }
            }

            /**
             * A representation of a parameterized type that is a super type of a raw type but preserves the minimal type information
             * that is required for allowing creating correct erasures for overridden methods. All members' types are erased and all
             * type arguments are reduced to their erasure.
             */
            public static class ForReifiedType extends OfParameterizedType {

                /**
                 * The represented parameterized type.
                 */
                private final Generic parameterizedType;

                /**
                 * Creates a new reified parameterized type.
                 *
                 * @param parameterizedType The represented parameterized type.
                 */
                protected ForReifiedType(Generic parameterizedType) {
                    this.parameterizedType = parameterizedType;
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    Generic superClass = super.getSuperClass();
                    return superClass == null
                            ? Generic.UNDEFINED
                            : new LazyProjection.WithResolvedErasure(superClass, Visitor.Reifying.INHERITING);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    return new TypeList.Generic.ForDetachedTypes.WithResolvedErasure(super.getInterfaces(), Visitor.Reifying.INHERITING);
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                    return new FieldList.TypeSubstituting(this, super.getDeclaredFields(), Visitor.TypeErasing.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                    return new MethodList.TypeSubstituting(this, super.getDeclaredMethods(), Visitor.TypeErasing.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeArguments() {
                    return new TypeList.Generic.ForDetachedTypes(parameterizedType.getTypeArguments(), Visitor.TypeErasing.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    Generic ownerType = parameterizedType.getOwnerType();
                    return ownerType == null
                            ? Generic.UNDEFINED
                            : ownerType.accept(Visitor.Reifying.INHERITING);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return parameterizedType.asErasure();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }
            }

            /**
             * Represents an erasure as a generic type where all type variables are representing their own arguments.
             */
            public static class ForGenerifiedErasure extends OfParameterizedType {

                /**
                 * The represented erasure.
                 */
                private final TypeDescription typeDescription;

                /**
                 * Creates a new generified erasure.
                 *
                 * @param typeDescription The represented erasure.
                 */
                protected ForGenerifiedErasure(TypeDescription typeDescription) {
                    this.typeDescription = typeDescription;
                }

                /**
                 * Represents the supplied type description as a generified erasure if it is generified or as a non-generic type if not so.
                 *
                 * @param typeDescription The represented erasure.
                 * @return An appropriate generic type.
                 */
                public static Generic of(TypeDescription typeDescription) {
                    return typeDescription.isGenerified()
                            ? new ForGenerifiedErasure(typeDescription)
                            : new OfNonGenericType.ForErasure(typeDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return typeDescription;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeArguments() {
                    return new TypeList.Generic.ForDetachedTypes(typeDescription.getTypeVariables(), Visitor.AnnotationStripper.INSTANCE);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    TypeDescription declaringType = typeDescription.getDeclaringType();
                    return declaringType == null
                            ? Generic.UNDEFINED
                            : of(declaringType);
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Empty();
                }
            }
        }

        /**
         * A base implementation of a generic type description that represents a type variable.
         */
        abstract class OfTypeVariable extends AbstractBase {

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return Sort.VARIABLE;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription asErasure() {
                TypeList.Generic upperBounds = getUpperBounds();
                return upperBounds.isEmpty()
                        ? TypeDescription.OBJECT
                        : upperBounds.get(0).asErasure();
            }

            /**
             * {@inheritDoc}
             */
            public Generic getSuperClass() {
                throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getInterfaces() {
                throw new IllegalStateException("A type variable does not imply an interface type definition: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                throw new IllegalStateException("A type variable does not imply field definitions: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                throw new IllegalStateException("A type variable does not imply method definitions: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getComponentType() {
                throw new IllegalStateException("A type variable does not imply a component type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeArguments() {
                throw new IllegalStateException("A type variable does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                throw new IllegalStateException("A type variable does not imply type arguments: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getLowerBounds() {
                throw new IllegalStateException("A type variable does not imply lower bounds: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public Generic getOwnerType() {
                throw new IllegalStateException("A type variable does not imply an owner type: " + this);
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return toString();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return getSymbol();
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return visitor.onTypeVariable(this);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(java.lang.reflect.Type type) {
                return equals(Sort.describe(type));
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                throw new IllegalStateException("A type variable does not imply a super type definition: " + this);
            }

            @Override
            public int hashCode() {
                return getTypeVariableSource().hashCode() ^ getSymbol().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof Generic)) {
                    return false;
                }
                Generic typeDescription = (Generic) other;
                return typeDescription.getSort().isTypeVariable()
                        && getSymbol().equals(typeDescription.getSymbol())
                        && getTypeVariableSource().equals(typeDescription.getTypeVariableSource());
            }

            @Override
            public String toString() {
                return getSymbol();
            }

            /**
             * Implementation of a symbolic type variable.
             */
            public static class Symbolic extends Generic.AbstractBase {

                /**
                 * The symbol of the symbolic type variable.
                 */
                private final String symbol;

                /**
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a symbolic type variable.
                 *
                 * @param symbol           The symbol of the symbolic type variable.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                public Symbolic(String symbol, AnnotationSource annotationSource) {
                    this.symbol = symbol;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public Sort getSort() {
                    return Sort.VARIABLE_SYMBOLIC;
                }

                /**
                 * {@inheritDoc}
                 */
                public String getSymbol() {
                    return symbol;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    throw new IllegalStateException("A symbolic type variable does not imply an erasure: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getUpperBounds() {
                    throw new IllegalStateException("A symbolic type variable does not imply an upper type bound: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableSource getTypeVariableSource() {
                    throw new IllegalStateException("A symbolic type variable does not imply a variable source: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    throw new IllegalStateException("A symbolic type variable does not imply a super type definition: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    throw new IllegalStateException("A symbolic type variable does not imply an interface type definition: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                    throw new IllegalStateException("A symbolic type variable does not imply field definitions: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                    throw new IllegalStateException("A symbolic type variable does not imply method definitions: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getComponentType() {
                    throw new IllegalStateException("A symbolic type variable does not imply a component type: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeArguments() {
                    throw new IllegalStateException("A symbolic type variable does not imply type arguments: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic findBindingOf(Generic typeVariable) {
                    throw new IllegalStateException("A symbolic type variable does not imply type arguments: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getLowerBounds() {
                    throw new IllegalStateException("A symbolic type variable does not imply lower bounds: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Generic getOwnerType() {
                    throw new IllegalStateException("A symbolic type variable does not imply an owner type: " + this);
                }

                /**
                 * {@inheritDoc}
                 */
                public String getTypeName() {
                    return toString();
                }

                /**
                 * {@inheritDoc}
                 */
                public String getActualName() {
                    return getSymbol();
                }

                /**
                 * {@inheritDoc}
                 */
                public <T> T accept(Visitor<T> visitor) {
                    return visitor.onTypeVariable(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public StackSize getStackSize() {
                    return StackSize.SINGLE;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isArray() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isPrimitive() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    if (type == null) {
                        throw new NullPointerException();
                    }
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<TypeDefinition> iterator() {
                    throw new IllegalStateException("A symbolic type variable does not imply a super type definition: " + this);
                }

                @Override
                public int hashCode() {
                    return symbol.hashCode();
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) {
                        return true;
                    } else if (!(other instanceof Generic)) {
                        return false;
                    }
                    Generic typeDescription = (Generic) other;
                    return typeDescription.getSort().isTypeVariable() && getSymbol().equals(typeDescription.getSymbol());
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
                 * The annotation reader to query for the variable's annotations.
                 */
                private final AnnotationReader annotationReader;

                /**
                 * Creates a description of a loaded type variable.
                 *
                 * @param typeVariable The represented type variable.
                 */
                public ForLoadedType(TypeVariable<?> typeVariable) {
                    this(typeVariable, AnnotationReader.NoOp.INSTANCE);
                }

                /**
                 * Creates a description of a loaded type variable with an annotation.
                 *
                 * @param typeVariable     The represented type variable.
                 * @param annotationReader The annotation reader to query for the variable's annotations.
                 */
                protected ForLoadedType(TypeVariable<?> typeVariable, AnnotationReader annotationReader) {
                    this.typeVariable = typeVariable;
                    this.annotationReader = annotationReader;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableSource getTypeVariableSource() {
                    GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                    if (genericDeclaration instanceof Class) {
                        return TypeDescription.ForLoadedType.of((Class<?>) genericDeclaration);
                    } else if (genericDeclaration instanceof Method) {
                        return new MethodDescription.ForLoadedMethod((Method) genericDeclaration);
                    } else if (genericDeclaration instanceof Constructor) {
                        return new MethodDescription.ForLoadedConstructor((Constructor<?>) genericDeclaration);
                    } else {
                        throw new IllegalStateException("Unknown declaration: " + genericDeclaration);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getUpperBounds() {
                    return new TypeVariableBoundList(typeVariable.getBounds(), annotationReader);
                }

                /**
                 * {@inheritDoc}
                 */
                public String getSymbol() {
                    return typeVariable.getName();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationReader.asList();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean represents(java.lang.reflect.Type type) {
                    return typeVariable == type || super.represents(type);
                }

                /**
                 * A list of type variable bounds for a loaded {@link TypeVariable} that resolves annotations..
                 */
                protected static class TypeVariableBoundList extends TypeList.Generic.AbstractBase {

                    /**
                     * The type variable bounds.
                     */
                    private final java.lang.reflect.Type[] bound;

                    /**
                     * The annotation reader to query for the type bounds.
                     */
                    private final AnnotationReader annotationReader;

                    /**
                     * Creates a new list for a {@link TypeVariable}'s bound.
                     *
                     * @param bound            The type variable bounds.
                     * @param annotationReader The annotation reader to query for the type bounds.
                     */
                    protected TypeVariableBoundList(java.lang.reflect.Type[] bound, AnnotationReader annotationReader) {
                        this.bound = bound;
                        this.annotationReader = annotationReader;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic get(int index) {
                        return Sort.describe(bound[index], annotationReader.ofTypeVariableBoundType(index));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return bound.length;
                    }
                }
            }

            /**
             * A type variable with explicit annotations that replace the annotations that are declared by the provided type variable.
             */
            public static class WithAnnotationOverlay extends OfTypeVariable {

                /**
                 * The type variable to represent.
                 */
                private final Generic typeVariable;

                /**
                 * The annotation source to query for the declared annotations.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a new type definition for a type variable with explicit annotations.
                 *
                 * @param typeVariable     The type variable to represent.
                 * @param annotationSource The annotation source to query for the declared annotations.
                 */
                public WithAnnotationOverlay(Generic typeVariable, AnnotationSource annotationSource) {
                    this.typeVariable = typeVariable;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getUpperBounds() {
                    return typeVariable.getUpperBounds();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeVariableSource getTypeVariableSource() {
                    return typeVariable.getTypeVariableSource();
                }

                /**
                 * {@inheritDoc}
                 */
                public String getSymbol() {
                    return typeVariable.getSymbol();
                }
            }
        }

        /**
         * A lazy projection of a generic type. Such projections allow to only read generic type information in case it is required. This
         * is meaningful as the Java virtual needs to process generic type information which requires extra resources. Also, this allows
         * the extraction of non-generic type information even if the generic type information is invalid.
         */
        abstract class LazyProjection extends AbstractBase {

            /**
             * Resolves the actual generic type.
             *
             * @return An actual description of the represented generic type.
             */
            protected abstract Generic resolve();

            /**
             * {@inheritDoc}
             */
            public Sort getSort() {
                return resolve().getSort();
            }

            /**
             * {@inheritDoc}
             */
            public FieldList<FieldDescription.InGenericShape> getDeclaredFields() {
                return resolve().getDeclaredFields();
            }

            /**
             * {@inheritDoc}
             */
            public MethodList<MethodDescription.InGenericShape> getDeclaredMethods() {
                return resolve().getDeclaredMethods();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getUpperBounds() {
                return resolve().getUpperBounds();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getLowerBounds() {
                return resolve().getLowerBounds();
            }

            /**
             * {@inheritDoc}
             */
            public Generic getComponentType() {
                return resolve().getComponentType();
            }

            /**
             * {@inheritDoc}
             */
            public TypeList.Generic getTypeArguments() {
                return resolve().getTypeArguments();
            }

            /**
             * {@inheritDoc}
             */
            public Generic findBindingOf(Generic typeVariable) {
                return resolve().findBindingOf(typeVariable);
            }

            /**
             * {@inheritDoc}
             */
            public TypeVariableSource getTypeVariableSource() {
                return resolve().getTypeVariableSource();
            }

            /**
             * {@inheritDoc}
             */
            public Generic getOwnerType() {
                return resolve().getOwnerType();
            }

            /**
             * {@inheritDoc}
             */
            public String getTypeName() {
                return resolve().getTypeName();
            }

            /**
             * {@inheritDoc}
             */
            public String getSymbol() {
                return resolve().getSymbol();
            }

            /**
             * {@inheritDoc}
             */
            public String getActualName() {
                return resolve().getActualName();
            }

            /**
             * {@inheritDoc}
             */
            public <T> T accept(Visitor<T> visitor) {
                return resolve().accept(visitor);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return asErasure().getStackSize();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return asErasure().isArray();
            }

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return asErasure().isPrimitive();
            }

            /**
             * {@inheritDoc}
             */
            public boolean represents(java.lang.reflect.Type type) {
                return resolve().represents(type);
            }

            @Override
            public int hashCode() {
                return resolve().hashCode();
            }

            @Override
            public boolean equals(Object other) {
                return this == other || other instanceof TypeDefinition && resolve().equals(other);
            }

            @Override
            public String toString() {
                return resolve().toString();
            }

            /**
             * A lazy projection of a type with a lazy resolution of super class and interface types. A lazy navigation
             * must only be used for describing types that are guaranteed to define a super class and interface types,
             * i.e. non-generic types and parameterized types. Lazy navigation can also be applied to array types where
             * the usage does however make little sense as those properties are never generic.
             */
            public abstract static class WithLazyNavigation extends LazyProjection {

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    return LazySuperClass.of(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    return LazyInterfaceList.of(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<TypeDefinition> iterator() {
                    return new TypeDefinition.SuperClassIterator(this);
                }

                /**
                 * A lazy super class description for a lazy projection.
                 */
                protected static class LazySuperClass extends WithLazyNavigation {

                    /**
                     * The lazy projection for which this description is a delegate.
                     */
                    private final LazyProjection delegate;

                    /**
                     * Creates a new lazy super class description.
                     *
                     * @param delegate The lazy projection for which this description is a delegate.
                     */
                    protected LazySuperClass(LazyProjection delegate) {
                        this.delegate = delegate;
                    }

                    /**
                     * Resolves a lazy super class description.
                     *
                     * @param delegate The lazy projection for which this description is a delegate.
                     * @return A lazy description of the super class or {@code null} if the delegate does not define a super class.
                     */
                    protected static Generic of(LazyProjection delegate) {
                        return delegate.asErasure().getSuperClass() == null
                                ? Generic.UNDEFINED
                                : new LazySuperClass(delegate);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return resolve().getDeclaredAnnotations();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription asErasure() {
                        return delegate.asErasure().getSuperClass().asErasure();
                    }

                    @Override
                    @CachedReturnPlugin.Enhance("resolved")
                    protected Generic resolve() {
                        return delegate.resolve().getSuperClass();
                    }
                }

                /**
                 * A lazy interface type description for a lazy projection.
                 */
                protected static class LazyInterfaceType extends WithLazyNavigation {

                    /**
                     * The lazy projection for which this description is a delegate.
                     */
                    private final LazyProjection delegate;

                    /**
                     * The index of the interface in question.
                     */
                    private final int index;

                    /**
                     * The raw interface that is declared by the erasure of the represented lazy projection.
                     */
                    private final TypeDescription.Generic rawInterface;

                    /**
                     * Creates a new lazy interface type.
                     *
                     * @param delegate     The lazy projection for which this description is a delegate.
                     * @param index        The index of the interface in question.
                     * @param rawInterface The raw interface that is declared by the erasure of the represented lazy projection.
                     */
                    protected LazyInterfaceType(LazyProjection delegate, int index, Generic rawInterface) {
                        this.delegate = delegate;
                        this.index = index;
                        this.rawInterface = rawInterface;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return resolve().getDeclaredAnnotations();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public TypeDescription asErasure() {
                        return rawInterface.asErasure();
                    }

                    @Override
                    @CachedReturnPlugin.Enhance("resolved")
                    protected Generic resolve() {
                        return delegate.resolve().getInterfaces().get(index);
                    }
                }

                /**
                 * A lazy representation of a lazy projection's interfaces.
                 */
                protected static class LazyInterfaceList extends TypeList.Generic.AbstractBase {

                    /**
                     * The lazy projection for which this description is a delegate.
                     */
                    private final LazyProjection delegate;

                    /**
                     * A list of raw interface types declared by the lazy projection's erasure.
                     */
                    private final TypeList.Generic rawInterfaces;

                    /**
                     * Creates a new lazy interface list.
                     *
                     * @param delegate      The lazy projection for which this description is a delegate.
                     * @param rawInterfaces A list of raw interface types declared by the lazy projection's erasure.
                     */
                    protected LazyInterfaceList(LazyProjection delegate, TypeList.Generic rawInterfaces) {
                        this.delegate = delegate;
                        this.rawInterfaces = rawInterfaces;
                    }

                    /**
                     * Resolves a lazy interface list.
                     *
                     * @param delegate The delegate for which to represent interfaces.
                     * @return A lazy list representing the delegate's interfaces lazily.
                     */
                    protected static TypeList.Generic of(LazyProjection delegate) {
                        return new LazyInterfaceList(delegate, delegate.asErasure().getInterfaces());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Generic get(int index) {
                        return new LazyInterfaceType(delegate, index, rawInterfaces.get(index));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int size() {
                        return rawInterfaces.size();
                    }
                }

                /**
                 * A description of an annotated lazy type with lazy navigation.
                 */
                protected abstract static class OfAnnotatedElement extends WithLazyNavigation {

                    /**
                     * Returns the current type's annotation reader.
                     *
                     * @return The current type's annotation reader.
                     */
                    protected abstract AnnotationReader getAnnotationReader();

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return getAnnotationReader().asList();
                    }
                }
            }

            /**
             * A lazy projection of a type that resolves super class and interface types eagerly.
             */
            public abstract static class WithEagerNavigation extends LazyProjection {

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    return resolve().getSuperClass();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    return resolve().getInterfaces();
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<TypeDefinition> iterator() {
                    return resolve().iterator();
                }

                /**
                 * A description of an annotated lazy type with eager navigation.
                 */
                protected abstract static class OfAnnotatedElement extends WithEagerNavigation {

                    /**
                     * Returns the current type's annotation reader.
                     *
                     * @return The current type's annotation reader.
                     */
                    protected abstract AnnotationReader getAnnotationReader();

                    /**
                     * {@inheritDoc}
                     */
                    public AnnotationList getDeclaredAnnotations() {
                        return getAnnotationReader().asList();
                    }
                }
            }

            /**
             * A lazy projection of a generic super type.
             */
            public static class ForLoadedSuperClass extends LazyProjection.WithLazyNavigation.OfAnnotatedElement {

                /**
                 * The type of which the super class is represented.
                 */
                private final Class<?> type;

                /**
                 * Creates a new lazy projection of a type's super class.
                 *
                 * @param type The type of which the super class is represented.
                 */
                public ForLoadedSuperClass(Class<?> type) {
                    this.type = type;
                }

                /**
                 * {@inheritDoc}
                 */
                @CachedReturnPlugin.Enhance("resolved")
                protected Generic resolve() {
                    java.lang.reflect.Type superClass = type.getGenericSuperclass();
                    return superClass == null
                            ? Generic.UNDEFINED
                            : Sort.describe(superClass, getAnnotationReader());
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    Class<?> superClass = type.getSuperclass();
                    return superClass == null
                            ? TypeDescription.UNDEFINED
                            : ForLoadedType.of(superClass);
                }

                @Override
                protected AnnotationReader getAnnotationReader() {
                    return AnnotationReader.DISPATCHER.resolveSuperClassType(type);
                }
            }

            /**
             * A lazy projection of a field's type.
             */
            public static class ForLoadedFieldType extends LazyProjection.WithEagerNavigation.OfAnnotatedElement {

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
                @CachedReturnPlugin.Enhance("resolved")
                protected Generic resolve() {
                    return Sort.describe(field.getGenericType(), getAnnotationReader());
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return ForLoadedType.of(field.getType());
                }

                @Override
                protected AnnotationReader getAnnotationReader() {
                    return AnnotationReader.DISPATCHER.resolveFieldType(field);
                }
            }

            /**
             * A lazy projection of a method's generic return type.
             */
            public static class ForLoadedReturnType extends LazyProjection.WithEagerNavigation.OfAnnotatedElement {

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
                @CachedReturnPlugin.Enhance("resolved")
                protected Generic resolve() {
                    return Sort.describe(method.getGenericReturnType(), getAnnotationReader());
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return ForLoadedType.of(method.getReturnType());
                }

                @Override
                protected AnnotationReader getAnnotationReader() {
                    return AnnotationReader.DISPATCHER.resolveReturnType(method);
                }
            }

            /**
             * A lazy projection of the parameter type of a {@link Constructor}.
             */
            public static class OfConstructorParameter extends LazyProjection.WithEagerNavigation.OfAnnotatedElement {

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
                private final Class<?>[] erasure;

                /**
                 * Creates a lazy projection of a constructor's parameter.
                 *
                 * @param constructor The constructor of which a parameter type is represented.
                 * @param index       The parameter's index.
                 * @param erasure     The erasure of the parameter type.
                 */
                @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is never exposed outside of the class")
                public OfConstructorParameter(Constructor<?> constructor, int index, Class<?>[] erasure) {
                    this.constructor = constructor;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                @CachedReturnPlugin.Enhance("delegate")
                protected Generic resolve() {
                    java.lang.reflect.Type[] type = constructor.getGenericParameterTypes();
                    return erasure.length == type.length
                            ? Sort.describe(type[index], getAnnotationReader())
                            : OfNonGenericType.ForLoadedType.of(erasure[index]);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return TypeDescription.ForLoadedType.of(erasure[index]);
                }

                @Override
                protected AnnotationReader getAnnotationReader() {
                    return AnnotationReader.DISPATCHER.resolveParameterType(constructor, index);
                }
            }

            /**
             * A lazy projection of the parameter type of a {@link Method}.
             */
            public static class OfMethodParameter extends LazyProjection.WithEagerNavigation.OfAnnotatedElement {

                /**
                 * The method of which a parameter type is represented.
                 */
                private final Method method;

                /**
                 * The parameter's index.
                 */
                private final int index;

                /**
                 * The erasures of the method's parameter types.
                 */
                private final Class<?>[] erasure;

                /**
                 * Creates a lazy projection of a constructor's parameter.
                 *
                 * @param method  The method of which a parameter type is represented.
                 * @param index   The parameter's index.
                 * @param erasure The erasures of the method's parameter types.
                 */
                @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is never exposed outside of the class")
                public OfMethodParameter(Method method, int index, Class<?>[] erasure) {
                    this.method = method;
                    this.index = index;
                    this.erasure = erasure;
                }

                @Override
                @CachedReturnPlugin.Enhance("resolved")
                protected Generic resolve() {
                    java.lang.reflect.Type[] type = method.getGenericParameterTypes();
                    return erasure.length == type.length
                            ? Sort.describe(type[index], getAnnotationReader())
                            : OfNonGenericType.ForLoadedType.of(erasure[index]);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return TypeDescription.ForLoadedType.of(erasure[index]);
                }

                @Override
                protected AnnotationReader getAnnotationReader() {
                    return AnnotationReader.DISPATCHER.resolveParameterType(method, index);
                }
            }

            /**
             * A lazy projection that applies a visitor only when resolving the generic type but not when reading the erasure.
             */
            public static class WithResolvedErasure extends LazyProjection.WithEagerNavigation {

                /**
                 * The unresolved generic type.
                 */
                private final Generic delegate;

                /**
                 * The visitor to apply for resolving the generic type.
                 */
                private final Visitor<? extends Generic> visitor;

                /**
                 * The annotation source to apply.
                 */
                private final AnnotationSource annotationSource;

                /**
                 * Creates a lazy projection with a resolved erasure that retains the delegates type annotations.
                 *
                 * @param delegate The unresolved generic type.
                 * @param visitor  The visitor to apply for resolving the generic type.
                 */
                public WithResolvedErasure(Generic delegate, Visitor<? extends Generic> visitor) {
                    this(delegate, visitor, delegate);
                }

                /**
                 * Creates a lazy projection with a resolved erasure.
                 *
                 * @param delegate         The unresolved generic type.
                 * @param visitor          The visitor to apply for resolving the generic type.
                 * @param annotationSource The annotation source representing this type's type annotations.
                 */
                public WithResolvedErasure(Generic delegate, Visitor<? extends Generic> visitor, AnnotationSource annotationSource) {
                    this.delegate = delegate;
                    this.visitor = visitor;
                    this.annotationSource = annotationSource;
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return annotationSource.getDeclaredAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription asErasure() {
                    return delegate.asErasure();
                }

                @Override
                @CachedReturnPlugin.Enhance("resolved")
                protected Generic resolve() {
                    return delegate.accept(visitor);
                }
            }
        }

        /**
         * A builder for creating describing a generic type as a {@link Generic}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        abstract class Builder {

            /**
             * Represents an undefined {@link java.lang.reflect.Type} within a build step.
             */
            private static final java.lang.reflect.Type UNDEFINED = null;

            /**
             * The type annotations of the current annotated type.
             */
            protected final List<? extends AnnotationDescription> annotations;

            /**
             * Creates a new builder for a generic type description.
             *
             * @param annotations The type annotations of the current annotated type.
             */
            protected Builder(List<? extends AnnotationDescription> annotations) {
                this.annotations = annotations;
            }

            /**
             * Creates a raw type of a type description.
             *
             * @param type The type to represent as a raw type.
             * @return A builder for creating a raw type.
             */
            public static Builder rawType(Class<?> type) {
                return rawType(ForLoadedType.of(type));
            }

            /**
             * Creates a raw type of a type description.
             *
             * @param type The type to represent as a raw type.
             * @return A builder for creating a raw type.
             */
            public static Builder rawType(TypeDescription type) {
                return new Builder.OfNonGenericType(type);
            }

            /**
             * Creates a raw type of a type description where the supplied owner type is used as .
             *
             * @param type      The type to represent as a raw type.
             * @param ownerType The raw type's (annotated) declaring type or {@code null} if no owner type should be declared.
             * @return A builder for creating a raw type.
             */
            public static Builder rawType(Class<?> type, Generic ownerType) {
                return rawType(ForLoadedType.of(type), ownerType);
            }

            /**
             * Creates a raw type of a type description.
             *
             * @param type      The type to represent as a raw type.
             * @param ownerType The raw type's (annotated) declaring type or {@code null} if no owner type should be declared.
             * @return A builder for creating a raw type.
             */
            public static Builder rawType(TypeDescription type, Generic ownerType) {
                TypeDescription declaringType = type.getDeclaringType();
                if (declaringType == null && ownerType != null) {
                    throw new IllegalArgumentException(type + " does not have a declaring type: " + ownerType);
                } else if (declaringType != null && (ownerType == null || !declaringType.equals(ownerType.asErasure()))) {
                    throw new IllegalArgumentException(ownerType + " is not the declaring type of " + type);
                }
                return new Builder.OfNonGenericType(type, ownerType);
            }

            /**
             * Creates an unbound wildcard without type annotations.
             *
             * @return A description of an unbound wildcard without type annotations.
             */
            public static Generic unboundWildcard() {
                return unboundWildcard(Collections.<AnnotationDescription>emptySet());
            }

            /**
             * Creates an unbound wildcard.
             *
             * @param annotation The type annotations of the unbound wildcard.
             * @return A description of an unbound wildcard.
             */
            public static Generic unboundWildcard(Annotation... annotation) {
                return unboundWildcard(Arrays.asList(annotation));
            }

            /**
             * Creates an unbound wildcard.
             *
             * @param annotations The type annotations of the unbound wildcard.
             * @return A description of an unbound wildcard.
             */
            public static Generic unboundWildcard(List<? extends Annotation> annotations) {
                return unboundWildcard(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * Creates an unbound wildcard.
             *
             * @param annotation The type annotations of the unbound wildcard.
             * @return A description of an unbound wildcard.
             */
            public static Generic unboundWildcard(AnnotationDescription... annotation) {
                return unboundWildcard(Arrays.asList(annotation));
            }

            /**
             * Creates an unbound wildcard.
             *
             * @param annotations The type annotations of the unbound wildcard.
             * @return A description of an unbound wildcard.
             */
            public static Generic unboundWildcard(Collection<? extends AnnotationDescription> annotations) {
                return OfWildcardType.Latent.unbounded(new Explicit(new ArrayList<AnnotationDescription>(annotations)));
            }

            /**
             * Creates a symbolic type variable of the given name.
             *
             * @param symbol The symbolic name of the type variable.
             * @return A builder for creating a type variable.
             */
            public static Builder typeVariable(String symbol) {
                return new OfTypeVariable(symbol);
            }

            /**
             * Creates a parameterized type without an owner type or with a non-generic owner type.
             *
             * @param rawType   A raw version of the type to describe as a parameterized type.
             * @param parameter The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(Class<?> rawType, java.lang.reflect.Type... parameter) {
                return parameterizedType(rawType, Arrays.asList(parameter));
            }

            /**
             * Creates a parameterized type without an owner type or with a non-generic owner type.
             *
             * @param rawType    A raw version of the type to describe as a parameterized type.
             * @param parameters The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(Class<?> rawType, List<? extends java.lang.reflect.Type> parameters) {
                return parameterizedType(rawType, UNDEFINED, parameters);
            }

            /**
             * Creates a parameterized type.
             *
             * @param rawType    A raw version of the type to describe as a parameterized type.
             * @param ownerType  The owner type of the parameterized type.
             * @param parameters The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(Class<?> rawType, java.lang.reflect.Type ownerType, List<? extends java.lang.reflect.Type> parameters) {
                return parameterizedType(ForLoadedType.of(rawType),
                        ownerType == null
                                ? null
                                : Sort.describe(ownerType),
                        new TypeList.Generic.ForLoadedTypes(parameters));
            }

            /**
             * Creates a parameterized type without an owner type or with a non-generic owner type.
             *
             * @param rawType   A raw version of the type to describe as a parameterized type.
             * @param parameter The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(TypeDescription rawType, TypeDefinition... parameter) {
                return parameterizedType(rawType, Arrays.asList(parameter));
            }

            /**
             * Creates a parameterized type without an owner type or with a non-generic owner type.
             *
             * @param rawType    A raw version of the type to describe as a parameterized type.
             * @param parameters The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(TypeDescription rawType, Collection<? extends TypeDefinition> parameters) {
                return parameterizedType(rawType, Generic.UNDEFINED, parameters);
            }

            /**
             * Creates a parameterized type.
             *
             * @param rawType    A raw version of the type to describe as a parameterized type.
             * @param ownerType  The owner type of the parameterized type.
             * @param parameters The type arguments to attach to the raw type as parameters.
             * @return A builder for creating a parameterized type.
             */
            public static Builder parameterizedType(TypeDescription rawType, Generic ownerType, Collection<? extends TypeDefinition> parameters) {
                TypeDescription declaringType = rawType.getDeclaringType();
                if (ownerType == null && declaringType != null && rawType.isStatic()) {
                    ownerType = declaringType.asGenericType();
                }
                if (!rawType.represents(TargetType.class)) {
                    if (!rawType.isGenerified()) {
                        throw new IllegalArgumentException(rawType + " is not a parameterized type");
                    } else if (ownerType == null && declaringType != null && !rawType.isStatic()) {
                        throw new IllegalArgumentException(rawType + " requires an owner type");
                    } else if (ownerType != null && !ownerType.asErasure().equals(declaringType)) {
                        throw new IllegalArgumentException(ownerType + " does not represent required owner for " + rawType);
                    } else if (ownerType != null && (rawType.isStatic() ^ ownerType.getSort().isNonGeneric())) {
                        throw new IllegalArgumentException(ownerType + " does not define the correct parameters for owning " + rawType);
                    } else if (rawType.getTypeVariables().size() != parameters.size()) {
                        throw new IllegalArgumentException(parameters + " does not contain number of required parameters for " + rawType);
                    }
                }
                return new Builder.OfParameterizedType(rawType, ownerType, new TypeList.Generic.Explicit(new ArrayList<TypeDefinition>(parameters)));
            }

            /**
             * Transforms this type into the upper bound of a wildcard type.
             *
             * @return A generic type description of a wildcard type with this builder's type as an upper bound.
             */
            public Generic asWildcardUpperBound() {
                return asWildcardUpperBound(Collections.<AnnotationDescription>emptySet());
            }

            /**
             * Transforms this type into the upper bound of a wildcard type.
             *
             * @param annotation Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an upper bound.
             */
            public Generic asWildcardUpperBound(Annotation... annotation) {
                return asWildcardUpperBound(Arrays.asList(annotation));
            }

            /**
             * Transforms this type into the upper bound of a wildcard type.
             *
             * @param annotations Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an upper bound.
             */
            public Generic asWildcardUpperBound(List<? extends Annotation> annotations) {
                return asWildcardUpperBound(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * Transforms this type into the upper bound of a wildcard type.
             *
             * @param annotation Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an upper bound.
             */
            public Generic asWildcardUpperBound(AnnotationDescription... annotation) {
                return asWildcardUpperBound(Arrays.asList(annotation));
            }

            /**
             * Transforms this type into the upper bound of a wildcard type.
             *
             * @param annotations Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an upper bound.
             */
            public Generic asWildcardUpperBound(Collection<? extends AnnotationDescription> annotations) {
                return OfWildcardType.Latent.boundedAbove(build(), new Explicit(new ArrayList<AnnotationDescription>(annotations)));
            }

            /**
             * Transforms this type into the lower bound of a wildcard type.
             *
             * @return A generic type description of a wildcard type with this builder's type as an lower bound.
             */
            public Generic asWildcardLowerBound() {
                return asWildcardLowerBound(Collections.<AnnotationDescription>emptySet());
            }

            /**
             * Transforms this type into the lower bound of a wildcard type.
             *
             * @param annotation Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an lower bound.
             */
            public Generic asWildcardLowerBound(Annotation... annotation) {
                return asWildcardLowerBound(Arrays.asList(annotation));
            }

            /**
             * Transforms this type into the lower bound of a wildcard type.
             *
             * @param annotations Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an lower bound.
             */
            public Generic asWildcardLowerBound(List<? extends Annotation> annotations) {
                return asWildcardLowerBound(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * Transforms this type into the lower bound of a wildcard type.
             *
             * @param annotation Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an lower bound.
             */
            public Generic asWildcardLowerBound(AnnotationDescription... annotation) {
                return asWildcardLowerBound(Arrays.asList(annotation));
            }

            /**
             * Transforms this type into the lower bound of a wildcard type.
             *
             * @param annotations Type annotations to be declared by the wildcard type.
             * @return A generic type description of a wildcard type with this builder's type as an lower bound.
             */
            public Generic asWildcardLowerBound(Collection<? extends AnnotationDescription> annotations) {
                return OfWildcardType.Latent.boundedBelow(build(), new Explicit(new ArrayList<AnnotationDescription>(annotations)));
            }

            /**
             * Represents the built type into an array.
             *
             * @return A builder for creating an array of the currently built type.
             */
            public Builder asArray() {
                return asArray(1);
            }

            /**
             * Represents the built type into an array.
             *
             * @param arity The arity of the array.
             * @return A builder for creating an array of the currently built type.
             */
            public Builder asArray(int arity) {
                if (arity < 1) {
                    throw new IllegalArgumentException("Cannot define an array of a non-positive arity: " + arity);
                }
                TypeDescription.Generic typeDescription = build();
                while (--arity > 0) {
                    typeDescription = new OfGenericArray.Latent(typeDescription, Empty.INSTANCE);
                }
                return new Builder.OfGenericArrayType(typeDescription);
            }

            /**
             * Defines type annotations to be declared by the current type.
             *
             * @param annotation Type annotations to be declared by the current type.
             * @return A new builder where the current type declares the supplied type annotations.
             */
            public Builder annotate(Annotation... annotation) {
                return annotate(Arrays.asList(annotation));
            }

            /**
             * Defines type annotations to be declared by the current type.
             *
             * @param annotations Type annotations to be declared by the current type.
             * @return A new builder where the current type declares the supplied type annotations.
             */
            public Builder annotate(List<? extends Annotation> annotations) {
                return annotate(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * Defines type annotations to be declared by the current type.
             *
             * @param annotation Type annotations to be declared by the current type.
             * @return A new builder where the current type declares the supplied type annotations.
             */
            public Builder annotate(AnnotationDescription... annotation) {
                return annotate(Arrays.asList(annotation));
            }

            /**
             * Defines type annotations to be declared by the current type.
             *
             * @param annotations Type annotations to be declared by the current type.
             * @return A new builder where the current type declares the supplied type annotations.
             */
            public Builder annotate(Collection<? extends AnnotationDescription> annotations) {
                return doAnnotate(new ArrayList<AnnotationDescription>(annotations));
            }

            /**
             * Creates a new builder for the current type and the applied type annotations.
             *
             * @param annotations Type annotations to be declared by the current type.
             * @return A new builder where the current type declares the supplied type annotations.
             */
            protected abstract Builder doAnnotate(List<? extends AnnotationDescription> annotations);

            /**
             * Finalizes the build and finalizes the created type as a generic type description.
             *
             * @return A generic type description of the built type.
             */
            public Generic build() {
                return doBuild();
            }

            /**
             * Finalizes the build and finalizes the created type as a generic type description.
             *
             * @param annotation Type annotations place for the built generic type to declare.
             * @return A generic type description of the built type.
             */
            public Generic build(Annotation... annotation) {
                return build(Arrays.asList(annotation));
            }

            /**
             * Finalizes the build and finalizes the created type as a generic type description.
             *
             * @param annotations Type annotations place for the built generic type to declare.
             * @return A generic type description of the built type.
             */
            public Generic build(List<? extends Annotation> annotations) {
                return build(new AnnotationList.ForLoadedAnnotations(annotations));
            }

            /**
             * Finalizes the build and finalizes the created type as a generic type description.
             *
             * @param annotation Type annotations place for the built generic type to declare.
             * @return A generic type description of the built type.
             */
            public Generic build(AnnotationDescription... annotation) {
                return build(Arrays.asList(annotation));
            }

            /**
             * Finalizes the build and finalizes the created type as a generic type description.
             *
             * @param annotations Type annotations place for the built generic type to declare.
             * @return A generic type description of the built type.
             */
            public Generic build(Collection<? extends AnnotationDescription> annotations) {
                return doAnnotate(new ArrayList<AnnotationDescription>(annotations)).doBuild();
            }

            /**
             * Builds the generic type.
             *
             * @return The generic type.
             */
            protected abstract Generic doBuild();

            /**
             * A generic type builder for building a non-generic type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfNonGenericType extends Builder {

                /**
                 * The type's erasure.
                 */
                private final TypeDescription typeDescription;

                /**
                 * The raw type's (annotated) declaring type or {@code null} if no such type is defined.
                 */
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                private final Generic ownerType;

                /**
                 * Creates a builder for a non-generic type.
                 *
                 * @param typeDescription The type's erasure.
                 */
                protected OfNonGenericType(TypeDescription typeDescription) {
                    this(typeDescription, typeDescription.getDeclaringType());
                }

                /**
                 * Creates a builder for a non-generic type.
                 *
                 * @param typeDescription The type's erasure.
                 * @param ownerType       The raw type's raw declaring type or {@code null} if no such type is defined.
                 */
                private OfNonGenericType(TypeDescription typeDescription, TypeDescription ownerType) {
                    this(typeDescription, ownerType == null
                            ? Generic.UNDEFINED
                            : ownerType.asGenericType());
                }

                /**
                 * Creates a builder for a non-generic type.
                 *
                 * @param typeDescription The type's erasure.
                 * @param ownerType       The raw type's (annotated) declaring type.
                 */
                protected OfNonGenericType(TypeDescription typeDescription, Generic ownerType) {
                    this(typeDescription, ownerType, Collections.<AnnotationDescription>emptyList());
                }

                /**
                 * Creates a builder for a non-generic type.
                 *
                 * @param typeDescription The type's erasure.
                 * @param ownerType       The raw type's (annotated) declaring type.
                 * @param annotations     The type's type annotations.
                 */
                protected OfNonGenericType(TypeDescription typeDescription, Generic ownerType, List<? extends AnnotationDescription> annotations) {
                    super(annotations);
                    this.ownerType = ownerType;
                    this.typeDescription = typeDescription;
                }

                @Override
                protected Builder doAnnotate(List<? extends AnnotationDescription> annotations) {
                    return new OfNonGenericType(typeDescription, ownerType, CompoundList.of(this.annotations, annotations));
                }

                @Override
                protected Generic doBuild() {
                    if (typeDescription.represents(void.class) && !annotations.isEmpty()) {
                        throw new IllegalArgumentException("The void non-type cannot be annotated");
                    }
                    return new Generic.OfNonGenericType.Latent(typeDescription, ownerType, new Explicit(annotations));
                }
            }

            /**
             * A generic type builder for building a parameterized type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfParameterizedType extends Builder {

                /**
                 * The raw base type.
                 */
                private final TypeDescription rawType;

                /**
                 * The generic owner type.
                 */
                private final Generic ownerType;

                /**
                 * The parameter types.
                 */
                private final List<? extends Generic> parameterTypes;

                /**
                 * Creates a builder for a parameterized type.
                 *
                 * @param rawType        The raw base type.
                 * @param ownerType      The generic owner type.
                 * @param parameterTypes The parameter types.
                 */
                protected OfParameterizedType(TypeDescription rawType, Generic ownerType, List<? extends Generic> parameterTypes) {
                    this(rawType, ownerType, parameterTypes, Collections.<AnnotationDescription>emptyList());
                }

                /**
                 * Creates a builder for a parameterized type.
                 *
                 * @param rawType        The raw base type.
                 * @param ownerType      The generic owner type.
                 * @param parameterTypes The parameter types.
                 * @param annotations    The type's type annotations.
                 */
                protected OfParameterizedType(TypeDescription rawType,
                                              Generic ownerType,
                                              List<? extends Generic> parameterTypes,
                                              List<? extends AnnotationDescription> annotations) {
                    super(annotations);
                    this.rawType = rawType;
                    this.ownerType = ownerType;
                    this.parameterTypes = parameterTypes;
                }

                @Override
                protected Builder doAnnotate(List<? extends AnnotationDescription> annotations) {
                    return new OfParameterizedType(rawType, ownerType, parameterTypes, CompoundList.of(this.annotations, annotations));
                }

                @Override
                protected Generic doBuild() {
                    return new Generic.OfParameterizedType.Latent(rawType, ownerType, parameterTypes, new Explicit(annotations));
                }
            }

            /**
             * A generic type builder building a generic array type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfGenericArrayType extends Builder {

                /**
                 * The generic component type.
                 */
                private final Generic componentType;

                /**
                 * Creates a type builder for building a generic array type.
                 *
                 * @param componentType The generic component type.
                 */
                protected OfGenericArrayType(Generic componentType) {
                    this(componentType, Collections.<AnnotationDescription>emptyList());
                }

                /**
                 * Creates a type builder for building a generic array type.
                 *
                 * @param componentType The generic component type.
                 * @param annotations   The type's type annotations.
                 */
                protected OfGenericArrayType(Generic componentType, List<? extends AnnotationDescription> annotations) {
                    super(annotations);
                    this.componentType = componentType;
                }

                @Override
                protected Builder doAnnotate(List<? extends AnnotationDescription> annotations) {
                    return new OfGenericArrayType(componentType, CompoundList.of(this.annotations, annotations));
                }

                @Override
                protected Generic doBuild() {
                    return new Generic.OfGenericArray.Latent(componentType, new Explicit(annotations));
                }
            }

            /**
             * A generic type builder building a symbolic type variable.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfTypeVariable extends Builder {

                /**
                 * The variable's symbol.
                 */
                private final String symbol;

                /**
                 * Creates a new builder for a symbolic type variable.
                 *
                 * @param symbol The variable's symbol.
                 */
                protected OfTypeVariable(String symbol) {
                    this(symbol, Collections.<AnnotationDescription>emptyList());
                }

                /**
                 * Creates a new builder for a symbolic type variable.
                 *
                 * @param symbol      The variable's symbol.
                 * @param annotations The type's type annotations.
                 */
                protected OfTypeVariable(String symbol, List<? extends AnnotationDescription> annotations) {
                    super(annotations);
                    this.symbol = symbol;
                }

                @Override
                protected Builder doAnnotate(List<? extends AnnotationDescription> annotations) {
                    return new OfTypeVariable(symbol, CompoundList.of(this.annotations, annotations));
                }

                @Override
                protected Generic doBuild() {
                    return new Generic.OfTypeVariable.Symbolic(symbol, new Explicit(annotations));
                }
            }
        }
    }

    /**
     * An abstract base implementation of a type description.
     */
    abstract class AbstractBase extends TypeVariableSource.AbstractBase implements TypeDescription {

        /**
         * The {@link TypeDefinition#RAW_TYPES_PROPERTY} property.
         */
        public static final boolean RAW_TYPES;

        /*
         * Reads the raw type property.
         */
        static {
            boolean rawTypes;
            try {
                rawTypes = Boolean.parseBoolean(AccessController.doPrivileged(new GetSystemPropertyAction(RAW_TYPES_PROPERTY)));
            } catch (Exception ignored) {
                rawTypes = false;
            }
            RAW_TYPES = rawTypes;
        }

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
            // (2) For arrays, there are special assignment rules.
            if (targetType.isArray()) {
                return sourceType.isArray()
                        ? isAssignable(sourceType.getComponentType(), targetType.getComponentType())
                        : sourceType.represents(Object.class) || ARRAY_INTERFACES.contains(sourceType.asGenericType());
            }
            // (3) Interfaces do not extend the Object type but are assignable to the Object type.
            if (sourceType.represents(Object.class)) {
                return !targetType.isPrimitive();
            }
            // (4) The sub type has a super type and this super type is assignable to the super type.
            Generic superClass = targetType.getSuperClass();
            if (superClass != null && sourceType.isAssignableFrom(superClass.asErasure())) {
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

        /**
         * {@inheritDoc}
         */
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(ForLoadedType.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return isAssignable(this, typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(ForLoadedType.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return isAssignable(typeDescription, this);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInHierarchyWith(Class<?> type) {
            return isAssignableTo(type) || isAssignableFrom(type);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInHierarchyWith(TypeDescription typeDescription) {
            return isAssignableTo(typeDescription) || isAssignableFrom(typeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription asErasure() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public Generic asGenericType() {
            return new Generic.OfNonGenericType.ForErasure(this);
        }

        /**
         * {@inheritDoc}
         */
        public Sort getSort() {
            return Sort.NON_GENERIC;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInstance(Object value) {
            return isAssignableFrom(value.getClass());
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        /**
         * {@inheritDoc}
         */
        public int getActualModifiers(boolean superFlag) {
            int actualModifiers = getModifiers() | (getDeclaredAnnotations().isAnnotationPresent(Deprecated.class)
                    ? Opcodes.ACC_DEPRECATED
                    : EMPTY_MASK);
            if (isPrivate()) {
                actualModifiers = actualModifiers & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
            } else if (isProtected()) {
                actualModifiers = actualModifiers & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC) | Opcodes.ACC_PUBLIC;
            } else {
                actualModifiers = actualModifiers & ~Opcodes.ACC_STATIC;
            }
            return superFlag ? (actualModifiers | Opcodes.ACC_SUPER) : actualModifiers;
        }

        /**
         * {@inheritDoc}
         */
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
                Generic superClass = getSuperClass();
                // The object type itself is non generic and implicitly returns a non-generic signature
                if (superClass == null) {
                    superClass = TypeDescription.Generic.OBJECT;
                }
                superClass.accept(new Generic.Visitor.ForSignatureVisitor(signatureWriter.visitSuperclass()));
                generic = generic || !superClass.getSort().isNonGeneric();
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

        /**
         * {@inheritDoc}
         */
        public boolean isSamePackage(TypeDescription typeDescription) {
            PackageDescription thisPackage = getPackage(), otherPackage = typeDescription.getPackage();
            return thisPackage == null || otherPackage == null
                    ? thisPackage == otherPackage
                    : thisPackage.equals(otherPackage);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPrimitive() || (isArray()
                    ? getComponentType().isVisibleTo(typeDescription)
                    : isPublic() || isProtected() || isSamePackage(typeDescription)/* || equals(typeDescription.asErasure()) */);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAccessibleTo(TypeDescription typeDescription) {
            return isPrimitive() || (isArray()
                    ? getComponentType().isVisibleTo(typeDescription)
                    : isPublic() || isSamePackage(typeDescription)/* || equals(typeDescription.asErasure()) */);
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getInheritedAnnotations() {
            Generic superClass = getSuperClass();
            AnnotationList declaredAnnotations = getDeclaredAnnotations();
            if (superClass == null) {
                return declaredAnnotations;
            } else {
                Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>();
                for (AnnotationDescription annotationDescription : declaredAnnotations) {
                    annotationTypes.add(annotationDescription.getAnnotationType());
                }
                return new AnnotationList.Explicit(CompoundList.of(declaredAnnotations, superClass.asErasure().getInheritedAnnotations().inherited(annotationTypes)));
            }
        }

        /**
         * {@inheritDoc}
         */
        public String getActualName() {
            if (isArray()) {
                TypeDescription typeDescription = this;
                int dimensions = 0;
                do {
                    dimensions++;
                    typeDescription = typeDescription.getComponentType();
                } while (typeDescription.isArray());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(typeDescription.getActualName());
                for (int i = 0; i < dimensions; i++) {
                    stringBuilder.append("[]");
                }
                return stringBuilder.toString();
            } else {
                return getName();
            }
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public boolean isAnnotationReturnType() {
            return isPrimitive()
                    || represents(String.class)
                    || (isAssignableTo(Enum.class) && !represents(Enum.class))
                    || (isAssignableTo(Annotation.class) && !represents(Annotation.class))
                    || represents(Class.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationReturnType());
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnnotationValue() {
            return isPrimitive()
                    || represents(String.class)
                    || isAssignableTo(TypeDescription.class)
                    || isAssignableTo(AnnotationDescription.class)
                    || isAssignableTo(EnumerationDescription.class)
                    || (isArray() && !getComponentType().isArray() && getComponentType().isAnnotationValue());
        }

        /**
         * {@inheritDoc}
         */
        @SuppressFBWarnings(value = "EC_UNRELATED_CLASS_AND_INTERFACE", justification = "Fits equality contract for type definitions")
        public boolean represents(java.lang.reflect.Type type) {
            return equals(Sort.describe(type));
        }

        /**
         * {@inheritDoc}
         */
        public String getTypeName() {
            return getName();
        }

        /**
         * {@inheritDoc}
         */
        public TypeVariableSource getEnclosingSource() {
            MethodDescription enclosingMethod = getEnclosingMethod();
            return enclosingMethod == null
                    ? (isStatic() ? TypeVariableSource.UNDEFINED : getEnclosingType()) // Top-level classes (non-static) have no enclosing type.
                    : enclosingMethod;
        }

        /**
         * {@inheritDoc}
         */
        public <T> T accept(TypeVariableSource.Visitor<T> visitor) {
            return visitor.onType(this);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isPackageType() {
            return getSimpleName().equals(PackageDescription.PACKAGE_CLASS_NAME);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isGenerified() {
            if (!getTypeVariables().isEmpty()) {
                return true;
            } else if (isStatic()) {
                return false;
            }
            TypeDescription declaringType = getDeclaringType();
            return declaringType != null && declaringType.isGenerified();
        }

        /**
         * {@inheritDoc}
         */
        public int getInnerClassCount() {
            if (isStatic()) {
                return 0;
            }
            TypeDescription declaringType = getDeclaringType();
            return declaringType == null
                    ? 0
                    : declaringType.getInnerClassCount() + 1;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isInnerClass() {
            return !isStatic() && isNestedClass();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isNestedClass() {
            return getDeclaringType() != null;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription asBoxed() {
            if (represents(boolean.class)) {
                return ForLoadedType.of(Boolean.class);
            } else if (represents(byte.class)) {
                return ForLoadedType.of(Byte.class);
            } else if (represents(short.class)) {
                return ForLoadedType.of(Short.class);
            } else if (represents(char.class)) {
                return ForLoadedType.of(Character.class);
            } else if (represents(int.class)) {
                return ForLoadedType.of(Integer.class);
            } else if (represents(long.class)) {
                return ForLoadedType.of(Long.class);
            } else if (represents(float.class)) {
                return ForLoadedType.of(Float.class);
            } else if (represents(double.class)) {
                return ForLoadedType.of(Double.class);
            } else {
                return this;
            }
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription asUnboxed() {
            if (represents(Boolean.class)) {
                return ForLoadedType.of(boolean.class);
            } else if (represents(Byte.class)) {
                return ForLoadedType.of(byte.class);
            } else if (represents(Short.class)) {
                return ForLoadedType.of(short.class);
            } else if (represents(Character.class)) {
                return ForLoadedType.of(char.class);
            } else if (represents(Integer.class)) {
                return ForLoadedType.of(int.class);
            } else if (represents(Long.class)) {
                return ForLoadedType.of(long.class);
            } else if (represents(Float.class)) {
                return ForLoadedType.of(float.class);
            } else if (represents(Double.class)) {
                return ForLoadedType.of(double.class);
            } else {
                return this;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Object getDefaultValue() {
            if (represents(boolean.class)) {
                return false;
            } else if (represents(byte.class)) {
                return (byte) 0;
            } else if (represents(short.class)) {
                return (short) 0;
            } else if (represents(char.class)) {
                return (char) 0;
            } else if (represents(int.class)) {
                return 0;
            } else if (represents(long.class)) {
                return 0L;
            } else if (represents(float.class)) {
                return 0f;
            } else if (represents(double.class)) {
                return 0d;
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isNestHost() {
            return equals(getNestHost());
        }

        /**
         * {@inheritDoc}
         */
        public boolean isNestMateOf(Class<?> type) {
            return isNestMateOf(ForLoadedType.of(type));
        }

        /**
         * {@inheritDoc}
         */
        public boolean isNestMateOf(TypeDescription typeDescription) {
            return getNestHost().equals(typeDescription.getNestHost());
        }

        /**
         * {@inheritDoc}
         */
        public boolean isMemberType() {
            return !isLocalType() && !isAnonymousType() && getDeclaringType() != null;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<TypeDefinition> iterator() {
            return new SuperClassIterator(this);
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof TypeDefinition)) {
                return false;
            }
            TypeDefinition typeDefinition = (TypeDefinition) other;
            return typeDefinition.getSort().isNonGeneric() && getName().equals(typeDefinition.asErasure().getName());
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

            /**
             * {@inheritDoc}
             */
            public boolean isPrimitive() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public boolean isArray() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            public TypeDescription getComponentType() {
                return TypeDescription.UNDEFINED;
            }

            /**
             * {@inheritDoc}
             */
            public String getDescriptor() {
                return "L" + getInternalName() + ";";
            }

            /**
             * {@inheritDoc}
             */
            public String getCanonicalName() {
                if (isAnonymousType() || isLocalType()) {
                    return NO_NAME;
                }
                String internalName = getInternalName();
                TypeDescription enclosingType = getEnclosingType();
                if (enclosingType != null && internalName.startsWith(enclosingType.getInternalName() + "$")) {
                    return enclosingType.getCanonicalName() + "." + internalName.substring(enclosingType.getInternalName().length() + 1);
                } else {
                    return getName();
                }
            }

            /**
             * {@inheritDoc}
             */
            public String getSimpleName() {
                String internalName = getInternalName();
                TypeDescription enclosingType = getEnclosingType();
                int simpleNameIndex;
                if (enclosingType != null && internalName.startsWith(enclosingType.getInternalName() + "$")) {
                    simpleNameIndex = enclosingType.getInternalName().length() + 1;
                } else {
                    simpleNameIndex = internalName.lastIndexOf('/');
                    if (simpleNameIndex == -1) {
                        return internalName;
                    }
                }
                while (simpleNameIndex < internalName.length() && !Character.isLetter(internalName.charAt(simpleNameIndex))) {
                    simpleNameIndex += 1;
                }
                return internalName.substring(simpleNameIndex);
            }

            /**
             * {@inheritDoc}
             */
            public StackSize getStackSize() {
                return StackSize.SINGLE;
            }

            /**
             * An implementation of a type description that delegates all properties but the type's name to a delegate.
             */
            public abstract static class WithDelegation extends OfSimpleType {

                /**
                 * Returns the delegate type description to this type instance.
                 *
                 * @return The delegate type description.
                 */
                protected abstract TypeDescription delegate();

                /**
                 * {@inheritDoc}
                 */
                public Generic getSuperClass() {
                    return delegate().getSuperClass();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getInterfaces() {
                    return delegate().getInterfaces();
                }

                /**
                 * {@inheritDoc}
                 */
                public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
                    return delegate().getDeclaredFields();
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
                    return delegate().getDeclaredMethods();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription getDeclaringType() {
                    return delegate().getDeclaringType();
                }

                /**
                 * {@inheritDoc}
                 */
                public MethodDescription.InDefinedShape getEnclosingMethod() {
                    return delegate().getEnclosingMethod();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription getEnclosingType() {
                    return delegate().getEnclosingType();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList getDeclaredTypes() {
                    return delegate().getDeclaredTypes();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isAnonymousType() {
                    return delegate().isAnonymousType();
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isLocalType() {
                    return delegate().isLocalType();
                }

                /**
                 * {@inheritDoc}
                 */
                public PackageDescription getPackage() {
                    return delegate().getPackage();
                }

                /**
                 * {@inheritDoc}
                 */
                public AnnotationList getDeclaredAnnotations() {
                    return delegate().getDeclaredAnnotations();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList.Generic getTypeVariables() {
                    return delegate().getTypeVariables();
                }

                /**
                 * {@inheritDoc}
                 */
                public int getModifiers() {
                    return delegate().getModifiers();
                }

                @Override
                public String getGenericSignature() {
                    // Embrace use of native generic signature by direct delegation.
                    return delegate().getGenericSignature();
                }

                @Override
                public int getActualModifiers(boolean superFlag) {
                    // Embrace use of native actual modifiers by direct delegation.
                    return delegate().getActualModifiers(superFlag);
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeDescription getNestHost() {
                    return delegate().getNestHost();
                }

                /**
                 * {@inheritDoc}
                 */
                public TypeList getNestMembers() {
                    return delegate().getNestMembers();
                }
            }
        }
    }

    /**
     * A type description implementation that represents a loaded type.
     */
    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "Field is only used as a cache store and is implicitly recomputed")
    class ForLoadedType extends AbstractBase implements Serializable {

        /**
         * The class's serial version UID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * A dispatcher for invking methods on {@link Class} reflectively.
         */
        private static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

        /**
         * A cache of type descriptions for commonly used types to avoid unnecessary allocations.
         */
        @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT", justification = "This collection is not exposed.")
        private static final Map<Class<?>, TypeDescription> TYPE_CACHE;

        /*
         * Initializes the type cache.
         */
        static {
            TYPE_CACHE = new HashMap<Class<?>, TypeDescription>();
            TYPE_CACHE.put(TargetType.class, new ForLoadedType(TargetType.class));
            TYPE_CACHE.put(Object.class, new ForLoadedType(Object.class));
            TYPE_CACHE.put(String.class, new ForLoadedType(String.class));
            TYPE_CACHE.put(Boolean.class, new ForLoadedType(Boolean.class));
            TYPE_CACHE.put(Byte.class, new ForLoadedType(Byte.class));
            TYPE_CACHE.put(Short.class, new ForLoadedType(Short.class));
            TYPE_CACHE.put(Character.class, new ForLoadedType(Character.class));
            TYPE_CACHE.put(Integer.class, new ForLoadedType(Integer.class));
            TYPE_CACHE.put(Long.class, new ForLoadedType(Long.class));
            TYPE_CACHE.put(Float.class, new ForLoadedType(Float.class));
            TYPE_CACHE.put(Double.class, new ForLoadedType(Double.class));
            TYPE_CACHE.put(void.class, new ForLoadedType(void.class));
            TYPE_CACHE.put(boolean.class, new ForLoadedType(boolean.class));
            TYPE_CACHE.put(byte.class, new ForLoadedType(byte.class));
            TYPE_CACHE.put(short.class, new ForLoadedType(short.class));
            TYPE_CACHE.put(char.class, new ForLoadedType(char.class));
            TYPE_CACHE.put(int.class, new ForLoadedType(int.class));
            TYPE_CACHE.put(long.class, new ForLoadedType(long.class));
            TYPE_CACHE.put(float.class, new ForLoadedType(float.class));
            TYPE_CACHE.put(double.class, new ForLoadedType(double.class));
        }

        /**
         * The loaded type this instance represents.
         */
        private final Class<?> type;

        /**
         * Creates a new immutable type description for a loaded type. This constructor should not normally be used.
         * Use {@link ForLoadedType#of(Class)} instead.
         *
         * @param type The type to be represented by this type description.
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        /**
         * Returns the type's actual name where it is taken into consideration that this type might be loaded anonymously.
         * In this case, the remainder of the types name is suffixed by {@code /<id>} which is removed when using this method
         * but is retained when calling {@link Class#getName()}.
         *
         * @param type The type for which to resolve its name.
         * @return The type's actual name.
         */
        public static String getName(Class<?> type) {
            String name = type.getName();
            int anonymousLoaderIndex = name.indexOf('/');
            return anonymousLoaderIndex == -1
                    ? name
                    : name.substring(0, anonymousLoaderIndex);
        }

        /**
         * Returns a new immutable type description for a loaded type.
         *
         * @param type The type to be represented by this type description.
         * @return The type description representing the given type.
         */
        public static TypeDescription of(Class<?> type) {
            TypeDescription typeDescription = TYPE_CACHE.get(type);
            return typeDescription == null
                    ? new ForLoadedType(type)
                    : typeDescription;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return this.type.isAssignableFrom(type) || super.isAssignableFrom(type);
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return typeDescription instanceof ForLoadedType && type.isAssignableFrom(((ForLoadedType) typeDescription).type) || super.isAssignableFrom(typeDescription);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return type.isAssignableFrom(this.type) || super.isAssignableTo(type);
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return typeDescription instanceof ForLoadedType && ((ForLoadedType) typeDescription).type.isAssignableFrom(type) || super.isAssignableTo(typeDescription);
        }

        @Override
        public boolean isInHierarchyWith(Class<?> type) {
            return type.isAssignableFrom(this.type) || this.type.isAssignableFrom(type) || super.isInHierarchyWith(type);
        }

        @Override
        public boolean isInHierarchyWith(TypeDescription typeDescription) {
            return typeDescription instanceof ForLoadedType && (((ForLoadedType) typeDescription).type.isAssignableFrom(type)
                    || type.isAssignableFrom(((ForLoadedType) typeDescription).type)) || super.isInHierarchyWith(typeDescription);
        }

        @Override
        public boolean represents(java.lang.reflect.Type type) {
            return type == this.type || super.represents(type);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getComponentType() {
            Class<?> componentType = type.getComponentType();
            return componentType == null
                    ? TypeDescription.UNDEFINED
                    : ForLoadedType.of(componentType);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isArray() {
            return type.isArray();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        @Override
        public boolean isAnnotation() {
            return type.isAnnotation();
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            if (RAW_TYPES) {
                return type.getSuperclass() == null
                        ? TypeDescription.Generic.UNDEFINED
                        : Generic.OfNonGenericType.ForLoadedType.of(type.getSuperclass());
            }
            return type.getSuperclass() == null
                    ? TypeDescription.Generic.UNDEFINED
                    : new Generic.LazyProjection.ForLoadedSuperClass(type);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            if (RAW_TYPES) {
                return isArray()
                        ? ARRAY_INTERFACES
                        : new TypeList.Generic.ForLoadedTypes(type.getInterfaces());
            }
            return isArray()
                    ? ARRAY_INTERFACES
                    : new TypeList.Generic.OfLoadedInterfaceTypes(type);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            Class<?> declaringType = type.getDeclaringClass();
            return declaringType == null
                    ? TypeDescription.UNDEFINED
                    : ForLoadedType.of(declaringType);
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
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

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            Class<?> enclosingType = type.getEnclosingClass();
            return enclosingType == null
                    ? TypeDescription.UNDEFINED
                    : ForLoadedType.of(enclosingType);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return new TypeList.ForLoadedTypes(type.getDeclaredClasses());
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return type.isAnonymousClass();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return type.isLocalClass();
        }

        @Override
        public boolean isMemberType() {
            return type.isMemberClass();
        }

        /**
         * {@inheritDoc}
         */
        @CachedReturnPlugin.Enhance("declaredFields")
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.ForLoadedFields(type.getDeclaredFields());
        }

        /**
         * {@inheritDoc}
         */
        @CachedReturnPlugin.Enhance("declaredMethods")
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.ForLoadedMethods(type);
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            if (type.isArray() || type.isPrimitive()) {
                return PackageDescription.UNDEFINED;
            } else {
                Package aPackage = type.getPackage();
                if (aPackage == null) {
                    String name = type.getName();
                    int index = name.lastIndexOf('.');
                    return index == -1
                            ? new PackageDescription.Simple(EMPTY_NAME)
                            : new PackageDescription.Simple(name.substring(0, index));
                } else {
                    return new PackageDescription.ForLoadedPackage(aPackage);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public StackSize getStackSize() {
            return StackSize.of(type);
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return getName(type);
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public String getDescriptor() {
            String name = type.getName();
            int anonymousLoaderIndex = name.indexOf('/');
            return anonymousLoaderIndex == -1
                    ? Type.getDescriptor(type)
                    : "L" + name.substring(0, anonymousLoaderIndex).replace('.', '/') + ";";
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return type.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            if (RAW_TYPES) {
                return new TypeList.Generic.Empty();
            }
            return TypeList.Generic.ForLoadedTypes.OfTypeVariables.of(type);
        }

        /**
         * {@inheritDoc}
         */
        @CachedReturnPlugin.Enhance("declaredAnnotations")
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotations(type.getDeclaredAnnotations());
        }

        /**
         * {@inheritDoc}
         */
        public Generic asGenericType() {
            return Generic.OfNonGenericType.ForLoadedType.of(type);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return TypeDescription.ForLoadedType.of(DISPATCHER.getNestHost(type));
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return new TypeList.ForLoadedTypes(DISPATCHER.getNestMembers(type));
        }

        @Override
        public boolean isNestHost() {
            return DISPATCHER.getNestHost(type) == type;
        }

        @Override
        public boolean isNestMateOf(Class<?> type) {
            return DISPATCHER.isNestmateOf(this.type, type) || super.isNestMateOf(ForLoadedType.of(type));
        }

        @Override
        public boolean isNestMateOf(TypeDescription typeDescription) {
            return typeDescription instanceof ForLoadedType && DISPATCHER.isNestmateOf(type, ((ForLoadedType) typeDescription).type) || super.isNestMateOf(typeDescription);
        }

        /**
         * A dispatcher for using methods of {@link Class} that are not declared for Java 6.
         */
        protected interface Dispatcher {

            /**
             * Returns the specified class's nest host.
             *
             * @param type The class for which to locate the nest host.
             * @return The nest host of the specified class.
             */
            Class<?> getNestHost(Class<?> type);

            /**
             * Returns the nest members of the other class.
             *
             * @param type The type to get the nest members for.
             * @return An array containing all nest members of the specified type's nest group.
             */
            Class<?>[] getNestMembers(Class<?> type);

            /**
             * Returns {@code true} if the specified type is a nest mate of the other type.
             *
             * @param type      The type to evaluate for being a nest mate of another type.
             * @param candidate The candidate type.
             * @return {@code true} if the specified type is a nest mate of the other class.
             */
            boolean isNestmateOf(Class<?> type, Class<?> candidate);

            /**
             * An action to resolve the dispatcher for invoking methods of {@link Class} reflectively.
             */
            enum CreationAction implements PrivilegedAction<Dispatcher> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Dispatcher run() {
                    try {
                        return new ForJava11CapableVm(Class.class.getMethod("getNestHost"),
                                Class.class.getMethod("getNestMembers"),
                                Class.class.getMethod("isNestmateOf", Class.class));
                    } catch (NoSuchMethodException ignored) {
                        return ForLegacyVm.INSTANCE;
                    }
                }
            }

            /**
             * A dispatcher for a legacy VM.
             */
            enum ForLegacyVm implements Dispatcher {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getNestHost(Class<?> type) {
                    return type;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?>[] getNestMembers(Class<?> type) {
                    return new Class<?>[]{type};
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isNestmateOf(Class<?> type, Class<?> candidate) {
                    return type == candidate;
                }
            }

            /**
             * A dispatcher for a Java 11-capable VM.
             */
            class ForJava11CapableVm implements Dispatcher {

                /**
                 * The {@code java.lang.Class#getNestHost} method.
                 */
                private final Method getNestHost;

                /**
                 * The {@code java.lang.Class#getNestMembers} method.
                 */
                private final Method getNestMembers;

                /**
                 * The {@code java.lang.Class#isNestmateOf} method.
                 */
                private final Method isNestmateOf;

                /**
                 * Creates a dispatcher for a Java 11-capable VM.
                 *
                 * @param getNestHost    The {@code java.lang.Class#getNestHost} method.
                 * @param getNestMembers The {@code java.lang.Class#getNestMembers} method.
                 * @param isNestmateOf   The {@code java.lang.Class#isNestmateOf} method.
                 */
                protected ForJava11CapableVm(Method getNestHost, Method getNestMembers, Method isNestmateOf) {
                    this.getNestHost = getNestHost;
                    this.getNestMembers = getNestMembers;
                    this.isNestmateOf = isNestmateOf;
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?> getNestHost(Class<?> type) {
                    try {
                        return (Class<?>) getNestHost.invoke(type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access Class::getNestHost", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Could not invoke Class:getNestHost", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<?>[] getNestMembers(Class<?> type) {
                    try {
                        return (Class<?>[]) getNestMembers.invoke(type);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access Class::getNestMembers", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Could not invoke Class:getNestMembers", exception.getCause());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isNestmateOf(Class<?> type, Class<?> candidate) {
                    try {
                        return (Boolean) isNestmateOf.invoke(type, candidate);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Could not access Class::isNestmateOf", exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Could not invoke Class:isNestmateOf", exception.getCause());
                    }
                }
            }
        }
    }

    /**
     * A projection for an array type based on an existing {@link TypeDescription}.
     */
    class ArrayProjection extends AbstractBase {

        /**
         * Modifiers that every array in Java implies.
         */
        private static final int ARRAY_IMPLIED = Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT;

        /**
         * Modifiers that no array in Java displays.
         */
        private static final int ARRAY_EXCLUDED = Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_STATIC;

        /**
         * The base component type which is itself not an array.
         */
        private final TypeDescription componentType;

        /**
         * The arity of this array.
         */
        private final int arity;

        /**
         * Creates a new array projection.
         *
         * @param componentType The base component type of the array which is itself not an array.
         * @param arity         The arity of this array.
         */
        protected ArrayProjection(TypeDescription componentType, int arity) {
            this.componentType = componentType;
            this.arity = arity;
        }

        /**
         * Creates an array projection of an arity of one.
         *
         * @param componentType The component type of the array.
         * @return A projection of the component type as an array of the given value with an arity of one.
         */
        public static TypeDescription of(TypeDescription componentType) {
            return of(componentType, 1);
        }

        /**
         * Creates an array projection.
         *
         * @param componentType The component type of the array.
         * @param arity         The arity of this array.
         * @return A projection of the component type as an array of the given value with the supplied arity.
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

        /**
         * {@inheritDoc}
         */
        public boolean isArray() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getComponentType() {
            return arity == 1
                    ? componentType
                    : new ArrayProjection(componentType, arity - 1);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isPrimitive() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            return TypeDescription.Generic.OBJECT;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return ARRAY_INTERFACES;
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            return TypeDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public String getSimpleName() {
            StringBuilder stringBuilder = new StringBuilder(componentType.getSimpleName());
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return false;
        }

        @Override
        public boolean isMemberType() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty<FieldDescription.InDefinedShape>();
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty<MethodDescription.InDefinedShape>();
        }

        /**
         * {@inheritDoc}
         */
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getInheritedAnnotations() {
            return new AnnotationList.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            return PackageDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            String descriptor = componentType.getDescriptor();
            StringBuilder stringBuilder = new StringBuilder(descriptor.length() + arity);
            for (int index = 0; index < arity; index++) {
                stringBuilder.append('[');
            }
            for (int index = 0; index < descriptor.length(); index++) {
                char character = descriptor.charAt(index);
                stringBuilder.append(character == '/' ? '.' : character);
            }
            return stringBuilder.toString();
        }

        /**
         * {@inheritDoc}
         */
        public String getDescriptor() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getDescriptor()).toString();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return TypeDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return (getComponentType().getModifiers() & ~ARRAY_EXCLUDED) | ARRAY_IMPLIED;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            return new TypeList.Generic.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return new TypeList.Explicit(this);
        }
    }

    /**
     * <p>
     * A latent type description for a type without methods or fields.
     * </p>
     * <p>
     * <b>Important</b>: This type does not define most of its properties and should only be used as a simple placeholder. For more
     * complex placeholders, use an {@link net.bytebuddy.dynamic.scaffold.InstrumentedType.Default}.
     * </p>
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
        private final Generic superClass;

        /**
         * The interfaces that this type implements.
         */
        private final List<? extends Generic> interfaces;

        /**
         * Creates a new latent type.
         *
         * @param name        The name of the type.
         * @param modifiers   The modifiers of the type.
         * @param superClass  The super type or {@code null} if no such type exists.
         * @param anInterface The interfaces that this type implements.
         */
        public Latent(String name, int modifiers, Generic superClass, Generic... anInterface) {
            this(name, modifiers, superClass, Arrays.asList(anInterface));
        }

        /**
         * Creates a new latent type.
         *
         * @param name       The name of the type.
         * @param modifiers  The modifiers of the type.
         * @param superClass The super type or {@code null} if no such type exists.
         * @param interfaces The interfaces that this type implements.
         */
        public Latent(String name, int modifiers, Generic superClass, List<? extends Generic> interfaces) {
            this.name = name;
            this.modifiers = modifiers;
            this.superClass = superClass;
            this.interfaces = interfaces;
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            return superClass;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.Explicit(interfaces);
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            throw new IllegalStateException("Cannot resolve enclosing method of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            throw new IllegalStateException("Cannot resolve enclosing type of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            throw new IllegalStateException("Cannot resolve inner types of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            throw new IllegalStateException("Cannot resolve anonymous type property of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            throw new IllegalStateException("Cannot resolve local class property of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            throw new IllegalStateException("Cannot resolve declared fields of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            throw new IllegalStateException("Cannot resolve declared methods of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            String name = getName();
            int index = name.lastIndexOf('.');
            return new PackageDescription.Simple(index == -1
                    ? EMPTY_NAME
                    : name.substring(0, index));
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            throw new IllegalStateException("Cannot resolve declared annotations of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            throw new IllegalStateException("Cannot resolve declared type of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return modifiers;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            throw new IllegalStateException("Cannot resolve type variables of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            throw new IllegalStateException("Cannot resolve nest host of a latent type description: " + this);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            throw new IllegalStateException("Cannot resolve nest mates of a latent type description: " + this);
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

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            return TypeDescription.Generic.OBJECT;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return new TypeList.Generic.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return MethodDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            return TypeDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return new TypeList.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return new FieldList.Empty<FieldDescription.InDefinedShape>();
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return new MethodList.Empty<MethodDescription.InDefinedShape>();
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            return packageDescription;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return packageDescription.getDeclaredAnnotations();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return TypeDescription.UNDEFINED;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            return new TypeList.Generic.Empty();
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return PackageDescription.PACKAGE_MODIFIERS;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return packageDescription.getName() + "." + PackageDescription.PACKAGE_CLASS_NAME;
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return new TypeList.Explicit(this);
        }
    }

    /**
     * A delegating type description that always attempts to load the super types of a delegate type.
     */
    class SuperTypeLoading extends AbstractBase {

        /**
         * The delegate type description.
         */
        private final TypeDescription delegate;

        /**
         * The class loader to use for loading a super type.
         */
        private final ClassLoader classLoader;

        /**
         * A delegate for loading a type.
         */
        private final ClassLoadingDelegate classLoadingDelegate;

        /**
         * Creates a super type loading type description.
         *
         * @param delegate    The delegate type description.
         * @param classLoader The class loader to use for loading a super type.
         */
        public SuperTypeLoading(TypeDescription delegate, ClassLoader classLoader) {
            this(delegate, classLoader, ClassLoadingDelegate.Simple.INSTANCE);
        }

        /**
         * Creates a super type loading type description.
         *
         * @param delegate             The delegate type description.
         * @param classLoader          The class loader to use for loading a super type.
         * @param classLoadingDelegate A delegate for loading a type.
         */
        public SuperTypeLoading(TypeDescription delegate, ClassLoader classLoader, ClassLoadingDelegate classLoadingDelegate) {
            this.delegate = delegate;
            this.classLoader = classLoader;
            this.classLoadingDelegate = classLoadingDelegate;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return delegate.getDeclaredAnnotations();
        }

        /**
         * {@inheritDoc}
         */
        public int getModifiers() {
            return delegate.getModifiers();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getTypeVariables() {
            return delegate.getTypeVariables();
        }

        /**
         * {@inheritDoc}
         */
        public String getDescriptor() {
            return delegate.getDescriptor();
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return delegate.getName();
        }

        /**
         * {@inheritDoc}
         */
        public Generic getSuperClass() {
            Generic superClass = delegate.getSuperClass();
            return superClass == null
                    ? Generic.UNDEFINED
                    : new ClassLoadingTypeProjection(superClass, classLoader, classLoadingDelegate);
        }

        /**
         * {@inheritDoc}
         */
        public TypeList.Generic getInterfaces() {
            return new ClassLoadingTypeList(delegate.getInterfaces(), classLoader, classLoadingDelegate);
        }

        /**
         * {@inheritDoc}
         */
        public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
            return delegate.getDeclaredFields();
        }

        /**
         * {@inheritDoc}
         */
        public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
            return delegate.getDeclaredMethods();
        }

        /**
         * {@inheritDoc}
         */
        public StackSize getStackSize() {
            return delegate.getStackSize();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isArray() {
            return delegate.isArray();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isPrimitive() {
            return delegate.isPrimitive();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getComponentType() {
            return delegate.getComponentType();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getDeclaringType() {
            return delegate.getDeclaringType();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getDeclaredTypes() {
            return delegate.getDeclaredTypes();
        }

        /**
         * {@inheritDoc}
         */
        public MethodDescription.InDefinedShape getEnclosingMethod() {
            return delegate.getEnclosingMethod();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getEnclosingType() {
            return delegate.getEnclosingType();
        }

        /**
         * {@inheritDoc}
         */
        public String getSimpleName() {
            return delegate.getSimpleName();
        }

        /**
         * {@inheritDoc}
         */
        public String getCanonicalName() {
            return delegate.getCanonicalName();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnonymousType() {
            return delegate.isAnonymousType();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isLocalType() {
            return delegate.isLocalType();
        }

        /**
         * {@inheritDoc}
         */
        public PackageDescription getPackage() {
            return delegate.getPackage();
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getNestHost() {
            return delegate.getNestHost();
        }

        /**
         * {@inheritDoc}
         */
        public TypeList getNestMembers() {
            return delegate.getNestMembers();
        }

        /**
         * A class loading delegate is responsible for resolving a type given a class loader and a type name.
         */
        public interface ClassLoadingDelegate {

            /**
             * Loads a type.
             *
             * @param name        The type's name,
             * @param classLoader The class loader to load the type from which might be {@code null} to represent the bootstrap class loader.
             * @return The loaded type.
             * @throws ClassNotFoundException If the type could not be found.
             */
            Class<?> load(String name, ClassLoader classLoader) throws ClassNotFoundException;

            /**
             * A simple class loading delegate that simply loads a type.
             */
            enum Simple implements ClassLoadingDelegate {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Class<?> load(String name, ClassLoader classLoader) throws ClassNotFoundException {
                    return Class.forName(name, false, classLoader);
                }
            }
        }

        /**
         * A type projection that attempts to load any super type of the delegate type.
         */
        protected static class ClassLoadingTypeProjection extends TypeDescription.Generic.LazyProjection {

            /**
             * The delegate type description.
             */
            private final Generic delegate;

            /**
             * The class loader to use for loading types which might be {@code null} to represent the bootstrap class loader.
             */
            private final ClassLoader classLoader;

            /**
             * A delegate for loading a type.
             */
            private final ClassLoadingDelegate classLoadingDelegate;

            /**
             * Creates a class loading type description.
             *
             * @param delegate             The delegate type description.
             * @param classLoader          The class loader to use for loading types which might be {@code null} to represent the bootstrap class loader.
             * @param classLoadingDelegate A delegate for loading a type.
             */
            protected ClassLoadingTypeProjection(Generic delegate, ClassLoader classLoader, ClassLoadingDelegate classLoadingDelegate) {
                this.delegate = delegate;
                this.classLoader = classLoader;
                this.classLoadingDelegate = classLoadingDelegate;
            }

            /**
             * {@inheritDoc}
             */
            public AnnotationList getDeclaredAnnotations() {
                return delegate.getDeclaredAnnotations();
            }

            /**
             * {@inheritDoc}
             */
            @CachedReturnPlugin.Enhance("erasure")
            public TypeDescription asErasure() {
                try {
                    return ForLoadedType.of(classLoadingDelegate.load(delegate.asErasure().getName(), classLoader));
                } catch (ClassNotFoundException ignored) {
                    return delegate.asErasure();
                }
            }

            @Override
            protected Generic resolve() {
                return delegate;
            }

            /**
             * {@inheritDoc}
             */
            @CachedReturnPlugin.Enhance("superClass")
            public Generic getSuperClass() {
                Generic superClass = delegate.getSuperClass();
                if (superClass == null) {
                    return Generic.UNDEFINED;
                } else {
                    try {
                        return new ClassLoadingTypeProjection(superClass,
                                classLoadingDelegate.load(delegate.asErasure().getName(), classLoader).getClassLoader(),
                                classLoadingDelegate);
                    } catch (ClassNotFoundException ignored) {
                        return superClass;
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            @CachedReturnPlugin.Enhance("interfaces")
            public TypeList.Generic getInterfaces() {
                TypeList.Generic interfaces = delegate.getInterfaces();
                try {
                    return new ClassLoadingTypeList(interfaces,
                            classLoadingDelegate.load(delegate.asErasure().getName(), classLoader).getClassLoader(),
                            classLoadingDelegate);
                } catch (ClassNotFoundException ignored) {
                    return interfaces;
                }
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<TypeDefinition> iterator() {
                return new SuperClassIterator(this);
            }
        }

        /**
         * A type list that attempts loading any type.
         */
        protected static class ClassLoadingTypeList extends TypeList.Generic.AbstractBase {

            /**
             * The delegate type list.
             */
            private final TypeList.Generic delegate;

            /**
             * The class loader to use for loading types which might be {@code null} to represent the bootstrap class loader.
             */
            private final ClassLoader classLoader;

            /**
             * A delegate for loading a type.
             */
            private final ClassLoadingDelegate classLoadingDelegate;

            /**
             * Creates a class loading type list.
             *
             * @param delegate             The delegate type list.
             * @param classLoader          The class loader to use for loading types which might be {@code null} to represent the bootstrap class loader.
             * @param classLoadingDelegate A delegate for loading a type.
             */
            protected ClassLoadingTypeList(TypeList.Generic delegate, ClassLoader classLoader, ClassLoadingDelegate classLoadingDelegate) {
                this.delegate = delegate;
                this.classLoader = classLoader;
                this.classLoadingDelegate = classLoadingDelegate;
            }

            /**
             * {@inheritDoc}
             */
            public Generic get(int index) {
                return new ClassLoadingTypeProjection(delegate.get(index), classLoader, classLoadingDelegate);
            }

            /**
             * {@inheritDoc}
             */
            public int size() {
                return delegate.size();
            }
        }
    }
}
