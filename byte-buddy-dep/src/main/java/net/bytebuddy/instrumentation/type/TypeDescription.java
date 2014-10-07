package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.ModifierReviewable;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotatedElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface.
 */
public interface TypeDescription extends ByteCodeElement, DeclaredInType, ModifierReviewable, AnnotatedElement {

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
     * <p>&nbsp;</p>
     * Implementations of this methods are allowed to delegate to
     * {@link net.bytebuddy.instrumentation.type.TypeDescription#isAssignableFrom(Class)}
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
     * {@link net.bytebuddy.instrumentation.type.TypeDescription#isAssignableTo(Class)}
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
     * Checks if the type described by this entity is an array.
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
     * Returns the canonical internalName of this type.
     *
     * @return The canonical internalName of this type.
     */
    String getCanonicalName();

    /**
     * Returns the generic signature of this type or {@code null} if no such signature is available.
     * <p>&nbsp;</p>
     * <b>Note</b>: Loaded classes do currently not provide such a signature.
     *
     * @return The generic signature of this type or {@code null} if no such signature is available.
     */
    String getGenericSignature();

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

    /**
     * Checks if this type is defined in a sealed package.
     *
     * @return {@code true} if the class is defined in a sealed package.
     */
    boolean isSealed();

    /**
     * Returns the {@link java.lang.ClassLoader} which is able to locate the byte code representation for
     * this type description or {@code null} if no such information is available.
     *
     * @return The {@link java.lang.ClassLoader} which can locate this type's class file or {@code null} if
     * no such {@link java.lang.ClassLoader} is available.
     */
    ClassLoader getClassLoader();

    AnnotationList getInheritedAnnotations();

    /**
     * An abstract base implementation of a type description.
     */
    abstract static class AbstractTypeDescription extends AbstractModifierReviewable implements TypeDescription {

        @Override
        public boolean isInstance(Object object) {
            return isAssignableFrom(object.getClass());
        }

        @Override
        public String getInternalName() {
            return getName().replace('.', '/');
        }

        @Override
        public String getPackageName() {
            int packageIndex = getName().lastIndexOf('.');
            if (packageIndex == -1) {
                return "";
            } else {
                return getName().substring(0, packageIndex);
            }
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
            return actualModifiers | (superFlag ? Opcodes.ACC_SUPER : 0);
        }

        @Override
        public String getGenericSignature() {
            return null; // Currently, generics signatures supported poorly.
        }

        @Override
        public boolean isVisibleTo(TypeDescription typeDescription) {
            return isPublic()
                    || (isPackagePrivate() && typeDescription.getPackageName().equals(getPackageName()))
                    || (isProtected() && getEnclosingClass() != null && getEnclosingClass().isAssignableFrom(typeDescription));
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            AnnotationList declaredAnnotations = getDeclaredAnnotations();
            if (getSupertype() == null) {
                return declaredAnnotations;
            } else {
                Set<TypeDescription> annotationTypes = new HashSet<TypeDescription>(declaredAnnotations.size());
                for (AnnotationDescription annotationDescription : declaredAnnotations) {
                    annotationTypes.add(annotationDescription.getAnnotationType());
                }
                return new AnnotationList.Explicit(join(declaredAnnotations, getSupertype().getInheritedAnnotations().inherited(annotationTypes)));
            }
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

        public abstract static class OfSimpleType extends AbstractTypeDescription {

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
                // (1) Both types are equal.
                if (sourceType.equals(targetType)) {
                    return true;
                }
                // The sub type has a super type and this super type is assignable to the super type.
                TypeDescription targetTypeSuperType = targetType.getSupertype();
                if (targetTypeSuperType != null && targetTypeSuperType.isAssignableTo(sourceType)) {
                    return true;
                }
                // (2) If the target type is an interface, any of this type's interfaces might be assignable to it.
                if (sourceType.isInterface()) {
                    for (TypeDescription interfaceType : targetType.getInterfaces()) {
                        if (interfaceType.isAssignableTo(sourceType)) {
                            return true;
                        }
                    }
                }
                // (3) None of these criteria are true, i.e. the types are not assignable.
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
            public boolean isInstance(Object object) {
                return isAssignableFrom(object.getClass());
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
            public TypeDescription getComponentType() {
                return null;
            }

            @Override
            public boolean represents(Class<?> type) {
                return type.getName().equals(getName());
            }

            @Override
            public String getDescriptor() {
                return "L" + getInternalName() + ";";
            }

            @Override
            public String getSimpleName() {
                return getName().substring(getPackageName().length() + 1, getName().length());
            }

            @Override
            public String getPackageName() {
                int packageIndex = getInternalName().lastIndexOf('/');
                return packageIndex == -1
                        ? ""
                        : getName().substring(0, packageIndex);
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
    static class ForLoadedType extends AbstractTypeDescription {

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
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(type.getDeclaredAnnotations());
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            return new AnnotationList.ForLoadedAnnotation(type.getAnnotations());
        }

        @Override
        public boolean isSealed() {
            return type.getPackage() != null && type.getPackage().isSealed();
        }

        @Override
        public ClassLoader getClassLoader() {
            return type.getClassLoader();
        }

        @Override
        public String toString() {
            return "TypeDescription.ForLoadedType{" + type + "}";
        }
    }

    static class ArrayProjection extends AbstractTypeDescription {

        private static final int ARRAY_MODIFIERS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT;

        public static TypeDescription of(TypeDescription componentType, int arity) {
            if (arity == 0) {
                return componentType;
            } else if (arity < 0) {
                throw new IllegalArgumentException("Arrays cannot have a negative arity");
            }
            while (componentType.isArray()) {
                componentType = componentType.getComponentType();
                arity++;
            }
            return new ArrayProjection(componentType, arity);
        }

        private final TypeDescription componentType;

        private final int arity;

        protected ArrayProjection(TypeDescription componentType, int arity) {
            this.componentType = componentType;
            this.arity = arity;
        }

        @Override
        public boolean isAssignableFrom(Class<?> type) {
            return isAssignableFrom(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableFrom(TypeDescription typeDescription) {
            return isArrayAssignable(this, typeDescription);
        }

        @Override
        public boolean isAssignableTo(Class<?> type) {
            return isAssignableTo(new ForLoadedType(type));
        }

        @Override
        public boolean isAssignableTo(TypeDescription typeDescription) {
            return typeDescription.represents(Object.class) || isArrayAssignable(typeDescription, this);
        }

        private static boolean isArrayAssignable(TypeDescription sourceType, TypeDescription targetType) {
            int sourceArity = 0, targetArity = 0;
            while (sourceType.isArray()) {
                sourceArity++;
                sourceType = sourceType.getComponentType();
            }
            while (targetType.isArray()) {
                targetArity++;
                targetType = targetType.getComponentType();
            }
            return sourceArity == targetArity && sourceType.isAssignableFrom(targetType);
        }

        @Override
        public boolean represents(Class<?> type) {
            int arity = 0;
            while (type.isArray()) {
                type = type.getComponentType();
                arity++;
            }
            return arity == this.arity && componentType.represents(type);
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
        public TypeDescription getSupertype() {
            return new ForLoadedType(Object.class);
        }

        @Override
        public TypeList getInterfaces() {
            return null;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingClass() {
            return null;
        }

        @Override
        public String getSimpleName() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(componentType.getSimpleName());
            for (int i = 0; i < arity; i++) {
                stringBuilder.append("[]");
            }
            return stringBuilder.toString();
        }

        @Override
        public String getCanonicalName() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(componentType.getName());
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
        public FieldList getDeclaredFields() {
            return new FieldList.Empty();
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Empty();
        }

        @Override
        public StackSize getStackSize() {
            return StackSize.SINGLE;
        }

        @Override
        public boolean isSealed() {
            return componentType.isSealed();
        }

        @Override
        public ClassLoader getClassLoader() {
            return componentType.getClassLoader();
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
        public String getName() {
            return getDescriptor();
        }

        @Override
        public String getDescriptor() {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arity; i++) {
                stringBuilder.append('[');
            }
            return stringBuilder.append(componentType.getInternalName()).toString();
        }

        @Override
        public TypeDescription getDeclaringType() {
            return null;
        }

        @Override
        public int getModifiers() {
            return ARRAY_MODIFIERS;
        }
    }
}
