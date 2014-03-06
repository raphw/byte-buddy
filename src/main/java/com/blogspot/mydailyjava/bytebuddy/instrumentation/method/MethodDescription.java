package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierReviewable;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.DeclaredInType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Implementations of this interface describe a Java method, i.e. a method or a constructor. Implementations of this
 * interface must provide meaningful {@code equal(Object)} and {@code hashCode()} implementations.
 */
public interface MethodDescription extends ModifierReviewable, ByteCodeMethod, DeclaredInType, AnnotatedElement {

    /**
     * The internal internalName of a Java constructor.
     */
    static final String CONSTRUCTOR_INTERNAL_NAME = "<init>";

    /**
     * An abstract base implementation of a method description.
     */
    static abstract class AbstractMethodDescription extends AbstractModifierReviewable implements MethodDescription {

        @Override
        public String getUniqueSignature() {
            return getInternalName() + getDescriptor();
        }

        @Override
        public int getStackSize() {
            return getParameterTypes().getStackSize() + (isStatic() ? 0 : 1);
        }

        @Override
        public String getDescriptor() {
            StringBuilder descriptor = new StringBuilder("(");
            for (TypeDescription parameterType : getParameterTypes()) {
                descriptor.append(parameterType.getDescriptor());
            }
            return descriptor.append(")").append(getReturnType().getDescriptor()).toString();
        }

        @Override
        public boolean isOverridable() {
            return !(isConstructor() || isFinal() || isPrivate() || isStatic() || getDeclaringType().isFinal());
        }

