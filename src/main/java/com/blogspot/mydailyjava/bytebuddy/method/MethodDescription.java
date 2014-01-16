package com.blogspot.mydailyjava.bytebuddy.method;

import com.blogspot.mydailyjava.bytebuddy.method.matcher.VerboseMethodDescription;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

public interface MethodDescription extends AnnotatedElement, Member, GenericDeclaration, ByteCodeMethod, VerboseMethodDescription {

    static abstract class AbstractMethodDescription implements MethodDescription {

        @Override
        public String[] getExceptionTypesInternalNames() {
            return makeInternalNameArray(getExceptionTypes());
        }

        @Override
        public String getUniqueSignature() {
            return getInternalName() + getDescriptor();
        }

        @Override
        public String getDeclaringClassInternalName() {
            return org.objectweb.asm.Type.getInternalName(getDeclaringClass());
        }

        @Override
        public String getDeclaringSuperClassInternalName() {
            return org.objectweb.asm.Type.getInternalName(getDeclaringClass().getSuperclass());
        }

        @Override
        public boolean isAbstract() {
            return Modifier.isAbstract(getModifiers());
        }

        private static String[] makeInternalNameArray(Class<?>[] types) {
            if (types.length == 0) {
                return null;
            }
            String[] internalName = new String[types.length];
            int i = 0;
            for (Class<?> type : types) {
                internalName[i] = org.objectweb.asm.Type.getInternalName(type);
            }
            return internalName;
        }
    }

    static class ForConstructor extends AbstractMethodDescription {

        private static final String DYNAMIC_CONSTRUCTOR_INTERNAL_NAME = "<init>";

        private final Constructor<?> constructor;

        public ForConstructor(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Class<?> getReturnType() {
            return void.class;
        }

        @Override
        public Type getGenericReturnType() {
            return void.class;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return constructor.getParameterTypes();
        }

        @Override
        public Type[] getGenericParameterTypes() {
            return constructor.getGenericParameterTypes();
        }

        @Override
        public Annotation[][] getParameterAnnotations() {
            return constructor.getParameterAnnotations();
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return constructor.getExceptionTypes();
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
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return constructor.isAnnotationPresent(annotationClass);
        }

        @Override
        public <S extends Annotation> S getAnnotation(Class<S> annotationClass) {
            return constructor.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return constructor.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return constructor.getDeclaredAnnotations();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return constructor.getDeclaringClass();
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
        public TypeVariable<?>[] getTypeParameters() {
            return constructor.getTypeParameters();
        }

        @Override
        public String getInternalName() {
            return DYNAMIC_CONSTRUCTOR_INTERNAL_NAME;
        }

        @Override
        public String getDescriptor() {
            return org.objectweb.asm.Type.getConstructorDescriptor(constructor);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && constructor.equals(((ForConstructor) o).constructor);
        }

        @Override
        public int hashCode() {
            return constructor.hashCode();
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
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Type getGenericReturnType() {
            return method.getGenericReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public Type[] getGenericParameterTypes() {
            return method.getGenericParameterTypes();
        }

        @Override
        public Annotation[][] getParameterAnnotations() {
            return method.getParameterAnnotations();
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return method.getExceptionTypes();
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
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return method.isAnnotationPresent(annotationClass);
        }

        @Override
        public <S extends Annotation> S getAnnotation(Class<S> annotationClass) {
            return method.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return method.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return method.getDeclaredAnnotations();
        }

        @Override
        public Class<?> getDeclaringClass() {
            return method.getDeclaringClass();
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
        public TypeVariable<?>[] getTypeParameters() {
            return method.getTypeParameters();
        }

        @Override
        public String getInternalName() {
            return method.getName();
        }

        @Override
        public String getDescriptor() {
            return org.objectweb.asm.Type.getMethodDescriptor(method);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && method.equals(((ForMethod) other).method);
        }

        @Override
        public int hashCode() {
            return method.hashCode();
        }

        @Override
        public String toString() {
            return "MethodDescription.ForMethod{" + method + "}";
        }
    }

    Class<?> getReturnType();

    Type getGenericReturnType();

    Class<?>[] getParameterTypes();

    Type[] getGenericParameterTypes();

    Annotation[][] getParameterAnnotations();

    Class<?>[] getExceptionTypes();

    boolean isVarArgs();

    boolean isConstructor();

    boolean isBridge();

    boolean represents(Method method);

    boolean represents(Constructor<?> constructor);
}
