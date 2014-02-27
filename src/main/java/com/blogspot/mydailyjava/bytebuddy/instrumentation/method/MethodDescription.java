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

public interface MethodDescription extends ModifierReviewable, ByteCodeMethod, DeclaredInType, AnnotatedElement {

    static final String CONSTRUCTOR_INTERNAL_NAME = "<init>";

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
        public boolean equals(Object other) {
            return other == this || other instanceof MethodDescription
                    && getUniqueSignature().equals(((MethodDescription) other).getUniqueSignature())
                    && getDeclaringType().equals(((MethodDescription) other).getDeclaringType());
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
        public int hashCode() {
            return (getDeclaringType().getInternalName() + "." + getUniqueSignature()).hashCode();
        }
    }

    static class ForConstructor extends AbstractMethodDescription {

        private final Constructor<?> constructor;

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

    static class ForMethod extends AbstractMethodDescription {

        private final Method method;

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

    TypeDescription getReturnType();

    TypeList getParameterTypes();

    Annotation[][] getParameterAnnotations();

    TypeList getExceptionTypes();

    boolean isVarArgs();

    boolean isConstructor();

    boolean isBridge();

    boolean represents(Method method);

    boolean represents(Constructor<?> constructor);

    boolean isOverridable();

    int getStackSize();

    int getParameterOffset(int parameterIndex);
}