        @Override
        public int getParameterOffset(int parameterIndex) {
            int offset = isStatic() ? 0 : 1;
            int currentIndex = 0;
            for (TypeDescription parameterType : getParameterTypes()) {
                if (currentIndex == parameterIndex) {
                    return offset;
                } else {
                    currentIndex++;
                    offset += parameterType.getStackSize().getSize();
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public boolean equals(Object other) {
            return other == this || other instanceof MethodDescription
                    && getUniqueSignature().equals(((MethodDescription) other).getUniqueSignature())
                    && getDeclaringType().equals(((MethodDescription) other).getDeclaringType());
        }

        @Override
        public int hashCode() {
            return (getDeclaringType().getInternalName() + "." + getUniqueSignature()).hashCode();
        }
    }

    /**
     * An implementation of a method description for a loaded constructor.
     */
    static class ForConstructor extends AbstractMethodDescription {

        private final Constructor<?> constructor;

        /**
         * Creates a new immutable method description for a loaded constructor.
         *
         * @param constructor The loaded constructor to be represented by this method description.
         */
        public ForConstructor(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(constructor.getDeclaringClass());
        }

        @Override
        public TypeDescription getReturnType() {
            return new TypeDescription.ForLoadedType(void.class);
        }

        @Override
        public TypeList getParameterTypes() {
            return new TypeList.ForLoadedType(constructor.getParameterTypes());
        }


        @Override
        public Annotation[][] getParameterAnnotations() {
            return constructor.getParameterAnnotations();
        }

        @Override
        public TypeList getExceptionTypes() {
            return new TypeList.ForLoadedType(constructor.getExceptionTypes());
        }

        @Override
        public boolean isVarArgs() {
            return constructor.isVarArgs();
        }

        @Override
        public boolean isConstructor() {
            return true;
        }

        @Override
        public boolean isBridge() {
            return false;
        }

        @Override
        public boolean represents(Method method) {
            return false;
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return this.constructor.equals(constructor);
        }

        @Override
        public String getName() {
            return constructor.getName();
        }

        @Override
        public int getModifiers() {
            return constructor.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return constructor.isSynthetic();
        }

        @Override
        public String getInternalName() {
            return CONSTRUCTOR_INTERNAL_NAME;
        }

        @Override
        public String getDescriptor() {
            return Type.getConstructorDescriptor(constructor);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return constructor.getDeclaredAnnotations();
        }

        @Override
        public Annotation[] getAnnotations() {
            return constructor.getAnnotations();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return constructor.getAnnotation(annotationClass);
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return constructor.isAnnotationPresent(annotationClass);
        }

        @Override
        public String toString() {
            return "MethodDescription.ForConstructor{" + constructor + "}";
        }
    }

    /**
     * An implementation of a method description for a loaded method.
     */
    static class ForMethod extends AbstractMethodDescription {

        private final Method method;

        /**
         * Creates a new immutable method description for a loaded method.
         *
         * @param method The loaded method to be represented by this method description.
         */
        public ForMethod(Method method) {
            this.method = method;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return new TypeDescription.ForLoadedType(method.getDeclaringClass());
        }

        @Override
        public TypeDescription getReturnType() {
            return new TypeDescription.ForLoadedType(method.getReturnType());
        }

        @Override
        public TypeList getParameterTypes() {
            return new TypeList.ForLoadedType(method.getParameterTypes());
        }

        @Override
        public Annotation[][] getParameterAnnotations() {
            return method.getParameterAnnotations();
        }

        @Override
        public TypeList getExceptionTypes() {
            return new TypeList.ForLoadedType(method.getExceptionTypes());
        }

        @Override
        public boolean isVarArgs() {
            return method.isVarArgs();
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public boolean isBridge() {
            return method.isBridge();
        }

        @Override
        public boolean represents(Method method) {
            return this.method.equals(method);
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return false;
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return method.isSynthetic();
        }

        @Override
        public String getInternalName() {
            return method.getName();
        }

        @Override
        public String getDescriptor() {
            return Type.getMethodDescriptor(method);
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return method.getDeclaredAnnotations();
        }

        @Override
        public Annotation[] getAnnotations() {
            return method.getAnnotations();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return method.getAnnotation(annotationClass);
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return method.isAnnotationPresent(annotationClass);
        }

        @Override
        public String toString() {
            return "MethodDescription.ForMethod{" + method + "}";
        }
    }

    /**
     * A latent method description describes a method that is not attached to a declaring
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription} but stands for itself.
     */
    static class Latent extends AbstractMethodDescription {

        private final String internalName;
        private final TypeDescription declaringType;
        private final TypeDescription returnType;
        private final List<TypeDescription> parameterTypes;
        private final int modifiers;

        /**
         * Creates an immutable latent method description.
         *
         * @param internalName   The internal name of the method.
         * @param declaringType  The type that is declaring this method latently.
         * @param returnType     The return type of this method.
         * @param parameterTypes The parameter types of this method.
         * @param modifiers      The modifiers of this method.
         */
        public Latent(String internalName,
                      TypeDescription declaringType,
                      TypeDescription returnType,
                      List<TypeDescription> parameterTypes,
                      int modifiers) {
            this.internalName = internalName;
            this.declaringType = declaringType;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.modifiers = modifiers;
        }

        @Override
        public TypeDescription getReturnType() {
            return returnType;
        }

        @Override
        public TypeList getParameterTypes() {
            return new TypeList.Explicit(parameterTypes);
        }

        @Override
        public Annotation[][] getParameterAnnotations() {
            return new Annotation[0][0];
        }

        @Override
        public TypeList getExceptionTypes() {
            return new TypeList.Empty();
        }

        @Override
        public boolean isConstructor() {
            return CONSTRUCTOR_INTERNAL_NAME.equals(internalName);
        }

        @Override
        public boolean represents(Method method) {
            return false;
        }

        @Override
        public boolean represents(Constructor<?> constructor) {
            return false;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String getName() {
            return isConstructor() ? getDeclaringType().getName() : internalName;
        }

        @Override
        public String getInternalName() {
            return internalName;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String toString() {
            return "MethodDescription.Latent{" +
                    "internalName='" + internalName + '\'' +
                    ", declaringType=" + declaringType +
                    ", returnType=" + returnType +
                    ", parameterTypes=" + parameterTypes +
                    ", modifiers=" + modifiers +
                    '}';
        }
    }

    /**
     * Returns a description of the return type of the method described by this instance.
     *
     * @return A description of the return type of the method described by this instance.
     */
    TypeDescription getReturnType();

    /**
     * Returns a list of type descriptions of the method described by this instance.
     *
     * @return A list of type descriptions of the method described by this instance.
     */
    TypeList getParameterTypes();

    /**
     * Returns the parameter annotations of the method described by this instance.
     *
     * @return The parameter annotations of the method described by this instance.
     */
    Annotation[][] getParameterAnnotations();

    /**
     * Returns a description of the exception types of the method described by this instance.
     *
     * @return A description of the exception types of the method described by this instance.
     */
    TypeList getExceptionTypes();

    /**
     * Specifies if this method description represents a constructor.
     *
     * @return {@code true} if this method description represents a constructor.
     */
    boolean isConstructor();

    /**
     * Verifies if a method description represents a given loaded method.
     *
     * @return {@code true} if this method description represents the given loaded method.
     */
    boolean represents(Method method);

    /**
     * Verifies if a method description represents a given loaded constructor.
     *
     * @return {@code true} if this method description represents the given loaded constructor.
     */
    boolean represents(Constructor<?> constructor);

    /**
     * Verifies if this method description represents an overridable method.
     *
     * @return {@code true} if this method description represents an overridable method.
     */
    boolean isOverridable();

    /**
     * Returns the size of the local variable array that is required for this method, i.e. the size of all parameters
     * if they were loaded on the stack including a reference to {@code this} if this method represented a non-static
     * method.
     *
     * @return The size of this method on the operand stack.
     */
    int getStackSize();

    /**
     * Returns the offset of the parameter at {@code parameterIndex} on the described method's local variable array.
     *
     * @param parameterIndex The parameter index of interest.
     * @return The offset of this parameter.
     */
    int getParameterOffset(int parameterIndex);
}
