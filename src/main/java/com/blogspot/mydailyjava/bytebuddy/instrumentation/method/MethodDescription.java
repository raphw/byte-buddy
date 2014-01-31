package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierReviewable;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface MethodDescription extends ModifierReviewable, ByteCodeMethod, AnnotatedElement {

    static abstract class AbstractMethodDescription extends ModifierReviewable.AbstractModifierReviewable implements MethodDescription {

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
        public int hashCode() {
            return (getDeclaringType().getInternalName() + getUniqueSignature()).hashCode();
        }
    }

    static class ForConstructor extends AbstractMethodDescription {

        private static final String DYNAMIC_CONSTRUCTOR_INTERNAL_NAME = "<init>";

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
        public boolean isDeclaredInInterface() {
            return false;
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
            return DYNAMIC_CONSTRUCTOR_INTERNAL_NAME;
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
        public boolean isOverridable() {
            return false;
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
        public boolean isDeclaredInInterface() {
            return method.getDeclaringClass().isInterface();
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
        public boolean isOverridable() {
            return !(isFinal() || isPrivate() || isStatic() || getDeclaringType().isFinal());
        }

        @Override
        public String toString() {
            return "MethodDescription.ForMethod{" + method + "}";
        }
    }

    TypeDescription getDeclaringType();

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

    boolean isDeclaredInInterface();

    int getStackSize();
}
