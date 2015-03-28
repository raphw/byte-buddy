package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.ByteCodeElement;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.utility.JavaType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * Implementations of this interface represent a Java type, i.e. a class or interface.
 */
public interface TypeDescription extends ByteCodeElement {

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
     * Returns all interfaces that are implemented by this type, either directly or indirectly.
     *
     * @return A list of all interfaces of this type in random order.
     */
    TypeList getInheritedInterfaces();

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
     * Returns the package internalName of the type described by this instance.
     *
     * @return The package internalName of the type described by this instance.
     */
    PackageDescription getPackage();

    /**
     * Returns the size of the type described by this instance.
     *
     * @return The size of the type described by this instance.
     */
    StackSize getStackSize();

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
    boolean isWrapper();

    /**
     * An abstract base implementation of a type description.
     */
    abstract class AbstractTypeDescription extends AbstractModifierReviewable implements TypeDescription {

        /**
         * Collects all interfaces for a given type description.
         *
         * @param typeDescription An interface type to check for other interfaces.
         * @param interfaces      A collection of already discovered interfaces.
         */
        private static void collect(TypeDescription typeDescription, Set<TypeDescription> interfaces) {
            if (interfaces.add(typeDescription)) {
                for (TypeDescription interfaceType : typeDescription.getInterfaces()) {
                    collect(interfaceType, interfaces);
                }
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
            return null; // Currently, generics signatures supported poorly.
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
            return isPublic() || isProtected() || isSamePackage(typeDescription);
        }

        @Override
        public TypeList getInheritedInterfaces() {
            Set<TypeDescription> interfaces = new HashSet<TypeDescription>();
            TypeDescription current = this;
            do {
                for (TypeDescription interfaceType : current.getInterfaces()) {
                    collect(interfaceType, interfaces);
                }
            } while ((current = current.getSupertype()) != null);
            return new TypeList.Explicit(new ArrayList<TypeDescription>(interfaces));
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

        /**
         * Returns the name of this type's package.
         *
         * @return The name of this type's package or {@code null} if this type is defined in the default package.
         */
        protected String getPackageName() {
            String name = getName();
            int packageIndex = name.lastIndexOf('.');
            return packageIndex == -1
                    ? null
                    : name.substring(0, packageIndex);
        }

        @Override
        public boolean isConstantPool() {
            return represents(int.class)
                    || represents(long.class)
                    || represents(float.class)
                    || represents(double.class)
                    || represents(String.class)
                    || represents(Class.class)
                    || JavaType.METHOD_HANDLE.representedBy(this)
                    || JavaType.METHOD_TYPE.representedBy(this);
        }

        @Override
        public boolean isWrapper() {
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
        public boolean equals(Object other) {
            return other == this || other instanceof TypeDescription
                    && getName().equals(((TypeDescription) other).getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public String toString() {
            return (isPrimitive() ? "" : (isInterface() ? "interface" : "class") + " ") + getName();
        }

        /**
         * An adapter implementation of a {@link net.bytebuddy.instrumentation.type.TypeDescription} that
         * describes any type that is not an array or a primitive type.
         */
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
                // Interfaces do not extend the Object type but are assignable to the Object type.
                if (sourceType.represents(Object.class) && !targetType.isPrimitive()) {
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
                int simpleNameIndex = getInternalName().lastIndexOf('$');
                simpleNameIndex = simpleNameIndex == -1 ? getInternalName().lastIndexOf('/') : simpleNameIndex;
                return simpleNameIndex == -1 ? getInternalName() : getInternalName().substring(simpleNameIndex + 1);
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
    class ForLoadedType extends AbstractTypeDescription {

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
        public TypeDescription getSupertype() {
            return type.getSuperclass() == null ? null : new TypeDescription.ForLoadedType(type.getSuperclass());
        }

        @Override
        public TypeList getInterfaces() {
            return type.isArray()
                    ? new TypeList.ForLoadedType(Cloneable.class, Serializable.class)
                    : new TypeList.ForLoadedType(type.getInterfaces());
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
        public TypeDescription getEnclosingType() {
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
        public PackageDescription getPackage() {
            Package aPackage = type.getPackage();
            return aPackage == null
                    ? null
                    : new PackageDescription.ForLoadedPackage(aPackage);
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
    }

    /**
     * A projection for an array type based on an existing {@link net.bytebuddy.instrumentation.type.TypeDescription}.
     */
    class ArrayProjection extends AbstractTypeDescription {

        /**
         * The modifiers of any array type.
         */
        private static final int ARRAY_MODIFIERS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_ABSTRACT;

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
            return arity == 0 ? componentType : new ArrayProjection(componentType, arity);
        }

        /**
         * Checks if two types are assignable to another by first checking the array dimensions and by later
         * checking the component types if the arity matches.
         *
         * @param sourceType The source type to which another type is to be assigned to.
         * @param targetType The target type that is to be assigned to the source type.
         * @return {@code true} if both types are assignable to one another.
         */
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
            return new TypeList.ForLoadedType(Cloneable.class, Serializable.class);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingType() {
            return null;
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
            StringBuilder stringBuilder = new StringBuilder(componentType.getCanonicalName());
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
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public AnnotationList getInheritedAnnotations() {
            return new AnnotationList.Empty();
        }

        @Override
        public PackageDescription getPackage() {
            return null;
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
            return null;
        }

        @Override
        public int getModifiers() {
            return ARRAY_MODIFIERS;
        }
    }
}
