package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ByteCodeElement;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierReviewable;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface.
 */
public interface TypeDescription extends ByteCodeElement, DeclaredInType, ModifierReviewable, AnnotatedElement {

    /**
     * An abstract base implementation of a type description.
     */
    static abstract class AbstractTypeDescription extends AbstractModifierReviewable implements TypeDescription {

        private static class UniqueSignatureFilter implements MethodMatcher {

            private final Set<String> foundSignatures = new HashSet<String>();

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return foundSignatures.add(methodDescription.getUniqueSignature());
            }

            @Override
            public String toString() {
                return "UniqueSignatureFilter{foundSignatures=" + foundSignatures + '}';
            }
        }

        @Override
        public boolean isInstance(Object object) {
            return isAssignableFrom(object.getClass());
        }

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public MethodList getReachableMethods() {
            List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>();
            MethodMatcher uniqueSignatureFilter = new UniqueSignatureFilter();
            methodDescriptions.addAll(getDeclaredMethods().filter(uniqueSignatureFilter));
            MethodMatcher subclassFilter = not(MethodMatchers.isPrivate())
                    .and(not(MethodMatchers.isPackagePrivate()).or(isVisibleFromPackage(getPackageName())))
                    .and(isMethod())
                    .and(uniqueSignatureFilter);
            if (getSupertype() != null) {
                methodDescriptions.addAll(getSupertype().getReachableMethods().filter(subclassFilter));
            }
            for (TypeDescription anInterface : getInterfaces()) {
                methodDescriptions.addAll(anInterface.getReachableMethods().filter(uniqueSignatureFilter));
            }
            return new MethodList.Explicit(methodDescriptions);
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPublic() || typeDescription.getPackageName().equals(getPackageName());
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof TypeDescription
                    && getName().equals(((TypeDescription) other).getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }

    /**
     * A type description implementation that represents a loaded type.
     */
    static class ForLoadedType extends AbstractTypeDescription {

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
        public boolean isInstance(Object object) {
            return type.isInstance(object);
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return this.type.isAssignableFrom(type);
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return typeDescription.isAssignableTo(type);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return type.isAssignableFrom(this.type);
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return typeDescription.isAssignableFrom(type);
        }

        @Override
        public boolean represents(Class<?> type) {
            return type == this.type;
        }

        @Override
        public boolean isInterface() {
            return type.isInterface();
        }

        @Override
        public boolean isArray() {
            return type.isArray();
        }

        @Override
        public TypeDescription getComponentType() {
            return type.getComponentType() == null ? null : new TypeDescription.ForLoadedType(type.getComponentType());
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
        public boolean isSynthetic() {
            return type.isSynthetic();
        }

        @Override
        public TypeDescription getSupertype() {
            return type.getSuperclass() == null ? null : new TypeDescription.ForLoadedType(type.getSuperclass());
        }

        @Override
        public TypeList getInterfaces() {
            return new TypeList.ForLoadedType(type.getInterfaces());
        }

        @Override
        public TypeDescription getDeclaringType() {
            Class<?> declaringType = type.getDeclaringClass();
            return declaringType == null ? null : new TypeDescription.ForLoadedType(declaringType);
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
                return null;
            }
        }

        @Override
        public TypeDescription getEnclosingClass() {
            Class<?> enclosingType = type.getEnclosingClass();
            return enclosingType == null ? null : new TypeDescription.ForLoadedType(enclosingType);
        }

        @Override
        public String getSimpleName() {
            return type.getSimpleName();
        }

        @Override
        public String getCanonicalName() {
            return type.getCanonicalName();
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
        public FieldList getDeclaredFields() {
            return new FieldList.ForLoadedField(type.getDeclaredFields());
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.ForLoadedType(type);
        }

        @Override
        public String getPackageName() {
            return type.getPackage().getName();
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.of(type);
        }

        @Override
        public String getName() {
            return type.getName();
        }

        @Override
        public String getDescriptor() {
            return Type.getDescriptor(type);
        }

        @Override
        public int getModifiers() {
            return type.getModifiers();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            return type.getAnnotation(annotationType);
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return type.isAnnotationPresent(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return type.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return type.getDeclaredAnnotations();
        }

        @Override
        public String toString() {
            return "TypeDescription.ForLoadedType{" + type + "}";
        }
    }

    /**
     * Checks if {@code object} is an instance of the type represented by this instance.
     *
     * @param object The object of interest.
     * @return {@code true} if the object is an instance of the type described by this instance.
     */
    boolean isInstance(Object object);

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
     * <p/>
     * Implementations of this methods are allowed to delegate to
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription#isAssignableFrom(Class)}
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
     * <p/>
     * Implementations of this methods are allowed to delegate to
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription#isAssignableTo(Class)}
     *
     * @param typeDescription The type of interest.
     * @return {@code true} if this type is assignable to {@code type}.
     */
    boolean isAssignableTo(TypeDescription typeDescription);

    /**
     * Checks if the type described by this instance represents {@code type}.
     *
     * @param type The type of interest.
     * @return {@code true} if the type described by this instance represents {@code type}.
     */
    boolean represents(Class<?> type);

    /**
     * Checks if the type described by this entity is an array
     *
     * @return {@code true} if this type description represents an array.
     */
    boolean isArray();

    /**
     * Returns the component type of this type.
     *
     * @return The component type of this array or {@code null} if this type description does not represent an array.
     */
    TypeDescription getComponentType();

    /**
     * Checks if the type described by this entity is a primitive type.
     *
     * @return {@code true} if this type description represents a primitive type.
     */
    boolean isPrimitive();

    /**
     * Returns the component type of this type.
     *
     * @return The component type of this array or {@code null} if type does not have a super type as for the
     * {@link java.lang.Object} type.
     */
    TypeDescription getSupertype();

    /**
     * Returns a list of interfaces that are implemented by this type.
     *
     * @return A list of interfaces that are implemented by this type.
     */
    TypeList getInterfaces();

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
    TypeDescription getEnclosingClass();

    /**
     * Returns the simple internalName of this type.
     *
     * @return The simple internalName of this type.
     */
    String getSimpleName();

    /**
     * Returns the canonical internalName of this type.
     *
     * @return The canonical internalName of this type.
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
     * Returns a list of fields that are declared by this type.
     *
     * @return A list of fields that are declared by this type.
     */
    FieldList getDeclaredFields();

    /**
     * Returns a list of methods that are declared by this type.
     *
     * @return A list of methods that are declared by this type.
     */
    MethodList getDeclaredMethods();

    /**
     * Returns a list of all reachable methods of this type, i.e. a list of all methods with unique signatures that can
     * be called from within this type.
     *
     * @return A list of all reachable methods.
     */
    MethodList getReachableMethods();

    /**
     * Returns the package internalName of the type described by this instance.
     *
     * @return The package internalName of the type described by this instance.
     */
    String getPackageName();

    /**
     * Returns the size of the type described by this instance.
     *
     * @return The size of the type described by this instance.
     */
    StackSize getStackSize();
}
