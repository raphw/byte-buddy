package com.blogspot.mydailyjava.bytebuddy.instrumentation.type;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ByteCodeElement;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierReviewable;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
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

public interface TypeDescription extends ByteCodeElement, DeclaredInType, ModifierReviewable, AnnotatedElement {

    static abstract class AbstractTypeDescription extends AbstractModifierReviewable implements TypeDescription {

        private static class UniqueSignatureFilter implements MethodMatcher {

            private final Set<String> foundSignatures = new HashSet<String>();

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return foundSignatures.add(methodDescription.getUniqueSignature());
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
                    .and(not(MethodMatchers.isPackagePrivate()).or(isDefinedInPackage(getPackageName())))
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
            return isPublic()
                    || typeDescription.equals(this)
                    || (isPackagePrivate() && getPackageName().equals(typeDescription.getPackageName()))
                    || (isProtected() && isAssignableTo(typeDescription));
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

    static class ForLoadedType extends AbstractTypeDescription {

        private final Class<?> type;

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
                return new MethodDescription.ForMethod(enclosingMethod);
            } else if (enclosingConstructor != null) {
                return new MethodDescription.ForConstructor(enclosingConstructor);
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

    boolean isInstance(Object object);

    boolean isAssignableFrom(Class<?> type);

    boolean isAssignableFrom(TypeDescription typeDescription);

    boolean isAssignableTo(Class<?> type);

    boolean isAssignableTo(TypeDescription typeDescription);

    boolean represents(Class<?> type);

    boolean isInterface();

    boolean isArray();

    TypeDescription getComponentType();

    boolean isPrimitive();

    boolean isAnnotation();

    TypeDescription getSupertype();

    TypeList getInterfaces();

    MethodDescription getEnclosingMethod();

    TypeDescription getEnclosingClass();

    String getSimpleName();

    String getCanonicalName();

    boolean isAnonymousClass();

    boolean isLocalClass();

    boolean isMemberClass();

    FieldList getDeclaredFields();

    MethodList getDeclaredMethods();

    MethodList getReachableMethods();

    String getPackageName();

    boolean isVisibleTo(TypeDescription typeDescription);

    StackSize getStackSize();
}
